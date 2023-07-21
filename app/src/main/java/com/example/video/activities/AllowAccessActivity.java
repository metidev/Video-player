package com.example.video.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import com.example.video.R;
import com.google.android.exoplayer2.util.Util;

public class AllowAccessActivity extends AppCompatActivity {
    Button allow_btn;
    public static final int STORAGE_PERMISSION_REQUEST = 1;
    public static final int STORAGE_PERMISSION_ABBOVE10 = 2;
    public static final int REQUEST_SETTINGS = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allow_access);
        allow_btn = findViewById(R.id.allow_access);
        SharedPreferences preferences = getSharedPreferences("AllowAccess", MODE_PRIVATE);

        String value = preferences.getString("Allow", "");
        if (value.equals("OK")) {
            startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
            finish();
        } else {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("Allow", "OK");
            editor.apply();
        }

        allow_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Util.SDK_INT < Build.VERSION_CODES.R) {

                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
                        finish();
                    } else {
                        ActivityCompat.requestPermissions(AllowAccessActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
                    }
                } else {
                    if (Environment.isExternalStorageManager()) {
                        startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
                        finish();
                    } else {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.addCategory("android.intent.category.DEFAULT");
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, STORAGE_PERMISSION_ABBOVE10);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            startActivityForResult(intent, STORAGE_PERMISSION_ABBOVE10);
                        }
                    }
                }
            }
        });

    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                String per = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    {
                        boolean showRational = shouldShowRequestPermissionRationale(per);
                        if (!showRational) {
                            //user clicked on never ask again
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("App permission")
                                    .setMessage("For playing videos in this app, you have to allow for access all then videos in this device.Allow access and we are ready to play go" + "\n\n" + "Now follow below step" + "\n\n" +
                                            "Open Setting From bellow button" + "\n"
                                            + "Click on Permissions" + "\n" + "Allow access for storage")
                                    .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivityForResult(intent, REQUEST_SETTINGS);
                                        }
                                    }).create().show();

                        } else {
                            ActivityCompat.requestPermissions(AllowAccessActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
                        }
                    }
                } else {
                    startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
                    finish();
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STORAGE_PERMISSION_ABBOVE10){
            startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
            finish();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(AllowAccessActivity.this, MainActivity.class));
            finish();
        }
    }
}