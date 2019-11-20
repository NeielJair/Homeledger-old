package com.njair.homeledger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;
//import com.google.firebase.messaging.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateSharedPreferences(this, 1);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).slideUp();*/
                startActivity(new Intent(MainActivity.this, GroupMain.class));
            }
        });

        //region [logTokenButton]
        /*Button logTokenButton = findViewById(R.id.logTokenButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get token
                // [START retrieve_current_token]
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "getInstanceId failed", task.getException());
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult().getToken();

                                // Log and toast
                                String msg = getString(R.string.msg_token_fmt, token);
                                Log.d(TAG, msg);
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).slideUp();
                            }
                        });
                // [END retrieve_current_token]
            }
        });*/
        //endregion

        //region [suscribeButton]
        /*Button subscribeButton = findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Subscribing to test topic");
                // [START subscribe_topics]
                FirebaseMessaging.getInstance().subscribeToTopic("test")
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                String msg = getString(R.string.msg_subscribed);
                                if (!task.isSuccessful()) {
                                    msg = getString(R.string.msg_subscribe_failed);
                                }
                                Log.d(TAG, msg);
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).slideUp();
                            }
                        });
                // [END subscribe_topics]
            }
        });*/
        //endregion
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendMessage(){
        // The topic name can be optionally prefixed with "/topics/".
        String topic = "highScores";

        // See documentation on defining a message payload.
                /*Message message = Message.builder()
                        .putData("score", "850")
                        .putData("time", "2:45")
                        .setTopic(topic)
                        .build();*/

        // Send a message to the devices subscribed to the provided topic.
                //String response = FirebaseMessaging.getInstance().send(message);
        // Response is a message ID string.
                //System.out.println("Successfully sent message: " + response);
    }

    private static void updateSharedPreferences(Context appContext, int currentRevision) {
        SharedPreferences s_settings = appContext.getSharedPreferences("Settings", 0);
        SharedPreferences t_settings = appContext.getSharedPreferences("Transactions", 0);
        List<Transaction> transactions = new ArrayList<>();

        int revision = s_settings.getInt("v", 0);
        if(revision == currentRevision) return;

        int size = t_settings.getInt("t_size", 0);

        switch (revision) {
            case 0:
                for (int i = 0; i < size; i++) {
                    transactions.add(new Transaction(t_settings.getString("t" + i + "_description", ""),
                            Member.getFromSharedPreferences(appContext, t_settings.getString("t" + i + "_senderName", "")),
                            Member.getFromSharedPreferences(appContext, t_settings.getString("t" + i + "_recipientName", "")),
                            t_settings.getFloat("t" + i + "_amount", 0f),
                            t_settings.getString("t" + i + "_timestamp", new java.text.SimpleDateFormat(Transaction.timeStampFormat, Locale.getDefault()).format(new Date()))));
                }
                break;
        }

        if(!transactions.isEmpty()){
            t_settings.edit().clear().apply();
            Transaction.writeToSharedPreferences(appContext, transactions);
        }

        s_settings.edit().putInt("v", currentRevision).apply();
    }
}
