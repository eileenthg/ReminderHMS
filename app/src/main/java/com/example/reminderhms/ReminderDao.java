package com.example.reminderhms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Reminder reminder);

    @Query("SELECT * FROM reminder_table ORDER BY id ASC")
    LiveData<List<Reminder>> getAllReminders();

    @Query("SELECT * FROM reminder_table ORDER BY id ASC")
    Reminder[] getAllReminderArray();

    @Query("SELECT * FROM reminder_table ORDER BY id DESC LIMIT 1")
    Reminder[] getAnyReminder();

    @Query("SELECT * FROM reminder_table ORDER BY id DESC LIMIT 1")
    LiveData<Reminder> getLatestReminder();

    @Query("DELETE FROM reminder_table WHERE id = :userId")
    void deleteById(int userId);


    @Delete
    void deleteReminder(Reminder reminder);
}
