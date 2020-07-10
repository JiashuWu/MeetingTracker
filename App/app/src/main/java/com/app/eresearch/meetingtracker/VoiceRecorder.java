package com.app.eresearch.meetingtracker;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// This class record the audio to send to the google for speech recognition. It also saves the audio file
// into the wav file and upload the audio file to the server for further diarization.
// reference source: https://github.com/Thumar/SpeechAPI/blob/master/app/src/main/java/com/app/androidkt/speechapi/VoiceRecorder.java
public class VoiceRecorder {

    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{44100};
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_BPP = 16;
    static String filePath;
    //timestamp of the last time that voice is heard
    private long mLastVoiceHeardMillis = Long.MAX_VALUE;
    //timestamp when the current voice is started
    private final Object mLock = new Object();
    private long mVoiceStartedMillis;
    private OutputStream outputStream = null;
    private final Callback mCallback;
    private AudioRecord mAudioRecord;
    private Thread mThread;
    private byte[] mBuffer;

    public static abstract class Callback {
        //Called when the recorder starts hearing voice
        public void onVoiceStart() {
        }
        //Called when the recorder is hearing voice
        public void onVoice(byte[] data, int size) {
        }
        //Called when the recorder stops hearing voice
        public void onVoiceEnd() {
        }
    }

    VoiceRecorder(@NonNull Callback callback) {
        mCallback = callback;
    }

    //Starts recording audio.
    public void start() {
        mAudioRecord = createAudioRecord();
        if (mAudioRecord == null) {
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        mAudioRecord.startRecording();
        String filename = getTempFilename();
        try {
            outputStream = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Start processing the captured audio.
        mThread = new Thread(new ProcessVoice());
        mThread.start();
    }

    //Stops recording audio
    void stop() {
        synchronized (mLock) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final String path = getFilename();
            copyWaveFile(getTempFilename(), path);
            deleteTempFile();
            filePath = path;
            dismiss();
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mBuffer = null;
        }
    }

    // Dismisses the currently ongoing utterance
    void dismiss() {
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }
    }

    // Retrieves the sample rate currently used to record audio
    int getSampleRate() {
        if (mAudioRecord != null) {
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }

    // Creates a new AudioRecord
    private AudioRecord createAudioRecord() {
        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate, CHANNEL, ENCODING, sizeInBytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mBuffer = new byte[sizeInBytes];
                return audioRecord;
            } else {
                audioRecord.release();
            }
        }
        return null;
    }

    // Continuously processes the captured audio and notifies {@link #mCallback} of corresponding events
    private class ProcessVoice implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (mLock) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    final int size = mAudioRecord.read(mBuffer, 0, mBuffer.length);
                    if (size > 0) {
                    }
                    if (AudioRecord.ERROR_INVALID_OPERATION != size) {
                        try {
                            outputStream.write(mBuffer);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    final long now = System.currentTimeMillis();
                    if (isHearingVoice(mBuffer, size)) {
                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                            mVoiceStartedMillis = now;
                            mCallback.onVoiceStart();
                        }
                        mCallback.onVoice(mBuffer, size);
                        mLastVoiceHeardMillis = now;
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                            end();
                        }
                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                        mCallback.onVoice(mBuffer, size);
                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                            end();
                        }
                    }
                }
            }
        }

        private void end() {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }

        private boolean isHearingVoice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

    }

    // get the name of the output file
    private String getFilename() {
        return getPath(System.currentTimeMillis()+ "audio.wav");
    }

    // get the name of the temporary pcm file
    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }
        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    // get the directory of the output file
    private String getPath(String nameOfFile) {
        File file = new File(getFileDirectory());
        file.mkdirs();
        String pathName = getFileDirectory() + nameOfFile;
        return pathName;
    }

    // get the output file directory on the mobile phone
    private String getFileDirectory(){
        return (Environment.getExternalStorageDirectory().getAbsolutePath() + "/SpkDiarization/");
    }

    // delete the temporary pcm file
    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    // turn the temporary file which was pcm into wav file.
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        long totalAudioLen;
        long totalDataLen;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[3584];

        try {
            fileInputStream = new FileInputStream(inFilename);
            fileOutputStream = new FileOutputStream(outFilename);
            totalAudioLen = fileInputStream.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(fileOutputStream, totalAudioLen, totalDataLen, (long) RECORDER_SAMPLERATE, channels, byteRate);
            while (fileInputStream.read(data) != -1) {
                fileOutputStream.write(data);
            }
            fileInputStream.close();
            fileOutputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // write the header of the wav file
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
