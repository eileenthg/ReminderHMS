package com.example.reminderhms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.reminderhms.databinding.ActivityMainBinding;
import com.example.reminderhms.databinding.ActivityReminderQractivityBinding;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.hmsscankit.WriterException;
import com.huawei.hms.ml.scan.HmsBuildBitmapOption;
import com.huawei.hms.ml.scan.HmsScan;

import java.util.Calendar;

public class ReminderQRActivity extends AppCompatActivity {

    private ActivityReminderQractivityBinding binding;

    private static final String LOG = "ReminderQR";
    public static final String EXTRA_REMINDER = "com.example.android.reminderhms.NEW_REMINDER";

    //json parameters
    public static final String JSON_TEXT = "Reminder_text";
    public static final String JSON_TIME = "Reminder_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.v(LOG, "create view");
        super.onCreate(savedInstanceState);
        binding = ActivityReminderQractivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent data = getIntent();
        String json = data.getStringExtra(EXTRA_REMINDER);
        int type = HmsScan.QRCODE_SCAN_TYPE;
        int width = 400;
        int height = 400;

        HmsBuildBitmapOption options = null;
        /*
        setBitmapBackgroundColor(): Sets the background color of a barcode. If you do not call this API, white (Color.WHITE) is used by default.
        setBitmapColor(): Sets the barcode color. If you do not call this API, black (Color.BLACK) is used by default.
        setBitmapMargin(): Sets the border width of a barcode. If you do not call this API, the default border width 1 is used.
         */

        try {
            // If the HmsBuildBitmapOption object is not constructed, set options to null.
            Bitmap qrBitmap = ScanUtil.buildBitmap(json, type, width, height, options);
            binding.imageView.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            Log.w("buildBitmap", e);
        }

        binding.buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.v(LOG, "back");
                finish();
            }
        });
    }
}