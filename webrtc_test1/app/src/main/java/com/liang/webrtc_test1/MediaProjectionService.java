package com.liang.webrtc_test1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import org.webrtc.VideoCapturer;

public class MediaProjectionService extends Service {
    //private Thread workerThread;
    private VideoCapturer videoCapturer;
    private VideoFormat videoFormat;
    private int videoFPSSelector;
    private MediaProjectionServiceBinder mediaProjectionServiceBinder;
    private Boolean isRunning = false;

    Service service = this;

    NotificationManager notificationManager;
    NotificationChannel notificationChannel;
    RemoteViews remoteViews;
    MyBroadcastReceiver myBroadcastReceiver;

    public MediaProjectionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionServiceBinder = new MediaProjectionServiceBinder();

        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(getString(R.string.websocket_broadcast_id));
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mediaProjectionServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(getString(R.string.notification_channel_id_screen),
                getString(R.string.notification_channel_name_screen),
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(notificationChannel);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }



    private void createNotification() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(getString(R.string.notification_channel_id_camera),
                getString(R.string.notification_channel_name_camera),
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent intent_pause = new Intent(getString(R.string.websocket_broadcast_id));
        intent_pause.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                getString(R.string.websocket_broadcast_msg_type_control));
        intent_pause.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_pause_cast));

        Intent intent_resume = new Intent(getString(R.string.websocket_broadcast_id));
        intent_resume.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                getString(R.string.websocket_broadcast_msg_type_control));
        intent_resume.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_resume_cast));

        Intent intent_stop = new Intent(getString(R.string.websocket_broadcast_id));
        intent_stop.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                getString(R.string.websocket_broadcast_msg_type_control));
        intent_stop.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_stop_cast));

        remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        if(videoCapturer.isScreencast())
            remoteViews.setTextViewText(R.id.notification_tv1, "screen cast is running");
        else
            remoteViews.setTextViewText(R.id.notification_tv1, "camera cast is running");
        remoteViews.setOnClickPendingIntent(R.id.notification_pause, PendingIntent.getBroadcast(this, 4, intent_pause, 0));
        remoteViews.setOnClickPendingIntent(R.id.notification_resume, PendingIntent.getBroadcast(this, 5, intent_resume, 0));
        remoteViews.setOnClickPendingIntent(R.id.notification_stop, PendingIntent.getBroadcast(this, 6, intent_stop, 0));
        remoteViews.setBoolean(R.id.notification_resume, "setEnabled", false);

        Notification notification = new Notification.Builder(service, getString(R.string.notification_channel_id_camera))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(remoteViews)
                .build();

        startForeground(getResources().getInteger(R.integer.notification_id_camera_service), notification);
    }

    class MediaProjectionServiceBinder extends Binder{
        public void startCapture(VideoCapturer videoCapturer1, VideoFormat videoFormat1, int videoFPSSelector1){
            videoCapturer = videoCapturer1;
            videoFormat = videoFormat1;
            videoFPSSelector = videoFPSSelector1;
            isRunning = true;
            createNotification();

            videoCapturer.startCapture((int)(videoFormat.width * 0.6), (int)(videoFormat.height * 0.6), videoFormat.max_fps - videoFPSSelector);
        }

        public void stopCapture() {
            if (isRunning) {
                try {
                    isRunning = false;
                    videoCapturer.stopCapture();
                    stopForeground(true);
                } catch (Exception ex) {
                    //don't know how to handle this
                }
            }
        }
    }

    class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg_type = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_type_header));
            if(msg_type.equals(getString(R.string.websocket_broadcast_msg_type_control))){
                int action = intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_header), 0);
                if(action == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_stop_cast)){
                    if(isRunning){
                        try{
                            isRunning = false;
                            videoCapturer.stopCapture();
                            stopForeground(true);
                            unregisterReceiver(myBroadcastReceiver);
                            Intent intent1 = new Intent(service, MediaProjectionService.class);
                            stopService(intent1);
                        }catch(Exception ex){

                        }
                    }
                }else if(action == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_pause_cast)){
                    if(isRunning) {
                        try {
                            isRunning = false;
                            videoCapturer.stopCapture();
                        } catch (Exception ex) {

                        }

                        if(videoCapturer.isScreencast())
                            remoteViews.setTextViewText(R.id.notification_tv1, "screen cast is paused");
                        else
                            remoteViews.setTextViewText(R.id.notification_tv1, "camera cast is paused");
                        remoteViews.setBoolean(R.id.notification_resume, "setEnabled", true);
                        remoteViews.setBoolean(R.id.notification_pause, "setEnabled", false);

                        Notification notification = new Notification.Builder(service, getString(R.string.notification_channel_id_camera))
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setCustomContentView(remoteViews)
                                .build();

                        notificationManager.notify(getResources().getInteger(R.integer.notification_id_camera_service), notification);
                    }
                }else if(action == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_resume_cast)){
                    isRunning = true;
                    videoCapturer.startCapture(videoFormat.width, videoFormat.height, videoFormat.max_fps - videoFPSSelector);

                    if(videoCapturer.isScreencast())
                        remoteViews.setTextViewText(R.id.notification_tv1, "screen cast is running");
                    else
                        remoteViews.setTextViewText(R.id.notification_tv1, "camera cast is running");
                    remoteViews.setBoolean(R.id.notification_pause, "setEnabled", true);
                    remoteViews.setBoolean(R.id.notification_resume, "setEnabled", false);

                    Notification notification = new Notification.Builder(service, getString(R.string.notification_channel_id_camera))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setCustomContentView(remoteViews)
                            .build();

                    notificationManager.notify(getResources().getInteger(R.integer.notification_id_camera_service), notification);
                }else{
                }
            }
        }
    }
}