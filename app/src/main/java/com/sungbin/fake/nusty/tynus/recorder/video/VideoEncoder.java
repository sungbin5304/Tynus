package com.sungbin.fake.nusty.tynus.recorder.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import com.sungbin.fake.nusty.tynus.recorder.encoder.BaseEncoder;

import java.util.Objects;

public class VideoEncoder extends BaseEncoder {
    private VideoEncodeConfig mConfig;
    private Surface mSurface;


    public VideoEncoder(VideoEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected void onEncoderConfigured(MediaCodec encoder) {
        mSurface = encoder.createInputSurface();
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

    public Surface getInputSurface() {
        return Objects.requireNonNull(mSurface, "doesn't prepare()");
    }

    @Override
    public void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }


}
