package com.njair.homeledger.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference groupRef;

    public String name;
    public String password;
    public ArrayList<Member> people = new ArrayList<>();
    public ArrayList<Member> admins = new ArrayList<>();
    public List<Transaction> movements = new ArrayList<>();


    public void init(String name){
        groupRef = database.getReference(name);
    }
}
