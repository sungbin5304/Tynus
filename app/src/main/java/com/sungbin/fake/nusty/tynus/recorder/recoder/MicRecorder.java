package com.sungbin.fake.nusty.tynus.recorder.recoder;

import android.media.*;
import android.os.*;
import android.util.SparseLongArray;
import com.sungbin.fake.nusty.tynus.recorder.audio.AudioEncodeConfig;
import com.sungbin.fake.nusty.tynus.recorder.audio.AudioEncoder;
import com.sungbin.fake.nusty.tynus.recorder.encoder.BaseEncoder;
import com.sungbin.fake.nusty.tynus.recorder.encoder.Encoder;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaCodec.*;


class MicRecorder implements Encoder {
    private static final String TAG = "MicRecorder";

    private final AudioEncoder mEncoder;
    private final HandlerThread mRecordThread;
    private RecordHandler mRecordHandler;
    private AudioRecord mMic;
    private int mSampleRate;
    private int mChannelConfig;

    private AtomicBoolean mForceStop = new AtomicBoolean(false);
    private BaseEncoder.Callback mCallback;
    private CallbackDelegate mCallbackDelegate;
    private int mChannelsSampleRate;

    MicRecorder(AudioEncodeConfig config) {
        mEncoder = new AudioEncoder(config);
        mSampleRate = config.sampleRate;
        mChannelsSampleRate = mSampleRate * config.channelCount;
        mChannelConfig = config.channelCount == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        mRecordThread = new HandlerThread(TAG);
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = (BaseEncoder.Callback) callback;
    }

    void setCallback(BaseEncoder.Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void prepare() {
        Looper myLooper = Objects.requireNonNull(Looper.myLooper(), "Should prepare in HandlerThread");
        mCallbackDelegate = new CallbackDelegate(myLooper, mCallback);
        mRecordThread.start();
        mRecordHandler = new RecordHandler(mRecordThread.getLooper());
        mRecordHandler.sendEmptyMessage(MSG_PREPARE);
    }

    @Override
    public void stop() {
        mCallbackDelegate.removeCallbacksAndMessages(null);
        mForceStop.set(true);
        if (mRecordHandler != null) mRecordHandler.sendEmptyMessage(MSG_STOP);
    }

    @Override
    public void release() {
        if (mRecordHandler != null) mRecordHandler.sendEmptyMessage(MSG_RELEASE);
        mRecordThread.quitSafely();
    }

    void releaseOutputBuffer(int index) {
        Message.obtain(mRecordHandler, MSG_RELEASE_OUTPUT, index, 0).sendToTarget();
    }


    ByteBuffer getOutputBuffer(int index) {
        return mEncoder.getOutputBuffer(index);
    }


    private static class CallbackDelegate extends Handler {
        private BaseEncoder.Callback mCallback;

        CallbackDelegate(Looper l, BaseEncoder.Callback callback) {
            super(l);
            this.mCallback = callback;
        }


        void onError(Encoder encoder, Exception exception) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onError(encoder, exception);
                }
            }).sendToTarget();
        }

        void onOutputFormatChanged(BaseEncoder encoder, MediaFormat format) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onOutputFormatChanged(encoder, format);
                }
            }).sendToTarget();
        }

        void onOutputBufferAvailable(BaseEncoder encoder, int index, MediaCodec.BufferInfo info) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onOutputBufferAvailable(encoder, index, info);
                }
            }).sendToTarget();
        }

    }

    private static final int MSG_PREPARE = 0;
    private static final int MSG_FEED_INPUT = 1;
    private static final int MSG_DRAIN_OUTPUT = 2;
    private static final int MSG_RELEASE_OUTPUT = 3;
    private static final int MSG_STOP = 4;
    private static final int MSG_RELEASE = 5;

    private class RecordHandler extends Handler {

        private LinkedList<MediaCodec.BufferInfo> mCachedInfos = new LinkedList<>();
        private LinkedList<Integer> mMuxingOutputBufferIndices = new LinkedList<>();
        private int mPollRate = 2048_000 / mSampleRate;

        RecordHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            int mFormat = AudioFormat.ENCODING_PCM_16BIT;
            switch (msg.what) {
                case MSG_PREPARE:
                    AudioRecord r = createAudioRecord(mSampleRate, mChannelConfig, mFormat);
                    if (r == null) {
                        mCallbackDelegate.onError(MicRecorder.this, new IllegalArgumentException());
                        break;
                    } else {
                        r.startRecording();
                        mMic = r;
                    }
                    try {
                        mEncoder.prepare();
                    } catch (Exception e) {
                        mCallbackDelegate.onError(MicRecorder.this, e);
                        break;
                    }
                case MSG_FEED_INPUT:
                    if (!mForceStop.get()) {
                        int index = pollInput();
                        if (index >= 0) {
                            feedAudioEncoder(index);
                            if (!mForceStop.get()) sendEmptyMessage(MSG_DRAIN_OUTPUT);
                        } else {
                            sendEmptyMessageDelayed(MSG_FEED_INPUT, mPollRate);
                        }
                    }
                    break;
                case MSG_DRAIN_OUTPUT:
                    offerOutput();
                    pollInputIfNeed();
                    break;
                case MSG_RELEASE_OUTPUT:
                    mEncoder.releaseOutputBuffer(msg.arg1);
                    mMuxingOutputBufferIndices.poll();
                    pollInputIfNeed();
                    break;
                case MSG_STOP:
                    if (mMic != null) {
                        mMic.stop();
                    }
                    mEncoder.stop();
                    break;
                case MSG_RELEASE:
                    if (mMic != null) {
                        mMic.release();
                        mMic = null;
                    }
                    mEncoder.release();
                    break;
            }
        }

        private void offerOutput() {
            while (!mForceStop.get()) {
                MediaCodec.BufferInfo info = mCachedInfos.poll();
                if (info == null) {
                    info = new MediaCodec.BufferInfo();
                }
                int index = mEncoder.getEncoder().dequeueOutputBuffer(info, 1);
                if (index == INFO_OUTPUT_FORMAT_CHANGED) {
                    mCallbackDelegate.onOutputFormatChanged(mEncoder, mEncoder.getEncoder().getOutputFormat());
                }
                if (index < 0) {
                    info.set(0, 0, 0, 0);
                    mCachedInfos.offer(info);
                    break;
                }
                mMuxingOutputBufferIndices.offer(index);
                mCallbackDelegate.onOutputBufferAvailable(mEncoder, index, info);

            }
        }

        private int pollInput() {
            return mEncoder.getEncoder().dequeueInputBuffer(0);
        }

        private void pollInputIfNeed() {
            if (mMuxingOutputBufferIndices.size() <= 1 && !mForceStop.get()) {
                removeMessages(MSG_FEED_INPUT);
                sendEmptyMessageDelayed(MSG_FEED_INPUT, 0);
            }
        }
    }

    private void feedAudioEncoder(int index) {
        if (index < 0 || mForceStop.get()) return;
        final AudioRecord r = Objects.requireNonNull(mMic, "maybe release");
        final boolean eos = r.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED;
        final ByteBuffer frame = mEncoder.getInputBuffer(index);
        int offset = frame.position();
        int limit = frame.limit();
        int read = 0;
        if (!eos) {
            read = r.read(frame, limit);
            if (read < 0) {
                read = 0;
            }
        }

        long pstTs = calculateFrameTimestamp(read << 3);
        int flags = BUFFER_FLAG_KEY_FRAME;

        if (eos) {
            flags = BUFFER_FLAG_END_OF_STREAM;
        }

        mEncoder.queueInputBuffer(index, offset, read, pstTs, flags);
    }

    private static final int LAST_FRAME_ID = -1;
    private SparseLongArray mFramesUsCache = new SparseLongArray(2);

    private long calculateFrameTimestamp(int totalBits) {
        int samples = totalBits >> 4;
        long frameUs = mFramesUsCache.get(samples, -1);
        if (frameUs == -1) {
            frameUs = samples * 1000_000 / mChannelsSampleRate;
            mFramesUsCache.put(samples, frameUs);
        }
        long timeUs = SystemClock.elapsedRealtimeNanos() / 1000;
        timeUs -= frameUs;
        long currentUs;
        long lastFrameUs = mFramesUsCache.get(LAST_FRAME_ID, -1);
        if (lastFrameUs == -1) {
            currentUs = timeUs;
        } else {
            currentUs = lastFrameUs;
        }

        if (timeUs - currentUs >= (frameUs << 1)) {
            currentUs = timeUs;
        }
        mFramesUsCache.put(LAST_FRAME_ID, currentUs + frameUs);
        return currentUs;
    }

    private static AudioRecord createAudioRecord(int sampleRateInHz, int channelConfig, int audioFormat) {
        int minBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (minBytes <= 0) {
            return null;
        }
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                minBytes * 2);

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            return null;
        }
        return record;
    }

}
