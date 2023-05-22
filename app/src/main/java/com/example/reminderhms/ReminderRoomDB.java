package com.example.reminderhms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


@SuppressLint("RestrictedApi")
@Database(entities = {Reminder.class}, version = 3, exportSchema = false)
public abstract class ReminderRoomDB extends RoomDatabase {
    public abstract ReminderDao reminderDao();
    private static ReminderRoomDB INSTANCE;


    public static ReminderRoomDB getDatabase(final Context context){
        if (INSTANCE == null){
            synchronized (ReminderRoomDB.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ReminderRoomDB.class, "reminder_database")
                            .fallbackToDestructiveMigration() //because I couldn't migrate for the life of me.
                            //.addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback(){

                @Override
                public void onOpen (@NonNull SupportSQLiteDatabase db){
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    /**
     * AsyncTask
     * Populate the database in the background.
     */
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final ReminderDao mDao;


        PopulateDbAsync(ReminderRoomDB db) {
            mDao = db.reminderDao();
        }

        @Override
        protected Void doInBackground(Void... voids) {


            // If we have no words, then create the initial list of words
            if (mDao.getAnyReminder().length < 1) {
                Reminder reminder = new Reminder(0, "Sample reminder", (long)1000000);
                mDao.insert(reminder);
                reminder = new Reminder(0, "Sample reminder2", (long)10002314);
                mDao.insert(reminder);

            }




            return null;
        }
    }


}
