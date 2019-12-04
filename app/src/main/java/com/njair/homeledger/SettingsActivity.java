package com.njair.homeledger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setSwipeIcon();
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    setSwipeIcon();
                }
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    setSwipeIcon();
                }
            });
        }

        private void setSwipeIcon(){
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            int valLeftSwipe, valRightSwipe;

            try {
                valLeftSwipe = Integer.parseInt(settings.getString("pref_leftSwipe", "0"));
                valRightSwipe = Integer.parseInt(settings.getString("pref_rightSwipe", "3"));
            } catch(Exception e) {
                valLeftSwipe = 0;
                valRightSwipe = 3;
            }

            DropDownPreference leftSwipe = (DropDownPreference) findPreference("pref_leftSwipe");
            DropDownPreference rightSwipe = (DropDownPreference) findPreference("pref_rightSwipe");

            switch(valLeftSwipe){
                case -1:
                    leftSwipe.setIcon(R.drawable.ic_empty_24dp);
                    break;

                case 0:
                    leftSwipe.setIcon(R.drawable.ic_pencil_color_24dp);
                    break;

                case 1:
                    leftSwipe.setIcon(R.drawable.ic_duplicate_color_24dp);
                    break;

                case 2:
                    leftSwipe.setIcon(R.drawable.ic_payment_color_24dp);
                    break;

                case 3:
                    leftSwipe.setIcon(R.drawable.ic_trash_color_24dp);
                    break;
            }

            switch(valRightSwipe){
                case -1:
                    rightSwipe.setIcon(R.drawable.ic_empty_24dp);
                    break;

                case 0:
                    rightSwipe.setIcon(R.drawable.ic_pencil_color_24dp);
                    break;

                case 1:
                    rightSwipe.setIcon(R.drawable.ic_duplicate_color_24dp);
                    break;

                case 2:
                    rightSwipe.setIcon(R.drawable.ic_payment_color_24dp);
                    break;

                case 3:
                    rightSwipe.setIcon(R.drawable.ic_trash_color_24dp);
                    break;
            }
        }
    }
}