package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Member {
    @Exclude
    public String nodeId;
    @Exclude
    public Boolean isAdmin;

    private String uid = null;
    @Exclude
    private String username = null;
    @Exclude
    private String email = null;
    @Exclude
    private String nickname = null;

    public String name;
    public int color;

    public static Member dummy = new Member();

    public Member(){
        nodeId = "";
        isAdmin = false;
        name = "";
        color = Color.BLACK;
    }

    public Member(String name){
        nodeId = "";
        isAdmin = false;
        this.name = name;
        this.color = Color.BLACK;
    }

    public Member(String name, int color){
        nodeId = "";
        isAdmin = false;
        this.name = name;
        this.color = color;
    }

    public String getName(){
        return name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color){
        this.color = color;
    }

    public String getUid(){
        return uid;
    }

    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getNickname(){
        return nickname;
    }

    public void setNodeId(String nodeId){
        this.nodeId = nodeId;
    }

    public String getNodeId(){
        return nodeId;
    }

    public void setUid(String uid){
        this.uid = uid;
    }

    public void setUser(String username, String email, String nickname){
        this.username = username;
        this.email = email;
        this.nickname = nickname;
    }

    public void setAdmin(Boolean isAdmin){
        this.isAdmin = isAdmin;
    }

    public boolean equals(Member member){
        return getName().equals(member.getName());
    }

    public static Member findMember(List<Member> members, String name){
        for(Member member : members){
            if(member.getName().equals(name))
                return member;
        }

        return new Member(name);
    }

    public static int indexOf(List<Member> members, String name){
        for(int i = 0; i < members.size(); i++){
            if(members.get(i).getName().equals(name))
                return i;
        }

        return -1;
    }

    public static void check(List<Member> memberList, Member member){
        int size = memberList.size();

        for(int i = 0; i < size; i++){
            if(memberList.get(i).getName().equals(member.getName())){
                memberList.set(i, member);
                return;
            }
        }

        memberList.add(member);
    }

    public static Member findMemberById(List<Member> members, String id){
        for(Member member : members){
            if(member.nodeId.equals(id))
                return member;
        }

        return new Member();
    }

    public static List<String> getNameList(List<Member> memberList){
        List<String> newList = new ArrayList<>(memberList.size());

        for(Member member : memberList){
            newList.add(member.getName());
        }

        return newList;
    }

    public static String getNames(List<Member> memberList){
        StringBuilder sb = new StringBuilder();

        for(Member member : memberList){
            if(sb.length() == 0)
                sb.insert(0, member.getName());
            else
                sb.append(", ").append(member.getName());
        }

        return sb.toString();
    }

    public static void writeToSharedPreferences(Context appContext, List<Member> members){
        SharedPreferences settings = appContext.getSharedPreferences("Members", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("m_size", members.size());

        for(int i = 0; i < members.size(); i++){
            Member member = members.get(i);

            editor.putString("m"+i + "_name", member.name);
            editor.putInt("m"+i + "_color", member.color);
        }

        editor.apply();
    }

    public static List<Member> readFromSharedPreferences(Context appContext){
        SharedPreferences settings = appContext.getSharedPreferences("Members", 0);
        List<Member> members = new ArrayList<>();

        int size = settings.getInt("m_size", 0);

        for(int i = 0; i < size; i++){
            members.add(new Member(settings.getString("m"+i + "_name", ""), settings.getInt("m"+i + "_color", Color.BLACK)));
        }

        return members;
    }

    public static void download(final Context context, String roomKey, final List<Member> memberList, final Runnable onChange, final Runnable onCancelled){
        memberList.clear();
        Log.wtf("Update members", "Init");

        DatabaseReference root = FirebaseDatabase.getInstance().getReference().child("groups").child(roomKey).child("members");
        root.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot memberSnapshot : dataSnapshot.getChildren()){
                    Member m = memberSnapshot.getValue(Member.class);
                    m.setNodeId(memberSnapshot.getKey());

                    Log.wtf("Update", m.getName());
                    memberList.add(m);
                }

                onChange.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "An error has occurred: " + databaseError.toString(), Toast.LENGTH_SHORT).show();

                onCancelled.run();
            }
        });
    }

    public void upload(String roomKey, @Nullable OnCompleteListener<Void> onCompleteListener){
        DatabaseReference mRef;

        if(nodeId == null || nodeId.equals("")){
            mRef = FirebaseDatabase.getInstance().getReference().child("groups").child(roomKey).child("members").push();
            setNodeId(mRef.getKey());
        } else
            mRef = FirebaseDatabase.getInstance().getReference().child("groups").child(roomKey).child("members").child(nodeId);

        //Turn into HashMap
        HashMap<String, Object> map = new HashMap<>();

        map.put("name", getName());
        map.put("color", getColor());
        map.put("uid", getUid());

        mRef.updateChildren(map).addOnCompleteListener(onCompleteListener);
    }

    public static Member getFromSharedPreferences(Context appContext, String name){
        SharedPreferences settings = appContext.getSharedPreferences("Members", 0);

        String memberName;
        for(int i = 0; i < settings.getInt("m_size", 0); i++){
            memberName = settings.getString("m"+i + "_name", "");

            if(memberName.equals(name)){
                return new Member(memberName, settings.getInt("m"+i + "_color", Color.BLACK));
            }
        }

        return new Member(name, Color.BLACK);
    }
}
