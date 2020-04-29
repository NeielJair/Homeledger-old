package com.njair.homeledger

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.njair.homeledger.extensions.Util.Companion.months
import com.njair.homeledger.extensions.bindView
import com.njair.homeledger.service.*
import com.njair.homeledger.service.Group.*
import com.njair.homeledger.service.Member.Companion.getNameList
import com.njair.homeledger.ui.fragments.MembersFragment
import com.njair.homeledger.ui.fragments.SummaryFragment
import com.njair.homeledger.ui.fragments.TransactionsFragment
import java.util.*

class GroupMain : AppCompatActivity() {
    private var activity: Activity = this
    
    var roomKey = ""
    val actionBar: ActionBar by lazy { supportActionBar!! }
    
    private val navView: BottomNavigationView        by bindView(R.id.nav_view)
    private val fabShowNavView: FloatingActionButton by bindView(R.id.fabShowNavView)
    private val fabHideNavView: FloatingActionButton by bindView(R.id.fabHideNavView)
    private var navViewAnimState = ANIM_SHOWN
    private var isNavViewLocked = false
    
    lateinit var group: Group
    
    private val inflater: LayoutInflater by lazy{ this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }
    private val vABMonth: View by lazy { inflater.inflate(R.layout.layout_action_bar_text_button, null) }
    
    private var calendar: Calendar = Calendar.getInstance()
    private var year = calendar[Calendar.YEAR]
    private var month = calendar[Calendar.MONTH]
    
    val transactionsFragment = TransactionsFragment()
    val summaryFragment = SummaryFragment()
    val membersFragment = MembersFragment()
    
    var fragmentHeight: Int = 0
    val navViewHeight: Int by lazy { navView.height }
    private val fM = supportFragmentManager
    
    private var active: Fragment = transactionsFragment
    
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val fT = fM.beginTransaction()
        
        when (item.itemId) {
            R.id.navigation_transactions -> {
                if (active !== transactionsFragment)
                    fT.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                fT.hide(active).show(transactionsFragment).commit()
                
                active = transactionsFragment
                vABMonth.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_summary      -> {
                if (active !== summaryFragment)
                    if (active === membersFragment)
                        fT.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                    else  //if(active == transactionsFragment)
                        fT.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                fT.hide(active).show(summaryFragment).commit()
                
                active = summaryFragment
                vABMonth.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_members      -> {
                if (active !== membersFragment)
                    fT.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                fT.hide(active).show(membersFragment).commit()
                active = membersFragment
                vABMonth.visibility = View.GONE
                return@OnNavigationItemSelectedListener true
            }
        }
        
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_main)
    
        fabShowNavView.hide()
        fabShowNavView.setOnClickListener {
            isNavViewLocked = false
            showNavView(null)
        }
    
        fabHideNavView.setOnClickListener {
            hideNavView(null)
            isNavViewLocked = true
        }
        activity = this
        
        roomKey = intent.getStringExtra("RoomKey")!!
        
        fM.beginTransaction().add(R.id.nav_host_fragment, membersFragment, "Members").hide(membersFragment).commit()
        fM.beginTransaction().add(R.id.nav_host_fragment, summaryFragment, "Summary").hide(summaryFragment).commit()
        fM.beginTransaction().add(R.id.nav_host_fragment, transactionsFragment, "Transactions").commit()
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val thisInstance = this
        
        val progressDialog: ProgressDialog = object : ProgressDialog(this, true) {
            override fun dismiss() {
                super.dismiss()
                transactionsFragment.init(group)
                summaryFragment.setGroup(group)
                membersFragment.setGroup(group)
                summaryFragment.summaryAdapter.update()
                summaryFragment.updateUI()
                membersFragment.updateUI()
                summaryFragment.summaryAdapter.notifyDataSetChanged()
                membersFragment.membersAdapter.notifyDataSetChanged()
            }
        }
            .apply {
                setMessage(resources.getString(R.string.fetching_data))
                show()
            }
        
        group = Group(this, roomKey, year, month).apply {
            metadataListener = object : MetadataListener {
                override fun onAdminsChange(adminList: List<String>) {}
                override fun onCurrencyChange(currencySymbol: String) {
                    transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                    summaryFragment.summaryAdapter.notifyDataSetChanged()
                    
                    /*if (transactionsFragment::textViewCurrency.isInitialized)
                        transactionsFragment.textViewCurrency.text = currencySymbol*/
                }
    
                override fun onNameChange(name: String) {
                    actionBar.title = name
                }
    
                override fun onDeletion(group: Group) {
                    Toast.makeText(activity, getString(R.string.group_deleted_prompt, group.name), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            
            membersListener = object : MembersListener {
                    override fun onMemberAdded(memberList: List<Member>, position: Int, member: Member) {
                        transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                        transactionsFragment.aADebtor?.add(member.name)
                        transactionsFragment.aADebtor?.notifyDataSetChanged()
                        transactionsFragment.aALender?.add(member.name)
                        transactionsFragment.aALender?.notifyDataSetChanged()
            
                        membersFragment.membersAdapter.notifyItemInserted(position)
                        //membersFragment.membersAdapter.notifyDataSetChanged()
                        summaryFragment.summaryAdapter.update()
                        membersFragment.updateUI()
                    }
        
                    override fun onMemberChanged(memberList: List<Member>, position: Int, member: Member) {
                        transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                        transactionsFragment.aADebtor?.clear()
                        transactionsFragment.aADebtor?.add(getString(R.string.debtor))
                        transactionsFragment.aADebtor?.addAll(getNameList(memberList))
                        transactionsFragment.aADebtor?.notifyDataSetChanged()
                        transactionsFragment.aALender?.clear()
                        transactionsFragment.aALender?.add(getString(R.string.lender))
                        transactionsFragment.aALender?.addAll(getNameList(memberList))
                        transactionsFragment.aALender?.notifyDataSetChanged()
            
                        membersFragment.membersAdapter.notifyItemChanged(position)
                        //membersFragment.membersAdapter.notifyDataSetChanged()
                        summaryFragment.summaryAdapter.update()
                        membersFragment.updateUI()
                    }
        
                    override fun onMemberRemoved(memberList: List<Member>, position: Int, member: Member) {
                        transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                        transactionsFragment.aADebtor?.remove(member.name)
                        transactionsFragment.aALender?.remove(member.name)
                        
                        if (position == -1)
                            membersFragment.membersAdapter.notifyDataSetChanged()
                        else {
                            membersFragment.membersAdapter.notifyItemRemoved(position)
                            membersFragment.membersAdapter.notifyItemRangeChanged(position, memberList.size - position)
                        }
                        
                        //membersFragment.membersAdapter.notifyDataSetChanged();
                        summaryFragment.summaryAdapter.update()
                        membersFragment.updateUI()
                    }
            }
            
            summaryListener = object : SummaryListener {
                override fun onSummaryChanged(groupSummary: Summary, monthlySummary: Summary) {
                    summaryFragment.summaryAdapter.update()
                }
            }
            
            transactionsListener = object : TransactionsListener {
                override fun onTransactionAdded(key: String, position: Int) {
                    transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                    //transactionsFragment.transactionsAdapter.notifyItemInserted(positionList);
                    transactionsFragment.updateUI()
                }
    
                override fun onTransactionChanged(key: String, position: Int) {
                    transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                    //transactionsFragment.transactionsAdapter.notifyItemChanged(positionList);
                    transactionsFragment.updateUI()
                }
    
                override fun onTransactionRemoved(key: String, position: Int) {
                    transactionsFragment.transactionsAdapter.notifyItemRemoved(position)
                    transactionsFragment.transactionsAdapter.notifyItemRangeChanged(position, transactionsFragment.transactionsAdapter.itemCount)
                    //transactionsFragment.transactionsAdapter.notifyDataSetChanged()
                    transactionsFragment.updateUI()
                }
            }
        }
        
        Handler().postDelayed({ progressDialog.dismiss() }, 1000)
        /*(findViewById<View>(R.id.swipeRefreshLayout) as SwipeRefreshLayout).setOnRefreshListener {
            finish()
            startActivity(intent)
        }*/
        
        setUpActionBar()
    
        val navHostFragment = findViewById<View>(R.id.nav_host_fragment)
        
        navView.viewTreeObserver.addOnGlobalLayoutListener { //navView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            fragmentHeight = navHostFragment.height
            transactionsFragment.notifyHeightChanged(fragmentHeight)
        }
    }

    private fun setUpActionBar() {
        actionBar.setDisplayHomeAsUpEnabled(true)
        
        actionBar.setCustomView(
            vABMonth,
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL or Gravity.END
            )
        )
        
        actionBar.setDisplayShowCustomEnabled(true)
        
        vABMonth.findViewById<View>(R.id.imageButton).setOnClickListener {
            MonthYearPickerDialog(activity, null, year, month,
                resources.getString(R.string.ok), resources.getString(R.string.cancel))
                .setOnDateSetListener(object : MonthYearPickerDialog.OnDateSetListener {
                    override fun onDateSet(year: Int, month: Int) {
                        setTime(year, month)
                    }
                })
                .show()
        }
        (vABMonth.findViewById<View>(R.id.textViewMonth) as TextView).text = months[month]
        (vABMonth.findViewById<View>(R.id.textViewYear)  as TextView).text = year.toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            super.finish()
            return true
        }
        
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() =
        super.finish()

    fun showNavView(duration: Int?) {
        if (isNavViewLocked)
            return
        
        if (navViewAnimState == ANIM_HIDDEN) {
            navViewAnimState = ANIM_SLIDING
            
            ObjectAnimator.ofFloat(navView, "translationY", 0f).apply {
                this.duration = (duration ?: slideAnimationDuration).toLong()
                addListener( onEnd = {
                    navViewAnimState = ANIM_SHOWN
                    fabShowNavView.hide()
                    fabHideNavView.show()
                    navView.visibility = View.VISIBLE
                    
                    //transactionsFragment.notifyHeightChanged(fragmentHeight)
                    //setNewHeight(fragmentHeight /*- navViewHeight*/)
                })
    
                start()
            }
        }
    }

    fun hideNavView(duration: Int?) {
        if (isNavViewLocked)
            return
        
        if (navViewAnimState == ANIM_SHOWN) {
            navViewAnimState = ANIM_SLIDING
            
            ObjectAnimator.ofFloat(navView, "translationY", navView.height.toFloat()).apply {
                this.duration = (duration ?: slideAnimationDuration).toLong()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        navViewAnimState = ANIM_HIDDEN
                        fabShowNavView.show()
                        fabHideNavView.hide()
                        navView.visibility = View.GONE
                        
                        //transactionsFragment.notifyHeightChanged(fragmentHeight + navViewHeight)
                        //setNewHeight(fragmentHeight /*+ navViewHeight*/)
                    }
                })
                start()
            }
        }
    }

    override fun onDestroy() {
        group.detach()
        super.onDestroy()
    }

    private fun setTime(year: Int, month: Int) {
        this.year = year
        this.month = month
        (vABMonth.findViewById<View>(R.id.textViewMonth) as TextView).text = months[month]
        (vABMonth.findViewById<View>(R.id.textViewYear) as TextView).text = year.toString()
        group.changeTime(year, month)
        transactionsFragment.updateUI()
    }

    companion object {
        private const val ANIM_SHOWN: Byte = 0
        private const val ANIM_SLIDING: Byte = 1
        private const val ANIM_HIDDEN: Byte = 2
        private const val slideAnimationDuration = 100
    }
}