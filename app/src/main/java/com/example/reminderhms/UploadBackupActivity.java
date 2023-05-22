package com.example.reminderhms;

import static com.huawei.hms.support.hwid.request.HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.reminderhms.databinding.ActivityUploadBackupBinding;

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

import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadBackupActivity extends AppCompatActivity {

    private ActivityUploadBackupBinding binding;

    //Sign in credentials
    //private DriveCredential mCredential;
    // huawei account AT
    private String accessToken;
    private String unionId;

    // Define the request code for signInIntent.
    private static int REQUEST_SIGN_IN_LOGIN = 1002;

    // Define the log flag.
    private static final String TAG = "Account";

    //Other static stuff
    private static final String BACKUP_FILE_NAME = "AppDataBackUp.json";
    private static File mBackupFile;
    private static final String personalBackUpFolder = "ReminderHMS";

    private DriveCredential.AccessMethod refreshAT = new DriveCredential.AccessMethod() {
        // 此处做简单处理，正式使用请参考华为云空间服务开发者指南-客户端开发-存储鉴权信息章节
        @Override
        public String refreshToken() {
            return accessToken;
        }
    };

    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<String, String>();

    static {
        MIME_TYPE_MAP.put(".doc", "application/msword");
        MIME_TYPE_MAP.put(".jpg", "image/jpeg");
        MIME_TYPE_MAP.put(".mp3", "audio/x-mpeg");
        MIME_TYPE_MAP.put(".mp4", "video/mp4");
        MIME_TYPE_MAP.put(".pdf", "application/pdf");
        MIME_TYPE_MAP.put(".png", "image/png");
        MIME_TYPE_MAP.put(".txt", "text/plain");
        MIME_TYPE_MAP.put(".json", "application/json");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.huaweiIdAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                driveLogin();
            }
        });

        binding.backup.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                requestPermissions(DRIVE_REQ_LIST, UPLOAD_CODE);
            }
        });

        binding.restore.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                requestPermissions(DRIVE_REQ_LIST, DOWNLOAD_CODE);
            }
        });


        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void driveLogin() {
        List<Scope> scopeList = new ArrayList<>();
        //scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_APPDATA)); // Permissions to upload and store app data.
        scopeList.add(HuaweiIdAuthAPIManager.HUAWEIID_BASE_SCOPE); // Basic account permissions.

        HuaweiIdAuthParams authParams = new HuaweiIdAuthParamsHelper(DEFAULT_AUTH_REQUEST_PARAM)
                .setAccessToken()
                .setIdToken()
                .setScopeList(scopeList)
                .createParams();
        // Call the account API to obtain account information.
        HuaweiIdAuthService client = HuaweiIdAuthManager.getService(this, authParams);
        startActivityForResult(client.getSignInIntent(), REQUEST_SIGN_IN_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult, requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == REQUEST_SIGN_IN_LOGIN) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                accessToken = huaweiAccount.getAccessToken();
                unionId = huaweiAccount.getUnionId();
                int returnCode = CredentialManager.getInstance().init(unionId, accessToken, refreshAT);
                if (DriveCode.SUCCESS == returnCode) {
                    showTips("login ok");

                } else if (DriveCode.SERVICE_URL_NOT_ENABLED == returnCode) {
                    showTips("drive is not enabled");
                } else {
                    showTips("login error");
                }
            } else {
                Log.d(TAG, "onActivityResult, signIn failed: " + ((ApiException) authHuaweiIdTask.getException()).getStatusCode());
                Toast.makeText(getApplicationContext(), "onActivityResult, signIn failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showTips(final String toastText) {
        UploadBackupActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                binding.textView.setText(toastText);
            }
        });
    }

    private Drive buildDrive() {
        Drive service = new Drive.Builder(CredentialManager.getInstance().getCredential(), getApplicationContext()).build();
        return service;
    }



    //Permissions
    final int PERMISSIONS_LENGTH = 3;
    final int UPLOAD_CODE = 1010;
    final int DOWNLOAD_CODE = 1020;
    static String[] DRIVE_REQ_LIST = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NETWORK_STATE};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UPLOAD_CODE && grantResults.length == PERMISSIONS_LENGTH && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED&& grantResults[2] == PackageManager.PERMISSION_GRANTED) {
           doBackUpData();

        } else if (requestCode == DOWNLOAD_CODE && grantResults.length == PERMISSIONS_LENGTH && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED&& grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            doDownloadData();

        } else {
            //Log.v(TAG, "ping QR fail");
            Toast.makeText(getApplicationContext(), "Missing permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void doBackUpData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (accessToken == null) {
                        showTips("please click 'Login'.");
                        return;
                    }

                    Drive drive = buildDrive();

                    //get write file perms

                    java.io.File fileObject = new java.io.File("/sdcard/" + BACKUP_FILE_NAME);

                    //write the json to upload
                    Reminder[] list = ReminderRoomDB.getDatabase(getApplicationContext()).reminderDao().getAllReminderArray();

                    JSONArray jArr = new JSONArray();
                    for (Reminder reminder : list) {
                        try {
                            JSONObject json = new JSONObject()
                                    .put(ReminderQRActivity.JSON_TEXT, reminder.getText())
                                    .put(ReminderQRActivity.JSON_TIME, reminder.getTime());
                            jArr.put(json);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    FileWriter fw = new FileWriter(fileObject);
                    fw.write(jArr.toString());
                    fw.close();

                    Map<String, String> appProperties = new HashMap<>();
                    appProperties.put("appProperties", "property");
                    File file = new File();
                    file.setFileName(personalBackUpFolder + System.currentTimeMillis())
                            .setMimeType("application/vnd.huawei-apps.folder")
                            .setAppSettings(appProperties)
                            .setParentFolder(Collections.singletonList("applicationData"));
                    File directoryCreated = drive.files().create(file).execute();

                    String mimeType = mimeType(fileObject);
                    File content = new File();
                    content.setFileName(fileObject.getName())
                            .setMimeType(mimeType)
                            .setParentFolder(Collections.singletonList(directoryCreated.getId()));
                    mBackupFile = drive.files()
                            .create(content, new FileContent(mimeType, fileObject))
                            .setFields("*")
                            .execute();

                    showTips("Backup success");
                } catch (IOException e) {
                    Log.e(TAG, "BackUpData error: " + e.toString());
                    showTips("BackupData error");
                }
            }

        }).start();
    }

    private String mimeType(java.io.File file) {
        if (file != null && file.exists() && file.getName().contains(".")) {
            String fileName = file.getName();
            String suffix = fileName.substring(fileName.lastIndexOf("."));
            if (MIME_TYPE_MAP.keySet().contains(suffix)) {
                return MIME_TYPE_MAP.get(suffix);
            }
        }
        return "*/*";
    }

    private void doDownloadData() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {

                try {
                    if (accessToken == null) {
                        showTips("please click 'Login'.");
                        return;
                    }
                    ///*
                    Drive drive = buildDrive();
                    String containers = "applicationData";
                    String queryFile = "fileName = '" + BACKUP_FILE_NAME+ "'";
                    Drive.Files.List request = drive.files().list();
                    FileList files;
                    while (true) {
                        // Query a file.
                        files = request.setQueryParam(queryFile)
                                .setPageSize(10)
                                .setOrderBy("fileName")
                                .setFields("category,nextCursor,files/id,files/fileName,files/size")
                                .setContainers(containers).execute();
                        if (files == null || files.getFiles().size() <= 0) {
                            showTips("No backup found");
                            return;
                        }
                        String nextCursor = files.getNextCursor();
                        if (!StringUtils.isNullOrEmpty(nextCursor)) {
                            request.setCursor(files.getNextCursor());
                        } else {
                            break;
                        }
                    }
                    Log.v(TAG, "query ok");
                    // Download a file.
                    /*
                    if(backupFiles.size() == 0){

                        return;
                    }

                     */

                    mBackupFile = files.getFiles().get(files.getFiles().size() - 1);
                    long size = mBackupFile.getSize();
                    Drive.Files.Get get = drive.files().get(mBackupFile.getId());
                    MediaHttpDownloader downloader = get.getMediaHttpDownloader();

                    downloader.setContentRange(0, size - 1);
                    String restoreFileName = "restoreFileName.jpg";
                    java.io.File f = new java.io.File("/sdcard/" + restoreFileName);
                    get.executeContentAndDownloadTo(new FileOutputStream(f));

                    byte[] content = Files.readAllBytes(Paths.get(f.getPath()));
                    String jsonRAW = new String(content, StandardCharsets.UTF_8);
                    JSONArray jsonArray = new JSONArray(jsonRAW);

                    Reminder[] latestReminder = ReminderRoomDB.getDatabase(getApplicationContext()).reminderDao().getAnyReminder();
                    int id;
                    if (latestReminder.length == 0)
                        id = 0;
                    else
                        id = latestReminder[0].getId() + 1;

                    for (int count = 0; count < jsonArray.length(); count++, id++) {
                        String text = jsonArray.getJSONObject(count).getString(ReminderQRActivity.JSON_TEXT);
                        long time = jsonArray.getJSONObject(count).getLong(ReminderQRActivity.JSON_TIME);

                        if (time > Calendar.getInstance().getTime().getTime()) {

                            Reminder reminder = new Reminder(id, text, time);
                            ReminderRoomDB.getDatabase(getApplicationContext()).reminderDao().insert(reminder);
                            Intent intent = new Intent(getApplicationContext(), ReminderBroadcast.class);
                            intent.putExtra(ReminderBroadcast.EXTRA_ID, reminder.getId());
                            intent.putExtra(ReminderBroadcast.EXTRA_REMINDER, reminder.getText());
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), reminder.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getTime(), pendingIntent);
                        }
                    }
                    showTips("Backup load success");
                } catch (IOException e) {
                    Log.e(TAG, "LoadData error: " + e.toString());
                    showTips("LoadData error");
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error: " + e.toString());
                    showTips("JSON error");
                }
            }

        }).start();
    }
}