package com.hoho.phonecallapp.phonecallui;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.telecom.Call;
import android.telecom.VideoProfile;

import androidx.annotation.RequiresApi;

/**
 * author: aJIEw
 * description: 电话接打的管理类
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallManager {

    public static Call call;

    private Context context;
    private AudioManager audioManager;

    public PhoneCallManager(Context context) {
        this.context = context;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 接听电话
     */
    public void answer() {
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
            openSpeaker();
        }
    }

    /**
     * 断开电话，包括来电时的拒接以及接听后的挂断
     */
    public void disconnect() {
        if (call != null) {
            call.disconnect();
        }
    }

    /**
     * 打开免提
     */
    public void openSpeaker() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    /**
     * 销毁资源
     * */
    public void destroy() {
        call = null;
        context = null;
        audioManager = null;
    }
}
