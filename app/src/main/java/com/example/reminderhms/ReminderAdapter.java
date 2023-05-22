package com.example.reminderhms;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>{
    private final LayoutInflater mInflater;
    private List<Reminder> mReminders;
    private Context context;

    ReminderAdapter(Context context){
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.reminder_item, parent, false);
        return new ReminderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {

        if(mReminders != null){
            Reminder current = mReminders.get(position);
            holder.reminderTextView.setText(current.getText());

            Date date = new Date(current.getTime());
            holder.timeTextView.setText(DateFormat.getDateTimeInstance(DateFormat.DEFAULT,DateFormat.SHORT).format(date)); //implement convert from unix to proper

        } else {
           holder.reminderTextView.setText(R.string.loading);
           holder.timeTextView.setText(R.string.loading);
        }
    }

    void setReminders(List<Reminder> reminders){
        mReminders = reminders;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(mReminders != null)
            return mReminders.size();
        else return 0;
    }

    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView reminderTextView;
        private final TextView timeTextView;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            reminderTextView = itemView.findViewById((R.id.reminderText));
            timeTextView = itemView.findViewById(R.id.reminderTime);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ReminderQRActivity.class);
                    Reminder reminder = mReminders.get(getLayoutPosition());

                    try {
                        String json = new JSONObject()
                                .put(ReminderQRActivity.JSON_TEXT, reminder.getText())
                                .put(ReminderQRActivity.JSON_TIME, reminder.getTime())
                                .toString();
                        intent.putExtra(ReminderQRActivity.EXTRA_REMINDER, json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    context.startActivity(intent);
                }
            });
        }
    }

    public Reminder getReminderAtPosition (int position) {return mReminders.get(position);}


}
