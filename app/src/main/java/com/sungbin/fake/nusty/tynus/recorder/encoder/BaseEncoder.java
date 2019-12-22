package com.sungbin.fake.nusty.tynus.recorder.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Looper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;


public abstract class BaseEncoder implements Encoder {
    public static abstract class Callback implements Encoder.Callback {
        void onInputBufferAvailable() {
        }

        public void onOutputFormatChanged(BaseEncoder encoder, MediaFormat format) {
        }

        public void onOutputBufferAvailable(BaseEncoder encoder, int index, MediaCodec.BufferInfo info) {
        }
    }

    BaseEncoder() {
    }

    public BaseEncoder(String codecName) {
        this.mCodecName = codecName;
    }

    @Override
    public void setCallback(Encoder.Callback callback) {
        if (!(callback instanceof Callback)) {
            throw new IllegalArgumentException();
        }
        this.setCallback((Callback) callback);
    }

    private void setCallback(Callback callback) {
        if (this.mEncoder != null) throw new IllegalStateException("mEncoder is not null");
        this.mCallback = callback;
    }

    @Override
    public void prepare() throws IOException {
        if (Looper.myLooper() == null
                || Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("should run in a HandlerThread");
        }
        if (mEncoder != null) {
            throw new IllegalStateException("prepared!");
        }
        MediaFormat format = createMediaFormat();

        String mimeType = format.getString(MediaFormat.KEY_MIME);
        final MediaCodec encoder = createEncoder(mimeType);
        try {
            if (this.mCallback != null) {
                encoder.setCallback(mCodecCallback);
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            onEncoderConfigured(encoder);
            encoder.start();
        } catch (MediaCodec.CodecException e) {
            throw e;
        }
        mEncoder = encoder;
    }

    protected void onEncoderConfigured(MediaCodec encoder) {
    }

    private MediaCodec createEncoder(String type) throws IOException {
        try {
            if (this.mCodecName != null) {
                return MediaCodec.createByCodecName(mCodecName);
            }
        } catch (IOException ignored) {
        }
        return MediaCodec.createEncoderByType(type);
    }

    protected abstract MediaFormat createMediaFormat();

    public final MediaCodec getEncoder() {
        return Objects.requireNonNull(mEncoder, "doesn't prepare()");
    }

    public final ByteBuffer getOutputBuffer(int index) {
        return getEncoder().getOutputBuffer(index);
    }

    public final ByteBuffer getInputBuffer(int index) {
        return getEncoder().getInputBuffer(index);
    }

    public final void queueInputBuffer(int index, int offset, int size, long pstTs, int flags) {
        getEncoder().queueInputBuffer(index, offset, size, pstTs, flags);
    }

    public final void releaseOutputBuffer(int index) {
        getEncoder().releaseOutputBuffer(index, false);
    }

    @Override
    public void stop() {
        if (mEncoder != null) {
            mEncoder.stop();
        }
    }

    @Override
    public void release() {
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
    }

    private String mCodecName;
    private MediaCodec mEncoder;
    private Callback mCallback;


    private MediaCodec.Callback mCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NotNull MediaCodec codec, int index) {
            mCallback.onInputBufferAvailable();
        }

        @Override
        public void onOutputBufferAvailable(@NotNull MediaCodec codec, int index, @NotNull MediaCodec.BufferInfo info) {
            mCallback.onOutputBufferAvailable(BaseEncoder.this, index, info);
        }

        @Override
        public void onError(@NotNull MediaCodec codec, @NotNull MediaCodec.CodecException e) {
            mCallback.onError(BaseEncoder.this, e);
        }

        @Override
        public void onOutputFormatChanged(@NotNull MediaCodec codec, @NotNull MediaFormat format) {
            mCallback.onOutputFormatChanged(BaseEncoder.this, format);
        }
    };


}
