package com.app.eresearch.meetingtracker;

import okhttp3.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

class AndroidHTTPUtils {

    // Post method to send the file through http using server url. Also, file name and file uuid are
    // specified.
    HttpResponse doPost(String url, String uuid, String filename, String filePath) throws IOException {

        // Create a http client and set the time out to be 30 seconds because the audio file can be
        // very large.
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).
                writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

        // Specify the uuid and file name to let the server know.
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("uuid", uuid)
                .addFormDataPart("file", filename,
                        RequestBody.create(new File(filePath), MediaType.parse("audio/wav")))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return new HttpResponse(response);
        }
    }

    // Get method
    String doGet(String url){
        OkHttpClient client = new OkHttpClient();

        // Create an instance of sync result class
        final SyncResult syncResult = new SyncResult();

        final Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // If the server is ready and has sent the response to the client, the method will call
            // set result method from sync result class to pass the result to the ResultActivity.
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    String myResponse = response.body().string();
                    syncResult.setResult(myResponse);
                }
            }
        });
        return syncResult.getResult();
    }

    // When the client get the response, it will send the doDelete request to the server, the server
    // will delete the specified file to save the storage space.
    HttpResponse doDelete(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).delete().build();

        try (Response response = client.newCall(request).execute()) {
            return new HttpResponse(response);
        }

    }

    // HttpResponse class is created to help get the message body
    class HttpResponse {
        private int responseCode;
        private String responseBody;

        HttpResponse(Response response) throws IOException {
            responseCode = response.code();
            responseBody = response.body().string();
        }

        String getResponseBody() {
            return responseBody;
        }
    }

}
