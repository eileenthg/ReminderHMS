package com.example.reminderhms;

import static com.huawei.hms.support.hwid.request.HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import com.example.reminderhms.databinding.ActivityReminderNewBinding;
import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.base.http.FileContent;
import com.huawei.cloud.base.media.MediaHttpDownloader;
import com.huawei.cloud.base.util.StringUtils;
import com.huawei.cloud.client.exception.DriveCode;
import com.huawei.cloud.services.drive.Drive;
import com.huawei.cloud.services.drive.DriveScopes;
import com.huawei.cloud.services.drive.model.File;
import com.huawei.cloud.services.drive.model.FileList;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;


import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.O)
public class NewReminderActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER = "com.example.android.reminderhms.NEW_REMINDER";
    public static final String EXTRA_TIME = "com.example.android.reminderhms.NEW_TIME"; //in unix timestamp

    public static final String TAG = "ActivityReminderNew";

    static final int CAMERA_REQ_CODE = 0;
    static String[] CAMERA_REQ_LIST = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
    static final int REQUEST_CODE_SCAN_ONE = 0;

    static Calendar cal = new Calendar.Builder().build();
    static Resources res;

    private ActivityReminderNewBinding binding;


    //pickers
    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener{

        EditText text;

        TimePickerFragment(EditText view){
            text = view;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState){
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            text.setText(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(cal.getTime()));
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        EditText text;

        DatePickerFragment(EditText view){
            text = view;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen by the user
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            text.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(cal.getTime()));
        }
    }

    // Use the onRequestPermissionsResult function to receive the permission verification result.
    //also boots QR scanner once succeeds
    final int PERMISSIONS_LENGTH = 2;
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //Log.v(TAG, "ping QR");
        // Check whether requestCode is set to the value of CAMERA_REQ_CODE during permission application, and then check whether the permission is enabled.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ_CODE && grantResults.length == PERMISSIONS_LENGTH && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

            // Call the barcode scanning API to build the scanning capability.
            // QRCODE_SCAN_TYPE are set for the barcode format, indicating that Scan Kit will support only QR code.
            HmsScanAnalyzerOptions options = new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE).create();
            int RESULT = ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, options);



        } else {
            //Log.v(TAG, "ping QR fail");
            Toast.makeText(getApplicationContext(), "Missing permissions", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            Log.v(TAG, "ping QR fail");
            return;
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            // Input an image for scanning and return the result.
            Log.v(TAG, "ping QR OK");
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                Log.v(TAG, "ping QR obj not null");
                // Display the parsing result.
                final JSONObject object;
                try {
                    object = new JSONObject(obj.originalValue);
                    cal.setTime(new Date(Long.parseLong(object.getString(ReminderQRActivity.JSON_TIME))));
                    binding.editTextTime.setText(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(cal.getTime()));
                    binding.editTextDate.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(cal.getTime()));
                    binding.editWord.setText(object.getString(ReminderQRActivity.JSON_TEXT));
                } catch (JSONException e) {
                    Log.v(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }


    }

    @SuppressLint("ClickableViewAccessibility") //TODO: learn how to override performClick()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();

        binding = ActivityReminderNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonQR.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Log.v(TAG, "ping onClick");
                requestPermissions(CAMERA_REQ_LIST, CAMERA_REQ_CODE);
            }
        });


        binding.editTextDate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    //Toast.makeText(getApplicationContext(),"Not implement. eh.", Toast.LENGTH_LONG).show();
                    //Log.v(TAG, "pinged date");
                    DialogFragment newFragment = new DatePickerFragment(binding.editTextDate);
                    newFragment.show(getSupportFragmentManager(), "datePicker");
                }

                //Log.v(TAG, "pinged other");
                return true;
            }
        });


        binding.editTextTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    //Toast.makeText(getApplicationContext(),"Not implement. eh.", Toast.LENGTH_LONG).show();
                    //Log.v(TAG, "pinged time");
                    DialogFragment newFragment = new TimePickerFragment(binding.editTextTime);
                    newFragment.show(getSupportFragmentManager(), "timePicker");
                }

                //Log.v(TAG, "pinged other");
                return true;
            }
        });

        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(binding.editTextDate.getText()) || TextUtils.isEmpty(binding.editTextTime.getText()) || TextUtils.isEmpty(binding.editWord.getText())){
                    Toast.makeText(getApplicationContext(),"Missing info", Toast.LENGTH_SHORT).show();
                } else {
                    if(cal.compareTo(Calendar.getInstance()) > 0) {

                        Intent replyIntent = new Intent();
                        String reminderText = binding.editWord.getText().toString();
                        replyIntent.putExtra(EXTRA_REMINDER, reminderText);

                        Long reminderTime = cal.getTime().getTime();
                        replyIntent.putExtra(EXTRA_TIME, reminderTime);
                        setResult(RESULT_OK, replyIntent);
                        finish();
                    } else
                        Toast.makeText(getApplicationContext(),"Reminder time cannot be earlier than current time.", Toast.LENGTH_LONG).show();
                }

            }
        });


    }
}