package com.app.eresearch.meetingtracker;

import java.util.Hashtable;

// This class help to decode the json result got from the server.
public class JsonReader {

    public static Hashtable<long[],Integer>  readJson(String result){
        String[] sentences = result.split(",");
        String[] temp = new String[sentences.length-1];
        Hashtable<long[],Integer> finalTimeline = new Hashtable<>();
        int numberOfSpeaker = temp.length;
        int i =-1;
        for (String sentence:sentences) {
            sentence=sentence.replaceAll("\"","").replaceAll("\\{","").replaceAll("\\}","");
            sentence = sentence.replaceAll("\\\\","").replaceAll("result:","").replaceAll("uuid:","");
            sentence = sentence.replaceAll("\\s","");
            sentence = sentence.substring(2);
            if (i>=0) temp[i] = sentence;
            i++;
        }
        int speakerNo = 1;
        for (String sentence: temp) {
            String[] timeline = sentence.split(";");
            for (String times:
                    timeline) {
                String[] startEnd = times.split("==>");
                // Get the start and finish time of each speaker.
                long start = timeConverter(startEnd[0]);
                long end = timeConverter(startEnd[1]);
                long[] startFinish = new long[2];
                startFinish[0] = start;
                startFinish[1] = end;
                // Add the speaker Id to the start time and finish time of each speech
                finalTimeline.put(startFinish,speakerNo);
            }
            speakerNo++;
        }
        return finalTimeline;
    }

    // Convert the time format of the server to milliseconds.
    private static long timeConverter(String time){
        long min;
        long sec;
        long msec;
        String [] minSec = time.split(":" );
        min = Long.parseLong(minSec[0]);
        String[] secMsec = minSec[1].split("\\.");
        sec = Long.parseLong(secMsec[0]);
        msec = Long.parseLong(secMsec[1]);
        return msec + 1000*sec + 60000*min;
    }
}

