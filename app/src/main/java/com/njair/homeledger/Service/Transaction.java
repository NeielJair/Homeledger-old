package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.util.Predicate;
import com.njair.homeledger.R;
import com.njair.homeledger.ui.TransactionView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Transaction {
    public static final boolean t_pays = false;
    public static final boolean t_owes = true;

    public String description;
    public Member debtor;
    public float amount;
    public Member lender;
    public String timestamp;
    public boolean movement;

    public static final String timeStampFormat = "dd/MM/yyyy, HH:mm";

    public Transaction(Member debtor, Member lender, float amount){
        this.description = "";
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date());
        this.movement = t_owes;

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
        this.movement = t_owes;

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public Transaction(String description, Member debtor, Member lender, float amount, boolean movement){
        this.description = description;
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date());
        this.movement = movement;

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
        this.movement = t_owes;

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public Transaction(String description, Member debtor, Member lender, float amount, String timestamp, boolean movement){
        this.description = description;
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = timestamp;
        this.movement = movement;

        order();

        debtor.addFunds(-amount);
        lender.addFunds(amount);
    }

    public String toString(){
        return debtor.getName() + ((movement == t_owes) ? " owes " : " pays ") + lender.getName() + " "
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

    public Transaction setMovement(boolean movement){
        if(!(this.movement == movement)){
            Member _debtor = debtor;
            debtor = lender;
            lender = _debtor;
            this.movement = movement;
        }

        return this;
    }

    public void order(){
        if(amount < 0) {
            Member _debtor = debtor;
            debtor = lender;
            lender = _debtor;
            amount *= -1;
        }
    }

    public static List<Transaction> filterByMember(List<Transaction> transactions, Member member, boolean isDebtor){
        List<Transaction> ts = new ArrayList<>();

        for(Transaction t : transactions){
            if(isDebtor){
                if(t.debtor.equals(member))
                    ts.add(t);
            } else {
                if(t.lender.equals(member))
                    ts.add(t);
            }
        }

        return ts;
    }

    public boolean sharesParties(Transaction t){
        return (((debtor.equals(t.debtor)) && (lender.equals(t.lender))) || ((lender.equals(t.debtor)) && (debtor.equals(t.lender))));
    }

    public static Transaction combine(Transaction t, Transaction _t){
        Transaction res = t;

        if(t.debtor.equals(_t.debtor))
            res.addAmount((t.movement == _t.movement) ? _t.amount : -_t.amount);
        else if( t.debtor.equals(_t.lender))
            res.addAmount((t.movement == _t.movement) ? -_t.amount : _t.amount);

        res.order();

        return res;
    }

    public int blendedColors(float ratio){
        final float inverseRatio = 1f - ratio;
        return Color.rgb((int) ((Color.red(debtor.color) * ratio) + (Color.red(lender.color) * inverseRatio)),
                (int) ((Color.green(debtor.color) * ratio) + (Color.green(lender.color) * inverseRatio)),
                (int) ((Color.blue(debtor.color) * ratio) + (Color.blue(lender.color) * inverseRatio)));
    }

    public static List<Member> getDebtors(List<Transaction> transactions, boolean excludeLenders){
        List<Member> members = new ArrayList<>();

        for(Transaction t : transactions){
            boolean repeated = false;

            for(Member member : members){
                if(member.equals(t.debtor) || (excludeLenders && member.equals(t.lender))) {
                    repeated = true;
                    break;
                }
            }

            if(!repeated)
                members.add(t.debtor);
        }

        return members;
    }

    public static List<Transaction> sumTransactions(List<Transaction> transactions){
        List<Transaction> summary = new ArrayList<>();

        for(Transaction transaction : transactions){
            boolean shared = false;

            Transaction t = transaction;
            t.setMovement(t_owes);

            for(int i = 0; i < summary.size(); i++){
                Transaction _t = summary.get(i);

                if(transaction.sharesParties(_t)){
                    summary.set(i, Transaction.combine(t, _t));

                    Log.d("sumTransaction "+i, "Summed " + summary.get(i).toString());

                    shared = true;
                    break;
                }
            }

            if(!shared)
                summary.add(t);
        }

        List<Transaction> newSummary = new ArrayList<>();

        for(int i = 0; i < summary.size(); i++){
            if(summary.get(i).amount != 0f)
                newSummary.add(summary.get(i));
        }

        return newSummary;
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

            editor.putBoolean("t"+i + "_movement", transaction.movement);
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
                    settings.getString("t"+i + "_timestamp", new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date())),
                    settings.getBoolean("t"+i + "_movement", Transaction.t_owes)));
        }

        return transactions;
    }
}
