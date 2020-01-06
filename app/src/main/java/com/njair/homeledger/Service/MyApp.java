package com.njair.homeledger.Service;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

public class MyApp extends android.app.Application{
    public static Resources r;

    @Override
    public void onCreate() {
        super.onCreate();
        /* Enable disk persistence  */
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        //FirebaseDatabase.getInstance().getReference().keepSynced(true);

        r = getResources();
    }

    public static String getStr(int id){
        return r.getString(id);
    }

    public static String getStr(int id, Object... args){
        Object[] newArgs = new String[args.length];

        for(int i = 0; i < args.length; i++){
            Object arg = args[i];

            if(arg instanceof Integer)
                newArgs[i] = getStr((int) arg);
            else
                newArgs[i] = arg;
        }

        return String.format(getStr(id), newArgs);
    }

    public static String capitalize(String string){
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static void copyToClipboard(Context context, String label, String string){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, string));
    }

    public static void share(Activity activity, String description, String content){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TITLE, description);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.setType("text/plain");
        activity.startActivity(Intent.createChooser(intent, "Share with"));
    }
}