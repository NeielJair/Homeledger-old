package com.njair.homeledger.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.njair.homeledger.GroupMain
import com.njair.homeledger.R
import com.njair.homeledger.extensions.Database
import com.njair.homeledger.extensions.Util.Companion.colors
import com.njair.homeledger.extensions.bindView
import com.njair.homeledger.extensions.toHex
import com.njair.homeledger.service.Group
import com.njair.homeledger.service.Member
import com.njair.homeledger.service.OnTouchImageViewListener
import petrov.kristiyan.colorpicker.ColorPicker
import petrov.kristiyan.colorpicker.ColorPicker.OnFastChooseColorListener
import java.util.*
import kotlin.collections.ArrayList

class MembersFragment : Fragment() {
    private val activity: Activity by lazy { requireActivity() }
    private lateinit var roomKey: String
    
    val membersAdapter: MembersAdapter by lazy { MembersAdapter(activity) }
    
    private lateinit var root: View
    private val textViewDisplay: TextView  by lazy { root.findViewById<TextView>(R.id.textViewDisplay) }
    private val textViewDisplay2: TextView by lazy { root.findViewById<TextView>(R.id.textViewDisplay2) }
    
    private lateinit var group: Group
    private val colorList = colors
    
    private var groupRef: DatabaseReference? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_members, container, false)

        roomKey = (activity as GroupMain).roomKey
        groupRef = Database.REFERENCE.child(Database.GROUPS).child(roomKey)
        
        val membersRecyclerView: RecyclerView = root.findViewById(R.id.recyclerview_members)
        
        membersRecyclerView.adapter = membersAdapter
        membersRecyclerView.layoutManager = LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false)
        membersRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        //region [Manage FAB]
        val fabAdd: FloatingActionButton = root.findViewById(R.id.fabAdd)
        
        fabAdd.setOnClickListener { view ->
            if (group.memberList.size < colorList.size)
                AlertDialog.Builder(activity).apply {
                    setTitle(resources.getString(R.string.add_member))
    
                    val input = EditText(activity)
                    input.hint = resources.getString(R.string.member_name)
                    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
    
                    setView(input)
                    setPositiveButton(resources.getString(R.string.add)) { _, _ ->
                        val inputName = input.text.toString().trim { it <= ' ' }
        
                        if (checkNameAvailable(inputName))
                            Member(inputName, randomAvailableColor()).upload(roomKey)
                        else
                            Snackbar.make(view, resources.getString(R.string.member_name_not_available), Snackbar.LENGTH_LONG).show()
                    }
                    setNegativeButton(resources.getString(R.string.cancel), null)
                    show()
                }
            else
                Snackbar.make(view, getString(R.string.maximum_amount_of_members_reached), Snackbar.LENGTH_LONG).show()
        }
        
        membersRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy > 0 && fabAdd.visibility == View.VISIBLE)
                    fabAdd.hide()
                else if (dy < 0 && fabAdd.visibility != View.VISIBLE)
                    fabAdd.show()
            }
        })
        //endregion
        return root
    }

    fun setGroup(group: Group) {
        if(this::group.isInitialized)
            this.group.detach()
        
        this.group = group
        roomKey = group.key
    }

    fun updateUI() {
        val displayVisibility = if (membersAdapter.itemCount == 0) View.VISIBLE else View.GONE
        
        textViewDisplay.visibility  = displayVisibility
        textViewDisplay2.visibility = displayVisibility
    }

    private fun checkColorAvailable(color: Int): Boolean {
        for (member: Member in group.memberList) {
            if (member.color == color)
                return false
        }
        return true
    }

    private fun checkNameAvailable(name: String): Boolean {
        for (member: Member in group.memberList) {
            if ((member.name.trim { it <= ' ' } == name.trim { it <= ' ' })) {
                return false
            }
        }
        return true
    }

    private fun randomAvailableColor(): Int {
        val colorListLength = colorList.size
        if (group.memberList.size < colorListLength) {
            val randomInt = Random().nextInt(colorListLength)
            var i = randomInt
            while (i != randomInt - 1) {
                if (checkColorAvailable(colorList[i]))
                    return colorList[i]
                
                i = (i + 1) % colorListLength
            }
            
            if (checkColorAvailable(colorList[randomInt - 1]))
                return colorList[randomInt - 1]
        }
        return Color.BLACK
    }

    inner class MembersAdapter(private val context: Context) : RecyclerView.Adapter<MembersAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.layout_member, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val member = group.memberList[position]
            val iVLink = holder.imageViewLink
            member.invalidateDrawable()
            //holder.imageViewMemberIcon.setImageResource(getMemberIcon(member.type))
            holder.imageViewMemberIcon.setImageDrawable(member.drawable)
//            holder.imageViewMemberIcon.setColorFilter(member.color)
            holder.imageViewMemberIcon.postInvalidate()
            
            holder.imageViewMemberStar.visibility = if ((member.isAdmin)) View.VISIBLE else View.GONE
            holder.imageViewMemberStar.setColorFilter(member.color * 2 / 3)
            
            holder.textViewMemberName.text = member.name

            //region [Handle imageViewLink: link member to user]
            if (member.user.uid != null && member.user.uid != "")
                iVLink.visibility = View.GONE
            
            iVLink.setColorFilter(member.color)
            iVLink.setOnTouchListener(object : OnTouchImageViewListener(member.color) {
                override fun onTrigger() {
                    AlertDialog.Builder(activity)
                        .setMessage(String.format("Do you want to link the member '%s' with your user account?", member.name)) //TODO Extract string
                        .setPositiveButton(R.string.ok) { _, _ ->
                            //TODO Handle Link Event
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
                }
            })
            //endregion

            //region [Handle imageViewTrash: delete member]
            val iVTrash = holder.imageViewTrash
            
            iVTrash.setColorFilter(member.color)
            iVTrash.setOnTouchListener(object : OnTouchImageViewListener(member.color) {
                override fun onTrigger() {
                    AlertDialog.Builder(activity)
                        .setMessage(String.format(resources.getString(R.string.delete_member_), member.name))
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            if (!member.nodeId.isNullOrBlank())
                                member.remove(roomKey, OnCompleteListener<Void?> {
                                    Snackbar.make(requireActivity().findViewById(android.R.id.content), resources.getString(R.string.member__deleted, member.name), Snackbar.LENGTH_LONG)
                                        .setAction(resources.getString(R.string.undo)) { member.upload(roomKey) }
                                        .setActionTextColor(member.color / 3)
                                        .show()
                                })
                            else
                                Toast.makeText(context, resources.getString(R.string.an_error_has_occurred) + "member doesn't exist in database, try refreshing.",
                                    Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton(resources.getString(R.string.no), null)
                        .show()
                }
            })
            //endregion

            //region [Handle imageViewBrush: change member color]
            val iVBrush = holder.imageViewBrush
            iVBrush.setColorFilter(member.color)
            iVBrush.setOnTouchListener(object : OnTouchImageViewListener(member.color) {
                override fun onTrigger() {
                    if (group.memberList.size < colorList.size)
                        ColorPicker(activity).apply {
                            setTitle(resources.getString(R.string.choose_a_color_for_, member.name))
                            setColors(ArrayList( removeColorsFromArrayToHex (colorList) ))
                            
                            setOnFastChooseColorListener(object : OnFastChooseColorListener {
                                override fun setOnFastChooseColorListener(i: Int, color: Int) {
                                    if (color != Color.WHITE) {
                                        member.color = color
                                        member.upload(roomKey, null)
                                    }
                                }
        
                                override fun onCancel() {}
                            })
                            
                            setRoundColorButton(true)
                            show()
                        }
                    else
                        Snackbar.make(activity.findViewById(R.id.content), resources.getString(R.string.no_colors_are_available), Snackbar.LENGTH_LONG)
                            .show()
                }
            })
            //endregion
        }

        private fun removeColorsFromArrayToHex(list: IntArray) =
            list.map { if (checkColorAvailable(it)) it.toHex() else null }.filterNotNull()
    
        override fun getItemCount(): Int = if (this@MembersFragment::group.isInitialized) group.memberList.size else 0

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageViewMemberIcon: ImageView by itemView.bindView(R.id.imageViewMemberIcon)
            val imageViewMemberStar: ImageView by itemView.bindView(R.id.imageViewMemberStar)
            val textViewMemberName: TextView   by itemView.bindView(R.id.textViewMemberName)
            val imageViewLink: ImageView       by itemView.bindView(R.id.imageViewLink)
            val imageViewTrash: ImageView      by itemView.bindView(R.id.imageViewTrash)
            val imageViewBrush: ImageView      by itemView.bindView(R.id.imageViewBrush)
        }
    }
}