package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import com.njair.homeledger.R;
import com.njair.homeledger.ui.TransactionView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Transaction {
    public String description;
    public Member debtor;
    public float amount;
    public Member lender;
    public String timestamp;

    public static final String timeStampFormat = "dd/MM/yyyy, HH:mm";

    public Transaction(Member debtor, Member lender, float amount){
        this.description = "";
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date());

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public Transaction(String description, Member debtor, Member lender, float amount){
        this.description = description;
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date());

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public Transaction(String description, Member debtor, Member lender, float amount, String timestamp){
        this.description = description;
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = timestamp;

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public String toString(){
        return debtor.getName() + " pays " + lender.getName() + " "
                + Currency.getInstance(Locale.getDefault()).getSymbol() + amount;
    }

    public String getAmount(){
        return Currency.getInstance(Locale.getDefault()).getSymbol() + amount;
    }

    public void addAmount(float cash){
        debtor.addFunds(-cash);
        lender.addFunds(cash);
        amount += cash;
        order();
    }

    public void flip(){
        Transaction transaction = this;
        debtor = transaction.lender;
        lender = transaction.debtor;
        amount = -transaction.amount;
    }

    public void order(){
        if(amount < 0) flip();
    }

    public boolean sharesParties(Transaction t){
        return (((debtor.equals(t.debtor)) && (lender.equals(t.lender))) || ((lender.equals(t.debtor)) && (debtor.equals(t.lender))));
    }

    public static Transaction combine(Transaction t, Transaction _t){
        Transaction res = t;

        if(t.debtor.equals(_t.debtor))
            res.addAmount(_t.amount);
        else if(t.debtor.equals(_t.lender))
            res.addAmount(-_t.amount);

        return res;
    }

    public void adaptView(TransactionView view){
        TextView textViewSender;
        TextView textViewAmount;
        TextView textViewRecipient;

        textViewSender = view.findViewById(R.id.textViewSender);
        textViewAmount = view.findViewById(R.id.textViewAmount);
        textViewRecipient = view.findViewById(R.id.textViewRecipient);

        textViewSender.setText(debtor.getName());
        drawableChangeTint(textViewSender.getCompoundDrawables(), debtor.color);

        textViewAmount.setText(getAmount());

        textViewRecipient.setText(lender.getName());
        drawableChangeTint(textViewRecipient.getCompoundDrawables(), lender.color);
    }

    private void drawableChangeTint(Drawable[] drawables, int color){
        for(Drawable drawable : drawables){
            if(drawable != null)
                drawable.setTint(color);
        }
    }

    public static List<Transaction> sumTransactions(List<Transaction> transactions){
        List<Transaction> summary = new ArrayList<>();

        for(Transaction transaction : transactions){
            boolean shared = false;

            for(int i = 0; i < summary.size(); i++){
                Transaction t = summary.get(i);

                if(transaction.sharesParties(t)){
                    summary.set(i, Transaction.combine(transaction, t));


                    Log.d("sumTransaction "+i, "Summed " + summary.get(i).toString());

                    shared = true;
                    break;
                }
            }

            if(!shared){
                Log.d("addedTransaction ", "added " + transaction.toString());
                summary.add(transaction);
            }
        }

        return summary;
    }

    public static void writeToSharedPreferences(Context appContext, List<Transaction> transactions){
        SharedPreferences settings = appContext.getSharedPreferences("Transactions", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("t_size", transactions.size());

        for(int i = 0; i < transactions.size(); i++){
            Transaction transaction = transactions.get(i);

            editor.putString("t"+i + "_description", transaction.description);

            editor.putString("t"+i + "_debtorName", transaction.debtor.getName());
            //editor.putInt("t"+i + "_senderColor", transaction.debtor.color);

            editor.putString("t"+i + "_lenderName", transaction.lender.getName());
            //editor.putInt("t"+i + "_recipientColor", transaction.lender.color);

            editor.putFloat("t"+i + "_amount", transaction.amount);

            editor.putString("t"+i + "_timestamp", transaction.timestamp);
        }

        editor.apply();
    }

    public static List<Transaction> readFromSharedPreferences(Context appContext){
        SharedPreferences settings = appContext.getSharedPreferences("Transactions", 0);
        List<Transaction> transactions = new ArrayList<>();

        int size = settings.getInt("t_size", 0);

        for(int i = 0; i < size; i++){
            transactions.add(new Transaction(settings.getString("t"+i + "_description", ""),
                    Member.getFromSharedPreferences(appContext, settings.getString("t"+i + "_debtorName", "")),
                    Member.getFromSharedPreferences(appContext, settings.getString("t"+i + "_lenderName", "")),
                    settings.getFloat("t"+i + "_amount", 0f),
                    settings.getString("t"+i + "_timestamp", new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date()))));

            /*transactions.add(new Transaction(settings.getString("t"+i + "_description", ""),
                    new Member(settings.getString("t"+i + "_senderName", ""), settings.getInt("t"+i + "_senderColor", Color.RED)),
                    new Member(settings.getString("t"+i + "_recipientName", ""), settings.getInt("t"+i + "_recipientColor", Color.RED)),
                    settings.getFloat("t"+i + "_amount", 0f),
                    settings.getString("t"+i + "_timestamp", new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date()))));*/
        }

        return transactions;
    }
}
