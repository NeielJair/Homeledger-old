package com.njair.homeledger.ui.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.njair.homeledger.R;
import com.njair.homeledger.Service.Transaction;

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
        final SummaryAdapter summaryAdapter = new SummaryAdapter(getActivity(), Transaction.sumTransactions(transactionList));
        SummaryRecyclerView.setAdapter(summaryAdapter);
        SummaryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        SummaryRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        return root;
    }

    class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder>{
        private static final String TAG = "Transactions Adapter";
        private Context context;

        private List<Transaction> transactions;

        public SummaryAdapter(Context context, List<Transaction> transactions){
            this.context = context;
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_transaction, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).show();
            //transactions.get(position).adaptView((TransactionLayout) holder.view);

            final Transaction transaction = transactions.get(position);

            holder.textViewSender.setText(transaction.debtor.getName());
            drawableChangeTint(holder.textViewSender.getCompoundDrawables(), transaction.debtor.color);

            holder.textViewAmount.setText(transaction.getAmount());

            holder.textViewRecipient.setText(transaction.lender.getName());
            drawableChangeTint(holder.textViewRecipient.getCompoundDrawables(), transaction.lender.color);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        public void addItem(Transaction transaction){
            transactions.add(transaction);
            notifyDataSetChanged();
        }

        public void removeItem(int position){
            transactions.remove(position);
            notifyDataSetChanged();
        }

        public void update(List<Transaction> transactions){
            this.transactions = transactions;
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            TextView textViewSender;
            TextView textViewAmount;
            TextView textViewRecipient;

            public ViewHolder(View itemView){
                super(itemView);
                textViewSender = itemView.findViewById(R.id.textViewSender);
                textViewAmount = itemView.findViewById(R.id.textViewAmount);
                textViewRecipient = itemView.findViewById(R.id.textViewRecipient);
            }
        }

        private void drawableChangeTint(Drawable[] drawables, int color){
            for(Drawable drawable : drawables){
                if(drawable != null)
                    drawable.setTint(color);
            }
        }
    }
}