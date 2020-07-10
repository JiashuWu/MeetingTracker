package com.app.eresearch.meetingtracker;

import android.app.Application;
import java.util.ArrayList;

// This is a class to help pass the data between activities.
public class DataPasser extends Application {

    String currentResult="";
    ArrayList<Float> startTime;
    ArrayList<String> utterances;
    String uuid;

    // setter for the start time of each speech
    public void setStartTime(ArrayList<Float> startTime) {this.startTime = startTime;}

    // setter for the content of each utterance
    public  void setUtterances(ArrayList<String> utterances){
        this.utterances = utterances;
    }

    // setter for the current diarization result
    public void setCurrentResult(String currentResult){this.currentResult = currentResult;}

    // setter for the uuid of current file under process
    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    // getter for the start time of each speech
    public ArrayList<Float> getStartTime() {
        return startTime;
    }

    // getter for the content of each utterances
    public ArrayList<String> getUtterances() { return utterances; }

    // getter for the current diarization result
    public String getCurrentResult(){ return currentResult; }

    // getter for the uuid of current file under process
    public String getUuid(){ return this.uuid; }

    @Override
    public void onCreate() {
        super.onCreate();
        setStartTime(null);
        setUtterances(null);
    }

}
