package com.njair.homeledger.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.firebase.database.DatabaseReference
import com.njair.homeledger.GroupMain
import com.njair.homeledger.R
import com.njair.homeledger.extensions.Database
import com.njair.homeledger.extensions.Util.Companion.getMemberIcon
import com.njair.homeledger.extensions.Util.Companion.sdfTimestamp
import com.njair.homeledger.extensions.bindView
import com.njair.homeledger.service.Group
import com.njair.homeledger.service.Member
import com.njair.homeledger.service.Summary
import com.njair.homeledger.service.Transaction
import com.njair.homeledger.service.Transaction.Companion.OWES
import com.njair.homeledger.service.Transaction.Companion.PAYS
import com.njair.homeledger.ui.views.DateDividerView
import java.util.*

class SummaryFragment : Fragment() {
    private val activity: Activity by lazy { requireActivity() }
    private lateinit var root: View
    
    private lateinit var optionsMenu: Menu
//    private val memberList: List<Member> = ArrayList()
    
    val summaryAdapter: SummaryAdapter by lazy { SummaryAdapter(activity) }
    private var displayType = SUMMARY_TOTAL
    
    private val textViewDisplay: TextView        by lazy { root.findViewById<TextView>(R.id.textViewDisplay) }
    private val dateDividerView: DateDividerView by lazy { root.findViewById<DateDividerView>(R.id.viewDateDivider) }
    
    private var groupRef: DatabaseReference? = null
    private var roomKey: String? = null
    private lateinit var group: Group
//    private var summaryViewModel: SummaryViewModel? = null
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        summaryViewModel = ViewModelProviders.of(this).get(SummaryViewModel::class.java)
        root = inflater.inflate(R.layout.fragment_summary, container, false)
        
        roomKey = (activity as GroupMain).roomKey
        groupRef = Database.REFERENCE.child(Database.GROUPS).child(roomKey!!)
        
        val summaryRecyclerView: RecyclerView = root.findViewById(R.id.recyclerview_summary)
        summaryRecyclerView.layoutManager = LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false)
        summaryRecyclerView.addItemDecoration(DividerItemDecorator())
        summaryRecyclerView.adapter = summaryAdapter
        
        setHasOptionsMenu(true)
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        optionsMenu = menu
        inflater.inflate(R.menu.menu_summary_main, menu)
        
        if (displayType == SUMMARY_TOTAL) {
            optionsMenu.getItem(0).setIcon(R.drawable.ic_fast_rewind_white_24dp)
            optionsMenu.getItem(0).title = "Show monthly summary"
            dateDividerView.text = "Total"
        } else {
            optionsMenu.getItem(0).setIcon(R.drawable.ic_skip_previous_white_24dp)
            optionsMenu.getItem(0).title = "Show total summary"
            dateDividerView.text = "Monthly"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.actionbar_toggleSummary) {
            displayType = !displayType
            
            if (displayType == SUMMARY_TOTAL) {
                optionsMenu.getItem(0).setIcon(R.drawable.ic_fast_rewind_white_24dp)
                optionsMenu.getItem(0).title = "Show monthly summary"
                dateDividerView.text = "Total"
            } else {
                optionsMenu.getItem(0).setIcon(R.drawable.ic_skip_previous_white_24dp)
                optionsMenu.getItem(0).title = "Show total summary"
                dateDividerView.text = "Monthly"
            }
            
            summaryAdapter.update()
            return true
        }
        
        return false
    }

    fun setGroup(group: Group) {
        if (this::group.isInitialized) this.group.detach()
        this.group = group
    }

    fun updateUI() {
        textViewDisplay.visibility = if (summaryAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    inner class SummaryAdapter internal constructor(private val context: Context) : RecyclerView.Adapter<SummaryAdapter.ViewHolder>() {
        lateinit var summary: Summary
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_transaction_summary, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = summary[position]!!
            
            holder.imageViewDebtorIcon.setImageResource(getMemberIcon(transaction.debtor.type))
            holder.imageViewDebtorIcon.setColorFilter(transaction.debtor.color)
            holder.textViewDebtor.text = transaction.debtor.name
    
            holder.imageViewDebtorLine.setColorFilter(transaction.debtor.color)
    
            if (position != 0 && summary[position - 1]!!.debtor == transaction.debtor) {
                holder.imageViewDebtorLine.setImageResource(R.drawable.ic_next_black_24dp)
                
                holder.imageViewDebtorIcon.visibility = View.INVISIBLE
                holder.textViewDebtor.visibility      = View.INVISIBLE
                holder.textViewMovement.visibility    = View.INVISIBLE
            } else {
                holder.imageViewDebtorLine.setImageResource(R.drawable.ic_arrow_forward_black_24dp)
                
                holder.imageViewDebtorIcon.visibility = View.VISIBLE
                holder.textViewDebtor.visibility      = View.VISIBLE
                holder.textViewMovement.visibility    = View.VISIBLE
            }
            
            holder.textViewAmount.text = transaction.displayAmount(group.currencySymbol)
            
            holder.imageViewLenderIcon.setImageResource(getMemberIcon(transaction.lender.type))
            holder.imageViewLenderIcon.setColorFilter(transaction.lender.color)
            holder.textViewLender.text = transaction.lender.name
            holder.imageViewLenderLine.setColorFilter(transaction.lender.color)
        }

        override fun getItemCount(): Int = if (!this::summary.isInitialized) 0 else summary.size

        fun update() {
            if (this@SummaryFragment::group.isInitialized) 
                summary = if (displayType == SUMMARY_TOTAL) group.groupSummary else group.transactionList.summary
            
            notifyDataSetChanged()
            updateUI()
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), OnCreateContextMenuListener {
            private val transactionConstraintLayout: ConstraintLayout by itemView.bindView(R.id.constraintLayout_transaction)
            private val textViewTimestamp: TextView                   by itemView.bindView(R.id.textViewTimestamp)
            private val textViewDescription: TextView                 by itemView.bindView(R.id.textViewDescription)
            
            val imageViewDebtorIcon: ImageView by itemView.bindView(R.id.imageViewDebtorIcon)
            val textViewDebtor: TextView       by itemView.bindView(R.id.textViewDebtor)
            val imageViewDebtorLine: ImageView by itemView.bindView(R.id.imageViewDebtorLine)
            val textViewAmount: TextView       by itemView.bindView(R.id.textViewAmount)
            val textViewMovement: TextView     by itemView.bindView(R.id.textViewMovement)
            val imageViewLenderIcon: ImageView by itemView.bindView(R.id.imageViewLenderIcon)
            val textViewLender: TextView       by itemView.bindView(R.id.textViewLender)
            val imageViewLenderLine: ImageView by itemView.bindView(R.id.imageViewLenderLine)
            
            override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
                menu.add(Menu.NONE, 0, 0, getString(R.string.cancel_debts)).setOnMenuItemClickListener(onChange)
                //TODO extract string
                menu.add(Menu.NONE, 1, 1, "Check important dates").setOnMenuItemClickListener(onChange)
            }

            private val onChange = MenuItem.OnMenuItemClickListener { item ->
                val t = summaryAdapter.summary[adapterPosition]!!
                when (item.itemId) {
                    0 -> {
                        AlertDialog.Builder(activity)
                            .setMessage(String.format(resources.getString(R.string.pays_all_debts_to), t.debtor.name,
                                t.lender.name, t.displayAmount(group.currencySymbol)))
                            .setPositiveButton(R.string.ok) { _, _ ->
                                val transaction = t.copy()
                                transaction.description = resources.getString(R.string.debts_cancellation)
                                transaction.movement = PAYS
                                transaction.timestamp = sdfTimestamp.format(Date())
                                group.upload(transaction)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                        return@OnMenuItemClickListener true
                    }
                    1 -> {
                        val datesRecyclerView = RecyclerView(context).apply {
                            group.getSummaryTransactions(context, t.summaryDates, t.summaryKey,
                                onComplete = {
                                    it?.let {
                                        adapter = DatesAdapter(context, it)
                                        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                                        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    
                                        AlertDialog.Builder(activity)
                                            .setMessage("History: $t")
                                            .setView(this)
                                            .show()
                                        
                                        Log.wtf("TEST size", "${it.size}")
                                    }
                                }
                            )
                        }
                        
                        return@OnMenuItemClickListener true
                    }
                }
                
                false
            }

            init {
                transactionConstraintLayout.setOnCreateContextMenuListener(this)
                textViewTimestamp.visibility = View.GONE
                textViewDescription.visibility = View.GONE
            }
        }

    }

    internal inner class DividerItemDecorator : ItemDecoration() {
        private val mDivider: Drawable
        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val adapter = parent.adapter as SummaryAdapter
            val dividerLeft = parent.paddingLeft
            val dividerRight = parent.width - parent.paddingRight
            val childCount = parent.childCount
            
            for (i in 0 until childCount - 1) {
                if (adapter.summary[i]!!.debtor != adapter.summary[i + 1]!!.debtor) {
                    val child = parent.getChildAt(i)
                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val dividerTop = child.bottom + params.bottomMargin
                    val dividerBottom = dividerTop + mDivider.intrinsicHeight
                    
                    mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
                    mDivider.draw(canvas)
                }
            }
        }

        init {
            val attrs = intArrayOf(android.R.attr.listDivider)
            val ta = requireContext().obtainStyledAttributes(attrs)
            mDivider = ta.getDrawable(0)!!
            ta.recycle()
        }
    }

    inner class DatesAdapter(private val context: Context,
                             private val tList: List<Transaction>
    ) : RecyclerView.Adapter<DatesAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_transaction_plus, parent, false))

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction: Transaction = tList[position]
            val debtor: Member     = transaction.debtor
            val lender: Member     = transaction.lender
            
            holder.textViewTimestamp.text   = transaction.timestamp
            holder.textViewDescription.text = transaction.description
            
            holder.imageViewDebtorIcon.setImageResource(getMemberIcon(debtor.type))
            holder.imageViewDebtorIcon.setColorFilter(debtor.color)
            holder.textViewDebtor.text = debtor.name
            holder.imageViewDebtorLine.setColorFilter(debtor.color)
            
            holder.textViewAmount.text = transaction.displayAmount(group.currencySymbol)
            
            if (transaction.movement == OWES) {
                holder.textViewMovement.setText(R.string.owes)
                holder.textViewMovement.setTypeface(null, Typeface.NORMAL)
            } else {
                holder.textViewMovement.setText(R.string.pays)
                holder.textViewMovement.setTypeface(null, Typeface.BOLD)
            }
            
            holder.textViewMovement.setTextColor(transaction.blendedColors(.5f))
            holder.imageViewLenderIcon.setColorFilter(lender.color)
            holder.textViewLender.text = lender.name
            holder.imageViewLenderLine.setColorFilter(lender.color)
        }

        override fun getItemCount(): Int = tList.size
        
        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val transactionConstraintLayout: ConstraintLayout by itemView.bindView(R.id.constraintLayout_transaction)
            
            val textViewTimestamp: TextView    by itemView.bindView(R.id.textViewTimestamp)
            val textViewDescription: TextView  by itemView.bindView(R.id.textViewDescription)
            val imageViewDebtorIcon: ImageView by itemView.bindView(R.id.imageViewDebtorIcon)
            val textViewDebtor: TextView       by itemView.bindView(R.id.textViewDebtor)
            val imageViewDebtorLine: ImageView by itemView.bindView(R.id.imageViewDebtorLine)
            val textViewAmount: TextView       by itemView.bindView(R.id.textViewAmount)
            val textViewMovement: TextView     by itemView.bindView(R.id.textViewMovement)
            val imageViewLenderIcon: ImageView by itemView.bindView(R.id.imageViewLenderIcon)
            val textViewLender: TextView       by itemView.bindView(R.id.textViewLender)
            val imageViewLenderLine: ImageView by itemView.bindView(R.id.imageViewLenderLine)

            init {
                itemView.findViewById<View>(R.id.imageViewLeftSwipeCircle).visibility = View.GONE
                itemView.findViewById<View>(R.id.imageViewRightSwipeCircle).visibility = View.GONE
            }
        }

    }

    companion object {
        private const val SUMMARY_TOTAL = true
//        private const val SUMMARY_MONTHLY = false
    }
}