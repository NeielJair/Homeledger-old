package com.njair.homeledger.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Member {
    public String name;
    public int color;
    public float funds;
    public Map<Member, Float> loanPortfolio = new HashMap<>();

    public Member(String name, int color){
        this.name = name;
        this.color = color;
        this.funds = 0;
    }

    public String getName(){
        return name;
    }

    public int getColor() {
        return color;
    }

    public double getFunds(){
        return funds;
    }


    public float getLoan(Member debtor){
        return (loanPortfolio.containsKey(debtor) ? loanPortfolio.get(debtor) : 0);
    }

    public void addLoan(Member debtor, float amount){
        loanPortfolio.put(debtor, ((loanPortfolio.containsKey(debtor)) ? loanPortfolio.get(debtor) : 0) + amount);
    }

    public void addFunds(float amount){
        funds += amount;
    }

    public void lendTo(Member recipient){
        //float
    }

    public boolean equals(Member member){
        return getName().equals(member.getName());
    }

    public static Member findMember(List<Member> members, String name){
        for(Member member : members){
            if(member.getName().equals(name))
                return member;
        }

        return null;
    }

    public static List<String> getNameList(List<Member> members){
        List<String> newList = new ArrayList<>(members.size());

        for(Member member : members){
            newList.add(member.getName());
        }

        return newList;
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
