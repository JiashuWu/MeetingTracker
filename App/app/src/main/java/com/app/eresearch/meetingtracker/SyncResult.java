package com.app.eresearch.meetingtracker;

// This class is designed to receive the result constantly at the client side from the server. The
// processing and diarization of the audio may take a while. Therefore, the client need to restart
// the receiving process after each time out.
public class SyncResult {
    private static final long TIMEOUT = 20000L;
    private String result;
    public String getResult() {
        long startTimeMillis = System.currentTimeMillis();
        while (result == null && System.currentTimeMillis() - startTimeMillis < TIMEOUT) {
            synchronized (this) {
                try {
                    wait(TIMEOUT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    public void setResult(String result) {
        this.result = result;
        synchronized (this) {
            notify();
        }
    }
}
