package com.njair.homeledger.ui.transactions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.njair.homeledger.R;
import com.njair.homeledger.Service.Member;
import com.njair.homeledger.Service.Transaction;
import com.njair.homeledger.Service.Transaction.TransactionList;
import com.njair.homeledger.Service.Transaction.SortByStruct;
import com.njair.homeledger.ui.DialogDrawerView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private TransactionsViewModel transactionsViewModel;

    private TransactionList transactionList = new TransactionList();

    private FloatingActionButton fabWrite;
    private TransactionsAdapter transactionsAdapter;

    private Resources r;

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

        final Activity activity = requireActivity();
        final Context context = requireContext();
        r = getResources();

        transactionList = TransactionList.readFromSharedPreferences(activity);

        RecyclerView transactionsRecyclerView = root.findViewById(R.id.recyclerview_transactions);
        transactionsAdapter = new TransactionsAdapter(activity, transactionList);
        transactionsRecyclerView.setAdapter(transactionsAdapter);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        transactionsRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        enableSwipeToDeleteAndUndo(activity, transactionsAdapter, transactionsRecyclerView);

        setHasOptionsMenu(true);

        //region [Manage dialog drawer: add transaction]
        final DialogDrawerView dialogDrawer = root.findViewById(R.id.bottomDrawerTransaction);
        dialogDrawer.setTitle("Add transaction");

        final ConstraintLayout dialogLayout = root.findViewById(R.id.linearLayout);

        final List<Member> memberList = Member.readFromSharedPreferences(activity);

        final LinearLayout buttonDatePicker = dialogLayout.findViewById(R.id.buttonDatePicker);
        final TextView textViewDate = dialogLayout.findViewById(R.id.textViewDate);
        final LinearLayout buttonTimePicker = dialogLayout.findViewById(R.id.buttonTimePicker);
        final TextView textViewTime = dialogLayout.findViewById(R.id.textViewTime);
        final EditText editTextDescription = dialogLayout.findViewById(R.id.editTextDescription);
        final Spinner spinnerSender = dialogLayout.findViewById(R.id.spinnerSender);
        final EditText editTextAmount = dialogLayout.findViewById(R.id.editTextAmount);
        final Spinner spinnerMovement = dialogLayout.findViewById(R.id.spinnerMovement);
        final Spinner spinnerRecipient = dialogLayout.findViewById(R.id.spinnerRecipient);

        final ImageView imageViewDismiss = dialogDrawer.findViewById(R.id.imageViewDismiss);
        final Button buttonCancel = dialogDrawer.findViewById(R.id.buttonCancel);
        final Button buttonAdd = dialogDrawer.findViewById(R.id.buttonOk);

        final ArrayAdapter<String> aASender = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                addToBeginningOfArray(Member.getNameList(memberList).toArray(new String[0]), "Debtor"));
        spinnerSender.setAdapter(aASender);

        final ArrayAdapter<String> aARecipient = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                addToBeginningOfArray(Member.getNameList(memberList).toArray(new String[0]), "Lender"));
        spinnerRecipient.setAdapter(aARecipient);

        final Calendar date = Calendar.getInstance();

        final SimpleDateFormat sdfTimestamp = new SimpleDateFormat(Transaction.timeStampFormat, Locale.getDefault());
        final SimpleDateFormat sdfDate = new SimpleDateFormat(Transaction.dateFormat, Locale.getDefault());
        final SimpleDateFormat sdfTime = new SimpleDateFormat(Transaction.timeFormat, Locale.getDefault());

        textViewDate.setText(sdfDate.format(date.getTime()));
        textViewTime.setText(sdfTime.format(date.getTime()));

        final DatePickerDialog.OnDateSetListener setDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                date.set(Calendar.YEAR, year);
                date.set(Calendar.MONTH, monthOfYear);
                date.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                textViewDate.setText(sdfDate.format(date.getTime()));
            }
        };

        buttonDatePicker.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(activity, setDate, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        final TimePickerDialog.OnTimeSetListener setTime = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                // TODO Auto-generated method stub
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);

                textViewTime.setText(sdfTime.format(date.getTime()));
            }
        };

        buttonTimePicker.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(activity, setTime, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), true);
                dialog.show();
            }
        });

        buttonAdd.setEnabled(false);

        spinnerSender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                buttonAdd.setEnabled((position != 0) && (position != spinnerRecipient.getSelectedItemPosition())
                        && !((editTextAmount.getText().toString().equals("")) || (Float.parseFloat(editTextAmount.getText().toString()) == 0)));

                buttonAdd.setTextColor((buttonAdd.isEnabled()) ? (r.getColor(R.color.color4)) : (r.getColor(R.color.colorRedDisabled)));
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
                if(s.toString().trim().equals("."))
                    s.insert(0, "0");

                buttonAdd.setEnabled(!((s.toString().equals("")) || (Float.parseFloat(s.toString()) == 0)) && (spinnerSender.getSelectedItemPosition() != 0)
                        && (spinnerRecipient.getSelectedItemPosition() != 0) && (spinnerSender.getSelectedItemPosition() != spinnerRecipient.getSelectedItemPosition()));

                buttonAdd.setTextColor((buttonAdd.isEnabled()) ? (r.getColor(R.color.color4)) : (r.getColor(R.color.colorRedDisabled)));
            }
        });

        spinnerRecipient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                buttonAdd.setEnabled((position != 0) && (position != spinnerSender.getSelectedItemPosition())
                        && !((editTextAmount.getText().toString().equals("")) || (Float.parseFloat(editTextAmount.getText().toString()) == 0)));

                buttonAdd.setTextColor((buttonAdd.isEnabled()) ? (r.getColor(R.color.color4)) : (r.getColor(R.color.colorRedDisabled)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}

        });

        imageViewDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDrawer.slideDown();

                if(fabWrite.getVisibility() != View.VISIBLE)
                    fabWrite.show();

                resetTransactionDialog(dialogLayout);
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDrawer.slideDown();

                if(fabWrite.getVisibility() != View.VISIBLE)
                    fabWrite.show();

                resetTransactionDialog(dialogLayout);
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDrawer.slideDown();

                if(fabWrite.getVisibility() != View.VISIBLE)
                    fabWrite.show();

                transactionsAdapter.addItem(
                        new Transaction(editTextDescription.getText().toString(),
                                Member.findMember(memberList, spinnerSender.getSelectedItem().toString()),
                                Member.findMember(memberList, spinnerRecipient.getSelectedItem().toString()),
                                Float.parseFloat(editTextAmount.getText().toString()),
                                sdfTimestamp.format(date.getTime()),
                                spinnerMovement.getSelectedItem().equals("Owes"))
                );

                resetTransactionDialog(dialogLayout);
            }
        });



        //endregion

        //region [Handle FAB: add transaction]
        fabWrite = root.findViewById(R.id.fabWrite);
        fabWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!dialogDrawer.visible){
                    dialogDrawer.slideUp();

                    if(fabWrite.getVisibility() == View.VISIBLE)
                        fabWrite.hide();
                }
            }
        });

        transactionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabWrite.getVisibility() == View.VISIBLE && dialogDrawer.visible) {
                    fabWrite.hide();
                } else if ((dy < 0 || dy == 0) && fabWrite.getVisibility() != View.VISIBLE && !dialogDrawer.visible) {
                    fabWrite.show();
                }
            }
        });
        //endregion

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transactions_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionbar_sortby:
                final Activity activity = requireActivity();

                final List<Member> memberList = Member.readFromSharedPreferences(activity);
                final SortByStruct sortByStruct = SortByStruct.readFromSharedPreferences(activity);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.dialog_sort_transactions, null);

                builder.setTitle(r.getString(R.string.sortby));
                builder.setView(dialogLayout);

                final Switch switchLowestToHighest = dialogLayout.findViewById(R.id.switchLowestToHighest);
                final TextView textViewLowestToHighest = dialogLayout.findViewById(R.id.textViewLowestToHighest);
                final Spinner spinnerMember = dialogLayout.findViewById(R.id.spinnerMembers);
                final RadioGroup rg = dialogLayout.findViewById(R.id.radioGroup);

                final ArrayAdapter<String> aAMembers = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                        Member.getNameList(memberList).toArray(new String[0]));
                spinnerMember.setAdapter(aAMembers);

                if ((sortByStruct.member == null) || (sortByStruct.member.color == Color.BLACK))
                    sortByStruct.member = memberList.get(0);

                switch (sortByStruct.sortType) {
                    case Transaction.st_timestamp:
                        rg.check(R.id.radioButtonTimestamp);

                        switchLowestToHighest.setChecked(sortByStruct.lowestToHighest);

                        textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.newesttooldest) : r.getString(R.string.oldesttonewest));

                        switchLowestToHighest.setVisibility(View.VISIBLE);
                        textViewLowestToHighest.setVisibility(View.VISIBLE);
                        spinnerMember.setVisibility(View.GONE);
                        switchLowestToHighest.setEnabled(true);
                        textViewLowestToHighest.setEnabled(true);
                        spinnerMember.setEnabled(false);
                        break;

                    case Transaction.st_member:
                        rg.check(R.id.radioButtonMember);

                        Member debtor = Member.getFromSharedPreferences(activity, sortByStruct.member.getName());

                        if (debtor.getColor() != Color.BLACK)
                            spinnerMember.setSelection(aAMembers.getPosition(debtor.getName()));

                        switchLowestToHighest.setVisibility(View.GONE);
                        textViewLowestToHighest.setVisibility(View.GONE);
                        spinnerMember.setVisibility(View.VISIBLE);
                        switchLowestToHighest.setEnabled(false);
                        textViewLowestToHighest.setEnabled(false);
                        spinnerMember.setEnabled(true);
                        break;

                    case Transaction.st_amount:
                        rg.check(R.id.radioButtonAmount);

                        switchLowestToHighest.setChecked(sortByStruct.lowestToHighest);

                        textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.lowesttohighest) : r.getString(R.string.highesttolowest));

                        switchLowestToHighest.setVisibility(View.VISIBLE);
                        textViewLowestToHighest.setVisibility(View.VISIBLE);
                        spinnerMember.setVisibility(View.GONE);
                        switchLowestToHighest.setEnabled(true);
                        textViewLowestToHighest.setEnabled(true);
                        spinnerMember.setEnabled(false);
                        break;

                    case Transaction.st_movement:
                        rg.check(R.id.radioButtonMovement);

                        switchLowestToHighest.setChecked(sortByStruct.lowestToHighest);

                        textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.debtsfirst) : r.getString(R.string.loansfirst));

                        switchLowestToHighest.setVisibility(View.VISIBLE);
                        textViewLowestToHighest.setVisibility(View.VISIBLE);
                        spinnerMember.setVisibility(View.GONE);
                        switchLowestToHighest.setEnabled(true);
                        textViewLowestToHighest.setEnabled(true);
                        spinnerMember.setEnabled(false);
                        break;
                }

                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.radioButtonTimestamp:
                            case R.id.radioButtonAmount:
                            case R.id.radioButtonMovement:
                                switchLowestToHighest.setChecked(sortByStruct.lowestToHighest);

                                if (checkedId == R.id.radioButtonTimestamp) {
                                    sortByStruct.sortType = Transaction.st_timestamp;
                                    textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.newesttooldest) : r.getString(R.string.oldesttonewest));
                                } else if (checkedId == R.id.radioButtonAmount) {
                                    sortByStruct.sortType = Transaction.st_amount;
                                    textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.lowesttohighest) : r.getString(R.string.highesttolowest));
                                } else if (checkedId == R.id.radioButtonMovement) {
                                    sortByStruct.sortType = Transaction.st_movement;
                                    textViewLowestToHighest.setText((sortByStruct.lowestToHighest) ? r.getString(R.string.debtsfirst) : r.getString(R.string.loansfirst));
                                }

                                switchLowestToHighest.setVisibility(View.VISIBLE);
                                textViewLowestToHighest.setVisibility(View.VISIBLE);
                                spinnerMember.setVisibility(View.GONE);
                                switchLowestToHighest.setEnabled(true);
                                textViewLowestToHighest.setEnabled(true);
                                spinnerMember.setEnabled(false);
                                break;

                            case R.id.radioButtonMember:
                                sortByStruct.sortType = Transaction.st_member;

                                Member member = Member.getFromSharedPreferences(activity, sortByStruct.member.getName());

                                if (member.color != Color.BLACK) {
                                    sortByStruct.member = member;
                                    spinnerMember.setSelection(aAMembers.getPosition(member.getName()));
                                }

                                switchLowestToHighest.setVisibility(View.GONE);
                                textViewLowestToHighest.setVisibility(View.GONE);
                                spinnerMember.setVisibility(View.VISIBLE);
                                switchLowestToHighest.setEnabled(false);
                                textViewLowestToHighest.setEnabled(false);
                                spinnerMember.setEnabled(true);
                                break;
                        }
                    }
                });

                switchLowestToHighest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        sortByStruct.lowestToHighest = isChecked;

                        switch (sortByStruct.sortType) {
                            case Transaction.st_timestamp:
                                textViewLowestToHighest.setText((isChecked) ? r.getString(R.string.newesttooldest) : r.getString(R.string.oldesttonewest));
                                break;

                            case Transaction.st_amount:
                                textViewLowestToHighest.setText((isChecked) ? r.getString(R.string.lowesttohighest) : r.getString(R.string.highesttolowest));
                                break;

                            case Transaction.st_movement:
                                textViewLowestToHighest.setText((isChecked) ? r.getString(R.string.debtsfirst) : r.getString(R.string.loansfirst));
                                break;

                            default:
                                break;
                        }
                    }
                });

                textViewLowestToHighest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchLowestToHighest.performClick();
                    }
                });

                spinnerMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        sortByStruct.member = memberList.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }

                });

                builder.setPositiveButton(r.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        transactionsAdapter.sort(sortByStruct);
                        SortByStruct.writeToSharedPreferences(activity, sortByStruct);
                    }
                })
                        .setNegativeButton(r.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private static <T> T[] addToBeginningOfArray(T[] array, T element) {
        T[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[0] = element;
        System.arraycopy(array, 0, newArray, 1, array.length);

        return newArray;
    }

    private void resetTransactionDialog(ConstraintLayout dialogLayout){
        final TextView textViewDate = dialogLayout.findViewById(R.id.textViewDate);
        final TextView textViewTime = dialogLayout.findViewById(R.id.textViewTime);
        final EditText editTextDescription = dialogLayout.findViewById(R.id.editTextDescription);
        final Spinner spinnerSender = dialogLayout.findViewById(R.id.spinnerSender);
        final EditText editTextAmount = dialogLayout.findViewById(R.id.editTextAmount);
        final Spinner spinnerMovement = dialogLayout.findViewById(R.id.spinnerMovement);
        final Spinner spinnerRecipient = dialogLayout.findViewById(R.id.spinnerRecipient);

        final Calendar date = Calendar.getInstance();

        final SimpleDateFormat sdfDate = new SimpleDateFormat(Transaction.dateFormat, Locale.getDefault());
        final SimpleDateFormat sdfTime = new SimpleDateFormat(Transaction.timeFormat, Locale.getDefault());

        textViewDate.setText(sdfDate.format(date.getTime()));
        textViewTime.setText(sdfTime.format(date.getTime()));

        editTextDescription.setText("");
        editTextAmount.setText("");

        spinnerSender.setSelection(0);
        spinnerRecipient.setSelection(0);
        spinnerMovement.setSelection(0);
    }

    class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
        private Activity activity;
        private Context context;

        private TransactionList transactions;
        private SortByStruct sortByStruct;

        private TransactionsAdapter(Activity activity, TransactionList transactions) {
            this.activity = activity;
            this.transactions = transactions;
            //Transaction.sortByTransactions(this.transactions, sortByStruct);
            //this.sortByStruct = sortByStruct;
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
            //Toast.makeText(activity, "onBindViewHolder: called", Toast.LENGTH_SHORT).slideUp();
            //transactions.get(position).adaptView((TransactionLayout) holder.view);

            final Transaction transaction = transactions.getList().get(position);

            holder.textViewTimestamp.setText(transaction.timestamp);

            //region [Handle transactionConstraintLayout: display description]
            holder.transactionConstraintLayout.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() == MotionEvent.ACTION_UP) && (!transaction.description.equals("")))
                        Snackbar.make(activity.findViewById(android.R.id.content), transaction.description, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.dismiss), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                    }
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

            if (transaction.movement == Transaction.t_owes) {
                holder.textViewMovement.setText(R.string.owes);
                holder.textViewMovement.setTypeface(null, Typeface.NORMAL);
            } else {
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
            return transactions.getData().size();
        }

        public void addItem(Transaction transaction) {
            transactions.add(transaction);
            transactions.upload(activity);
            notifyDataSetChanged();
        }

        public void removeItem(int position) {
            transactions.removeAt(position);
            transactions.upload(activity);
            notifyItemRemoved(position);
        }

        public void restoreItem(Transaction item, int position) {
            /*transactions.add(position, item);
            Transaction.sortByTransactions(transactions, sortByStruct);
            int i = Transaction.getId(transactions, item);

            Transaction.writeToSharedPreferences(activity, transactions);
            notifyItemInserted(position);*/

            transactions.add(item);
            transactions.upload(activity);
            if(transactions.getPosition(item) != -1)
                notifyItemInserted(transactions.getPosition(item));
            else
                notifyDataSetChanged();
        }

        public void update(TransactionList transactions) {
            this.transactions = transactions;
            transactions.upload(activity);
            notifyDataSetChanged();
        }

        public void sort(SortByStruct sortByStruct) {
            this.sortByStruct = sortByStruct;
            transactions.sort(sortByStruct);
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout transactionConstraintLayout;

            TextView textViewTimestamp;

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

                textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);

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

    abstract class SwipeToDeleteCallback extends ItemTouchHelper.Callback {

        Context mContext;
        private Paint mClearPaint;
        private ColorDrawable mBackground;
        private int backgroundColor;
        private Drawable deleteDrawable;
        private int intrinsicWidth;
        private int intrinsicHeight;


        SwipeToDeleteCallback(Context context) {
            mContext = context;
            mBackground = new ColorDrawable();
            backgroundColor = Color.parseColor("#b80f0a");
            mClearPaint = new Paint();
            mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            deleteDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_delete_sweep_white_24dp);
            intrinsicWidth = deleteDrawable.getIntrinsicWidth();
            intrinsicHeight = deleteDrawable.getIntrinsicHeight();
        }


        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();

            boolean isCancelled = (dX == 0) && !isCurrentlyActive;

            if (isCancelled) {
                clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }

            mBackground.setColor(backgroundColor);
            mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            mBackground.draw(c);

            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
            int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int deleteIconRight = itemView.getRight() - deleteIconMargin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;


            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
            deleteDrawable.draw(c);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
            c.drawRect(left, top, right, bottom, mClearPaint);

        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.6f;
        }
    }

    private void enableSwipeToDeleteAndUndo(final Activity activity, final TransactionsAdapter mAdapter, final RecyclerView recyclerView) {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Transaction item = mAdapter.transactions.getList().get(position);

                mAdapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(activity.findViewById(android.R.id.content), r.getString(R.string.transactionremovedfromthelist), Snackbar.LENGTH_LONG);
                snackbar.setAction(r.getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mAdapter.restoreItem(item, position);
                        recyclerView.scrollToPosition(position);
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }
}