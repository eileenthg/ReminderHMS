package com.example.reminderhms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;


import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.reminderhms.databinding.ActivityMainBinding;
import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.client.exception.DriveCode;
import com.huawei.cloud.services.drive.DriveScopes;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //TODO: Display who is signed in.
    //TODO: Implement sign out, cancel auth
    //TODO: Actual backup procedure

    private ActivityMainBinding binding;
    private ViewModel mReminderViewModel;
    private Reminder latestReminder;

    protected static final String APP_ID = "104811659";

    ActivityResultLauncher<Intent> insertLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();

                        int id;
                        if(latestReminder == null)
                            id = 0;
                        else
                            id = latestReminder.getId() + 1;

                        String text = data.getStringExtra(NewReminderActivity.EXTRA_REMINDER);
                        Long time = data.getLongExtra(NewReminderActivity.EXTRA_TIME, 0);

                        Reminder reminder = new Reminder(id, text, time);
                        mReminderViewModel.insert(reminder);
                        Intent intent = new Intent(MainActivity.this, ReminderBroadcast.class);
                        intent.putExtra(ReminderBroadcast.EXTRA_ID, reminder.getId());
                        intent.putExtra(ReminderBroadcast.EXTRA_REMINDER, reminder.getText());
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, reminder.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getTime(), pendingIntent);
                    }
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        createNotificationChannel();

        RecyclerView recyclerView = findViewById((R.id.recyclerview));
        final ReminderAdapter adapter = new ReminderAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mReminderViewModel = new ViewModelProvider(this).get(ViewModel.class);

        mReminderViewModel.getAllReminders().observe(this, new Observer<List<Reminder>>(){

            @Override
            public void onChanged(List<Reminder> reminders) {
                adapter.setReminders(reminders);
            }
        });

        mReminderViewModel.getLatestReminder().observe(this, new Observer<Reminder>(){

            @Override
            public void onChanged(Reminder reminders) {
                latestReminder = reminders;
            }
        });


        // Add the functionality to swipe items in the
        // recycler view to delete that item
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Reminder myReminder = adapter.getReminderAtPosition(position);


                        Toast.makeText(MainActivity.this, "Deleting... ", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, ReminderBroadcast.class);
                        intent.putExtra(ReminderBroadcast.EXTRA_ID, myReminder.getId());
                        intent.putExtra(ReminderBroadcast.EXTRA_REMINDER, myReminder.getText());
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, myReminder.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(pendingIntent);
                        mReminderViewModel.deleteReminder(myReminder);
                    }
                });

        helper.attachToRecyclerView(recyclerView);


        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplicationContext(),"Not implement yet", Toast.LENGTH_LONG).show();
                ///*
                Intent intent = new Intent(MainActivity.this, NewReminderActivity.class);
                insertLauncher.launch(intent);
                 //*/
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //backup service page
            Intent intent = new Intent(getApplicationContext(), UploadBackupActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        CharSequence name = "ReminderHMS Channel";
        String description = "Channel for ReminderHMS";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(ReminderBroadcast.CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

}