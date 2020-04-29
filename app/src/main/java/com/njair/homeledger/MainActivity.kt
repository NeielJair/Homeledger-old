package com.njair.homeledger

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.njair.homeledger.extensions.Database
import com.njair.homeledger.extensions.Util
import com.njair.homeledger.ui.fragments.GroupsFragment
import com.njair.homeledger.ui.fragments.SettingsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

//TODO When you implement a way to change your nickname, also tell the program to
// check every group that member belongs to and update the name in the transactions
class MainActivity : AppCompatActivity() {
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    private val groupsFragment = GroupsFragment(this)
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = groupsFragment
    
    private val fM = supportFragmentManager
    
    //private val groupsFragment: GroupsFragment = GroupsFragment() //TODO link the activity and the fragment
    
//    private val navHostFragment: NavHostFragment by bindView(R.id.nav_host_fragment)

    @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        Util.init(this)
        
        Database.USER = mAuth.currentUser
        
        //createSignInIntent()
    
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        /*appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_groups, R.id.nav_user, R.id.nav_settings), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        */
    
        val drawerToggle: ActionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout,
            toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        
        drawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        drawerToggle.syncState()
        
        navView.setCheckedItem(R.id.navigation_groups)
        
        fM.beginTransaction().add(R.id.nav_host_fragment, groupsFragment, "Groups").commit()
        fM.beginTransaction().add(R.id.nav_host_fragment, settingsFragment, "Settings").hide(settingsFragment).commit()
        navView.setNavigationItemSelectedListener {
            val fT = fM.beginTransaction()
    
            when (it.itemId) {
                R.id.navigation_groups -> {
                    fT.hide(activeFragment).show(groupsFragment).commit()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    
                    activeFragment = groupsFragment
            
                    return@setNavigationItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    fT.hide(activeFragment).show(settingsFragment).commit()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    
                    activeFragment = settingsFragment
    
                    return@setNavigationItemSelectedListener true
                }
                else                   -> {
                    fT.hide(activeFragment).commit()
                    drawerLayout.closeDrawer(GravityCompat.START)
            
                    return@setNavigationItemSelectedListener true
                }
            }
    
            false
        }
        
        //region [Links]
        // ATTENTION: This was auto-generated to handle app links.
        /*Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();*/
        //endregion
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.wtf("sign in", resultCode.toString() + "")
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK && mAuth.currentUser != null) {
                // Successfully signed in
                login(mAuth.currentUser!!)
            } else  /*if (response != null)*/ {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                finish()
                startActivity(intent)
            }
        }
    }

    fun createSignInIntent() {
        if (Database.USER == null) {
            // Choose authentication providers
            val providers = listOf(
                AnonymousBuilder().build(),
                EmailBuilder().build(),
                GoogleBuilder().build()
            )

            // Create and launch sign-in intent
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setLogo(R.drawable.ic_logo)
                    .setAvailableProviders(providers)
                    .setIsSmartLockEnabled(false)
                    .build(), RC_SIGN_IN
            )
        } else
            login(Database.USER!!)
    }

    private fun login(currentUser: FirebaseUser) {
        Database.USER = currentUser
        
        Toast.makeText(this, "User: ${currentUser.displayName}", Toast.LENGTH_LONG).show()
    
        groupsFragment.login(currentUser)
    }

    fun logout() {
        groupsFragment.groupsAdapter.detach()
        mAuth.signOut()
        recreate()
    }

    companion object {
        //public static final int _VERSION = 2;
        const val RC_SIGN_IN = 123
    }
}