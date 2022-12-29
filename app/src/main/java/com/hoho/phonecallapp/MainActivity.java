package com.hoho.phonecallapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.aykuttasil.callrecord.CallRecord;
import com.hoho.phonecallapp.listenphonecall.CallListenerService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ezy.assist.compat.SettingsCompat;

/**
 * author: aJIEw
 * description:
 */
public class MainActivity extends AppCompatActivity {

    private Switch switchPhoneCall;

    private Switch switchListenCall;
    private CompoundButton.OnCheckedChangeListener switchCallCheckChangeListener;
    private CallRecord callRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initRecorder();
    }

    private void initRecorder() {
        callRecord = new CallRecord.Builder(this)
                .setLogEnable(true)
                .setRecordFileName("")
                .setRecordDirName("CallRecorderTest")
                .setRecordDirPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath())
                .setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                .setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setShowSeed(true)
                .setShowPhoneNumber(true)
                .build();
        callRecord.startCallReceiver();
    }

    private void initView() {
        switchPhoneCall = findViewById(R.id.switch_default_phone_call);
        switchListenCall = findViewById(R.id.switch_call_listenr);

        switchPhoneCall.setOnClickListener(v -> {
            // Android M 以上的系统发起将本应用设为默认电话应用的请求
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                if (switchPhoneCall.isChecked()) {
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                    intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                            getPackageName());
                    startActivity(intent);
                } else {
                    // 取消时跳转到默认设置页面
                    startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
                }
            } else {
                Toast.makeText(MainActivity.this, "Android 6.0 以上才支持修改默认电话应用！", Toast.LENGTH_LONG).show();
                switchPhoneCall.setChecked(false);
            }

        });

        // 使用使用 SettingsCompat 检查是否开启了权限
        switchCallCheckChangeListener = (buttonView, isChecked) -> {
            // 使用使用 SettingsCompat 检查是否开启了权限
            if (isChecked && !SettingsCompat.canDrawOverlays(MainActivity.this)) {
                askForDrawOverlay();
                switchListenCall.setOnCheckedChangeListener(null);
                switchListenCall.setChecked(false);
                switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);
                return;
            }

            Intent callListener = new Intent(MainActivity.this, CallListenerService.class);
            if (isChecked) {
                startService(callListener);
            } else {
                stopService(callListener);
            }
        };
        switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setOnClickListener(v -> {
            callPhone();
        });
    }

    private void askForDrawOverlay() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("允许显示悬浮框")
                .setMessage("为了使电话监听服务正常工作，必须允许这项权限")
                .setPositiveButton("去设置", (dialog, which) -> {
                    openDrawOverlaySettings();
                    dialog.dismiss();
                })
                .setNegativeButton("稍后再说", (dialog, which) -> dialog.dismiss());

        alertDialog.show();
    }

    /**
     * 跳转悬浮窗管理设置界面
     */
    private void openDrawOverlaySettings() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M 以上引导用户去系统设置中打开允许悬浮窗
            // 使用反射是为了用尽可能少的代码保证在大部分机型上都可用
            try {
                Context context = this;
                Class clazz = Settings.class;
                Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");
                Intent intent = new Intent(field.get(null).toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "请在悬浮窗管理中打开权限", Toast.LENGTH_LONG).show();
            }
        } else {
            // 6.0 以下则直接使用 SettingsCompat 中提供的接口
            SettingsCompat.manageDrawOverlays(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        switchPhoneCall.setChecked(isDefaultPhoneCallApp());
        switchListenCall.setChecked(isServiceRunning(CallListenerService.class));
    }

    /**
     * Android M 以上检查是否是系统默认电话应用
     */
    public boolean isDefaultPhoneCallApp() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager manger = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (manger != null) {
                String name = manger.getDefaultDialerPackage();
                return name.equals(getPackageName());
            }
        }
        return false;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private int PERMISSIONS_REQUEST_CODE = 123;
    List<String> permissionList = new ArrayList(Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS));


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void callPhone() {
        Uri uri = Uri.fromParts("tel", "+886999999999", null);
        Bundle extras = new Bundle();
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true);
        TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "沒有撥號權限", Toast.LENGTH_LONG).show();
            return;
        }
        telecomManager.placeCall(uri, extras);

//        Intent intent = new Intent(Intent.ACTION_DIAL);
//        intent.setData(Uri.parse("tel:+886928977229"));
//        startActivity(intent);
    }
}
