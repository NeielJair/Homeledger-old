package com.njair.homeledger.ui.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.njair.homeledger.R;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;

import java.util.ArrayList;
import java.util.List;

public class SummaryFragment extends Fragment {

    private List<Transaction> transactionList;

    private SummaryViewModel summaryViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        summaryViewModel =
                ViewModelProviders.of(this).get(SummaryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_summary, container, false);

        transactionList = Transaction.readFromSharedPreferences(getActivity());

        RecyclerView SummaryRecyclerView = root.findViewById(R.id.recyclerview_summary);

        List<Transaction> summedTransactions = Transaction.sumTransactions(transactionList);
        final SummaryAdapter summaryAdapter = new SummaryAdapter(getActivity(), Transaction.getDebtors(summedTransactions, true), summedTransactions);
        SummaryRecyclerView.setAdapter(summaryAdapter);
        SummaryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        SummaryRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        if(Transaction.getDebtors(transactionList, true).equals(new ArrayList<Member>()))
            Toast.makeText(getActivity(), "boo",Toast.LENGTH_LONG).show();

        return root;
    }

    class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder>{
        //region [SummaryAdapter section]
        private Context context;

        private List<Member> members;
        private List<Transaction> transactions;

        public SummaryAdapter(Context context, List<Member> members, List<Transaction> transactions){
            this.context = context;
            this.members = members;
            this.transactions = transactions;
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

            final Member debtor = members.get(position);

            holder.imageViewSenderIcon.setColorFilter(debtor.color);
            holder.textViewSender.setText(debtor.getName());

            LendersAdapter lendersAdapter = new LendersAdapter(context, Transaction.filterByMember(transactions, debtor, true));
            holder.recyclerViewLenders.setAdapter(lendersAdapter);
            holder.recyclerViewLenders.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
            holder.recyclerViewLenders.setNestedScrollingEnabled(false);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        public void addItem(Member member){
            members.add(member);
            notifyDataSetChanged();
        }

        public void removeItem(int position){
            members.remove(position);
            notifyDataSetChanged();
        }

        public void update(List<Member> members){
            this.members = members;
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            ImageView imageViewSenderIcon;
            TextView textViewSender;
            RecyclerView recyclerViewLenders;

            public ViewHolder(View itemView){
                super(itemView);
                imageViewSenderIcon = itemView.findViewById(R.id.imageViewSenderIcon);
                textViewSender = itemView.findViewById(R.id.textViewSender);
                recyclerViewLenders = itemView.findViewById(R.id.recyclerview_lenders);
            }
        }

        private void drawableChangeTint(Drawable[] drawables, int color){
            for(Drawable drawable : drawables){
                if(drawable != null)
                    drawable.setTint(color);
            }
        }
        //endregion

        class LendersAdapter extends RecyclerView.Adapter<LendersAdapter.ViewHolder> {
            private Context context;

            private List<Transaction> transactions;

            public LendersAdapter(Context context, List<Transaction> transactions) {
                this.context = context;
                this.transactions = transactions;
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_lenderitem, parent, false);
                ViewHolder viewHolder = new ViewHolder(view);
                return viewHolder;
            }

            @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
            @Override
            public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
                //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).slideUp();

                final Transaction transaction = transactions.get(position);

                if(position != 0) {
                    holder.imageViewSenderLine.setImageResource(R.drawable.ic_next_black_24dp);
                    holder.textViewMovement.setVisibility(View.INVISIBLE);
                }
                holder.imageViewSenderLine.setColorFilter(transaction.debtor.color);

                holder.textViewAmount.setText(transaction.getAmount());

                if(transaction.movement == Transaction.t_pays){
                    holder.textViewMovement.setText(R.string.pays);
                    holder.textViewMovement.setTypeface(null, Typeface.BOLD);
                    holder.textViewMovement.setVisibility(View.VISIBLE);
                }

                //holder.textViewMovement.setTextColor(transaction.blendedColors(.5f));

                holder.imageViewRecipientIcon.setColorFilter(transaction.lender.color);
                holder.textViewRecipient.setText(transaction.lender.getName());
                holder.imageViewRecipientLine.setColorFilter(transaction.lender.color);
            }

            @Override
            public int getItemCount() {
                return transactions.size();
            }

            public void addItem(Transaction transaction) {
                transactions.add(transaction);
                notifyDataSetChanged();
            }

            public void removeItem(int position) {
                transactions.remove(position);
                notifyDataSetChanged();
            }

            public void update(List<Transaction> transactions) {
                this.transactions = transactions;
                notifyDataSetChanged();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                ImageView imageViewSenderLine;
                TextView textViewAmount;
                TextView textViewMovement;
                ImageView imageViewRecipientIcon;
                TextView textViewRecipient;
                ImageView imageViewRecipientLine;

                public ViewHolder(View itemView) {
                    super(itemView);
                    imageViewSenderLine = itemView.findViewById(R.id.imageViewSenderLine);
                    textViewAmount = itemView.findViewById(R.id.textViewAmount);
                    textViewMovement = itemView.findViewById(R.id.textViewMovement);
                    imageViewRecipientIcon = itemView.findViewById(R.id.imageViewRecipientIcon);
                    textViewRecipient = itemView.findViewById(R.id.textViewRecipient);
                    imageViewRecipientLine = itemView.findViewById(R.id.imageViewRecipientLine);
                }
            }
        }
    }
}