package com.example.reminderhms;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ReminderRes {
    private ReminderDao reminderDao;
    private LiveData<List<Reminder>> allReminders;
    private LiveData<Reminder> latestReminder;

    ReminderRes(Application application){
        ReminderRoomDB db = ReminderRoomDB.getDatabase(application);
        reminderDao = db.reminderDao();
        allReminders = reminderDao.getAllReminders();
       latestReminder = reminderDao.getLatestReminder();
    }

    LiveData<List<Reminder>> getAllReminders(){return allReminders;}

    public LiveData<Reminder> getLatestReminder(){return latestReminder;}


    public void insert (Reminder reminder) {new insertAsyncTask(reminderDao).execute(reminder);}

    public void deleteReminder(Reminder reminder) { new deleteReminderAsyncTask(reminderDao).execute(reminder); };


    private static class insertAsyncTask extends AsyncTask<Reminder, Void, Void> {
        private ReminderDao mAsyncTaskDao;

        public insertAsyncTask(ReminderDao reminderDao) {
            mAsyncTaskDao = reminderDao;
        }

        @Override
        protected Void doInBackground(Reminder... reminders) {
            mAsyncTaskDao.insert(reminders[0]);



            return null;
        }
    }

    private static class deleteReminderAsyncTask extends AsyncTask<Reminder, Void, Void> {
        private ReminderDao mAsyncTaskDao;

        deleteReminderAsyncTask(ReminderDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Reminder... params) {
            mAsyncTaskDao.deleteReminder(params[0]);
            return null;
        }
    }

}
