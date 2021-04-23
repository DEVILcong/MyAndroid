package com.liang.webrtc_test1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.net.URI;

import com.pusher.java_websocket.client.WebSocketClient;
import com.pusher.java_websocket.handshake.ServerHandshake;

import org.json.JSONObject;


public class WebSocketService extends Service {
    String addr;
    double userID;
    Service currentService = this;
    MWebSocketClient mWebSocketClient;
    boolean if_websocket_connected;
    MyBroadcastReceiver myBrdcastReceiver;
    NotificationManager notificationManager;
    NotificationChannel notificationChannel;

    public WebSocketService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userID = intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), 0);
        addr = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_addr_header));
        notificationManager = (NotificationManager)getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        notificationChannel = new NotificationChannel(getString(R.string.notification_channel_id_ws),
                getString(R.string.notification_channel_name_ws), NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);

        mWebSocketClient = new MWebSocketClient(URI.create(addr));
        mWebSocketClient.connect();

        myBrdcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(getString(R.string.websocket_broadcast_id));
        registerReceiver(myBrdcastReceiver, intentFilter);
        
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg_type = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_type_header));
            if(msg_type.equals(getString(R.string.websocket_broadcast_msg_type_send))){
                mWebSocketClient.send(intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_header)));
            }else if(msg_type.equals(getString(R.string.websocket_broadcast_msg_type_control))){
                int action_id = intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_header), 0);
                if(action_id == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_close_ws)){
                    mWebSocketClient.close();
                }else if(action_id == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_close_ws_service)){
//                    mWebSocketClient.close();
//                    unregisterReceiver(myBrdcastReceiver);
                    stopSelf();
                }else if(action_id == getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_ws_connect)){
                    addr = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_addr_header));
                    userID = intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), 0);
                    if(mWebSocketClient != null){
                        mWebSocketClient = new MWebSocketClient(URI.create(addr));
                        mWebSocketClient.connect();
                    }else{
                        if(mWebSocketClient.isOpen()){
                            try{
                                mWebSocketClient.closeBlocking();
                            }catch(Exception ex){

                            }
                        }
                        mWebSocketClient = new MWebSocketClient(URI.create(addr));
                        mWebSocketClient.connect();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mWebSocketClient.close();
        unregisterReceiver(myBrdcastReceiver);
    }

    class MWebSocketClient extends WebSocketClient{
        public MWebSocketClient(URI uri){
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            if_websocket_connected = true;

            JSONObject tmp_json_object = new JSONObject();
            try {
                tmp_json_object.put(getString(R.string.websocket_msg_type_header),
                        getResources().getInteger(R.integer.websocket_msg_type_handshake));
                tmp_json_object.put(getString(R.string.websocket_msg_client_id_header), userID);
                tmp_json_object.put(getString(R.string.websocket_msg_client_type_header),
                        getResources().getInteger(R.integer.websocket_msg_client_type_phone));

                this.send(tmp_json_object.toString());

                Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header), getString(R.string.websocket_broadcast_msg_type_control));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                        getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_open_success));
                sendBroadcast(intent);
            }catch(Exception ex){

            }
        }

        @Override
        public void onMessage(String message) {
            Intent intent = new Intent();
            intent.setAction(getString(R.string.websocket_broadcast_id));
            intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                    getString(R.string.websocket_broadcast_msg_type_received));
            intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                    message);
            sendBroadcast(intent);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Notification notification = new Notification.Builder(getApplicationContext(), getString(R.string.notification_channel_id_ws))
                    .setContentTitle("WebSocket info")
                    .setContentText("WebSocket connection is closed")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(getResources().getInteger(R.integer.notification_id_ws_close), notification);
        }

        @Override
        public void onError(Exception ex) {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("WebSocket Error");
            bigTextStyle.setSummaryText("WebSocket Error");
            bigTextStyle.bigText("WebSocket error " + ex.toString());

            Intent intent1 = new Intent();
            intent1.setAction(getString(R.string.websocket_broadcast_id));
            intent1.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                    getString(R.string.websocket_broadcast_msg_type_error));
            intent1.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                    getString(R.string.websocket_error_connect));
            intent1.putExtra(getString(R.string.websocket_broadcast_msg_content_ws_error_reason),
                    ex.toString());
            sendBroadcast(intent1);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.notification_channel_id_ws))
                    .setStyle(bigTextStyle)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify(getResources().getInteger(R.integer.notification_id_ws_error), notification);
        }
    }
}