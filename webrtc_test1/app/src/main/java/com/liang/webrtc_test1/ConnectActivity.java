package com.liang.webrtc_test1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

public class ConnectActivity extends AppCompatActivity
    implements View.OnClickListener {

    EditText editText;
    Context context = this;
    Activity this_activity = this;
    int scanRequestCode = 2333;
    boolean isWebSocketServiceRunning = false;
    MyBroadcastReceiver myBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Button btn1 = findViewById(R.id.btn_connect_by_qrcode);
        btn1.setOnClickListener(this);

        btn1 = findViewById(R.id.btn_connect_by_addr);
        btn1.setOnClickListener(this);

        Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
        intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                getString(R.string.websocket_broadcast_msg_type_control));
        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_close_ws_service));
        sendBroadcast(intent);   //close existing websocket service

        editText = (EditText)findViewById(R.id.et1);
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(getString(R.string.websocket_broadcast_id));
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg_type = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_type_header));
            if(msg_type.equals(getString(R.string.websocket_broadcast_msg_type_error))){
                new AlertDialog.Builder(context)
                        .setTitle("connect error")
                        .setMessage(intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_header)) + " " +
                                intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_ws_error_reason)))
                        .create().show();
            }else if(msg_type.equals(getString(R.string.websocket_broadcast_msg_type_control))){
                if(intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_header), 0) ==
                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_open_success)) {
                    Intent intent1 = new Intent(context, MainActivity.class);
                    startActivity(intent1);
                    unregisterReceiver(myBroadcastReceiver);
                    this_activity.finish();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_connect_by_addr){
            if(!isWebSocketServiceRunning) {
                Intent intent = new Intent(this, WebSocketService.class);
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), getResources().getInteger(R.integer.websocket_broadcast_msg_content_fixed_userid));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_addr_header), editText.getText().toString());
                startService(intent);
                isWebSocketServiceRunning = true;
            }else{
                Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                        getString(R.string.websocket_broadcast_msg_type_control));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                        getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_ws_connect));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), getResources().getInteger(R.integer.websocket_broadcast_msg_content_fixed_userid));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_addr_header), editText.getText().toString());
                sendBroadcast(intent);
            }
            new AlertDialog.Builder(this)
                    .setTitle("Connecting")
                    .setMessage("Connecting, please wait")
                    .create().show();
        }else if(id == R.id.btn_connect_by_qrcode){
            HmsScanAnalyzerOptions options = new HmsScanAnalyzerOptions.Creator()
                    .setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE).create();
            ScanUtil.startScan(this, scanRequestCode, options);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK || data == null){
            return;
        }

        if(requestCode == scanRequestCode){
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if(!TextUtils.isEmpty(obj.getOriginalValue())){
                try {
                    JSONObject jsonObject = new JSONObject(obj.getOriginalValue());
                    if(!isWebSocketServiceRunning) {
                        Intent intent = new Intent(this, WebSocketService.class);
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_addr_header),
                                jsonObject.getString(getString(R.string.qrcode_msg_server_addr_header)));
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_userid_header),
                                jsonObject.getInt(getString(R.string.qrcode_msg_userid_header)));
                        startService(intent);
                        isWebSocketServiceRunning = true;
                    }else{
                        Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                                getString(R.string.websocket_broadcast_msg_type_control));
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                                getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_ws_connect));
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), jsonObject.getInt(getString(R.string.qrcode_msg_userid_header)));
                        intent.putExtra(getString(R.string.websocket_broadcast_msg_content_addr_header), jsonObject.getString(getString(R.string.qrcode_msg_server_addr_header)));
                        sendBroadcast(intent);
                    }
                }catch(Exception ex){
                    new AlertDialog.Builder(this)
                            .setTitle("ERROR")
                            .setMessage("read QRCode error\n" + ex.toString())
                            .create().show();
                }
            }
        }
    }
}