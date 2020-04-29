package com.njair.homeledger

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        val actionBar = supportActionBar
        title = resources.getString(R.string.action_settings)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setSwipeIcon()
        }

        override fun onResume() {
            super.onResume()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener { _, _ -> setSwipeIcon() }
        }

        override fun onPause() {
            super.onPause()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener { _, _ -> setSwipeIcon() }
        }

        private fun setSwipeIcon() {
            val settings = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            
            val valLeftSwipe: Int = settings.getString("pref_leftSwipe", "0")?.toInt() ?: 0
            val valRightSwipe: Int = settings.getString("pref_rightSwipe", "3")?.toInt() ?: 3
            
            val leftSwipe = findPreference<Preference>("pref_leftSwipe") as DropDownPreference
            val rightSwipe = findPreference<Preference>("pref_rightSwipe") as DropDownPreference
            
            when (valLeftSwipe) {
                -1 -> leftSwipe.setIcon(R.drawable.ic_empty_24dp)
                0  -> leftSwipe.setIcon(R.drawable.ic_pencil_color_24dp)
                1  -> leftSwipe.setIcon(R.drawable.ic_duplicate_color_24dp)
                2  -> leftSwipe.setIcon(R.drawable.ic_payment_color_24dp)
                3  -> leftSwipe.setIcon(R.drawable.ic_trash_color_24dp)
            }
            
            when (valRightSwipe) {
                -1 -> rightSwipe.setIcon(R.drawable.ic_empty_24dp)
                0  -> rightSwipe.setIcon(R.drawable.ic_pencil_color_24dp)
                1  -> rightSwipe.setIcon(R.drawable.ic_duplicate_color_24dp)
                2  -> rightSwipe.setIcon(R.drawable.ic_payment_color_24dp)
                3  -> rightSwipe.setIcon(R.drawable.ic_trash_color_24dp)
            }
        }
    }
}