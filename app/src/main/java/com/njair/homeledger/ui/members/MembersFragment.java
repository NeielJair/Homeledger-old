package com.njair.homeledger.ui.members;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njair.homeledger.GroupMain;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Group;
import com.njair.homeledger.Service.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MembersFragment extends Fragment {

    private String roomKey;

    public MembersAdapter membersAdapter;
    private TextView textViewDisplay;
    private TextView textViewDisplay2;
    private boolean hasDownloaded = false;

    public Resources r;

    public Group group;
    public DatabaseReference groupRef;

    private MembersViewModel membersViewModel;
    private final int[] colorList = {Color.RED, Color.BLUE, Color.rgb(0, 0, 128),
            Color.rgb(204, 0, 204), Color.rgb(0, 128, 0), Color.rgb(204,204,0),
            Color.rgb(255, 165, 0), Color.rgb(128,0,128), Color.rgb(204, 0, 102), Color.rgb(218,165,32)};

    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        membersViewModel =
                ViewModelProviders.of(this).get(MembersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_members, container, false);
        /*final TextView textView = root.findViewById(R.id.text_notifications);
        membersViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });*/

        r = getResources();

        roomKey = ((GroupMain) getActivity()).getRoomKey();
        groupRef = FirebaseDatabase.getInstance().getReference().child("groups").child(roomKey);

        RecyclerView membersRecyclerView = root.findViewById(R.id.recyclerview_members);
        membersAdapter = new MembersAdapter(getActivity());
        membersRecyclerView.setAdapter(membersAdapter);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        membersRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        textViewDisplay = root.findViewById(R.id.textViewDisplay);
        textViewDisplay2 = root.findViewById(R.id.textViewDisplay2);
        textViewDisplay.setVisibility(View.INVISIBLE);
        textViewDisplay2.setVisibility(View.INVISIBLE);

        //region [Manage FAB]
        final FloatingActionButton fabAdd = root.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(group.memberList.size() < colorList.length){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(r.getString(R.string.add_member));

                    final EditText input = new EditText(getActivity());
                    input.setHint(r.getString(R.string.member_name));
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                    builder.setView(input);

                    builder.setPositiveButton(r.getString(R.string.add), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String inputName = input.getText().toString().trim();

                            if(checkNameAvailable(inputName))
                                new Member(inputName, randomAvailableColor()).upload(roomKey, null);
                            else
                                Snackbar.make(view, r.getString(R.string.member_name_not_available), Snackbar.LENGTH_LONG).show();
                        }
                    });
                    builder.setNegativeButton(r.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });

                    builder.show();
                    }
                else Snackbar.make(view, getString(R.string.maximum_amount_of_members_reached), Snackbar.LENGTH_LONG).show();
            }
        });

        membersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabAdd.getVisibility() == View.VISIBLE) {
                    fabAdd.hide();
                } else if (dy < 0 && fabAdd.getVisibility() != View.VISIBLE) {
                    fabAdd.show();
                }
            }
        });
        //endregion

        return root;
    }

    public void setGroup(Group group){
        this.group = group;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean checkColorAvailable(int color){
        for(Member member : group.memberList){
            if(member.color == color)
                return false;
        }

        return true;
    }

    private boolean checkNameAvailable(String name){
        for(Member member : group.memberList){
            if(member.getName().trim().equals(name.trim())){
                return false;
            }
        }

        return true;
    }

    private int randomAvailableColor(){
        int colorListLength = colorList.length;

        if(group.memberList.size() < colorListLength){
            int color;
            int randomInt = new Random().nextInt(colorListLength);

            for(int i = randomInt; i != randomInt - 1; i = (i + 1) % colorListLength){
                if(checkColorAvailable(colorList[i]))
                    return colorList[i];
            }

            if(checkColorAvailable(colorList[randomInt - 1]))
                return colorList[randomInt - 1];
        }

        return Color.BLACK;
    }

    public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {
        private Context context;

        public MembersAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.layout_member, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).slideUp();
            //members.get(position).adaptView((TransactionLayout) holder.view);

            final Member member = group.memberList.get(position);

            holder.textViewMemberName.setText(member.getName());
            drawableChangeTint(holder.textViewMemberName.getCompoundDrawables(), member.color);

            //region [Handle imageViewTrash: delete member]
            final ImageView IVTrash = holder.imageViewTrash;
            IVTrash.setColorFilter(member.color);
            IVTrash.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            IVTrash.setColorFilter(member.color/3);
                            IVTrash.invalidate();
                            break;
                        }
                        case MotionEvent.ACTION_UP:
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(String.format(r.getString(R.string.delete_member_), member.getName()))
                                    .setPositiveButton(r.getString(R.string.yes), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if(member.getNodeId() != null && !member.getNodeId().equals("")){
                                                groupRef.child("members").child(member.getNodeId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Snackbar.make(requireActivity().findViewById(android.R.id.content),
                                                                String.format(r.getString(R.string.member__deleted), member.getName()), Snackbar.LENGTH_LONG)
                                                                .setAction(r.getString(R.string.undo), new View.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(View v) {
                                                                        member.upload(roomKey, null);
                                                                    }
                                                                })
                                                                .setActionTextColor(member.color/3)
                                                                .show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(context, r.getString(R.string.an_error_has_occurred) + "member doesn't exist in database, try refreshing.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    })
                                    .setNegativeButton(r.getString(R.string.no), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {}
                                    }).show();

                        case MotionEvent.ACTION_CANCEL: {
                            IVTrash.setColorFilter(member.color);
                            IVTrash.invalidate();
                            break;
                        }
                    }

                    return false;
                }
            });
            //endregion

            //region [Handle imageViewBrush: change member color]
            final ImageView IVBrush = holder.imageViewBrush;
            IVBrush.setColorFilter(member.color);
            IVBrush.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:{
                            IVBrush.setColorFilter(member.color/3);
                            IVBrush.invalidate();
                            break;}

                        case MotionEvent.ACTION_UP:
                            IVBrush.setColorFilter(member.color);
                            IVBrush.invalidate();

                            if(group.memberList.size() < colorList.length){
                                ColorPicker colorPicker = new ColorPicker(getActivity());
                                colorPicker.setTitle(String.format(getResources().getString(R.string.choose_a_color_for_), member.getName()));
                                colorPicker.setColors(removeColorsFromArrayToHex(colorList));
                                colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                                    @Override
                                    public void setOnFastChooseColorListener(int i, int color) {
                                        if(color != Color.WHITE){
                                            member.setColor(color);
                                            member.upload(roomKey, null);
                                        }
                                    }

                                    @Override
                                    public void onCancel() {}
                                });

                                colorPicker.setRoundColorButton(true);
                                colorPicker.show();
                            } else
                                Snackbar.make(v, r.getString(R.string.no_colors_are_available), Snackbar.LENGTH_LONG).show();

                            break;

                        case MotionEvent.ACTION_CANCEL:
                            IVBrush.setColorFilter(member.color);
                            IVBrush.invalidate();
                            break;
                    }

                    return false;
                }
            });
            //endregion
        }

        private ArrayList<String> removeColorsFromArrayToHex(int[] list){
            ArrayList<String> newList = new ArrayList<>();

            for(int i = 0; i < list.length; i++){
                if(checkColorAvailable(list[i]))
                    newList.add(String.format("#%06X", (0xFFFFFF & list[i])));
            }

            return newList;
        }

        @Override
        public int getItemCount() {
            int size = group.memberList.size();
            if(size == 0 && hasDownloaded){
                textViewDisplay.setVisibility(View.VISIBLE);
                textViewDisplay2.setVisibility(View.VISIBLE);

            }
            else{
                textViewDisplay.setVisibility(View.GONE);
                textViewDisplay2.setVisibility(View.GONE);
            }

            return group.memberList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewMemberName;
            ImageView imageViewTrash;
            ImageView imageViewBrush;

            public ViewHolder(View itemView) {
                super(itemView);
                textViewMemberName = itemView.findViewById(R.id.textViewMemberName);
                imageViewTrash = itemView.findViewById(R.id.imageViewTrash);
                imageViewBrush = itemView.findViewById(R.id.imageViewBrush);
            }
        }

        private void drawableChangeTint(Drawable[] drawables, int color) {
            for (Drawable drawable : drawables) {
                if (drawable != null)
                    drawable.setTint(color);
            }
        }
    }
}