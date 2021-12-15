package com.vidyo.vidyoconnector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.vidyoconnector.connect.ConnectParams;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

public class HomeActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int PERMISSIONS_REQUEST_ALL = 0x7c9;

    private TextView portal;
    private TextView room;
    private TextView pin;
    private TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ConnectorPkg.initialize();

        initCredentials();

        enable(false);
        requestPermissions();
    }

    private void initCredentials() {
        portal = findViewById(R.id.portal_field);
        portal.setText(ConnectParams.PORTAL_HOST);

        room = findViewById(R.id.room_field);
        room.setText(ConnectParams.ROOM_KEY);

        pin = findViewById(R.id.pin_field);
        pin.setText(ConnectParams.ROOM_PIN);

        name = findViewById(R.id.name_field);
        name.setText(ConnectParams.ROOM_DISPLAY_NAME);

        TextView note = findViewById(R.id.note);
        note.setText(Html.fromHtml(getString(R.string.note)));
    }

    public void onStartConference(View view) {
        Intent start = new Intent(this, VideoConferenceActivity.class);

        start.putExtra(VideoConferenceActivity.PORTAL_KEY, portal.getText().toString());
        start.putExtra(VideoConferenceActivity.ROOM_KEY, room.getText().toString());
        start.putExtra(VideoConferenceActivity.PIN_KEY, pin.getText().toString());
        start.putExtra(VideoConferenceActivity.NAME_KEY, name.getText().toString());

        startActivity(start);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > LOLLIPOP_MR1) {
            List<String> permissionsNeeded = new ArrayList<>();
            for (String permission : PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                    permissionsNeeded.add(permission);
            }

            if (permissionsNeeded.size() > 0) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_ALL);
            } else {
                enable(true);
            }
        } else {
            enable(true);
        }
    }

    private void enable(boolean enable) {
        findViewById(R.id.start_conference).setEnabled(enable);
        findViewById(R.id.start_conference).setAlpha(enable ? 1f : 0.2f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            requestPermissions();
        }
    }
}