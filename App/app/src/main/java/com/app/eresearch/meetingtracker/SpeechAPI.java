package com.app.eresearch.meetingtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeakerDiarizationConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.grpc.ManagedChannel;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

// This class uses the Google api to perform the speech to text task.
// Reference source: https://github.com/Thumar/SpeechAPI/blob/master/app/src/main/java/com/app/androidkt/speechapi/SpeechAPI.java

public class SpeechAPI {

    public static final List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    public static final String TAG = "SpeechAPI";
    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    // Reuse an access token if its expiration time is longer than this.
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes

    // refresh the current access token before it expires.
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    private static Handler handler;
    private final ArrayList<Listener> listeners = new ArrayList<>();

    private final StreamObserver<StreamingRecognizeResponse> streamObserver = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            boolean isFinal = false;
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (Listener listener : listeners) {
                    listener.onSpeechRecognized(text, isFinal);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");
        }

    };
    private Context context;
    private volatile AccessTokenTask accessTokenTask;
    private final Runnable fetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };
    private SpeechGrpc.SpeechStub api;
    private StreamObserver<StreamingRecognizeRequest> requestObserver;

    public SpeechAPI(Context context) {
        this.context = context;
        handler = new Handler();
        fetchAccessToken();

    }

    public void destroy() {
        handler.removeCallbacks(fetchAccessTokenRunnable);
        handler = null;
        // Release the gRPC channel.
        if (api != null) {
            final ManagedChannel channel = (ManagedChannel) api.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            api = null;
        }
    }

    private void fetchAccessToken() {
        if (accessTokenTask != null) {
            return;
        }
        accessTokenTask = new AccessTokenTask();
        accessTokenTask.execute();
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    // Starts recognizing speech audio.
    public void startRecognizing(int sampleRate) {
        if (api == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }

        // Configure the API
        requestObserver = api.streamingRecognize(streamObserver);

        SpeakerDiarizationConfig speakerDiarizationConfig =
                SpeakerDiarizationConfig.newBuilder()
                        .setEnableSpeakerDiarization(true)
                        .setMinSpeakerCount(1)
                        .setMaxSpeakerCount(100)
                        .build();

        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(RecognitionConfig.newBuilder()
                        .setLanguageCode("en-US")
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(sampleRate)
                        .setDiarizationConfig(speakerDiarizationConfig)
                        .build()
                )
                .setInterimResults(true)
                .setSingleUtterance(true)
                .build();

        StreamingRecognizeRequest streamingRecognizeRequest = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();
        requestObserver.onNext(streamingRecognizeRequest);
    }

    // Recognizes the speech audio. This method should be called every time a chunk of byte buffer
    // is ready.
    public void recognize(byte[] data, int size) {
        if (requestObserver == null) {
            return;
        }
        // Call the streaming recognition API
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
    }

    // Finishes recognizing speech audio.
    public void finishRecognizing() {
        if (requestObserver == null) {
            return;
        }
        requestObserver.onCompleted();
        requestObserver = null;
    }

    public interface Listener {
        //Called when a new piece of text was recognized by the Speech API.
        void onSpeechRecognized(String text, boolean isFinal);
    }

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {
        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);
            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }
            final InputStream stream = context.getResources().openRawResource(R.raw.credential);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            accessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            api = SpeechGrpc.newStub(channel);
            // Schedule access token refresh before it expires
            if (handler != null) {
                handler.postDelayed(fetchAccessTokenRunnable,
                        Math.max(accessToken.getExpirationTime().getTime() - System.currentTimeMillis() - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }
}
