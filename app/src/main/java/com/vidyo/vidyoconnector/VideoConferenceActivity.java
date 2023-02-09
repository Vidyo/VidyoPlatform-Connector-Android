package com.vidyo.vidyoconnector;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Device.LocalMicrophone;
import com.vidyo.VidyoClient.Device.LocalSpeaker;
import com.vidyo.VidyoClient.Endpoint.LogRecord;
import com.vidyo.VidyoClient.Endpoint.Participant;
import com.vidyo.vidyoconnector.event.ControlEvent;
import com.vidyo.vidyoconnector.event.IControlEventHandler;
import com.vidyo.vidyoconnector.utils.AppUtils;
import com.vidyo.vidyoconnector.utils.FontsUtils;
import com.vidyo.vidyoconnector.utils.Logger;
import com.vidyo.vidyoconnector.view.ControlView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Conference activity holding all connection and callbacks logic.
 */
public class VideoConferenceActivity extends FragmentActivity implements Connector.IConnect,
        Connector.IRegisterLocalCameraEventListener,
        Connector.IRegisterLocalMicrophoneEventListener,
        Connector.IRegisterLocalSpeakerEventListener,
        Connector.IRegisterLogEventListener,
        Connector.IRegisterParticipantEventListener,
        IControlEventHandler, View.OnLayoutChangeListener {

    public static final String PORTAL_KEY = "portal.key";
    public static final String ROOM_KEY = "room.key";
    public static final String PIN_KEY = "pin.key";
    public static final String NAME_KEY = "name.key";

    private FrameLayout videoView;
    private ControlView controlView;
    private View progressBar;

    private Connector connector;
    private LocalCamera lastSelectedLocalCamera;

    private LocalSpeaker localSpeaker;

    private final AtomicBoolean isCameraDisabledForBackground = new AtomicBoolean(false);
    private final AtomicBoolean isDisconnectAndQuit = new AtomicBoolean(false);

    private final List<LocalCamera> localCameraList = new LinkedList<>();

    @Override
    public void onStart() {
        super.onStart();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        if (connector != null) {
            ControlView.State state = controlView.getState();
            connector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);

            connector.setCameraPrivacy(state.isMuteCamera());
        }

        if (connector != null && lastSelectedLocalCamera != null && isCameraDisabledForBackground.getAndSet(false)) {
            connector.selectLocalCamera(lastSelectedLocalCamera);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (connector != null) {
            connector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background);
            connector.setCameraPrivacy(true);
        }

        if (!isFinishing() && connector != null && !controlView.getState().isMuteCamera() && !isCameraDisabledForBackground.getAndSet(true)) {
            connector.selectLocalCamera(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_conference);

        ConnectorPkg.setApplicationUIContext(this);

        videoView = findViewById(R.id.video_frame);

        progressBar = findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);

        controlView = findViewById(R.id.control_view);
        controlView.registerListener(this);

        String logLevel = "warning debug@VidyoClient " +
                "all@LmiPortalSession  all@LmiPortalMembership info@LmiResourceManagerUpdates " +
                "info@LmiPace info@LmiIce all@LmiSignaling info@VidyoCameraEffect";

        connector = new Connector(videoView, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 8,
                logLevel, AppUtils.configLogFile(this), 0);
        Logger.i("Connector instance has been created.");

        FontsUtils fontsUtils = new FontsUtils(this);
        connector.setFontFileName(fontsUtils.fontFile().getPath());

        controlView.showVersion(connector.getVersion());

        connector.registerLocalCameraEventListener(this);
        connector.registerLocalMicrophoneEventListener(this);
        connector.registerLocalSpeakerEventListener(this);
        connector.registerParticipantEventListener(this);

        connector.registerLogEventListener(this, logLevel);
//        connector.setCertificateAuthorityFile(AppUtils.writeCaCertificates(this));

        /* Await view availability */
        videoView.addOnLayoutChangeListener(this);
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        view.removeOnLayoutChangeListener(this);

        int width = view.getWidth();
        int height = view.getHeight();

        connector.showViewAt(view, 0, 0, width, height);
        Logger.i("Show View at: " + width + ", " + height);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        new Handler().postDelayed(this::updateView, DateUtils.SECOND_IN_MILLIS * 2);
    }

    @Override
    public void onSuccess() {
        runOnUiThread(() -> {
            Toast.makeText(VideoConferenceActivity.this, R.string.connected, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);

            controlView.connectedCall(true);
            controlView.updateConnectionState(ControlView.ConnectionState.CONNECTED);
            controlView.disable(false);

            startAudioDebugging();
        });
    }

    @Override
    public void onFailure(final Connector.ConnectorFailReason connectorFailReason) {
        Logger.i("onFailure: %s", connectorFailReason.name());

        runOnUiThread(() -> {
            Toast.makeText(VideoConferenceActivity.this, connectorFailReason.name(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);

            controlView.connectedCall(false);
            controlView.updateConnectionState(ControlView.ConnectionState.FAILED);
            controlView.disable(false);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            stopAudioDebugging();
        });
    }

    @Override
    public void onDisconnected(Connector.ConnectorDisconnectReason connectorDisconnectReason) {
        Logger.i("onDisconnected: %s", connectorDisconnectReason.name());

        runOnUiThread(() -> {
            Toast.makeText(VideoConferenceActivity.this, R.string.disconnected, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);

            controlView.connectedCall(false);
            controlView.updateConnectionState(ControlView.ConnectionState.DISCONNECTED);
            controlView.disable(false);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            stopAudioDebugging();

            /* Wrap up the conference */
            if (isDisconnectAndQuit.get()) {
                finish();
            }
        });
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        if (connector == null) return;

        switch (event.getCall()) {
            case CONNECT_DISCONNECT:
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                progressBar.setVisibility(View.VISIBLE);
                controlView.disable(true);
                boolean state = (boolean) event.getValue();
                controlView.updateConnectionState(state ? ControlView.ConnectionState.CONNECTING : ControlView.ConnectionState.DISCONNECTING);

                if (state) {
                    Intent intent = getIntent();

                    String portal = intent.getStringExtra(PORTAL_KEY);
                    String room = intent.getStringExtra(ROOM_KEY);
                    String pin = intent.getStringExtra(PIN_KEY);
                    String name = intent.getStringExtra(NAME_KEY);

                    Logger.i("Start connection: %s, %s, %s, %s", portal, room, pin, name);
                    connector.connectToRoomAsGuest(portal, name, room, pin, this);
                } else {
                    Logger.i("Disconnect initiated by user.");
                    if (connector != null) connector.disconnect();
                }
                break;
            case MUTE_CAMERA:
                boolean cameraPrivacy = (boolean) event.getValue();
                connector.setCameraPrivacy(cameraPrivacy);

                if (cameraPrivacy) {
                    connector.selectLocalCamera(null);
                } else {
                    if (lastSelectedLocalCamera != null)
                        connector.selectLocalCamera(lastSelectedLocalCamera);
                    else
                        connector.selectDefaultCamera();
                }
                break;
            case MUTE_MIC:
                connector.setMicrophonePrivacy((boolean) event.getValue());
                break;
            case MUTE_SPEAKER:
                connector.setSpeakerPrivacy((boolean) event.getValue());
                break;
            case CYCLE_CAMERA:
                for (LocalCamera localCamera : this.localCameraList)
                    if (localCamera.getPosition() != lastSelectedLocalCamera.getPosition()) {
                        Logger.i("Going to select: %s local camera", localCamera.getName());
                        connector.selectLocalCamera(localCamera);
                        break;
                    }
                break;
            case DEBUG_OPTION:
                boolean value = (boolean) event.getValue();
                if (value) {
                    connector.enableDebug(7776, "");
                } else {
                    connector.disableDebug();
                }

                Toast.makeText(VideoConferenceActivity.this, getString(R.string.debug_option) + value, Toast.LENGTH_SHORT).show();
                break;
            case SEND_LOGS:
                AppUtils.sendLogs(this);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (connector == null) {
            Logger.e("Connector is null!");
            finish();
            return;
        }

        Connector.ConnectorState state = connector.getState();

        if (state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Idle || state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Ready) {
            super.onBackPressed();
        } else {
            /* You are still connecting or connected */
            Toast.makeText(this, "You have to disconnect or await connection first", Toast.LENGTH_SHORT).show();

            /* Start disconnection if connected. Quit afterward. */
            if (state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Connected && !isDisconnectAndQuit.get()) {
                isDisconnectAndQuit.set(true);
                onControlEvent(new ControlEvent<>(ControlEvent.Call.CONNECT_DISCONNECT, false));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controlView != null) controlView.unregisterListener();

        if (connector != null) {
            connector.hideView(videoView);
            connector.disable();
            connector = null;
        }

        ConnectorPkg.setApplicationUIContext(null);
        Logger.i("Connector instance has been released.");
    }

    @Override
    public void onLocalCameraAdded(LocalCamera localCamera) {
        if (localCamera != null) {
            Logger.i("onLocalCameraAdded: %s", localCamera.name);
            localCameraList.add(localCamera);
        }
    }

    @Override
    public void onLocalCameraSelected(final LocalCamera localCamera) {
        if (localCamera != null) {
            Logger.i("onLocalCameraSelected: %s", localCamera.name);
            this.lastSelectedLocalCamera = localCamera;

//            localCamera.setTargetBitRate(800000);
//            localCamera.setMaxConstraint(320, 240, 1_000_000_000 / 5);
        }
    }

    @Override
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState deviceState) {

    }

    @Override
    public void onLocalCameraRemoved(LocalCamera localCamera) {
        if (localCamera != null) {
            Logger.i("onLocalCameraRemoved: %s", localCamera.name);
        }
    }

    @Override
    public void onLog(LogRecord logRecord) {
        /* Write log into a custom file */
    }

    private void updateView() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        FrameLayout.LayoutParams videoViewParams = new FrameLayout.LayoutParams(width, height);
        videoView.setLayoutParams(videoViewParams);

        videoView.addOnLayoutChangeListener(this);
        videoView.requestLayout();
    }

    private void startAudioDebugging() {
        if (localSpeaker == null) return;
        File dir = new File(getFilesDir(), "AudioRecording/Speaker");
        localSpeaker.enableDebugRecordings(dir.getAbsolutePath());
    }

    private void stopAudioDebugging() {
        if (localSpeaker == null) return;
        localSpeaker.disableDebugRecordings();
    }

    @Override
    public void onLocalMicrophoneAdded(LocalMicrophone localMicrophone) {

    }

    @Override
    public void onLocalMicrophoneRemoved(LocalMicrophone localMicrophone) {

    }

    @Override
    public void onLocalMicrophoneSelected(LocalMicrophone localMicrophone) {
    }

    @Override
    public void onLocalMicrophoneStateUpdated(LocalMicrophone localMicrophone, Device.DeviceState deviceState) {

    }

    @Override
    public void onLocalSpeakerAdded(LocalSpeaker localSpeaker) {

    }

    @Override
    public void onLocalSpeakerRemoved(LocalSpeaker localSpeaker) {

    }

    @Override
    public void onLocalSpeakerSelected(LocalSpeaker localSpeaker) {
        this.localSpeaker = localSpeaker;
    }

    @Override
    public void onLocalSpeakerStateUpdated(LocalSpeaker localSpeaker, Device.DeviceState deviceState) {

    }

    @Override
    public void onParticipantJoined(Participant participant) {
        Logger.i("Participant joined: %s", participant.getUserId());
    }

    @Override
    public void onParticipantLeft(Participant participant) {
        Logger.i("Participant left: %s", participant.getUserId());
    }

    @Override
    public void onDynamicParticipantChanged(ArrayList<Participant> arrayList) {

    }

    @Override
    public void onLoudestParticipantChanged(Participant participant, boolean b) {

    }
}