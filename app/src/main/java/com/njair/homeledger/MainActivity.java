package com.njair.homeledger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njair.homeledger.Service.Group;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.MyApp;
import com.njair.homeledger.Service.Transaction;
import com.njair.homeledger.Service.TransactionList;
import com.njair.homeledger.ui.UserMemberView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int _VERSION = 2;
    public static final int RC_SIGN_IN = 123;

    public DatabaseReference databaseRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private TextView textViewDisplay;
    private TextView textViewDisplay2;
    private TextView textViewDisplay3;
    private TextView textViewDisplay4;

    private boolean hasDownloaded = false;

    private GroupsAdapter groupsAdapter;

    private List<Group> groupList;

    private Resources r;

    private String group_id;

    @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        databaseRef = FirebaseDatabase.getInstance().getReference();
        r = getResources();
        group_id = r.getString(R.string.group_id).toLowerCase().replace(" ", "_");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        createSignInIntent();

        updateSharedPreferences(this);

        textViewDisplay = findViewById(R.id.textViewDisplay);
        textViewDisplay2 = findViewById(R.id.textViewDisplay2);
        textViewDisplay3 = findViewById(R.id.textViewDisplay3);
        textViewDisplay4 = findViewById(R.id.textViewDisplay4);

        textViewDisplay.setVisibility(View.GONE);
        textViewDisplay2.setVisibility(View.GONE);
        textViewDisplay3.setVisibility(View.GONE);
        textViewDisplay4.setVisibility(View.GONE);

        RecyclerView groupsRecyclerView = findViewById(R.id.recyclerview_groups);
        groupsAdapter = new GroupsAdapter(this);
        groupsRecyclerView.setAdapter(groupsAdapter);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        groupsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        final Context context = this;

        //region [Manage views]
        findViewById(R.id.fabCreateGroup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateGroupDialog(view.getContext());
            }
        });

        findViewById(R.id.fabJoinGroup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showJoinGroupDialog(view.getContext());
            }
        });

        final ImageView imageViewLogOut = findViewById(R.id.imageViewLogOut);
        final int imageViewColor = imageViewLogOut.getImageTintList().getDefaultColor();
        imageViewLogOut.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        imageViewLogOut.setColorFilter(imageViewColor/3);
                        imageViewLogOut.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(getString(R.string.log_out_or_delete_prompt))
                                .setPositiveButton(r.getString(R.string.log_out), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAuth.signOut();
                                recreate();
                            }
                        })
                                .setNegativeButton(r.getString(R.string.delete_account), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                mAuth.signOut();
                                                recreate();
                                            }
                                        });
                                    }
                                })
                                .setNeutralButton(R.string.cancel, null);

                        builder.create().show();

                    case MotionEvent.ACTION_CANCEL: {
                        imageViewLogOut.setColorFilter(imageViewColor);
                        imageViewLogOut.invalidate();
                        break;
                    }
                }

                return false;
            }
        });
        //endregion

        //region [Links]
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        //endregion
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                updateUI();

            } else if (response != null) {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private void createSignInIntent(){
        if(currentUser == null){
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.AnonymousBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setLogo(R.drawable.ic_logo)
                            .setAvailableProviders(providers)
                            .setIsSmartLockEnabled(false)
                            .build(), RC_SIGN_IN);
        } else{
            currentUser = mAuth.getCurrentUser();

            DatabaseReference uidDataRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid()).child("data");
            uidDataRef.child("username").setValue((currentUser.getDisplayName() == null || currentUser.getDisplayName().equals("")) ?
                    r.getString(R.string.guest) : currentUser.getDisplayName());
            uidDataRef.child("email").setValue(currentUser.getEmail());

            updateUI();
        }
    }

    private void updateUI(){
        Toast.makeText(this, "Logged in: " + currentUser.getDisplayName(), Toast.LENGTH_LONG).show();

        UserMemberView userMemberView = findViewById(R.id.viewUserMember);
        TextView textViewMemberName = userMemberView.findViewById(R.id.textViewMemberName);
        TextView textViewMemberEmail = userMemberView.findViewById(R.id.textViewMemberEmail);

        if(currentUser.getDisplayName() == null || currentUser.getDisplayName().equals(""))
            textViewMemberName.setText(r.getString(R.string.guest));
        else
            textViewMemberName.setText(currentUser.getDisplayName());

        if(currentUser.getEmail() == null || currentUser.getEmail().equals(""))
            textViewMemberEmail.setVisibility(View.GONE);
        else
            textViewMemberEmail.setText(currentUser.getEmail());
    }

    private void updateSharedPreferences(final Context context) {
        SharedPreferences s_settings = context.getSharedPreferences("Settings", 0);
        SharedPreferences t_settings = context.getSharedPreferences("Transactions", 0);
        final List<Transaction> transactions = new ArrayList<>();

        int version = s_settings.getInt("v", _VERSION);
        if((version == _VERSION) || (getSharedPreferences("Settings", 0).getString("RoomKey", "").equals(""))) return;

        s_settings.edit().putInt("v", _VERSION).apply();
    }

    private void showCreateGroupDialog(final Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        builder.setTitle(r.getString(R.string.create_group));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(12, 0, 12, 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        final EditText inputName = new EditText(context);
        inputName.setLayoutParams(lp);
        inputName.setHint(r.getString(R.string.group_name));
        inputName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        TextView tVAt = new TextView(context);
        tVAt.setText("@");

        final EditText inputKey = new EditText(context);
        inputKey.setLayoutParams(lp);
        inputKey.setHint(group_id);
        inputKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        inputKey.setFilters(getInputKeyFilter());

        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString();

                if(s.length() == 0)
                    inputKey.setHint(group_id);
                else {
                    StringBuilder sb = new StringBuilder();
                    String hint = "";

                    for(int i = 0; i < s.length(); i++){
                        Character curChar = name.charAt(i);

                        if(curChar.equals(' '))
                            hint = sb.append("_").toString();
                        else if(Character.isLetterOrDigit(curChar) || curChar.equals('_') || curChar.equals('-'))
                            hint = sb.append(Character.toString(curChar).toLowerCase()).toString();
                    }

                    inputKey.setHint(hint.equals("") ? group_id : hint);
                }
            }
        });

        inputKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String key = s.toString().replace("_", " ");

                if(s.length() == 0)
                    inputName.setHint(r.getString(R.string.group_name));
                else
                    inputName.setHint(key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase());
            }
        });

        LinearLayout lL = new LinearLayout(context);
        lL.setOrientation(LinearLayout.HORIZONTAL);
        lL.setLayoutParams(lp);
        lL.addView(tVAt);
        lL.addView(inputKey);

        linearLayout.addView(inputName);
        linearLayout.addView(lL);

        builder.setView(linearLayout);

        builder.setPositiveButton(r.getString(R.string.create), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(inputName.getText().toString().equals("") && inputKey.getText().toString().equals("")){
                    Toast.makeText(context, r.getString(R.string.group_name_id_can_not_be_empty), Toast.LENGTH_SHORT).show();
                } else if(!inputName.getText().toString().equals("") && inputKey.getHint().toString().equals(group_id)) {
                    Toast.makeText(context, String.format(r.getString(R.string.invalid_), r.getString(R.string.group_id).toLowerCase()), Toast.LENGTH_SHORT).show();
                } else {
                    final String name = (inputName.getText().toString().equals("")) ? (inputName.getHint().toString()) : (inputName.getText().toString());
                    final String key = (inputKey.getText().toString().equals("")) ? (inputKey.getHint().toString()) : (inputKey.getText().toString());

                    databaseRef.child("groups").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue() == null){
                                databaseRef.child("groups").child(key).child("name").setValue(name);

                                groupsAdapter.checkItem(setUpGroup(key));
                            } else {
                                Toast.makeText(context, String.format(r.getString(R.string._already_exists), r.getString(R.string.group)), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(context, r.getString(R.string.an_error_has_occurred) + databaseError.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        builder.setNegativeButton(r.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.create().show();
    }

    private void showJoinGroupDialog(final Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        builder.setTitle(r.getString(R.string.join) + " " + r.getString(R.string.group).toLowerCase());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView tVAt = new TextView(context);
        tVAt.setText("@");

        final EditText inputKey = new EditText(context);
        inputKey.setLayoutParams(lp);
        inputKey.setHint(r.getString(R.string.group_id).toLowerCase().replace(" ", "_"));
        inputKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        inputKey.setFilters(getInputKeyFilter());

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(lp);
        linearLayout.addView(tVAt);
        linearLayout.addView(inputKey);

        builder.setView(linearLayout);

        builder.setPositiveButton(r.getString(R.string.join), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(inputKey.getText().toString().equals("")){
                    Toast.makeText(context, String.format(getString(R.string._can_not_be_empty), r.getString(R.string.group_id)), Toast.LENGTH_SHORT).show();
                } else {
                    databaseRef.child("groups").child(inputKey.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue() != null){
                                String groupKey = inputKey.getText().toString();

                                groupsAdapter.checkItem(setUpGroup(groupKey));
                            } else {
                                Toast.makeText(context, String.format(r.getString(R.string._does_not_exist), r.getString(R.string.group) + " '" + inputKey.getText().toString() + "'"), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(context, r.getString(R.string.an_error_has_occurred) + databaseError.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        builder.setNegativeButton(r.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.create().show();
    }

    private InputFilter[] getInputKeyFilter(){
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder sb = new StringBuilder(end - start);

                for(int i = start; i < end; i++){
                    String character = Character.toString(source.charAt(i));

                    if (Character.isLetterOrDigit(source.charAt(i)) || character.equals("_") || character.equals("-") || character.equals(" "))
                        if(character.equals(" "))
                            sb.append("_");
                        else
                            sb.append(character.toLowerCase());
                }

                return sb.toString();
            }
        };

        return new InputFilter[] { filter };
    }

    private Group setUpGroup(String key){
        return new Group(this, key) {
            @Override
            public void onNameChange(String name) {
                groupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onMemberAdded(int position, Member member) {
                groupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onMemberChanged(int position, Member member) {
                groupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onMemberRemoved(int position, Member member) {
                groupsAdapter.notifyDataSetChanged();
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

            @Override
            public void onDeletion() {
                super.onDeletion();

                groupsAdapter.removeItem(this);
            }
        };
    }

    class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ViewHolder> {
        private Activity activity;
        private List<Group> groupList = new ArrayList<>();

        public GroupsAdapter(Activity activity) {
            this.activity = activity;

            SharedPreferences settings = activity.getSharedPreferences("Groups", 0);

            int size = settings.getInt("g_size", 0);

            for(int i = 0; i < size; i++)
                setUpGroup(settings.getString("g_"+i, ""));
        }

        @NonNull
        @Override
        public GroupsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(activity).inflate(R.layout.layout_group_preview, parent, false);
            GroupsAdapter.ViewHolder viewHolder = new GroupsAdapter.ViewHolder(view);
            return viewHolder;
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        @Override
        public void onBindViewHolder(@NonNull final GroupsAdapter.ViewHolder holder, final int position) {
            final Group group = groupList.get(position);
            String keyString = "@" + group.key;

            holder.textViewGroupName.setText(group.name);
            holder.textViewGroupKey.setText(keyString);
            holder.textViewMembers.setText(Member.getNames(group.memberList));

            holder.constraintLayout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP && !holder.isHeld){
                        Intent intent = new Intent(MainActivity.this, GroupMain.class);
                        intent.putExtra("RoomName", group.name);
                        intent.putExtra("RoomKey", group.key);
                        startActivity(intent);
                    }

                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            int size = groupList.size();

            if(size == 0){
                textViewDisplay.setVisibility(View.VISIBLE);
                textViewDisplay2.setVisibility(View.VISIBLE);
                textViewDisplay3.setVisibility(View.VISIBLE);
                textViewDisplay4.setVisibility(View.VISIBLE);
            } else {
                textViewDisplay.setVisibility(View.GONE);
                textViewDisplay2.setVisibility(View.GONE);
                textViewDisplay3.setVisibility(View.GONE);
                textViewDisplay4.setVisibility(View.GONE);
            }

            return size;
        }

        public void addItem(Group group) {
            groupList.add(group);
            Group.writeToSharedPreferences(activity, groupList);
            notifyDataSetChanged();
        }

        public void modifyItem(Group group, int position){
            groupList.set(position, group);
            Group.writeToSharedPreferences(activity, groupList);
            notifyDataSetChanged();
        }

        public void removeItem(int position){
            groupList.get(position).detach();
            groupList.remove(position);
            Group.writeToSharedPreferences(activity, groupList);
            notifyDataSetChanged();
        }

        public void removeItem(Group group){
            int size = groupList.size();

            for(int i = 0; i < size; i++){
                if(groupList.get(i).key.equals(group.key)) {
                    removeItem(i);
                    return;
                }
            }
        }

        public void checkItem(Group group){
            for(int i = 0; i < groupList.size(); i++){
                Group g = groupList.get(i);

                if(g.key.equals(group.key)){
                    modifyItem(g, i);
                    return;
                }
            }

            addItem(group);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
            ConstraintLayout constraintLayout;
            TextView textViewGroupName;
            TextView textViewGroupKey;
            TextView textViewMembers;

            private boolean isHeld = false;

            public ViewHolder(View itemView) {
                super(itemView);
                constraintLayout = itemView.findViewById(R.id.constraintlayout_group);
                constraintLayout.setOnCreateContextMenuListener(this);

                textViewGroupName = itemView.findViewById(R.id.textViewGroupName);
                textViewGroupKey = itemView.findViewById(R.id.textViewGroupKey);
                textViewMembers = itemView.findViewById(R.id.textViewMembers);
            }

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                isHeld = true;
                menu.add(Menu.NONE, 0, 0, "Share").setOnMenuItemClickListener(onChange);
                menu.add(Menu.NONE, 1, 1, "Leave group").setOnMenuItemClickListener(onChange);
                menu.add(Menu.NONE, 2, 2, "Delete group").setOnMenuItemClickListener(onChange);
            }

            private final MenuItem.OnMenuItemClickListener onChange = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    isHeld = false;

                    final int position = getAdapterPosition();
                    final Group g = groupsAdapter.groupList.get(position);

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                    switch (item.getItemId()){
                        case 0: //Share group
                            MyApp.share(activity, "Group id", "Hey, join my group in Homeledger. Here's its id: " + g.key);
                            break;

                        case 1: //Leave group
                            builder.setMessage(String.format(r.getString(R.string.leave_group_prompt), g.name))
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            groupsAdapter.removeItem(position);
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {}
                                    });

                            builder.create().show();
                            return true;

                        case 2: //Delete group
                            builder.setMessage(String.format(r.getString(R.string.delete_group_prompt), g.name))
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            groupsAdapter.removeItem(position);

                                            databaseRef.child("groups").child(g.key).setValue(null);
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {}
                        });

                            builder.create().show();
                            return true;
                    }
                    return false;
                }
            };
        }
    }
}
