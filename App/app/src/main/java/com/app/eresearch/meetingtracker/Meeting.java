package com.app.eresearch.meetingtracker;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class Meeting {

    private String meetingId;
    private String meetingCreatorLogin;
    private String meetingTopic;
    private String meetingNotes;
    private String meetingDateTime;
    private ArrayList<String> meetingAttendeeArrayList;
    private String meetingDiarization;

    public Meeting(HashMap<String, Object> meetingDetails) {
        this.meetingId = String.valueOf(meetingDetails.get("meetingId"));
        this.meetingCreatorLogin = String.valueOf(meetingDetails.get("meetingCreatorLogin"));
        this.meetingTopic = String.valueOf(meetingDetails.get("meetingTopic"));
        this.meetingNotes = String.valueOf(meetingDetails.get("meetingDetails"));
        this.meetingDateTime = String.valueOf(meetingDetails.get("meetingDateTime"));
        this.meetingAttendeeArrayList = Utils.convertAttendeeStringToArrayList(meetingDetails.get("attendeeEmail").toString());
        this.meetingDiarization = String.valueOf(meetingDetails.get("meetingDiarization"));
    }

    public String getMeetingTopic() {
        return this.meetingTopic;
    }

    public String getMeetingNotes() {
        return this.meetingNotes;
    }

    public String getMeetingDateTime() {
        return this.meetingDateTime;
    }

    public ArrayList<String> getMeetingAttendeeArrayList () {
        return this.meetingAttendeeArrayList;
    }

    public String getMeetingAttendeeString () {
        return String.join(", ", this.meetingAttendeeArrayList);
    }

    public String getMeetingId () {
        return this.meetingId;
    }

    public String getMeetingCreatorLogin () {
        return this.meetingCreatorLogin;
    }

    public String getMeetingDiarization () {
        return this.meetingDiarization;
    }

}
