package com.sungbin.fake.nusty.tynus.recorder.audio;

import android.media.MediaFormat;
import com.sungbin.fake.nusty.tynus.recorder.encoder.BaseEncoder;


public class AudioEncoder extends BaseEncoder {
    private final AudioEncodeConfig mConfig;

    public AudioEncoder(AudioEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

}
