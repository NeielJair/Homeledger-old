package com.njair.homeledger.ui.transactions;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.annotations.NotNull;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionsFragment extends Fragment {

    private TransactionsViewModel transactionsViewModel;
    private List<Transaction> transactionList = new ArrayList<>();

    private FloatingActionButton fabWrite;

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
        fabWrite = root.findViewById(R.id.fabWrite);
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
                final Spinner spinnerMovement = dialogLayout.findViewById(R.id.spinnerMovement);
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
                                                Float.parseFloat(editTextAmount.getText().toString()),
                                                spinnerMovement.getSelectedItem().equals("Owes"))
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

            holder.textViewTimestamp.setText(transaction.timestamp);

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

                                            if(fabWrite.getVisibility() != View.INVISIBLE)
                                                fabWrite.show();
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

            //region [Handle transactionConstraintLayout: display description]
            holder.transactionConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if((event.getAction() == MotionEvent.ACTION_UP) && (!transaction.description.equals("")))
                        Snackbar.make(getActivity().findViewById(android.R.id.content), transaction.description, Snackbar.LENGTH_LONG)
                                .setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {}
                        })
                                .show();

                    return false;
                }
            });
            //endregion

            holder.imageViewSenderIcon.setColorFilter(transaction.debtor.color);
            holder.textViewSender.setText(transaction.debtor.getName());
            holder.imageViewSenderLine.setColorFilter(transaction.debtor.color);

            holder.textViewAmount.setText(transaction.getAmount());

            if(transaction.movement == Transaction.t_owes)
                holder.textViewMovement.setText(R.string.owes);
            else{
                holder.textViewMovement.setText(R.string.pays);
                holder.textViewMovement.setTypeface(null, Typeface.BOLD);
            }


            holder.textViewMovement.setTextColor(transaction.blendedColors(.5f));

            holder.imageViewRecipientIcon.setColorFilter(transaction.lender.color);
            holder.textViewRecipient.setText(transaction.lender.getName());
            holder.imageViewRecipientLine.setColorFilter(transaction.lender.color);
        }

        private int blendColors(int color1, int color2, float ratio) {
            final float inverseRation = 1f - ratio;
            float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
            float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
            float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
            return Color.rgb((int) r, (int) g, (int) b);
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
            ConstraintLayout transactionConstraintLayout;

            TextView textViewTimestamp;
            ImageView imageViewDelete;

            ImageView imageViewSenderIcon;
            TextView textViewSender;
            ImageView imageViewSenderLine;

            TextView textViewAmount;
            TextView textViewMovement;

            ImageView imageViewRecipientIcon;
            TextView textViewRecipient;
            ImageView imageViewRecipientLine;

            public ViewHolder(View itemView){
                super(itemView);
                transactionConstraintLayout = itemView.findViewById(R.id.constraintlayout_transaction);

                textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
                imageViewDelete = itemView.findViewById(R.id.imageViewDelete);

                imageViewSenderIcon = itemView.findViewById(R.id.imageViewSenderIcon);
                textViewSender = itemView.findViewById(R.id.textViewSender);
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