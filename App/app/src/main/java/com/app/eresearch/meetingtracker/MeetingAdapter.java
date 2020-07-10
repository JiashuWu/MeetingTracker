package com.app.eresearch.meetingtracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {

    private ArrayList<Meeting> meetingArrayList;
    private DatabaseHelper databaseHelper;

    public static class MeetingViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView meetingCardView;
        public TextView meetingTopicTextView;
        public TextView meetingDateTimeTextView;
        public TextView meetingNotesTextView;
        public View meetingView;

        public MeetingViewHolder(View meetingView) {
            super(meetingView);
            this.meetingView = meetingView;
            meetingCardView = meetingView.findViewById(R.id.carditem_card);
            meetingTopicTextView = meetingView.findViewById(R.id.carditem_meetingtopic_textview);
            meetingDateTimeTextView = meetingView.findViewById(R.id.carditem_meetingdatetime_textview);
            meetingNotesTextView = meetingView.findViewById(R.id.carditem_meetingnotes_textview);
        }
    }

    public MeetingAdapter(ArrayList<Meeting> meetingArrayList) {
        this.meetingArrayList = meetingArrayList;
    }

    @Override
    public MeetingViewHolder onCreateViewHolder (ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        MeetingViewHolder meetingViewHolder = new MeetingViewHolder(view);
        return meetingViewHolder;
    }

    @Override
    public void onBindViewHolder(MeetingViewHolder meetingViewHolder, int position) {
        databaseHelper = new DatabaseHelper(meetingViewHolder.meetingView.getContext(), DatabaseHelper.databaseName, null, DatabaseHelper.databaseVersion);
        Meeting meeting = this.meetingArrayList.get(position);
        meetingViewHolder.meetingTopicTextView.setText(meeting.getMeetingTopic());
        meetingViewHolder.meetingDateTimeTextView.setText(Utils.reformatDateTimeString(meeting.getMeetingDateTime()));
        if (meeting.getMeetingNotes() == null || meeting.getMeetingNotes().equalsIgnoreCase("null")) {
            meetingViewHolder.meetingNotesTextView.setText("No notes. ");
        }
        else {
            meetingViewHolder.meetingNotesTextView.setText(meeting.getMeetingNotes());
        }
        meetingViewHolder.meetingCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(meetingViewHolder.meetingView.getContext());
                builder.setTitle("Select option");
                builder.setPositiveButton("View", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent (meetingViewHolder.meetingView.getContext(), DetailActivity.class);
                        intent.putExtra("callingOrigin", "MeetingAdapter");
                        intent.putExtra("meetingTopic", meeting.getMeetingTopic());
                        intent.putExtra("meetingNotes", meeting.getMeetingNotes());
                        intent.putExtra("meetingDateTime", meeting.getMeetingDateTime());
                        intent.putExtra("meetingAttendeeString", meeting.getMeetingAttendeeString());
                        intent.putExtra("meetingAttendeeArrayList", meeting.getMeetingAttendeeArrayList());
                        intent.putExtra("meetingId", meeting.getMeetingId());
                        intent.putExtra("meetingCreatorLogin", meeting.getMeetingCreatorLogin());
                        intent.putExtra("meetingDiarization", meeting.getMeetingDiarization());
                        meetingViewHolder.meetingView.getContext().startActivity(intent);
                        ((Activity) meetingViewHolder.meetingView.getContext()).finish();
                    }
                });
                builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                        sqLiteDatabase.delete("Meeting", "meetingId = ?", new String [] {meeting.getMeetingId()});
                        Intent intent = new Intent(meetingViewHolder.meetingView.getContext(), MeetingListActivity.class);
                        meetingViewHolder.meetingView.getContext().startActivity(intent);
                        ((Activity) meetingViewHolder.meetingView.getContext()).finish();
                    }
                });
                builder.setNeutralButton("Go Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Should do nothing here.
                    }
                });
                builder.setMessage("View or delete the meeting? ");
                builder.create();
                builder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.meetingArrayList.size();
    }

}
