package com.njair.homeledger.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.njair.homeledger.GroupMain
import com.njair.homeledger.R
import com.njair.homeledger.extensions.*
import com.njair.homeledger.extensions.Database.groupReference
import com.njair.homeledger.extensions.Util.Companion.getMemberIcon
import com.njair.homeledger.extensions.Util.Companion.roundToDecimalPlace
import com.njair.homeledger.extensions.Util.Companion.sdfDate
import com.njair.homeledger.extensions.Util.Companion.sdfTime
import com.njair.homeledger.service.*
import com.njair.homeledger.service.ListenerHashMap.DataListener
import com.njair.homeledger.service.Member.Companion.findMember
import com.njair.homeledger.service.Member.Companion.getFromSharedPreferences
import com.njair.homeledger.service.Member.Companion.getNameList
import com.njair.homeledger.service.Member.Companion.isNullOrBlack
import com.njair.homeledger.service.SortByStruct.Companion.readFromSharedPreferences
import com.njair.homeledger.service.SortByStruct.Companion.writeToSharedPreferences
import com.njair.homeledger.service.Transaction.Companion.OWES
import com.njair.homeledger.service.Transaction.Companion.PAYS
import com.njair.homeledger.ui.views.DialogDrawer
import com.njair.homeledger.ui.views.DialogDrawer.*
import com.njair.homeledger.ui.views.ToggleView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TransactionsFragment : Fragment() {
    private val activity: Activity by lazy { requireActivity() }
    private val groupMain: GroupMain
        get() = activity as GroupMain
    
    private var groupRef: DatabaseReference? = null
//    private var transactionsViewModel: TransactionsViewModel? = null
    
    private var group: Group = Group()
    
    private lateinit var root: View
    private val fabWrite: FloatingActionButton         by lazy { root.findViewById<FloatingActionButton>(R.id.fabWrite) }
    private val dialogDrawer: DialogDrawer by lazy { root.findViewById<DialogDrawer>(R.id.bottomDrawerTransaction) }
    private val transactionsRecyclerView: RecyclerView by lazy { root.findViewById<RecyclerView>(R.id.recyclerView_transactions) }
    
    val transactionsAdapter: TransactionsAdapter by lazy { TransactionsAdapter(activity) }
    
    private val textViewDisplay: TextView  by lazy { root.findViewById<TextView>(R.id.textViewDisplay) }
    private val textViewDisplay2: TextView by lazy { root.findViewById<TextView>(R.id.textViewDisplay2) }
    lateinit var textViewCurrency: TextView
    
    private var valLeftSwipe = 0
    private var valRightSwipe = 0
    private var paymentSetCurDate = false
    private var displayDividers = false
    private var prefNavViewHide = false
    var aADebtor: ArrayAdapter<String>? = null
    var aALender: ArrayAdapter<String>? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        transactionsViewModel = ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
        root = inflater.inflate(R.layout.fragment_transactions, container, false)
        
        val roomKey = groupMain.roomKey
        groupRef = groupReference(roomKey)
        val context = requireContext()
        
        transactionsRecyclerView.adapter = transactionsAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        setHasOptionsMenu(true)
        
        val aLDebtor = ArrayList<String>()
        val aLLender = ArrayList<String>()
        
        aLDebtor.add(resources.getString(R.string.debtor))
        aLLender.add(resources.getString(R.string.lender))
        
        aADebtor = Util.getSpinnerHintAdapter(activity, aLDebtor)
        
        aALender = Util.getSpinnerHintAdapter(activity, aLLender)

        //region [Enable item swipe]
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        
        valLeftSwipe = settings.getString("pref_leftSwipe", "0")?.toInt() ?: 0
        valRightSwipe = settings.getString("pref_rightSwipe", "3")?.toInt() ?: 3
        
        paymentSetCurDate = settings.getBoolean("pref_payment_currentDate", true)
        displayDividers = settings.getBoolean("pref_display_date_dividers", true)
        prefNavViewHide = settings.getBoolean("pref_nav_view_hide", true)
        
        val swipeToDeleteCallback = SwipeToDeleteCallback(context, transactionsAdapter, valLeftSwipe,
            valRightSwipe)
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(transactionsRecyclerView)
        //endregion

        //region [Handle dialog drawers & FABs]
        dialogDrawer.apply {
            parentActivity = groupMain
    
            setOnSlideListener(object : OnSlideListener {
                override fun onSlideUp() {
                    if (fabWrite.isShown)
                        fabWrite.hide()
            
                    if (prefNavViewHide)
                        groupMain.hideNavView(0)
                }
                override fun onSlideDown() {}
                override fun onClose() {
                    if (!fabWrite.isShown)
                        fabWrite.show()
            
                    if (prefNavViewHide)
                        groupMain.showNavView(0)
                }
            })
            
            val transactionContent = Content (
                layout = R.layout.dialog_create_transaction,
                title = resources.getString(R.string.add_transaction)
            )
            val splitBillContent = Content (
                layout = R.layout.dialog_split_bill,
                title = resources.getString(R.string.split_bill)
            )
    
            setUpTransactionDrawer(transactionContent)
            setUpSplitBillDrawer(splitBillContent)
    
            addContent(transactionContent, splitBillContent)
        }
        
        /*Write FAB: slide drawer up*/
        fabWrite.setOnClickListener {
            if (!dialogDrawer.visible) {
                dialogDrawer.slideUp()
            }
        }
    
        /*Misc*/
        transactionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fabWrite.visibility == View.VISIBLE && dialogDrawer.visible) {
                    fabWrite.hide()
                } else if ((dy < 0 || dy == 0) && fabWrite.visibility != View.VISIBLE && !dialogDrawer.visible) {
                    fabWrite.show()
                }
            }
        })
        //endregion
        return root
    }

    private fun setUpTransactionDrawer(content: Content) {
        val contentView = content.view
        
        val buttonDatePicker: LinearLayout = contentView.findViewById(R.id.buttonDatePicker)
        val textViewDate: TextView         = contentView.findViewById(R.id.textViewDate)
        val buttonTimePicker: LinearLayout = contentView.findViewById(R.id.buttonTimePicker)
        val textViewTime: TextView         = contentView.findViewById(R.id.textViewTime)
        val editTextDescription: EditText  = contentView.findViewById(R.id.editTextDescription)
        val spinnerDebtor: Spinner         = contentView.findViewById(R.id.spinnerDebtor)
        textViewCurrency                   = contentView.findViewById(R.id.textViewCurrency)
        val editTextAmount: EditText       = contentView.findViewById(R.id.editTextAmount)
        val toggleMovement: ToggleView     = contentView.findViewById(R.id.toggleMovement)
        val spinnerLender: Spinner         = contentView.findViewById(R.id.spinnerLender)
        
        val date = Calendar.getInstance()
        
        textViewDate.text = sdfDate.format(date.time)
        textViewTime.text = sdfTime.format(date.time)
        
        val onDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            date[Calendar.YEAR] = year
            date[Calendar.MONTH] = monthOfYear
            date[Calendar.DAY_OF_MONTH] = dayOfMonth
            textViewDate.text = sdfDate.format(date.time)
        }
        
        val onTimeSetListener = OnTimeSetListener { _, hourOfDay, minute ->
            date[Calendar.HOUR_OF_DAY] = hourOfDay
            date[Calendar.MINUTE] = minute
            textViewTime.text = sdfTime.format(date.time)
        }
    
        buttonDatePicker.setOnClickListener {
            DatePickerDialog(activity, onDateSetListener, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
                .show()
        }
        
        buttonTimePicker.setOnClickListener {
            TimePickerDialog(activity, onTimeSetListener, date[Calendar.HOUR_OF_DAY], date[Calendar.MINUTE], true)
                .show()
        }
        
        spinnerDebtor.adapter = aADebtor
        spinnerDebtor.addOnItemSelectedListener(
            onItemSelected = { _, _, position, _ ->
                dialogDrawer
                    .setButtonEnabled( DialogInterface.BUTTON_POSITIVE,
                        position != 0 && position != spinnerLender.selectedItemPosition
                        && !(editTextAmount.text.isBlank() || editTextAmount.text.toString().toFloat() == 0f)
                        && dialogDrawer.visible)
            }
        )
        
        editTextAmount.addTextChangedListener (
            afterTextChanged = { s ->
                s!!
                
                if (s.toString().trim { it <= ' ' } == ".")
                    s.insert(0, "0")
                dialogDrawer
                    .setButtonEnabled( DialogInterface.BUTTON_POSITIVE,
                        !(s.isBlank() || s.toString().toFloat() == 0f) && spinnerDebtor.selectedItemPosition != 0
                        && spinnerLender.selectedItemPosition != 0 && spinnerDebtor.selectedItemPosition != spinnerLender.selectedItemPosition
                        && dialogDrawer.visible)
            }
        )
        
        spinnerLender.adapter = aALender
        spinnerLender.addOnItemSelectedListener(
            onItemSelected = { _, _, position, _ ->
                dialogDrawer
                    .setButtonEnabled( DialogInterface.BUTTON_POSITIVE,
                    position != 0 && position != spinnerDebtor.selectedItemPosition
                    && !(editTextAmount.text.toString() == "" || editTextAmount.text.toString().toFloat() == 0f)
                    && dialogDrawer.visible)
            }
        )
    
        content.apply {
            onRefresh = { _, _ ->
                date.time = Calendar.getInstance().time
            
                date[Calendar.YEAR] = group.year
                date[Calendar.MONTH] = group.month
            
                textViewDate.text = sdfDate.format(date.time)
                textViewTime.text = sdfTime.format(date.time)
            
                editTextDescription.setText("")
                editTextAmount.setText("")
            
                spinnerDebtor.setSelection(0)
                spinnerLender.setSelection(0)
                toggleMovement.setSelection(ToggleView.ENTRY_FIRST)
            }
            onDisplay = {
                dialogDrawer.apply {
                    setButtonEnabled(DialogInterface.BUTTON_POSITIVE,
                        !(editTextAmount.text.isBlank() || editTextAmount.text.toString().toFloat() == 0f)
                        && spinnerLender.selectedItemPosition != 0 && spinnerDebtor.selectedItemPosition != spinnerLender.selectedItemPosition
                        && dialogDrawer.visible)
                
                    setButtonVisibility(DialogInterface.BUTTON_POSITIVE, View.VISIBLE)
                    setButtonVisibility(DialogInterface.BUTTON_NEGATIVE, View.VISIBLE)
                }
            }
            buttonPositive = resources.getText(R.string.add) to { _ ->
                group.upload( Transaction (
                    editTextDescription.text.toString(),
                    group.memberList.findMember(spinnerDebtor.selectedItem.toString()),
                    group.memberList.findMember(spinnerLender.selectedItem.toString()),
                    editTextAmount.text.toString().toFloat(),
                    date.timeInMillis,
                    toggleMovement.selectedEntry == ToggleView.ENTRY_FIRST
                ),
                    OnCompleteListener {
                        Snackbar.make(dialogDrawer, R.string.transaction_added, Snackbar.LENGTH_SHORT).show()
                        dialogDrawer.setButtonText(DialogInterface.BUTTON_NEGATIVE, resources.getString(R.string.done))
                    }
                )
            }
            buttonNegative = resources.getText(R.string.cancel) to { _ -> dialogDrawer.slideDown() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpSplitBillDrawer(content: Content) {
        val contentView = content.view
        
        val bDatePicker: LinearLayout            = contentView.findViewById(R.id.buttonDatePicker)
        val tVDate: TextView                     = contentView.findViewById(R.id.textViewDate)
        val bTimePicker: LinearLayout            = contentView.findViewById(R.id.buttonTimePicker)
        val tVTime: TextView                     = contentView.findViewById(R.id.textViewTime)
        val eTDescription: EditText              = contentView.findViewById(R.id.editTextDescription)
        val tVAmount: TextView                   = contentView.findViewById(R.id.textViewAmount)
        val tVAmountEach: TextView               = contentView.findViewById(R.id.textViewAmountEach)
        val rVSplitBillMembers: RecyclerView     = contentView.findViewById(R.id.recyclerView_split)
        val tVTransactions: TextView             = contentView.findViewById(R.id.textViewTransactions)
        val rVSplitBillTransaction: RecyclerView = contentView.findViewById(R.id.recyclerView_split_bill_transactions)
        val bAdd: Button                         = contentView.findViewById(R.id.buttonAdd)
    
        val date = Calendar.getInstance()
        
        val onDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            date[Calendar.YEAR] = year
            date[Calendar.MONTH] = monthOfYear
            date[Calendar.DAY_OF_MONTH] = dayOfMonth
            tVDate.text = sdfDate.format(date.time)
        }
        
        val onTimeSetListener = OnTimeSetListener { _, hourOfDay, minute ->
            date[Calendar.HOUR_OF_DAY] = hourOfDay
            date[Calendar.MINUTE] = minute
            tVTime.text = sdfTime.format(date.time)
        }
        
        bDatePicker.setOnClickListener {
            DatePickerDialog(activity, onDateSetListener, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
                .show()
        }
        
        bTimePicker.setOnClickListener {
            TimePickerDialog(activity, onTimeSetListener, date[Calendar.HOUR_OF_DAY], date[Calendar.MINUTE], true)
                .show()
        }
    
        val membersAdapter = SplitBillMembersAdapter(context!!)
        val transactionsAdapter = SplitBillTransactionsAdapter(context!!)
        
        membersAdapter.transactionsAdapter = transactionsAdapter
        transactionsAdapter.membersAdapter = membersAdapter
        
        membersAdapter.setDataListener(object : DataListener {
            override fun onDataSetChanged(map: ListenerHashMap<*, *>) {
                var totalAmount = 0f
                val amountEach: Float
                
                membersAdapter.amountMap.forEach { (_, amount) -> totalAmount += amount!! }
                
                amountEach =
                    if(totalAmount != 0f && membersAdapter.amountMap.size != 0)
                        roundToDecimalPlace(totalAmount / membersAdapter.amountMap.size, 2)
                    else
                        0f
                
                val amountText = "${group.currencySymbol}$totalAmount"
                tVAmount.text = amountText
                
                val amountEachText = "${group.currencySymbol}$amountEach"
                tVAmountEach.text = resources.getString(R.string._each, amountEachText)
                
                tVAmountEach.visibility = if (totalAmount == 0f || map.keys.size < 2) GONE else VISIBLE
                
                membersAdapter.update(amountEach)
                
                val transactionsSectionVisibility = if (map.keys.size < 2) GONE else VISIBLE
                tVTransactions.visibility = transactionsSectionVisibility
                rVSplitBillTransaction.visibility = transactionsSectionVisibility
                bAdd.visibility = transactionsSectionVisibility
                
                transactionsAdapter.notifyChange()
            }
        })
        
        rVSplitBillMembers.adapter = membersAdapter
        rVSplitBillMembers.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rVSplitBillMembers.isNestedScrollingEnabled = false
        //splitBillRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
    
        rVSplitBillTransaction.adapter = transactionsAdapter
        rVSplitBillTransaction.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rVSplitBillTransaction.isNestedScrollingEnabled = false
        rVSplitBillTransaction.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    
        tVTransactions.visibility = GONE
        rVSplitBillTransaction.visibility = GONE
        bAdd.visibility = GONE
        
        bAdd.setOnClickListener { transactionsAdapter.addItem() }
    
        content.apply {
            onRefresh = { _, _ ->
                date.time = Calendar.getInstance().time
                date[Calendar.YEAR] = group.year
                date[Calendar.MONTH] = group.month
            
                tVDate.text = sdfDate.format(date.time)
                tVTime.text = sdfTime.format(date.time)
            
                eTDescription.setText("")
            
                val amount = "${group.currencySymbol}0"
                tVAmount.text = amount
                tVAmountEach.visibility = View.INVISIBLE
            }
            onDisplay = { dialogDrawer.setButtonEnabled(DialogInterface.BUTTON_POSITIVE, false) }
            buttonPositive = resources.getString(R.string.add) to { _ ->
                AlertDialog.Builder(context).setMessage("Are you sure you want to add ${transactionsAdapter.tList.size} transactions?")
                
                var uploaded = 0
    
                transactionsAdapter.tList.forEach {
                    it.first ?: return@forEach; it.second ?: return@forEach
                    
                    group.upload( Transaction (
                        eTDescription.text.toString(),
                        group.memberList.findMember(it.first!!),
                        group.memberList.findMember(it.second!!),
                        it.fourth,
                        date.timeInMillis,
                        it.third
                    ))
                    
                    uploaded++
                }
    
                Snackbar.make(dialogDrawer, resources.getString(R.string.uploading__transactions, uploaded), Snackbar.LENGTH_SHORT).show()
    
                dialogDrawer.slideDown()
            }
            buttonNegative = resources.getText(R.string.cancel) to { _ -> dialogDrawer.slideDown() }
        }
    }

    fun init(group: Group) {
        this.group.detach()
        this.group = group
        transactionsAdapter.notifyDataSetChanged()
        updateUI()
        //transactionsAdapter.sort();
    }

    fun updateUI() {
        val displayVisibility = if (transactionsAdapter.itemCount == 0) View.VISIBLE else View.GONE
    
        textViewDisplay.visibility  = displayVisibility
        textViewDisplay2.visibility = displayVisibility
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_transactions_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionbar_sortBy -> {
                val sortByStruct = readFromSharedPreferences(activity)
                
                
                val inflater = activity.layoutInflater
                val dialogLayout = inflater.inflate(R.layout.dialog_sort_transactions, null)
                val builder = AlertDialog.Builder(activity)
                    .setTitle(resources.getString(R.string.sort_by))
                    .setView(dialogLayout)
                
                val switchLowestToHighest: Switch     = dialogLayout.findViewById(R.id.switchLowestToHighest)
                val textViewLowestToHighest: TextView = dialogLayout.findViewById(R.id.textViewLowestToHighest)
                val spinnerMember: Spinner            = dialogLayout.findViewById(R.id.spinnerMembers)
                val rg: RadioGroup                    = dialogLayout.findViewById(R.id.radioGroup)
                
                val aAMembers = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
                    getNameList(group.memberList).toTypedArray())
                
                spinnerMember.adapter = aAMembers
                
                if (sortByStruct.member.isNullOrBlack())
                    sortByStruct.member = group.memberList[0]
                
                when (sortByStruct.sortType) {
                    SortByStruct.TIME     -> {
                        rg.check(R.id.radioButtonTimestamp)
                        
                        switchLowestToHighest.isChecked = sortByStruct.lowestToHighest
                        textViewLowestToHighest.text    =
                            if (sortByStruct.lowestToHighest) resources.getString(R.string.newest_to_oldest) else resources.getString(R.string.oldest_to_newest)
                        
                        switchLowestToHighest.visibility   = View.VISIBLE
                        textViewLowestToHighest.visibility = View.VISIBLE
                        
                        switchLowestToHighest.isEnabled   = true
                        textViewLowestToHighest.isEnabled = true
    
                        spinnerMember.visibility = View.GONE
                        spinnerMember.isEnabled  = false
                    }
                    SortByStruct.MEMBER   -> {
                        rg.check(R.id.radioButtonMember)
                        
                        val debtor = getFromSharedPreferences(activity, sortByStruct.member!!.name)
                        
                        if (debtor.isBlack())
                            spinnerMember.setSelection(aAMembers.getPosition(debtor.name))
                        
                        switchLowestToHighest.visibility = View.GONE
                        textViewLowestToHighest.visibility = View.GONE
                        spinnerMember.visibility = View.VISIBLE
                        
                        switchLowestToHighest.isEnabled = false
                        textViewLowestToHighest.isEnabled = false
                        spinnerMember.isEnabled = true
                    }
                    SortByStruct.AMOUNT   -> {
                        rg.check(R.id.radioButtonAmount)
                        
                        switchLowestToHighest.isChecked = sortByStruct.lowestToHighest
                        textViewLowestToHighest.text    =
                            if (sortByStruct.lowestToHighest) resources.getString(R.string.lowest_to_highest) else resources.getString(R.string.highest_to_lowest)
                        
                        switchLowestToHighest.visibility   = View.VISIBLE
                        textViewLowestToHighest.visibility = View.VISIBLE
                        spinnerMember.visibility           = View.GONE
                        
                        switchLowestToHighest.isEnabled   = true
                        textViewLowestToHighest.isEnabled = true
                        spinnerMember.isEnabled           = false
                    }
                    SortByStruct.MOVEMENT -> {
                        rg.check(R.id.radioButtonMovement)
                        
                        switchLowestToHighest.isChecked = sortByStruct.lowestToHighest
                        textViewLowestToHighest.text    =
                            if (sortByStruct.lowestToHighest) resources.getString(R.string.debts_first) else resources.getString(R.string.loans_first)
                        
                        switchLowestToHighest.visibility   = View.VISIBLE
                        textViewLowestToHighest.visibility = View.VISIBLE
                        spinnerMember.visibility           = View.GONE
                        
                        switchLowestToHighest.isEnabled   = true
                        textViewLowestToHighest.isEnabled = true
                        spinnerMember.isEnabled           = false
                    }
                }
                
                rg.setOnCheckedChangeListener { _, checkedId ->
                    if (checkedId == R.id.radioButtonTimestamp
                        || checkedId == R.id.radioButtonAmount
                        || checkedId == R.id.radioButtonMovement) {
                        switchLowestToHighest.isChecked = sortByStruct.lowestToHighest
                        
                        when (checkedId) {
                            R.id.radioButtonTimestamp -> {
                                sortByStruct.sortType = SortByStruct.TIME
            
                                textViewLowestToHighest.text =
                                    if (sortByStruct.lowestToHighest) resources.getString(R.string.newest_to_oldest) else resources.getString(R.string.oldest_to_newest)
                            }
                            R.id.radioButtonAmount    -> {
                                sortByStruct.sortType = SortByStruct.AMOUNT
            
                                textViewLowestToHighest.text =
                                    if (sortByStruct.lowestToHighest) resources.getString(R.string.lowest_to_highest) else resources.getString(R.string.highest_to_lowest)
                            }
                            R.id.radioButtonMovement  -> {
                                sortByStruct.sortType = SortByStruct.MOVEMENT
            
                                textViewLowestToHighest.text =
                                    if (sortByStruct.lowestToHighest) resources.getString(R.string.debts_first) else resources.getString(R.string.loans_first)
                            }
                        }
    
                        switchLowestToHighest.visibility   = View.VISIBLE
                        textViewLowestToHighest.visibility = View.VISIBLE
                        spinnerMember.visibility           = View.GONE
    
                        switchLowestToHighest.isEnabled   = true
                        textViewLowestToHighest.isEnabled = true
                        spinnerMember.isEnabled           = false
                    }
                    else if (checkedId == R.id.radioButtonMember) {
                        sortByStruct.sortType = SortByStruct.MEMBER
                        
                        val member = getFromSharedPreferences(activity, sortByStruct.member!!.name)
                        
                        if (member.isBlack()) {
                            sortByStruct.member = member
                            spinnerMember.setSelection(aAMembers.getPosition(member.name))
                        }
                        
                        switchLowestToHighest.visibility = View.GONE
                        textViewLowestToHighest.visibility = View.GONE
                        spinnerMember.visibility = View.VISIBLE
                        switchLowestToHighest.isEnabled = false
                        textViewLowestToHighest.isEnabled = false
                        spinnerMember.isEnabled = true
                    }
                }
                
                switchLowestToHighest.setOnCheckedChangeListener { _, isChecked ->
                    sortByStruct.lowestToHighest = isChecked
    
                    textViewLowestToHighest.text =
                        when (sortByStruct.sortType) {
                            SortByStruct.TIME     -> if (isChecked) resources.getString(R.string.newest_to_oldest) else resources.getString(R.string.oldest_to_newest)
                            SortByStruct.AMOUNT   -> if (isChecked) resources.getString(R.string.lowest_to_highest) else resources.getString(R.string.highest_to_lowest)
                            SortByStruct.MOVEMENT -> if (isChecked) resources.getString(R.string.debts_first) else resources.getString(R.string.loans_first)
                            else                  -> ""
                        }
                }
                
                textViewLowestToHighest.setOnClickListener { switchLowestToHighest.performClick() }
                
                spinnerMember.addOnItemSelectedListener(
                    onItemSelected = { _, _, position, _ -> sortByStruct.member = group.memberList[position] }
                )
                
                builder
                    .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                        transactionsAdapter.sort(sortByStruct)
                        writeToSharedPreferences(activity, sortByStruct)
                    }
                    .setNegativeButton(resources.getString(R.string.cancel), null)
                    .show()
            }
        }
        
        return super.onOptionsItemSelected(item)
    }

    private fun upload(t: Transaction, onCompleteListener: OnCompleteListener<Void?>? = null) =
        group.upload(t, onCompleteListener)
    
    fun notifyHeightChanged(newHeight: Int) {
        dialogDrawer.totalMaxHeight = newHeight
    }

    inner class TransactionsAdapter internal constructor(private val activity: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == 0)
                DividerHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_date_divider, parent, false))
            else
                TransactionHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_transaction_plus, parent, false))

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        override fun onBindViewHolder(rVHolder: RecyclerView.ViewHolder, pos: Int) {
            val position = getListPosition(pos)
            val transaction: Transaction = group.transactionList[position]!!
            
            if (rVHolder.itemViewType == 0 || pos == 0) {
                //region [DividerHolder]
                val holder = rVHolder as DividerHolder
                val dateName = "${DateFormat.format("EEEE", transaction.time).toString().toCapital()} ${transaction.day.toOrdinal()}"
                
                holder.textViewDateName.text = dateName
                
                if (group.transactionList.sortType != SortByStruct.TIME
                    || position != 0 && group.transactionList.list[position - 1].day == transaction.day
                    || !displayDividers) {
                    holder.imageViewDivider.alpha      = .5f
                    holder.imageViewDivider.visibility = if (pos == 0) View.GONE else View.VISIBLE
                    holder.textViewDateName.visibility = View.GONE
                } else {
                    holder.imageViewDivider.alpha      = 1f
                    holder.textViewDateName.visibility = View.VISIBLE
                    holder.imageViewDivider.visibility = View.VISIBLE
                }
                //endregion
            } else {
                //region [TransactionHolder]
                val holder = rVHolder as TransactionHolder

                holder.imageViewLeftSwipeCircle.visibility =
                    if (valLeftSwipe == -1 || valLeftSwipe == 2 && transaction.movement == PAYS)
                        View.GONE
                    else
                        View.VISIBLE
                
                holder.imageViewRightSwipeCircle.visibility =
                    if (valRightSwipe == -1 || valRightSwipe == 2 && transaction.movement == PAYS)
                        View.GONE
                    else
                        View.VISIBLE
                
                when (valLeftSwipe) {
                    0    -> holder.imageViewLeftSwipeCircle.setColorFilter(R.color.colorEdit)
                    1    -> holder.imageViewLeftSwipeCircle.setColorFilter(R.color.colorDuplicate)
                    2    -> holder.imageViewLeftSwipeCircle.setColorFilter(R.color.colorPayment)
                    3    -> holder.imageViewLeftSwipeCircle.setColorFilter(R.color.colorDelete)
                    else -> holder.imageViewLeftSwipeCircle.visibility = View.GONE
                }
                when (valRightSwipe) {
                    0    -> holder.imageViewRightSwipeCircle.setColorFilter(R.color.colorEdit)
                    1    -> holder.imageViewRightSwipeCircle.setColorFilter(R.color.colorDuplicate)
                    2    -> holder.imageViewRightSwipeCircle.setColorFilter(R.color.colorPayment)
                    3    -> holder.imageViewRightSwipeCircle.setColorFilter(R.color.colorDelete)
                    else -> holder.imageViewRightSwipeCircle.visibility = View.GONE
                }
                
                holder.textViewTimestamp.text =
                    if (displayDividers && group.transactionList.sortType == SortByStruct.TIME)
                        transaction.hour
                    else
                        transaction.timestamp
                
                holder.textViewDescription.text = transaction.description

                //region [Handle transactionConstraintLayout: display description]
                holder.transactionConstraintLayout.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP && !transaction.description.isBlank())
                        Snackbar.make(activity.findViewById(android.R.id.content), transaction.description, Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.dismiss)) {}
                            .show()
                    false
                }
                //endregion
    
                holder.imageViewDebtorIcon.setImageResource(getMemberIcon(transaction.debtor.type))
                
                holder.imageViewDebtorIcon.setColorFilter(transaction.debtor.color)
                holder.imageViewDebtorIcon.postInvalidate()
                
                holder.textViewDebtor.text = transaction.debtor.name
                
                holder.imageViewDebtorLine.setColorFilter(transaction.debtor.color)
                
                holder.textViewAmount.text = transaction.displayAmount(group.currencySymbol)
    
                holder.textViewMovement.setText(if (transaction.movement == OWES) R.string.owes else R.string.pays)
                holder.textViewMovement.setTypeface(
                    null,
                    if (transaction.movement == OWES) Typeface.NORMAL else Typeface.BOLD
                )
                
                holder.textViewMovement.setTextColor(transaction.blendedColors(.5f))
    
                holder.imageViewLenderIcon.setImageResource(getMemberIcon(transaction.lender.type))
                
                holder.imageViewLenderIcon.setColorFilter(transaction.lender.color)
                holder.imageViewLenderIcon.postInvalidate()
                
                holder.textViewLender.text = transaction.lender.name
                
                holder.imageViewLenderLine.setColorFilter(transaction.lender.color)
                //endregion
            }
        }

        override fun getItemCount(): Int =
            group.transactionList.size * 2

        override fun getItemViewType(position: Int): Int =
            position % 2

        fun getListPosition(adapterPosition: Int): Int =
            adapterPosition / 2

        fun sort(sortByStruct: SortByStruct) {
            group.transactionList.sort(sortByStruct)
            notifyDataSetChanged()
        }

        internal inner class DividerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textViewDateName: TextView  by itemView.bindView(R.id.textViewDateName)
            val imageViewDivider: ImageView by itemView.bindView(R.id.imageViewDivider)
        }

        inner class TransactionHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), OnCreateContextMenuListener {
            val transactionConstraintLayout: ConstraintLayout by itemView.bindView(R.id.constraintLayout_transaction)
            val imageViewLeftSwipeCircle: ImageView           by itemView.bindView(R.id.imageViewLeftSwipeCircle)
            val imageViewRightSwipeCircle: ImageView          by itemView.bindView(R.id.imageViewRightSwipeCircle)
            val textViewTimestamp: TextView                   by itemView.bindView(R.id.textViewTimestamp)
            val textViewDescription: TextView                 by itemView.bindView(R.id.textViewDescription)
            val imageViewDebtorIcon: ImageView                by itemView.bindView(R.id.imageViewDebtorIcon)
            val textViewDebtor: TextView                      by itemView.bindView(R.id.textViewDebtor)
            val imageViewDebtorLine: ImageView                by itemView.bindView(R.id.imageViewDebtorLine)
            val textViewAmount: TextView                      by itemView.bindView(R.id.textViewAmount)
            val textViewMovement: TextView                    by itemView.bindView(R.id.textViewMovement)
            val imageViewLenderIcon: ImageView                by itemView.bindView(R.id.imageViewLenderIcon)
            val textViewLender: TextView                      by itemView.bindView(R.id.textViewLender)
            val imageViewLenderLine: ImageView                by itemView.bindView(R.id.imageViewLenderLine)
            
            override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
                menu.add(Menu.NONE, 0, 0, resources.getString(R.string.modify)).setOnMenuItemClickListener(onChange)
                menu.add(Menu.NONE, 1, 1, resources.getString(R.string.duplicate)).setOnMenuItemClickListener(onChange)
                
                if (group.transactionList.list[getListPosition(adapterPosition)].movement == OWES)
                    menu.add(Menu.NONE, 2, 2, resources.getString(R.string.register_payment)).setOnMenuItemClickListener(onChange)
                
                menu.add(Menu.NONE, 3, 3, resources.getString(R.string.delete)).setOnMenuItemClickListener(onChange)
            }

            private val onChange = MenuItem.OnMenuItemClickListener { item ->
                val position = getListPosition(adapterPosition)
                val t = group.transactionList.list[position]
                
                when (item.itemId) {
                    0 -> {
                        editTransaction(position)
                        return@OnMenuItemClickListener true
                    }
                    1 -> {
                        duplicateTransaction(t)
                        return@OnMenuItemClickListener true
                    }
                    2 -> {
                        payTransaction(t)
                        return@OnMenuItemClickListener true
                    }
                    3 -> {
                        deleteTransaction(position)
                        return@OnMenuItemClickListener true
                    }
                }
                false
            }

            init {
                transactionConstraintLayout.setOnCreateContextMenuListener(this)
            }
        }

    }

    internal inner class SwipeToDeleteCallback(
        context: Context,
        private val mAdapter: TransactionsAdapter,
        private val leftSwipe: Int,
        private val rightSwipe: Int
    ) : ItemTouchHelper.Callback() {
        private val mClearPaint: Paint = Paint()
        private val mBackground: ColorDrawable = ColorDrawable()
        private var prevDX: Float
        private val intrinsicWidth: Int
        private val intrinsicHeight: Int
        private val editBgColor: Int
        private val editDrawable: Drawable?
        private val duplicateBgColor: Int
        private val duplicateDrawable: Drawable?
        private val paymentBgColor: Int
        private val paymentDrawable: Drawable?
        private val deleteBgColor: Int
        private val deleteDrawable: Drawable?
        
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val adapterPosition = viewHolder.adapterPosition
            
            Log.wtf("adapter position $adapterPosition", "list position " + mAdapter.getListPosition(adapterPosition))
            
            if (adapterPosition == -1 || adapterPosition % 2 == 0)
                return makeMovementFlags(0, 0)
            
            val t = group.transactionList.list[mAdapter.getListPosition(adapterPosition)]
            
            return makeMovementFlags(0,
                    if (rightSwipe == -1 || rightSwipe == 2 && t.movement == PAYS)
                        0
                    else
                        ItemTouchHelper.LEFT
                or
                    if (leftSwipe == -1 || leftSwipe == 2 && t.movement == PAYS)
                        0
                    else
                        ItemTouchHelper.RIGHT
            )
        }

        override fun onMove(rV: RecyclerView, vH: RecyclerView.ViewHolder, vH1: RecyclerView.ViewHolder): Boolean =
            false

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.height
            
            clearCanvas(c, itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            
            if (viewHolder.adapterPosition == -1)
                return
            
            val t = group.transactionList.list[mAdapter.getListPosition(viewHolder.adapterPosition)]
            val isCancelled = (dX == 0f && !isCurrentlyActive || (if (dX > 0) leftSwipe else rightSwipe) == -1
                               || (if (dX > 0) leftSwipe else rightSwipe) == 2 && t.movement == PAYS)
            
            if (isCancelled) {
                clearCanvas(c, itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
                clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                return
            }
            
            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconMargin = (itemHeight - intrinsicHeight) / 2
            var iconLeft = 0
            var iconRight = 0
            val iconBottom = iconTop + intrinsicHeight
            var drawable = editDrawable
            
            when (if (dX > 0) leftSwipe else rightSwipe) {
                0 -> {
                    mBackground.color = editBgColor
                    drawable = editDrawable
                }
                1 -> {
                    mBackground.color = duplicateBgColor
                    drawable = duplicateDrawable
                }
                2 -> {
                    mBackground.color = paymentBgColor
                    drawable = paymentDrawable
                }
                3 -> {
                    mBackground.color = deleteBgColor
                    drawable = deleteDrawable
                }
            }
            
            if (dX > 0) {
                mBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                iconRight = itemView.left + iconMargin + intrinsicWidth
                iconLeft = itemView.left + iconMargin
            } else if (dX < 0) {
                mBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                iconLeft = itemView.right - iconMargin - intrinsicWidth
                iconRight = itemView.right - iconMargin
            }
            
            mBackground.draw(c)
            drawable!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            drawable.draw(c)
            prevDX = dX
            
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) =
            c.drawRect(left, top, right, bottom, mClearPaint)

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.5f

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
            val position = mAdapter.getListPosition(viewHolder.adapterPosition)
            val transaction = group.transactionList.list[position]
            
            mAdapter.notifyDataSetChanged()
            
            when (if (prevDX > 0) leftSwipe else rightSwipe) {
                0 -> editTransaction(position)
                1 -> duplicateTransaction(transaction)
                2 -> payTransaction(transaction)
                3 -> deleteTransaction(position)
            }
        }

        init {
            mClearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            prevDX = 0f
            
            editBgColor = ContextCompat.getColor(context, R.color.colorEdit)
            editDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pencil_white_24dp)!!
            
            duplicateBgColor = ContextCompat.getColor(context, R.color.colorDuplicate)
            duplicateDrawable = ContextCompat.getDrawable(context, R.drawable.ic_duplicate_white_24dp)
            paymentBgColor = ContextCompat.getColor(context, R.color.colorPayment)
            paymentDrawable = ContextCompat.getDrawable(context, R.drawable.ic_payment_white_24dp)
            deleteBgColor = ContextCompat.getColor(context, R.color.colorDelete)
            deleteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_trash_white_24dp)
            
            intrinsicWidth = editDrawable.intrinsicWidth
            intrinsicHeight = editDrawable.intrinsicHeight
        }
    }

    private fun editTransaction(position: Int) {
        val transaction = group.transactionList.list[position]
        val memberNameList = getNameList(group.memberList).toMutableList()
        
        if (transaction.debtor.name !in memberNameList) {
            memberNameList.add(transaction.debtor.name)
            group.memberList.add(Member(transaction.debtor.name))
        }
        
        if (transaction.lender.name !in memberNameList) {
            memberNameList.add(transaction.lender.name)
            group.memberList.add(Member(transaction.lender.name))
        }
        
        val dialog = activity.layoutInflater.inflate(R.layout.dialog_create_transaction, null)
        val builder = AlertDialog.Builder(activity)
            .setTitle(resources.getString(R.string.edit_transaction))
            .setView(dialog)
        
        val editTextDescription: EditText = dialog.findViewById(R.id.editTextDescription)
        val spinnerDebtor: Spinner        = dialog.findViewById(R.id.spinnerDebtor)
        val editTextAmount: EditText      = dialog.findViewById(R.id.editTextAmount)
        val toggleMovement: ToggleView = dialog.findViewById(R.id.toggleMovement)
        val spinnerLender: Spinner        = dialog.findViewById(R.id.spinnerLender)
        
        dialog.findViewById<View>(R.id.linearLayout_pickers).visibility = View.GONE
        editTextDescription.setText(transaction.description)
    
        spinnerDebtor.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
            Util.addToBeginningOfArray(memberNameList.toTypedArray(), resources.getString(R.string.debtor)))
        
        spinnerLender.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
            Util.addToBeginningOfArray(memberNameList.toTypedArray(), resources.getString(R.string.lender)))
    
        editTextAmount.setText(transaction.amount.toString())
    
        spinnerDebtor.setSelection(memberNameList.indexOf(transaction.debtor.name) + 1)
        spinnerLender.setSelection(memberNameList.indexOf(transaction.lender.name) + 1)
        
        toggleMovement.setSelection(ToggleView.ENTRY_FIRST)
        
        var shouldRefresh = true
        
        builder
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                shouldRefresh = false
                val t = Transaction(
                    editTextDescription.text.toString(),
                    group.memberList.findMember(spinnerDebtor.selectedItem.toString()),
                    group.memberList.findMember(spinnerLender.selectedItem.toString()),
                    editTextAmount.text.toString().toFloat(),
                    transaction.time,
                    toggleMovement.selectedEntry == ToggleView.ENTRY_FIRST
                )
            
                t.nodeId = transaction.nodeId
                upload(t)
            }
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .setOnDismissListener { if (shouldRefresh) transactionsAdapter.notifyDataSetChanged() }
        
        val alertDialog = builder.create()
        alertDialog.show()
        
        val buttonAdd = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        buttonAdd.setTextColor(resources.getColor(R.color.color4))
        
        spinnerDebtor.addOnItemSelectedListener(
            onItemSelected = { _, _, i, _ ->
                buttonAdd.isEnabled = (i != 0 && i != spinnerLender.selectedItemPosition
                                       && !(editTextAmount.text.toString() == "" || editTextAmount.text.toString().toFloat() == 0f))
    
                buttonAdd.setTextColor(if (buttonAdd.isEnabled) resources.getColor(R.color.color4) else resources.getColor(R.color.colorRedDisabled))
            }
        )
        
        editTextAmount.addTextChangedListener(
            afterTextChanged = { s ->
                if (s!!.toString().trim { it <= ' ' } == ".")
                    s.insert(0, "0")
                
                buttonAdd.isEnabled = (!(s.isBlank() || s.toString().toFloat() == 0f) && spinnerDebtor.selectedItemPosition != 0
                                       && spinnerLender.selectedItemPosition != 0 && spinnerDebtor.selectedItemPosition != spinnerLender.selectedItemPosition)
                
                buttonAdd.setTextColor(if (buttonAdd.isEnabled) resources.getColor(R.color.color4) else resources.getColor(R.color.colorRedDisabled))
            }
        )
        
        spinnerLender.addOnItemSelectedListener(
            onItemSelected = { _, _, i, _ ->
                buttonAdd.isEnabled = (i != 0 && i != spinnerDebtor.selectedItemPosition
                                       && !(editTextAmount.text.toString() == "" || editTextAmount.text.toString().toFloat() == 0f))
                
                buttonAdd.setTextColor(if (buttonAdd.isEnabled) resources.getColor(R.color.color4) else resources.getColor(R.color.colorRedDisabled))
            }
        )
    }

    private fun duplicateTransaction(transaction: Transaction) {
        val t = transaction.copy()
        t.time = System.currentTimeMillis()
        
        t.description = if (t.description.isBlank()) getString(R.string.duplicate) else "${t.description} - Duplicate"
        
        upload( t, OnCompleteListener { editTransaction(group.transactionList.indexOf(t)) } )
    }

    private fun payTransaction(transaction: Transaction) {
        if (transaction.movement == PAYS)
            return
    
        val t = transaction.copy()
    
        t.movement = PAYS
        t.time = if (paymentSetCurDate) System.currentTimeMillis() else t.time + 1
        
        group.upload(t, null)
    }

    private fun deleteTransaction(position: Int) {
        val transaction = group.transactionList.list[position]
        group.remove(transaction, OnCompleteListener {
            Snackbar
                .make(activity.findViewById(android.R.id.content), resources.getString(R.string.transaction_removed_from_the_list), Snackbar.LENGTH_INDEFINITE)
                .apply {
                    setAction(resources.getString(R.string.undo)) { group.upload(transaction) }
                    setActionTextColor(Color.YELLOW)
                    show()
                }
        })
    }
    
    @SuppressLint("ClickableViewAccessibility")
    inner class SplitBillMembersAdapter internal constructor (
        private val context: Context
    ) : RecyclerView.Adapter<SplitBillMembersAdapter.ViewHolder>() {
        lateinit var transactionsAdapter: SplitBillTransactionsAdapter
        private var recyclerView: RecyclerView? = null
        val amountMap: ListenerHashMap<String, Float> = ListenerHashMap()
        private val debtsMap: MutableMap<String, Float> = mutableMapOf()
        
        fun setDataListener(dataListener: DataListener?) {
            amountMap.setDataListener(dataListener)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.layout_split_bill_member, parent, false)
            return ViewHolder(view)
        }
        
        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val member = group.memberList[position]
            
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    holder.imageViewMemberIcon.setColorFilter(member.color)
                    holder.editTextAmount.isEnabled = true
                    holder.editTextAmount.setHint(R.string.amount)
                    holder.editTextAmount.setText(String.format(Locale.getDefault(), "%d", 0))
                    amountMap[member.name] = 0f
                } else {
                    holder.imageViewMemberIcon.setColorFilter(R.color.colorGray)
                    holder.editTextAmount.isEnabled = false
                    holder.editTextAmount.hint = ""
                    holder.editTextAmount.setText("")
                    amountMap.remove(member.name)
                }
            }
            
            holder.textViewMemberName.text = member.name
            holder.textViewCurrency.text = group.currencySymbol
            holder.editTextAmount.isEnabled = false
            
            holder.editTextAmount.addTextChangedListener(
                afterTextChanged = { s ->
                    val amt: Float? =
                        try {
                            s.toString().trim { it <= ' ' }.toFloat()
                        } catch (e: Exception) {
                            null
                        }
                    
                    if (amt != null)
                        amountMap[member.name] = amt
                    else if (s.isNullOrBlank())
                        amountMap[member.name] = 0f
                }
            )
            
            holder.imageViewPay.setColorFilter(member.color)
            holder.imageViewPay.setOnTouchListener(object : OnTouchImageViewListener(member.color) {
                override fun onTrigger() {
                    val amount = debtsMap[member.name]!!
                    
                    if (debtsMap[member.name] == null || debtsMap[member.name]!! >= 0f)
                        transactionsAdapter.addItem(debtor = member.name, amount = abs(amount))
                    else
                        transactionsAdapter.addItem(lender = member.name, amount = abs(amount))
                }
            })
            
            holder.imageViewPay.visibility = INVISIBLE
            holder.textViewHasToPay.visibility = INVISIBLE
        }

        override fun getItemCount() = group.memberList.size

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            this.recyclerView = recyclerView
        }

        @SuppressLint("StringFormatMatches")
        fun update(amountEach: Float) {
            for (i in 0 until itemCount) {
                val holder =
                    recyclerView!!.findViewHolderForAdapterPosition(i) as ViewHolder? ?: continue
                
                val memberName = group.memberList[i].name
                val amountPaid = amountMap[memberName]
                
                if (amountPaid == null) {
                    holder.imageViewPay.visibility = View.INVISIBLE
                    holder.textViewHasToPay.visibility = View.INVISIBLE
                    continue
                }
                
                val amountToPay = amountEach - amountPaid
                if (amountToPay == 0f) {
                    holder.imageViewPay.visibility = View.INVISIBLE
                    holder.textViewHasToPay.visibility = View.INVISIBLE
                    continue
                }
                
                val strAmountToPay = "${group.currencySymbol}${roundToDecimalPlace(abs(amountToPay), 2)}"
                
                if (amountToPay >= 0) {
                    holder.textViewHasToPay.text = resources.getString(R.string.has_to_pay_, strAmountToPay)
                    holder.textViewHasToPay.setTypeface(null, Typeface.NORMAL)
                } else {
                    holder.textViewHasToPay.text = resources.getString(R.string.must_be_paid_, strAmountToPay)
                    holder.textViewHasToPay.setTypeface(null, Typeface.BOLD)
                }
    
                holder.imageViewPay.visibility = View.VISIBLE
                holder.textViewHasToPay.visibility = View.VISIBLE
                
                debtsMap[memberName] = amountToPay
            }
        }

        fun refresh() {
            for (i in 0 until itemCount) {
                val holder = recyclerView!!.findViewHolderForAdapterPosition(i) as ViewHolder
                
                if (holder.checkBox.isChecked)
                    holder.checkBox.performClick()
            }
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkBox: CheckBox             by itemView.bindView(R.id.checkBox)
            val imageViewMemberIcon: ImageView by itemView.bindView(R.id.imageViewMemberIcon)
            val textViewMemberName: TextView   by itemView.bindView(R.id.textViewMemberName)
            val textViewCurrency: TextView     by itemView.bindView(R.id.textViewCurrency)
            val editTextAmount: EditText       by itemView.bindView(R.id.editTextAmount)
            val imageViewPay: ImageView        by itemView.bindView(R.id.imageViewPay)
            val textViewHasToPay: TextView     by itemView.bindView(R.id.textViewHasToPay)
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    inner class SplitBillTransactionsAdapter internal constructor (
        private val context: Context
    ) : RecyclerView.Adapter<SplitBillTransactionsAdapter.ViewHolder>() {
        lateinit var membersAdapter: SplitBillMembersAdapter
        private var recyclerView: RecyclerView? = null
        val tList: MutableList<Quadruple<String?, String?, Boolean, Float>> = mutableListOf()
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.layout_split_bill_transaction, parent, false)
            return ViewHolder(view)
        }
    
        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val quadruple = tList[position]
            val debtorName: String? = quadruple.first
            val lenderName: String? = quadruple.second
            val movement: Boolean   = quadruple.third
            val amount: Float       = quadruple.fourth
            
            holder.buttonRemove.setOnClickListener { removeItem(position) }
            holder.spinnerDebtor.adapter = aADebtor
            holder.spinnerDebtor.setSelection(
                if (debtorName == null)
                    0
                else
                    group.memberList.indexOfFirst { it.name == debtorName } + 1
            )
            holder.spinnerDebtor.addOnItemSelectedListener(
                onItemSelected = { _, _, i, _ ->
                    quadruple.first = holder.spinnerDebtor.getItemAtPosition(i) as String
                    notifyChange()
                }
            )
    
            holder.spinnerLender.adapter = aALender
            holder.spinnerLender.setSelection(
                if (lenderName == null)
                    0
                else
                    group.memberList.indexOfFirst { it.name == lenderName } + 1
            )
            holder.spinnerLender.addOnItemSelectedListener(
                onItemSelected = { _, _, i, _ ->
                    quadruple.second = holder.spinnerLender.getItemAtPosition(i) as String
                    notifyChange()
                }
            )
            
            holder.toggleMovement.setSelection(movement)
            holder.toggleMovement.onToggle = { entry -> quadruple.third = entry }
            
            holder.textViewCurrency.text = group.currencySymbol
            holder.editTextAmount.setText(amount.toString())
            holder.editTextAmount.addTextChangedListener(
                afterTextChanged = { s ->
                    val amt: Float? =
                        try {
                            s.toString().trim { it <= ' ' }.toFloat()
                        } catch (e: Exception) {
                            null
                        }
                    
                    amt?.let {
                        quadruple.fourth = amt
                    }
                    
                    notifyChange()
                }
            )
        }
        
        override fun getItemCount() = tList.size
        
        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            this.recyclerView = recyclerView
        }
        
        fun addItem(debtor: String? = null, lender: String? = null, amount: Float = 0f) {
            tList.add(Quadruple(debtor, lender, PAYS, amount))
            notifyItemInserted(tList.size)
            
            dialogDrawer.fullScroll(ScrollView.FOCUS_DOWN)
            notifyChange()
        }
        
        fun removeItem(position: Int) {
            tList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, tList.size)
            
            notifyChange()
        }
        
        fun isContentLegal(): Boolean {
            val membersChecked = membersAdapter.amountMap.keys
            
            if (membersChecked.size < 2 || tList.size == 0)
                return false
            
            tList.forEach {
                if (it.first == null
                    || it.first !in membersChecked
                    || it.second == null
                    || it.second !in membersChecked
                    || it.first == it.second
                    || it.fourth == 0f)
                    return@isContentLegal false
            }
            
            return true
        }
        
        fun notifyChange() {
            dialogDrawer.setButtonEnabled(DialogInterface.BUTTON_POSITIVE, isContentLegal())
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val buttonRemove: Button           by itemView.bindView(R.id.buttonRemove)
            val spinnerDebtor: Spinner         by itemView.bindView(R.id.spinnerDebtor)
            val spinnerLender: Spinner         by itemView.bindView(R.id.spinnerLender)
            val toggleMovement: ToggleView by itemView.bindView(R.id.toggleMovement)
            val textViewCurrency: TextView     by itemView.bindView(R.id.textViewCurrency)
            val editTextAmount: EditText       by itemView.bindView(R.id.editTextAmount)
        }
    }
}