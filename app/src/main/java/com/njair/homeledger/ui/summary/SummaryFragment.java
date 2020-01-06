package com.njair.homeledger.ui.summary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njair.homeledger.GroupMain;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Group;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SummaryFragment extends Fragment {

    private Activity activity;
    private Context context;
    private Resources r;

    private List<Member> memberList = new ArrayList<>();
    public SummaryAdapter summaryAdapter;
    private TextView textViewDisplay;

    private DatabaseReference groupRef;
    private String roomKey;
    private Group group = Group.dummy();

    private SummaryViewModel summaryViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        summaryViewModel =
                ViewModelProviders.of(this).get(SummaryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_summary, container, false);

        activity = requireActivity();
        context = requireContext();
        r = getResources();
        roomKey = ((GroupMain) getActivity()).getRoomKey();
        groupRef = FirebaseDatabase.getInstance().getReference().child("groups").child(roomKey);

        RecyclerView SummaryRecyclerView = root.findViewById(R.id.recyclerview_summary);

        summaryAdapter = new SummaryAdapter(activity);
        SummaryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        SummaryRecyclerView.addItemDecoration(new DividerItemDecorator());
        SummaryRecyclerView.setAdapter(summaryAdapter);

        textViewDisplay = root.findViewById(R.id.textViewDisplay);
        textViewDisplay.setVisibility(View.GONE);

        return root;
    }

    public void setGroup(Group group){
        this.group = group;
    }

    public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder>{
        private Context context;

        private List<Transaction> transactions;

        public SummaryAdapter(Context context){
            Log.wtf("Adapter", "Init");

            this.context = context;
            transactions = new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_transaction_summary, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).slideUp();
            //members.get(position).adaptView((TransactionLayout) holder.view);

            final Member debtor = transactions.get(position).debtor;

            holder.imageViewSenderIcon.setColorFilter(debtor.color);
            holder.textViewSender.setText(debtor.getName());

            final Transaction transaction = transactions.get(position);

            //region [Handle transactionConstraintLayout: display description]
            /*holder.transactionConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP)
                        Snackbar.make(activity.findViewById(android.R.id.content), transaction.description, Snackbar.LENGTH_LONG)
                                .setAction("cancel thingy", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                    }
                                })
                                .show();

                    return false;
                }
            });*/
            //endregion

            if((position != 0) && (transactions.get(position - 1).debtor.equals(transaction.debtor))){
                holder.imageViewSenderIcon.setVisibility(View.INVISIBLE);

                holder.textViewSender.setText(transaction.debtor.getName());
                holder.textViewSender.setVisibility(View.INVISIBLE);

                holder.imageViewSenderLine.setImageResource(R.drawable.ic_next_black_24dp);
                holder.imageViewSenderLine.setColorFilter(transaction.debtor.color);

                holder.textViewMovement.setVisibility(View.INVISIBLE);
            } else {
                holder.imageViewSenderIcon.setColorFilter(transaction.debtor.color);
                holder.imageViewSenderIcon.setVisibility(View.VISIBLE);

                holder.textViewSender.setText(transaction.debtor.getName());
                holder.textViewSender.setVisibility(View.VISIBLE);

                holder.imageViewSenderLine.setImageResource(R.drawable.ic_arrow_forward_black_24dp);
                holder.imageViewSenderLine.setColorFilter(transaction.debtor.color);

                holder.textViewMovement.setVisibility(View.VISIBLE);
            }

            holder.textViewAmount.setText(transaction.displayAmount());

            //holder.textViewMovement.setTextColor(transaction.blendedColors(.5f));

            holder.imageViewRecipientIcon.setColorFilter(transaction.lender.color);
            holder.textViewRecipient.setText(transaction.lender.getName());
            holder.imageViewRecipientLine.setColorFilter(transaction.lender.color);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        public void update(){
            transactions = new ArrayList<>();

            if(group.transactionList != null){
                transactions = group.transactionList.summedTransactions();
                Log.wtf("Summed transactions, size", Integer.toString(group.transactionList.size()));
            }

            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
            ConstraintLayout transactionConstraintLayout;

            ImageView imageViewSenderIcon;
            TextView textViewSender;
            ImageView imageViewSenderLine;

            TextView textViewAmount;
            TextView textViewMovement;

            ImageView imageViewRecipientIcon;
            TextView textViewRecipient;
            ImageView imageViewRecipientLine;

            public ViewHolder(View itemView) {
                super(itemView);

                transactionConstraintLayout = itemView.findViewById(R.id.constraintlayout_transaction);
                transactionConstraintLayout.setOnCreateContextMenuListener(this);

                imageViewSenderIcon = itemView.findViewById(R.id.imageViewSenderIcon);
                textViewSender = itemView.findViewById(R.id.textViewSender);
                imageViewSenderLine = itemView.findViewById(R.id.imageViewSenderLine);

                textViewAmount = itemView.findViewById(R.id.textViewAmount);
                textViewMovement = itemView.findViewById(R.id.textViewMovement);

                imageViewRecipientIcon = itemView.findViewById(R.id.imageViewRecipientIcon);
                textViewRecipient = itemView.findViewById(R.id.textViewRecipient);
                imageViewRecipientLine = itemView.findViewById(R.id.imageViewRecipientLine);
            }

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.add(Menu.NONE, 0, 0, getString(R.string.cancel_debts)).setOnMenuItemClickListener(onChange);
                //menu.put(Menu.NONE, 1, 1, "Duplicate").setOnMenuItemClickListener(onChange);
            }

            private final MenuItem.OnMenuItemClickListener onChange = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    final Transaction t = summaryAdapter.transactions.get(getAdapterPosition());

                    switch (item.getItemId()){
                        case 0: //Cancel debts
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(String.format(r.getString(R.string.pays_all_debts_to), t.debtor.getName(), t.lender.getName(), t.displayAmount()))
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            final Transaction transaction = t.copy();
                                            transaction.description = r.getString(R.string.debts_cancellation);
                                            transaction.movement = Transaction.t_pays;
                                            transaction.timestamp = new SimpleDateFormat(Transaction.timeStampFormat, Locale.getDefault()).format(new Date());

                                            transaction.upload(roomKey, null);
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {}
                                    });

                            builder.create().show();
                            return true;
                    }
                    return false;
                }
            };
        }
    }

    class DividerItemDecorator extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public DividerItemDecorator() {
            int[] attrs = { android.R.attr.listDivider };
            TypedArray ta = requireContext().obtainStyledAttributes(attrs);
            //Get Drawable and use as needed
            mDivider = ta.getDrawable(0);
            //Clean Up
            ta.recycle();
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            SummaryAdapter adapter = (SummaryAdapter) parent.getAdapter();

            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if((i == adapter.getItemCount() - 1) || !(adapter.transactions.get(i).debtor.equals(adapter.transactions.get(i + 1).debtor))){
                    View child = parent.getChildAt(i);

                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                    int dividerTop = child.getBottom() + params.bottomMargin;
                    int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

                    mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
                    mDivider.draw(canvas);
                }
            }
        }
    }
}