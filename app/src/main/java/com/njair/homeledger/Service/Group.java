package com.njair.homeledger.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.njair.homeledger.Service.Transaction;
import com.njair.homeledger.Service.Transaction.TransactionList;

import java.util.List;

public class Group {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference groupRef;

    public String id;
    public String name;
    public String password;
    public TransactionList transactionList;
    public List<Member> memberList;

    /*public void init(String name){
        groupRef = database.getReference(id);
    }*/


}
