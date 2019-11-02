package com.njair.homeledger.ui.transactions;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionsFragment extends Fragment {

    private TransactionsViewModel transactionsViewModel;
    private List<Transaction> transactionList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        transactionsViewModel =
                ViewModelProviders.of(this).get(TransactionsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_transactions, container, false);
        final TextView textView = root.findViewById(R.id.text_transactions);
        transactionsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //textView.setText(s);
            }
        });

        transactionList = Transaction.readFromSharedPreferences(getActivity());

        RecyclerView transactionsRecyclerView = root.findViewById(R.id.recyclerview_transactions);
        final TransactionsAdapter transactionsAdapter = new TransactionsAdapter(getActivity(), transactionList);
        transactionsRecyclerView.setAdapter(transactionsAdapter);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        transactionsRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        //region [Handle FAB: add transaction]
        final FloatingActionButton fabWrite = root.findViewById(R.id.fabWrite);
        fabWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final List<Member> memberList = Member.readFromSharedPreferences(getActivity());

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.dialog_create_transaction, null);

                builder.setTitle("Add transaction");
                builder.setView(dialogLayout);

                final EditText editTextDescription = dialogLayout.findViewById(R.id.editTextDescription);
                final Spinner spinnerSender = dialogLayout.findViewById(R.id.spinnerSender);
                final EditText editTextAmount = dialogLayout.findViewById(R.id.editTextAmount);
                final Spinner spinnerRecipient = dialogLayout.findViewById(R.id.spinnerRecipient);

                final ArrayAdapter<String> aASender = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
                        addToBeginningOfArray(Member.getNameList(memberList).toArray(new String[0]), "Choose the debtor"));
                spinnerSender.setAdapter(aASender);

                final ArrayAdapter<String> aARecipient = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
                        addToBeginningOfArray(Member.getNameList(memberList).toArray(new String[0]), "Choose the lender"));
                spinnerRecipient.setAdapter(aARecipient);

                builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                transactionsAdapter.addItem(
                                        new Transaction(editTextDescription.getText().toString(),
                                                Member.findMember(memberList, spinnerSender.getSelectedItem().toString()),
                                                Member.findMember(memberList, spinnerRecipient.getSelectedItem().toString()),
                                                Float.parseFloat(editTextAmount.getText().toString()))
                                );
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {}
                        });

                AlertDialog dialog = builder.create();
                dialog.show();

                final Button dialogBtnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                dialogBtnPositive.setEnabled(false);

                spinnerSender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        dialogBtnPositive.setEnabled((position != 0) && (position != spinnerRecipient.getSelectedItemPosition())
                                && !((editTextAmount.getText().toString().equals("")) || (Float.parseFloat(editTextAmount.getText().toString()) == 0)));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {}

                });

                editTextAmount.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        dialogBtnPositive.setEnabled(!((s.toString().equals("")) || (Float.parseFloat(s.toString()) == 0)) && (spinnerSender.getSelectedItemPosition() != 0)
                                && (spinnerRecipient.getSelectedItemPosition() != 0) && (spinnerSender.getSelectedItemPosition() != spinnerRecipient.getSelectedItemPosition()));
                    }
                });

                spinnerRecipient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        dialogBtnPositive.setEnabled((position != 0) && (position != spinnerSender.getSelectedItemPosition())
                                && !((editTextAmount.getText().toString().equals(""))  || (Float.parseFloat(editTextAmount.getText().toString()) == 0)));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {}

                });
            }
        });
        //endregion

        transactionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabWrite.getVisibility() == View.VISIBLE) {
                    fabWrite.hide();
                } else if (dy < 0 && fabWrite.getVisibility() != View.VISIBLE) {
                    fabWrite.show();
                }
            }
        });

        return root;
    }

    static <T> T[] addToBeginningOfArray(T[] array, T element){
        T[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[0] = element;
        System.arraycopy(array, 0, newArray, 1, array.length);

        return newArray;
    }

    class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder>{
        private static final String TAG = "Transactions Adapter";
        private Context context;

        private List<Transaction> transactions;

        public TransactionsAdapter(Context context, List<Transaction> transactions){
            this.context = context;
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_transaction_plus, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @SuppressLint("ClickableViewAccessibility") //TODO ClickableViewAccessibility
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            //Toast.makeText(getActivity(), "onBindViewHolder: called", Toast.LENGTH_SHORT).show();
            //transactions.get(position).adaptView((TransactionLayout) holder.view);

            final Transaction transaction = transactions.get(position);

            //region [Handle imageViewDelete: delete transaction]
            final ImageView IVDelete = holder.imageViewDelete;
            IVDelete.setColorFilter(Color.BLACK);
            IVDelete.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            IVDelete.setColorFilter(R.color.common_google_signin_btn_text_dark_pressed);
                            IVDelete.invalidate();

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(("Delete transaction " + transaction.description).trim() + " at " + transaction.timestamp + "?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            removeItem(position);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {}
                                    }).show();
                            break;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            IVDelete.setColorFilter(Color.BLACK);
                            IVDelete.invalidate();
                            break;
                        }
                    }
                    return false;
                }
            });
            //endregion

            //region [Handle transactionLinearLayout: display description]
            holder.transactionLinearLayout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()){
                        case(MotionEvent.ACTION_DOWN):
                            ((GridLayout)v.getParent()).setPressed(true);
                            break;

                        case(MotionEvent.ACTION_UP):
                            if(!transaction.description.equals(""))
                                Toast.makeText(getActivity(), transaction.description, Toast.LENGTH_LONG).show();

                        case(MotionEvent.ACTION_CANCEL):
                            ((GridLayout)v.getParent()).setPressed(false);
                            break;
                    }
                    return false;
                }
            });
            //endregion

            holder.textViewSender.setText(transaction.debtor.getName());
            drawableChangeTint(holder.textViewSender.getCompoundDrawables(), transaction.debtor.color);

            holder.textViewAmount.setText(transaction.getAmount());

            holder.textViewRecipient.setText(transaction.lender.getName());
            drawableChangeTint(holder.textViewRecipient.getCompoundDrawables(), transaction.lender.color);

            holder.textViewTimestamp.setText(transaction.timestamp);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        public void addItem(Transaction transaction){
            transactions.add(transaction);
            Transaction.writeToSharedPreferences(getActivity(), transactions);
            notifyDataSetChanged();
        }

        public void removeItem(int position){
            transactions.remove(position);
            Transaction.writeToSharedPreferences(getActivity(), transactions);
            notifyDataSetChanged();
        }

        public void update(List<Transaction> transactions){
            this.transactions = transactions;
            Transaction.writeToSharedPreferences(getActivity(), transactions);
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            LinearLayout transactionLinearLayout;

            ImageView imageViewDelete;
            TextView textViewSender;
            TextView textViewAmount;
            TextView textViewRecipient;
            TextView textViewTimestamp;

            public ViewHolder(View itemView){
                super(itemView);
                transactionLinearLayout = itemView.findViewById(R.id.linearlayout_transaction);

                imageViewDelete = itemView.findViewById(R.id.imageViewDelete);
                textViewSender = itemView.findViewById(R.id.textViewSender);
                textViewAmount = itemView.findViewById(R.id.textViewAmount);
                textViewRecipient = itemView.findViewById(R.id.textViewRecipient);
                textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
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