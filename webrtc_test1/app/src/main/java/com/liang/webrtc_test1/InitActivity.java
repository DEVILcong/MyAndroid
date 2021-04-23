package com.liang.webrtc_test1;

import android.annotation.SuppressLint;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import java.util.ArrayList;
import java.util.Map;

public class InitActivity extends AppCompatActivity {

    private Runnable runnableForStartActivity;
    private ActivityResultLauncher<String[]> activityResultLauncher;
    private Boolean isHaveAllPermissions = true;
    private Activity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        View mContentView = findViewById(R.id.fullscreen_content);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        currentActivity = this;

        runnableForStartActivity = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(currentActivity, ConnectActivity.class);
                startActivity(intent);
                currentActivity.finish();
            }
        };

        beforeRequestPermissions();
        requestPermissions();
    }

    private void beforeRequestPermissions(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    for(Map.Entry<String, Boolean> entry: result.entrySet()){
                        if(!entry.getValue()){
                            isHaveAllPermissions = false;
                            break;
                        }
                    }

                    if(!isHaveAllPermissions){
                        new AlertDialog.Builder(this)
                                .setTitle("Error")
                                .setMessage("This app can't work properly without these permisions")
                                .setNeutralButton("Quit", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        System.exit(0);
                                    }
                                })
                                .create().show();
                    }else{
                        new Handler().postDelayed(runnableForStartActivity, 2000);
                    }

                });
    }

    private void requestPermissions(){
        String[] needPermissions = getResources().getStringArray(R.array.permissions);
        String[] tmpArray_lostPermissions;
        ArrayList<String> lostPermissions = new ArrayList<>();

        for (String needPermission : needPermissions) {
            if (ContextCompat.checkSelfPermission(this, needPermission) == PERMISSION_DENIED) {
                lostPermissions.add(needPermission);
            }
        }

        if(lostPermissions.size() > 0){
            tmpArray_lostPermissions = lostPermissions.toArray(new String[0]);
            activityResultLauncher.launch(tmpArray_lostPermissions);
        }else{
            new Handler().postDelayed(runnableForStartActivity, 2000);
        }
    }


}