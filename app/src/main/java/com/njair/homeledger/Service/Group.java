package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.TransactionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Group {
    public String name;

    public String key;
    //public String password;
    public TransactionList transactionList;
    public List<Member> memberList = new ArrayList<>();
    public List<Member> adminList = new ArrayList<>();

    private DatabaseReference gRef;
    private DatabaseReference nRef;
    private DatabaseReference aRef;
    private Query mRef;
    private Query tRef;
    private ValueEventListener nameEventListener;
    private ValueEventListener adminsEventListener;
    private ChildEventListener membersEventListener;
    private ChildEventListener transactionsEventListener;
    private HashMap<DatabaseReference, ValueEventListener> usersHandlers = new HashMap<>();

    private boolean isDummy = false;

    public Group(){
        name = "";
        this.key = "";
        isDummy = true;
    }

    public Group(final Context context, String _key){
        this.key = _key;

        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        gRef = rootRef.child("groups").child(key);
        nRef = gRef.child("name");
        aRef = gRef.child("admins");
        mRef = gRef.child("members").orderByChild("name");
        tRef = gRef.child("transactions").orderByChild("timestamp");

        transactionList = new TransactionList(key);
        memberList = new ArrayList<>();
        adminList = new ArrayList<>();

        final List<String> adminIdList = new ArrayList<>();

        nameEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Get name and check if group exists (name node must always be present)
                name = dataSnapshot.getValue(String.class);

                if(name == null){
                    onDeletion();
                    return;
                }

                if(name.equals(""))
                    name = MyApp.capitalize(key.replace("_", " "));

                onNameChange(name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                onError(context, databaseError);
            }
        };

        adminsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Get admins
                for(DataSnapshot aS : dataSnapshot.child("admins").getChildren()){
                    if(aS.exists())
                        adminIdList.add(aS.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                onError(context, databaseError);
            }
        };

        membersEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                final Member member = dataSnapshot.getValue(Member.class);

                if(member == null)
                    return;

                member.setNodeId(dataSnapshot.getKey());
                member.setUid(dataSnapshot.child("uid").getValue(String.class));

                if(adminIdList.contains(member.getNodeId())){
                    member.setAdmin(true);
                    adminList.add(member);
                }

                if(member.getUid() != null){
                    DatabaseReference memberRef = rootRef.child("users").child(member.getUid()).child("data");
                    ValueEventListener memberEventListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot uS) {
                            member.setUser(uS.child("username").getValue(String.class),
                                    uS.child("email").getValue(String.class),
                                    uS.child("nickname").getValue(String.class));

                            memberList.add(member);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.wtf("Failed to get user", member.getUid());
                        }
                    };

                    memberRef.addValueEventListener(memberEventListener);
                    usersHandlers.put(memberRef, memberEventListener);
                } else
                    memberList.add(member);

                transactionList.replaceMember(member.getName(), member);

                onMemberAdded(memberList.size() - 1, member);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Member member = dataSnapshot.getValue(Member.class);

                if(member == null)
                    return;

                member.setNodeId(dataSnapshot.getKey());

                memberList.set(Member.indexOf(memberList, member.getName()), member);

                transactionList.replaceMember(member.getName(), member);

                onMemberChanged(memberList.size() - 1, member);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Member member = dataSnapshot.getValue(Member.class);

                if(member == null)
                    return;

                int position = Member.indexOf(memberList, member.getName());

                if(position != -1)
                    memberList.remove(position);

                transactionList.replaceMember(member.getName(), new Member(member.getName()));

                onMemberRemoved(position, member);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.wtf("Member moved", dataSnapshot.child("name").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                onError(context, databaseError);
            }
        };

        transactionsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //Get transactions
                if (!dataSnapshot.child("amount").exists() || !dataSnapshot.child("movement").exists())
                    throw new AssertionError("Invalid transaction");

                Transaction transaction = new Transaction(
                        dataSnapshot.child("description").getValue(String.class),
                        Member.findMember(memberList, dataSnapshot.child("debtor").getValue(String.class)),
                        Member.findMember(memberList, dataSnapshot.child("lender").getValue(String.class)),
                        dataSnapshot.child("amount").getValue(Float.class),
                        dataSnapshot.child("timestamp").getValue(String.class),
                        dataSnapshot.child("movement").getValue(Boolean.class));

                transactionList.put(dataSnapshot.getKey(), transaction);

                onTransactionAdded(dataSnapshot.getKey(), transactionList.getPosition(transaction));
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (!dataSnapshot.exists() || !dataSnapshot.child("amount").exists() || !dataSnapshot.child("movement").exists())
                    throw new AssertionError("Invalid transaction");

                Transaction transaction = new Transaction(
                        dataSnapshot.child("description").getValue(String.class),
                        Member.findMember(memberList, dataSnapshot.child("debtor").getValue(String.class)),
                        Member.findMember(memberList, dataSnapshot.child("lender").getValue(String.class)),
                        dataSnapshot.child("amount").getValue(Float.class),
                        dataSnapshot.child("timestamp").getValue(String.class),
                        dataSnapshot.child("movement").getValue(Boolean.class));

                transactionList.put(dataSnapshot.getKey(), transaction);

                onTransactionChanged(dataSnapshot.getKey(), transactionList.getPosition(transaction));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.child("amount").exists() || !dataSnapshot.child("movement").exists())
                    throw new AssertionError("Invalid transaction");

                int posList = transactionList.getPosition(new Transaction(
                        dataSnapshot.child("description").getValue(String.class),
                        Member.findMember(memberList, dataSnapshot.child("debtor").getValue(String.class)),
                        Member.findMember(memberList, dataSnapshot.child("lender").getValue(String.class)),
                        dataSnapshot.child("amount").getValue(Float.class),
                        dataSnapshot.child("timestamp").getValue(String.class),
                        dataSnapshot.child("movement").getValue(Boolean.class)));

                transactionList.remove(dataSnapshot.getKey());

                onTransactionRemoved(dataSnapshot.getKey(), posList);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                onError(context, databaseError);
            }
        };

        aRef.addValueEventListener(adminsEventListener);
        nRef.addValueEventListener(nameEventListener);
        mRef.addChildEventListener(membersEventListener);
        tRef.addChildEventListener(transactionsEventListener);
    }

    public void detach(){
        if(!isDummy){
            aRef.removeEventListener(adminsEventListener);
            nRef.removeEventListener(nameEventListener);
            mRef.removeEventListener(membersEventListener);
            tRef.removeEventListener(transactionsEventListener);

            for(Map.Entry<DatabaseReference, ValueEventListener> entry : usersHandlers.entrySet())
                entry.getKey().removeEventListener(entry.getValue());
        }
    }

    public abstract void onNameChange(String name);

    public abstract void onMemberAdded(int position, Member member);

    public abstract void onMemberChanged(int position, Member member);

    public abstract void onMemberRemoved(int position, Member member);

    public abstract void onTransactionAdded(String key, int position);

    public abstract void onTransactionChanged(String key, int position);

    public abstract void onTransactionRemoved(String key, int position);

    public void onDeletion(){
        detach();
    }

    public void onError(Context context, @NonNull DatabaseError databaseError){
        Toast.makeText(context, context.getResources().getString(R.string.an_error_has_occurred) + databaseError.toString(),
                Toast.LENGTH_LONG).show();;
    }

    public static Group dummy(){
        return new Group() {
            @Override
            public void onNameChange(String name) {

            }

            @Override
            public void onMemberAdded(int position, Member member) {

            }

            @Override
            public void onMemberChanged(int position, Member member) {

            }

            @Override
            public void onMemberRemoved(int position, Member member) {

            }

            @Override
            public void onTransactionAdded(String key, int position) {

            }

            @Override
            public void onTransactionChanged(String key, int position) {

            }

            @Override
            public void onTransactionRemoved(String key, int position) {

            }
        };
    }

    public void add(Member member, OnCompleteListener<Void> onCompleteListener){
        member.upload(key, onCompleteListener);
    }

    public void add(Transaction transaction, OnCompleteListener<Void> onCompleteListener){
        transaction.upload(key, onCompleteListener);
    }

    public static void writeToSharedPreferences(Context context, List<Group> groupList){
        SharedPreferences settings = context.getSharedPreferences("Groups", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("g_size", groupList.size());

        for(int i = 0; i < groupList.size(); i++){
            Group g = groupList.get(i);

            editor.putString("g_"+i, g.key);
        }

        editor.apply();
    }
}