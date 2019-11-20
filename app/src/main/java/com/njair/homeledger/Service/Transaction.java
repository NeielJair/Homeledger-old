package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Transaction {
    public static final boolean t_pays = false;
    public static final boolean t_owes = true;

    public static final int st_timestamp = 0;
    public static final int st_member = 1;
    public static final int st_amount = 2;
    public static final int st_movement = 3;

    protected int Id;
    public String description;
    public Member debtor;
    public float amount;
    public Member lender;
    public String timestamp;
    public boolean movement;

    public static final String timeStampFormat = "dd/MM/yyyy, HH:mm";
    public static final String dateFormat = "dd/MM/yyyy";
    public static final String timeFormat = "HH:mm";

    public Transaction(Member debtor, Member lender, float amount){
        this.description = "";
        this.debtor = debtor;
        this.lender = lender;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat(timeStampFormat, Locale.getDefault()).format(new Date());
        this.movement = t_owes;

        this.Id = -1;

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

        this.Id = -1;

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

        this.Id = -1;

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

        this.Id = -1;

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

        this.Id = -1;

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

    public boolean equals(Transaction t){
        return (((debtor.equals(t.debtor)) && (lender.equals(t.lender))) && (timestamp.equals(t.timestamp)) && (movement == t.movement) && (amount == t.amount));
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

    public static List<Transaction> sortByTransactions(List<Transaction> transactionList, SortByStruct sortByStruct){
        int sortType = sortByStruct.sortType;
        boolean lowestToHighest = sortByStruct.lowestToHighest;
        Member member = sortByStruct.member;

        if(sortType != st_timestamp)
            Transaction.sortByTransactions(transactionList, new SortByStruct(st_timestamp, true));

        int size = transactionList.size();

        Transaction t;

        switch(sortType){
            case st_timestamp:
                SimpleDateFormat formatter = new SimpleDateFormat(timeStampFormat, Locale.getDefault());

                for(int i = 0; i < size; i++){
                    t = transactionList.get(i);
                    Date curTimestamp;
                    Date tempTimestamp;
                    int j = i - 1;

                    try{
                        curTimestamp = formatter.parse(t.timestamp);

                        while(j >= 0){
                            tempTimestamp = formatter.parse(transactionList.get(j).timestamp);

                            if((lowestToHighest) ? (curTimestamp.before(tempTimestamp)) : (curTimestamp.after(tempTimestamp)))
                                break;

                            transactionList.set(j + 1, transactionList.get(j));
                            j--;
                        }

                        transactionList.set(j + 1, t);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                break;

            case st_member:
                Transaction _t;
                Member curMember;
                Member tempMember;
                int slots = 0;

                for(int i = 0; i < size; i++){
                    t = transactionList.get(i);
                    curMember = t.debtor;

                    int j = i - 1;

                    while(j >= 0 && curMember.equals(member)){
                        _t = transactionList.get(j);
                        tempMember = _t.debtor;

                        if(curMember.equals(tempMember))
                            break;

                        transactionList.set(j + 1, _t);
                        j--;
                    }

                    if(curMember.equals(member))
                        slots++;
                    transactionList.set(j + 1, t);
                }

                for(int i = slots; i < size; i++){
                    t = transactionList.get(i);
                    curMember = t.lender;

                    int j = i - 1;

                    while(j >= slots && curMember.equals(member)){
                        _t = transactionList.get(j);
                        tempMember = _t.lender;

                        if(curMember.equals(tempMember))
                            break;

                        transactionList.set(j + 1, _t);
                        j--;
                    }

                    transactionList.set(j + 1, t);
                }

                break;

            case st_amount:
                for(int i = 0; i < size; i++){
                    t = transactionList.get(i);
                    float curAmount= t.amount;
                    float tempAmount;
                    int j = i - 1;

                    while(j >= 0){
                        tempAmount = transactionList.get(j).amount;

                        if(!((lowestToHighest) ? (curAmount <= tempAmount) : (curAmount >= tempAmount)))
                            break;

                        transactionList.set(j + 1, transactionList.get(j));
                        j--;
                    }

                    transactionList.set(j + 1, t);
                }

                break;

            case st_movement:
                for(int i = 0; i < size; i++){
                    t = transactionList.get(i);
                    int j = i - 1;

                    while(j >= 0){
                        if(!((lowestToHighest) ? (t.movement == t_owes) : (t.movement == t_pays)))
                            break;

                        transactionList.set(j + 1, transactionList.get(j));
                        j--;
                    }

                    transactionList.set(j + 1, t);
                }

                break;
        }

        return transactionList;
    }

    public static int getPosition(List<Transaction> tList, Transaction t){
        for(int i = 0; i < tList.size(); i++){
            if(t.equals(tList.get(i)))
                return i;
        }

        return -1;
    }

    public static int getPositionByTimestamp(List<Transaction> tList, String timestamp){
        for(int i = 0; i < tList.size(); i++){
            if(tList.get(i).timestamp.equals(timestamp))
                return i;
        }

        return -1;
    }

    public static void writeToSharedPreferences(Context appContext, List<Transaction> transactionList){
        List<Transaction> transactions = transactionList;
        sortByTransactions(transactions, new SortByStruct(st_timestamp, true));

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

        sortByTransactions(transactions, new SortByStruct(st_timestamp, true));
        return transactions;
    }

    public static class SortByStruct {
        public int sortType;
        public boolean lowestToHighest;
        public Member member;

        public static SortByStruct base = new SortByStruct();

        public SortByStruct(){
            this.sortType = st_timestamp;
            this.lowestToHighest = true;
            this.member = Member.dummy;
        }

        public SortByStruct(int sortType, boolean lowestToHighest){
            this.sortType = sortType;
            this.lowestToHighest = lowestToHighest;
            this.member = Member.dummy;
        }

        public SortByStruct(int sortType, Member member){
            this.sortType = sortType;
            this.lowestToHighest = true;
            this.member = member;
        }

        public static void writeToSharedPreferences(Context appContext, SortByStruct sortByStruct){
            SharedPreferences settings = appContext.getSharedPreferences("Settings", 0);
            SharedPreferences.Editor editor = settings.edit();

            int sortType = sortByStruct.sortType;

            editor.putInt("sortBy_sortType", sortType);

            if((sortType == Transaction.st_timestamp) || (sortType == Transaction.st_amount) || (sortType == Transaction.st_movement))
                editor.putBoolean("sortBy_lowestToHighest", sortByStruct.lowestToHighest);
            else
                editor.putString("sortBy_memberName", sortByStruct.member.getName());

            editor.apply();
        }

        public static SortByStruct readFromSharedPreferences(Context appContext){
            SharedPreferences settings = appContext.getSharedPreferences("Settings", 0);

            SortByStruct sortByStruct = new SortByStruct();
            sortByStruct.sortType = settings.getInt("sortBy_sortType", Transaction.st_timestamp);

            if((sortByStruct.sortType == Transaction.st_timestamp) || (sortByStruct.sortType == Transaction.st_amount) || (sortByStruct.sortType == Transaction.st_movement))
                sortByStruct.lowestToHighest = settings.getBoolean("sortBy_lowestToHighest", true);
            else
                sortByStruct.member = Member.getFromSharedPreferences(appContext, settings.getString("sortBy_memberName", ""));

            return sortByStruct;

        }
    }

    public static class TransactionList {
        private List<Transaction> data;
        protected List<Transaction> list;
        protected SortByStruct sortByStruct;

        public TransactionList(){
            data = new ArrayList<>();
            list = new ArrayList<>();
            sortByStruct = SortByStruct.base;
        }

        public TransactionList(List<Transaction> transactionList){
            this.data = new ArrayList<>(transactionList);
            this.list = new ArrayList<>();
            this.sortByStruct = SortByStruct.base;

            reset();
        }

        public TransactionList(List<Transaction> transactionList, SortByStruct sbs){
            this.data = new ArrayList<>(transactionList);
            this.list = new ArrayList<>();
            this.sortByStruct = sbs;

            reset();
        }

        public TransactionList(SortByStruct sbs){
            data = new ArrayList<>();
            list = new ArrayList<>();
            sortByStruct = sbs;
        }

        public void sort(){
            list = new ArrayList<>(data);

            int sortType = sortByStruct.sortType;
            boolean lowestToHighest = sortByStruct.lowestToHighest;
            Member member = sortByStruct.member;

            if(sortType != st_timestamp)
                Transaction.sortByTransactions(list, new SortByStruct(st_timestamp, true));

            int size = list.size();

            Transaction t;

            switch(sortType){
                case st_timestamp:
                    SimpleDateFormat formatter = new SimpleDateFormat(timeStampFormat, Locale.getDefault());

                    for(int i = 0; i < size; i++){
                        t = list.get(i);
                        Date curTimestamp;
                        Date tempTimestamp;
                        int j = i - 1;

                        try{
                            curTimestamp = formatter.parse(t.timestamp);

                            while(j >= 0){
                                tempTimestamp = formatter.parse(list.get(j).timestamp);

                                if((lowestToHighest) ? (curTimestamp.before(tempTimestamp)) : (curTimestamp.after(tempTimestamp)))
                                    break;

                                list.set(j + 1, list.get(j));
                                j--;
                            }

                            list.set(j + 1, t);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    break;

                case st_member:
                    Transaction _t;
                    Member curMember;
                    Member tempMember;
                    int slots = 0;

                    for(int i = 0; i < size; i++){
                        t = list.get(i);
                        curMember = t.debtor;

                        int j = i - 1;

                        while(j >= 0 && curMember.equals(member)){
                            _t = list.get(j);
                            tempMember = _t.debtor;

                            if(curMember.equals(tempMember))
                                break;

                            list.set(j + 1, _t);
                            j--;
                        }

                        if(curMember.equals(member))
                            slots++;
                        list.set(j + 1, t);
                    }

                    for(int i = slots; i < size; i++){
                        t = list.get(i);
                        curMember = t.lender;

                        int j = i - 1;

                        while(j >= slots && curMember.equals(member)){
                            _t = list.get(j);
                            tempMember = _t.lender;

                            if(curMember.equals(tempMember))
                                break;

                            list.set(j + 1, _t);
                            j--;
                        }

                        list.set(j + 1, t);
                    }

                    break;

                case st_amount:
                    for(int i = 0; i < size; i++){
                        t = list.get(i);
                        float curAmount= t.amount;
                        float tempAmount;
                        int j = i - 1;

                        while(j >= 0){
                            tempAmount = list.get(j).amount;

                            if(!((lowestToHighest) ? (curAmount <= tempAmount) : (curAmount >= tempAmount)))
                                break;

                            list.set(j + 1, list.get(j));
                            j--;
                        }

                        list.set(j + 1, t);
                    }

                    break;

                case st_movement:
                    for(int i = 0; i < size; i++){
                        t = list.get(i);
                        int j = i - 1;

                        while(j >= 0){
                            if(!((lowestToHighest) ? (t.movement == t_owes) : (t.movement == t_pays)))
                                break;

                            list.set(j + 1, list.get(j));
                            j--;
                        }

                        list.set(j + 1, t);
                    }

                    break;
            }
        }

        public void sort(SortByStruct sbs){
            sortByStruct = sbs;

            sort();
        }

        public void set(List<Transaction> newData){
            data = new ArrayList<>(newData);

            for(int i = 0; i < data.size(); i++)
                data.get(i).Id = i;

            reset();
        }

        public void add(Transaction t){
            data.add(t);
            reset();
        }

        public void addNonDuplicate(Transaction t){
            if(!contains(t)) {
                add(t);
                reset();
            }
        }

        public void resetId() {
            for(int i = 0; i < data.size(); i++){
                data.get(i).Id = i;
            }
        }

        public void reset() {
            resetId();
            sort();
        }

        public int getId(Transaction t){
            for(int i = 0; i < data.size(); i++){
                Transaction _t = data.get(i);

                if(_t.Id == t.Id)
                    return _t.Id;
                else if(data.get(i).equals(t)) {
                    _t.Id = i;
                    return i;
                }
            }

            return -1;
        }

        public int getPosition(Transaction t){
            for(int i = 0; i < list.size(); i++){
                if(list.get(i).equals(t))
                    return i;
            }

            return -1;
        }

        public boolean contains(Transaction t){
            return (getId(t) != -1);
        }

        public void remove(Transaction t){
            int i = getId(t);

            if(i != -1) {
                data.remove(i);
                reset();
            }
        }

        public void removeBy(int Id){
            data.remove(Id);
            reset();
        }

        public void removeAt(int position){
            remove(list.get(position));
        }

        public List<Transaction> getData() {
            return data;
        }

        public List<Transaction> getList() {
            return list;
        }

        public SortByStruct getSortByStruct() {
            return sortByStruct;
        }

        public static TransactionList readFromSharedPreferences(Context appContext){
            return new TransactionList(Transaction.readFromSharedPreferences(appContext),
                    SortByStruct.readFromSharedPreferences(appContext));
        }

        public static void writeToSharedPreferences(Context appContext, TransactionList tl){
            Transaction.writeToSharedPreferences(appContext, tl.getData());
            SortByStruct.writeToSharedPreferences(appContext, tl.getSortByStruct());

            tl.reset();
        }

        public void upload(Context appContext) {
            TransactionList.writeToSharedPreferences(appContext, this);
        }
    }
}