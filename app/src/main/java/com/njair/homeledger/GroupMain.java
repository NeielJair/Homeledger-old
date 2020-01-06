package com.njair.homeledger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.njair.homeledger.Service.Group;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;
import com.njair.homeledger.ui.members.MembersFragment;
import com.njair.homeledger.ui.summary.SummaryFragment;
import com.njair.homeledger.ui.transactions.TransactionsFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

public class GroupMain extends AppCompatActivity {

    private Activity activity;

    public String roomName = "";
    public String roomKey = "";

    public Group group;

    final TransactionsFragment transactionsFragment = new TransactionsFragment();
    final SummaryFragment summaryFragment = new SummaryFragment();
    final MembersFragment membersFragment = new MembersFragment();
    final FragmentManager fM = getSupportFragmentManager();
    Fragment active = transactionsFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_transactions:
                    fM.beginTransaction().hide(active).show(transactionsFragment).commit();
                    active = transactionsFragment;
                    return true;

                case R.id.navigation_summary:
                    fM.beginTransaction().hide(active).show(summaryFragment).commit();
                    active = summaryFragment;
                    return true;

                case R.id.navigation_members:
                    fM.beginTransaction().hide(active).show(membersFragment).commit();
                    active = membersFragment;
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /*AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_transactions, R.id.navigation_summary, R.id.navigation_members)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);*/

        activity = this;

        final ActionBar actionBar = getSupportActionBar();

        actionBar.setDefaultDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        roomName = getIntent().getStringExtra("RoomName");
        roomKey = getIntent().getStringExtra("RoomKey");

        fM.beginTransaction().add(R.id.nav_host_fragment, membersFragment, "Members").hide(membersFragment).commit();
        fM.beginTransaction().add(R.id.nav_host_fragment, summaryFragment, "Summary").hide(summaryFragment).commit();
        fM.beginTransaction().add(R.id.nav_host_fragment, transactionsFragment, "Transactions").commit();

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        group = new Group(this, roomKey) {
            @Override
            public void onNameChange(String name) {
                actionBar.setTitle(name);
            }

            @Override
            public void onMemberAdded(int position, Member member) {
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();
                transactionsFragment.aASender.add(member.getName());
                transactionsFragment.aASender.notifyDataSetChanged();
                transactionsFragment.aARecipient.add(member.getName());
                transactionsFragment.aARecipient.notifyDataSetChanged();

                //membersFragment.membersAdapter.notifyItemInserted(position);
                membersFragment.membersAdapter.notifyDataSetChanged();
                summaryFragment.summaryAdapter.update();
            }

            @Override
            public void onMemberChanged(int position, Member member) {
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();

                //membersFragment.membersAdapter.notifyItemChanged(position);
                membersFragment.membersAdapter.notifyDataSetChanged();
                summaryFragment.summaryAdapter.update();
            }

            @Override
            public void onMemberRemoved(int position, Member member) {
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();
                transactionsFragment.aASender.remove(member.getName());
                transactionsFragment.aARecipient.remove(member.getName());

                membersFragment.membersAdapter.notifyItemRemoved(position);
                //membersFragment.membersAdapter.notifyDataSetChanged();
                summaryFragment.summaryAdapter.update();
            }

            @Override
            public void onTransactionAdded(String key, int position) {
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();
                //transactionsFragment.transactionsAdapter.notifyItemInserted(positionList);
                summaryFragment.summaryAdapter.update();

                Log.wtf("Transaction added from group", Integer.toString(position));
            }

            @Override
            public void onTransactionChanged(String key, int position) {
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();
                //transactionsFragment.transactionsAdapter.notifyItemChanged(positionList);
                summaryFragment.summaryAdapter.update();

                Log.wtf("Transaction changed from group", Integer.toString(position));
            }

            @Override
            public void onTransactionRemoved(String key, int position) {
                //transactionsFragment.transactionsAdapter.notifyItemRemoved(positionList);
                transactionsFragment.transactionsAdapter.notifyDataSetChanged();
                summaryFragment.summaryAdapter.update();

                Log.wtf("Transaction removed from group", Integer.toString(position));
            }

            @Override
            public void onDeletion() {
                super.onDeletion();

                Toast.makeText(activity, "The group was deleted - Quitting", Toast.LENGTH_LONG).show();
                finish();
            }
        };

        transactionsFragment.setGroup(group);
        summaryFragment.setGroup(group);
        membersFragment.setGroup(group);

        ((SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout)).setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                finish();
                startActivity(getIntent());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.finish();
    }

    public String getRoomKey(){
        return roomKey;
    }

    @Override
    protected void onDestroy() {
        group.detach();

        super.onDestroy();
    }
}
