package com.njair.homeledger.ui.members;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MembersViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public MembersViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("");
    }

    public LiveData<String> getText() {
        return mText;
    }
}