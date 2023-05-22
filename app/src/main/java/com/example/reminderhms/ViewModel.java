package com.example.reminderhms;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ViewModel extends AndroidViewModel {

    private ReminderRes mRepository;
    private LiveData<List<Reminder>> mAllReminders;
    private LiveData<Reminder> mLatestReminder;

    public ViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ReminderRes(application);
        mAllReminders = mRepository.getAllReminders();
        mLatestReminder = mRepository.getLatestReminder();
    }

    LiveData<List<Reminder>> getAllReminders() { return mAllReminders; } //wrapper

   public LiveData<Reminder> getLatestReminder(){return mRepository.getLatestReminder();}

    public void insert(Reminder reminder) { mRepository.insert(reminder); } //wrapper

    public void deleteReminder(Reminder reminder) {mRepository.deleteReminder(reminder);} //wrapper

}
