package com.liang.webrtc_test1;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CryptoOptions;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.ScreenCapturerAndroid;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.net.URI;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

class VideoFormat{
    Integer width;
    Integer height;
    Integer min_fps;
    Integer max_fps;
}

public class MainActivity
        extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, View.OnClickListener{

    Activity currentActivity = this;
    int userID;

    MediaProjectionManager mediaProjectionManager;
    ActivityResultLauncher<Intent> activityResultLauncher;
    Intent intentRequestForScreenCapture;

    ServiceConnection serviceConnectionForVideoCapture;
    Intent intentForVideoCapture;
    MediaProjectionService.MediaProjectionServiceBinder videoCaptureServiceBinder;


    ArrayList<String> videoDevicesNames;
    int videoDeviceSelector = 0;
    int videoDeviceResolutionSelector = 0;
    int videoFpsSelector = 0;
    HashMap<String, ArrayList<VideoFormat>> videoDevicesMap;
    Spinner devicesSpinner;
    Spinner resolutionSpinner;
    Spinner fpsSpinner;
    Button button0;

    String camera1TrackID;
    String camera2TrackID;
    String screenTrackID;
    String tmp_string;
    ArrayList<VideoFormat> tmp_videoFormatList;

    PeerConnectionFactory peerConnectionFactory;
    EglBase eglBase;
    SurfaceTextureHelper surfaceTextureHelper;
    MediaConstraints mediaConstraints;
    VideoCapturer videoCapturer;
    VideoSource videoSource;
    VideoTrack videoTrack;
    RtpTransceiver videoTransceier;
    RtpTransceiver audioTransceiver;

    MyBroadcastReceiver myBroadcastReceiver;

    MyPeerConnectionObserver myPeerConnectionObserver;
    ArrayList<PeerConnection.IceServer> iceServers;
    PeerConnection peerConnection;

    public class MyPeerConnectionObserver implements PeerConnection.Observer{
        JSONObject jsonObject;

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            jsonObject = new JSONObject();
            try {
                jsonObject.put(getString(R.string.websocket_msg_type_header),
                        getResources().getInteger(R.integer.websocket_msg_type_normal));
                jsonObject.put(getString(R.string.websocket_msg_client_id_header), userID);
                jsonObject.put(getString(R.string.websocket_msg_client_type_header),
                        getResources().getInteger(R.integer.websocket_msg_client_type_phone));
                jsonObject.put(getString(R.string.websocket_msg_content_type_header),
                        getResources().getInteger(R.integer.websocket_msg_content_type_ice));
                jsonObject.put("sdpMid", iceCandidate.sdpMid);
                jsonObject.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                jsonObject.put("candidate", iceCandidate.sdp);

                Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                        getString(R.string.websocket_broadcast_msg_type_send));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                        jsonObject.toString());
                sendBroadcast(intent);
            }catch(Exception e){
                new AlertDialog.Builder(currentActivity)
                        .setTitle("ERROR")
                        .setMessage("send ice message error " + e.toString())
                        .create().show();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {
            peerConnection.createOffer(new MySdpObserver(), mediaConstraints);
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }
    }

    class MySdpObserver implements SdpObserver {
        JSONObject jsonObject;

        public MySdpObserver(){
        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            peerConnection.setLocalDescription(new MySdpObserver(), sessionDescription);
            jsonObject = new JSONObject();

            try{
                jsonObject.put(getString(R.string.websocket_msg_type_header),
                        getResources().getInteger(R.integer.websocket_msg_type_normal));
                jsonObject.put(getString(R.string.websocket_msg_client_id_header), userID);
                jsonObject.put(getString(R.string.websocket_msg_client_type_header),
                        getResources().getInteger(R.integer.websocket_msg_client_type_phone));
                jsonObject.put(getString(R.string.websocket_msg_content_type_header), 1);
                jsonObject.put("type", sessionDescription.type.canonicalForm());
                jsonObject.put("sdp", sessionDescription.description);

                Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
                        getString(R.string.websocket_broadcast_msg_type_send));
                intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
                        jsonObject.toString());

                sendBroadcast(intent);
            }catch(Exception e){
                new AlertDialog.Builder(currentActivity)
                        .setTitle("ERROR")
                        .setMessage("send sdp message error " + e.toString())
                        .create().show();
            }
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {
            new AlertDialog.Builder(currentActivity)
                    .setTitle("ERROR")
                    .setMessage("create sdp error " + s)
                    .create().show();
        }

        @Override
        public void onSetFailure(String s) {
            new AlertDialog.Builder(currentActivity)
                    .setTitle("ERROR")
                    .setMessage("set sdp error " + s)
                    .create().show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initWebRTCParts();
        initService();

        myBindService();

        listVideoCapture();
        beforeStartScreenCapture();
        startPeerConnection(); //in the end
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPeerConnection();
        myUnBindService();
        unregisterReceiver(myBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initWebRTCParts(){
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        PeerConnectionFactory.Options peerConnectionOptions = new PeerConnectionFactory.Options();
        peerConnectionOptions.disableEncryption = false;
        peerConnectionOptions.disableNetworkMonitor = false;

        eglBase = EglBase.create();
        peerConnectionFactory = PeerConnectionFactory
                .builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setOptions(peerConnectionOptions)
                .createPeerConnectionFactory();

        iceServers = new ArrayList<>();
        for(String tmp_string: getResources().getStringArray(R.array.iceServers)){
            iceServers.add(PeerConnection.IceServer.builder(tmp_string).createIceServer());
        }

        myPeerConnectionObserver = new MyPeerConnectionObserver();

        mediaConstraints = new MediaConstraints();
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext());

    }

    private void initData(){
        button0 = (Button)findViewById(R.id.button_start);
        button0.setOnClickListener(this);

        videoDevicesNames = new ArrayList<String>();
        videoDevicesMap = new HashMap<String, ArrayList<VideoFormat>>();
        devicesSpinner = (Spinner)findViewById(R.id.devicesSpinner);
        resolutionSpinner = (Spinner)findViewById(R.id.resolutionSpinner);
        fpsSpinner = (Spinner)findViewById(R.id.fpsSpinner);

        camera1TrackID = this.getResources().getString(R.string.camera1TrackID);
        camera2TrackID = this.getResources().getString(R.string.camera2TrackID);
        screenTrackID = this.getResources().getString(R.string.screenTrackID);

        Intent intent = getIntent();
        userID = intent.getIntExtra(getString(R.string.websocket_broadcast_msg_content_userid_header), 0);

        IntentFilter intentFilter = new IntentFilter(getString(R.string.websocket_broadcast_id));
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);
    }


    private void startPeerConnection(){
//        stopPeerConnection();

        if(peerConnection == null) {
            PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            configuration.enableDtlsSrtp = true;
            configuration.enableDscp = false;
            configuration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            peerConnection = peerConnectionFactory.createPeerConnection(configuration, myPeerConnectionObserver);
            peerConnection.setBitrate(getResources().getInteger(R.integer.webrtc_minBitrate),
                    getResources().getInteger(R.integer.webrtc_curBitrate),
                    getResources().getInteger(R.integer.webrtc_maxBitrate));
        }

        videoTransceier = peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO);
        videoTransceier.setDirection(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY);
        //audioTransceiver = peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO);

//        if(peerConnection.connectionState() == PeerConnection.PeerConnectionState.CLOSED
//            || peerConnection.connectionState() == PeerConnection.PeerConnectionState.DISCONNECTED
//            || peerConnection.connectionState() == PeerConnection.PeerConnectionState.NEW){
//            peerConnection.createOffer(new MySdpObserver(), mediaConstraints);
//        }
    }

    private void initService(){
        intentForVideoCapture = new Intent(this, MediaProjectionService.class);

        serviceConnectionForVideoCapture = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                videoCaptureServiceBinder = (MediaProjectionService.MediaProjectionServiceBinder)service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
    }

    private void stopPeerConnection(){
        if(peerConnection != null && peerConnection.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
            peerConnection.close();
        }
    }

    private void myBindService(){
        bindService(intentForVideoCapture, serviceConnectionForVideoCapture, BIND_AUTO_CREATE);
    }

    private void myUnBindService(){
        unbindService(serviceConnectionForVideoCapture);
    }

    private void beforeStartScreenCapture(){
        mediaProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        intentRequestForScreenCapture = mediaProjectionManager.createScreenCaptureIntent();

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                activityResult -> {
                    if(activityResult.getResultCode() != RESULT_OK){
                        videoCapturer = null;
                        new AlertDialog.Builder(this)
                                .setTitle("Warning")
                                .setMessage("User denied screen sharing permission")
                                .create().show();
                    }else{
                        videoCapturer = new ScreenCapturerAndroid(activityResult.getData(), new MediaProjection.Callback() {
                            @Override
                            public void onStop() {
                                super.onStop();
                            }
                        });

                        startVideoTransfer();
                    }
                });
    }

    private void listVideoCapture(){
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        Collections.addAll(videoDevicesNames, enumerator.getDeviceNames());

        if(videoDevicesNames.size() == 0){
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Can't find any cameras")
                    .create().show();
        }else {
            ArrayList<VideoFormat> videoFormats;
            for(String videoCaptureName : videoDevicesNames){
                videoFormats = new ArrayList<>();
                for(CameraEnumerationAndroid.CaptureFormat format: enumerator.getSupportedFormats(videoCaptureName)){
                    VideoFormat videoFormat = new VideoFormat();
                    videoFormat.height = Integer.valueOf(format.height);
                    videoFormat.width = Integer.valueOf(format.width);
                    videoFormat.min_fps = Integer.valueOf(format.framerate.min / 1000);
                    videoFormat.max_fps = Integer.valueOf(format.framerate.max / 1000);   //strange

                    videoFormats.add(videoFormat);
                }
                videoDevicesMap.put(videoCaptureName, videoFormats);
            }

            videoDevicesNames.add(getString(R.string.screenCaptureDeviveName));
            videoFormats = new ArrayList<>();
            videoFormats.add(getScreenSize());
            videoDevicesMap.put(getString(R.string.screenCaptureDeviveName), videoFormats);

            ArrayAdapter<String> arrayAdapter_devicesSpinner = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, videoDevicesNames);
            devicesSpinner.setAdapter(arrayAdapter_devicesSpinner);
            devicesSpinner.setOnItemSelectedListener(this);
        }
    }

    @Override  //for spinners
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int parentId = parent.getId();
        ArrayList<String> strings = new ArrayList<>();

        if(parentId == R.id.devicesSpinner){
            videoDeviceSelector = position;
            for(VideoFormat videoFormat: videoDevicesMap.get(videoDevicesNames.get(videoDeviceSelector))){
                strings.add(videoFormat.width.toString() + "*" + videoFormat.height.toString());
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, strings);
            resolutionSpinner.setOnItemSelectedListener(this);
            resolutionSpinner.setAdapter(arrayAdapter);
        }else if(parentId == R.id.resolutionSpinner){
            videoDeviceResolutionSelector = position;
            tmp_string = videoDevicesNames.get(videoDeviceSelector);
            tmp_videoFormatList = videoDevicesMap.get(tmp_string);

            for(int min = tmp_videoFormatList.get(position).min_fps; min <= tmp_videoFormatList.get(position).max_fps; ++min){
                strings.add(Integer.valueOf(min).toString());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, strings);
            fpsSpinner.setOnItemSelectedListener(this);
            fpsSpinner.setAdapter(arrayAdapter);
        }else if(parentId == R.id.fpsSpinner){
            videoFpsSelector = position;
        }else{
            Toast.makeText(this, "select error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override  //for spinners
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        int parentId = v.getId();

        if(parentId == R.id.button_start) {
//            Intent intent = new Intent(getString(R.string.websocket_broadcast_id));
//            intent.putExtra(getString(R.string.websocket_broadcast_msg_type_header),
//                    getString(R.string.websocket_broadcast_msg_type_control));
//            intent.putExtra(getString(R.string.websocket_broadcast_msg_content_header),
//                    getResources().getInteger(R.integer.websocket_broadcast_msg_content_control_stop_cast));
//            sendBroadcast(intent);

            button0.setText(getString(R.string.start_button_running_text));
            button0.setEnabled(false);

            if (videoDevicesNames.get(videoDeviceSelector).equals(getString(R.string.screenCaptureDeviveName))) {
                activityResultLauncher.launch(intentRequestForScreenCapture);
            } else {
                startCameraCapture(videoDevicesNames.get(videoDeviceSelector));
            }
        }
    }

    private void startVideoTransfer(){
        if(videoCapturer == null){
            return;
        }

        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        VideoFormat videoFormat = videoDevicesMap.get(videoDevicesNames.get(videoDeviceSelector)).get(videoDeviceResolutionSelector);

        videoCaptureServiceBinder.startCapture(videoCapturer, videoFormat, videoFpsSelector);

        videoTrack = peerConnectionFactory.createVideoTrack(camera1TrackID, videoSource);
        videoTransceier.getSender().setTrack(videoTrack, true);
    }

    private VideoFormat getScreenSize(){
        VideoFormat tmp_videoformat = new VideoFormat();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        tmp_videoformat.width = displayMetrics.widthPixels;
        tmp_videoformat.height = displayMetrics.heightPixels;
        tmp_videoformat.max_fps = getResources().getInteger(R.integer.screenCaptureFPS);
        tmp_videoformat.min_fps = getResources().getInteger(R.integer.screenCaptureFPS);

        return tmp_videoformat;
    }

    private void startCameraCapture(String deviceName){
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        videoCapturer = enumerator.createCapturer(deviceName, null);

        startVideoTransfer();
    }

    class MyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg_type = intent.getStringExtra(getString(R.string.websocket_broadcast_msg_type_header));

            try {
                if (msg_type.equals(getString(R.string.websocket_broadcast_msg_type_received))) {
                    JSONObject tmp_json_object = new JSONObject(intent.getStringExtra(getString(R.string.websocket_broadcast_msg_content_header)));
                    int content_type = tmp_json_object.getInt(getString(R.string.websocket_msg_content_type_header));
                    if(content_type == getResources().getInteger(R.integer.websocket_msg_content_type_sdp)){
                        peerConnection.setRemoteDescription(new MySdpObserver(),
                                new SessionDescription(SessionDescription.Type.fromCanonicalForm(tmp_json_object.getString("type")), tmp_json_object.getString("sdp")));
                    }else if(content_type == getResources().getInteger(R.integer.websocket_msg_content_type_ice)){
                        Log.i("my_msg", tmp_json_object.toString());
                        peerConnection.addIceCandidate(new IceCandidate(tmp_json_object.getString("sdpMid"),
                                tmp_json_object.getInt("sdpMLineIndex"),
                                tmp_json_object.getString("candidate")));
                    }
                }
            }catch(Exception ex){
                new AlertDialog.Builder(currentActivity)
                        .setTitle("ERROR")
                        .setMessage("parse json string error " + ex.toString())
                        .create().show();
            }
        }
    }

}