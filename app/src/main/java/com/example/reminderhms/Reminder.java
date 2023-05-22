package com.example.reminderhms;

import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;

@Entity(tableName = "reminder_table")
public class Reminder {
    @PrimaryKey
    @NonNull
    private int id; //id used as request code AND notif id
    private String text;

    @NonNull
    private Long time; //Unix time stamp

    //constructor

    public Reminder(@NonNull int id, String text, @NonNull Long time) {
        this.id = id;
        this.text = text;
        this.time = time;
    }

    //getter
    public int getId(){return id;}
    public String getText(){return text;}
    public Long getTime(){return time;}
}
