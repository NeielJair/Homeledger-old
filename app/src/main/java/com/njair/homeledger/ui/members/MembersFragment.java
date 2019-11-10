package com.njair.homeledger.ui.members;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MembersFragment extends Fragment {

    private MembersViewModel membersViewModel;
    private List<Member> memberList = new ArrayList<>();
    private final int[] colorList = {Color.RED, Color.BLUE, Color.rgb(0, 0, 128),
            Color.rgb(204, 0, 204), Color.rgb(0, 128, 0), Color.rgb(204,204,0),
            Color.rgb(255, 165, 0), Color.rgb(128,0,128), Color.rgb(204, 0, 102), Color.rgb(218,165,32)};

    public View onCreateView(@NonNull final LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        membersViewModel =
                ViewModelProviders.of(this).get(MembersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_members, container, false);
        final TextView textView = root.findViewById(R.id.text_notifications);
        membersViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        memberList = Member.readFromSharedPreferences(getActivity());

        RecyclerView membersRecyclerView = root.findViewById(R.id.recyclerview_members);
        final MembersAdapter membersAdapter = new MembersAdapter(getActivity(), memberList);
        membersRecyclerView.setAdapter(membersAdapter);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        membersRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        final FloatingActionButton fabAdd = root.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(memberList.size() < colorList.length){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Add member");

                    final EditText input = new EditText(getActivity());
                    input.setHint("Member name");
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                    builder.setView(input);

                    builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String inputName = input.getText().toString().trim();

                            if(checkNameAvailable(inputName))
                                membersAdapter.addItem(new Member(inputName, randomAvailableColor()));
                            else
                                Snackbar.make(view, "Member name is not available.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                    }
                else Snackbar.make(view, "Maximum amount of users reached.", Snackbar.LENGTH_LONG).show();
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

        return root;
    }

    private boolean checkColorAvailable(int color){
        for(Member member : memberList){
            if(member.color == color)
                return false;
        }

        return true;
    }

    private boolean checkNameAvailable(String name){
        for(Member member : memberList){
            if(member.getName().trim().equals(name.trim())){
                return false;
            }
        }

        return true;
    }

    private int randomAvailableColor(){
        int colorListLength = colorList.length;

        if(memberList.size() < colorListLength){
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

    class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {
        private Context context;

        private List<Member> members;

        public MembersAdapter(Context context, List<Member> members) {
            this.context = context;
            this.members = members;
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
            //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).show();
            //members.get(position).adaptView((TransactionLayout) holder.view);

            final Member member = members.get(position);

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
                            builder.setMessage("Delete member " + member.getName() + "?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            removeItem(position);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
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

                            if(memberList.size() < colorList.length){
                                ColorPicker colorPicker = new ColorPicker(getActivity());
                                colorPicker.setTitle("Choose a color for " + member.getName());
                                colorPicker.setColors(removeColorsFromArrayToHex(colorList));
                                colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                                    @Override
                                    public void setOnFastChooseColorListener(int i,int color) {
                                        if(color != Color.WHITE){
                                            List<Member> newList = memberList;
                                            newList.set(position, new Member(member.getName(), color));

                                            update(newList);
                                        }
                                    }

                                    @Override
                                    public void onCancel() {}
                                });

                                colorPicker.setRoundColorButton(true);
                                colorPicker.show();
                            } else
                                Snackbar.make(v, "No colors are available.", Snackbar.LENGTH_LONG).show();

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
            return members.size();
        }

        public void addItem(Member member) {
            members.add(member);
            Member.writeToSharedPreferences(getActivity(), members);
            notifyDataSetChanged();
        }

        public void removeItem(int position){
            members.remove(position);
            Member.writeToSharedPreferences(getActivity(), members);
            notifyDataSetChanged();
        }

        public void update(List<Member> members) {
            this.members = members;
            Member.writeToSharedPreferences(getActivity(), members);
            notifyDataSetChanged();
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