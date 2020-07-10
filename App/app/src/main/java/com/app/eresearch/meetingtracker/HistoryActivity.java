package com.app.eresearch.meetingtracker;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.qap.ctimelineview.TimelineRow;
import org.qap.ctimelineview.TimelineViewAdapter;
import java.util.ArrayList;
import java.util.Hashtable;
import static com.app.eresearch.meetingtracker.JsonReader.readJson;

// History activity provide the diarization result in the form of a timeline. The timeline will show
// the speaker id, speech content and the start time of each speech
// Reference source: https://github.com/qapqap/TimelineView
public class HistoryActivity extends AppCompatActivity {

    ArrayList<Float> startTime;
    ArrayList<String> utterences;
    String result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        DataPasser myDP = (DataPasser) getApplication();
        startTime = myDP.getStartTime();
        utterences = myDP.getUtterances();
        result = myDP.getCurrentResult();
        // Call the readJson method in the JsonReader class to get the diarization result in the format
        // of which speaker says at what time.
        Hashtable<long[],Integer> finalTimeline = readJson(result);

        // Create timeline rows List
        ArrayList<TimelineRow> timelineRowsList = new ArrayList<>();
        // Initialize the current speaker number.
        int speakerName = 1;

        // Add some speaker portraits
        Hashtable<Integer,Integer> picture = new Hashtable<>();
        picture.put(1,R.drawable.butters);
        picture.put(2,R.drawable.biggles);
        picture.put(3,R.drawable.cop);
        picture.put(4,R.drawable.marjorine);
        picture.put(5,R.drawable.squirrel);
        picture.put(6,R.drawable.hitler);
        picture.put(7,R.drawable.cartman);

        for (int i =0; i<utterences.size();i++) {
            // Check which speaker's turn it is now
            for (long [] startEnd:
                 finalTimeline.keySet()) {
                // Get the start time in second.
                long start = Math.round(startTime.get(i)*1000);
                if(isThisUtterence(startEnd,start)){
                    speakerName = finalTimeline.get(startEnd);
                }
            }
            // Get the speaker's portrait according to the speaker number
            int pictureId = picture.get(speakerName);
            // Add start time, speaker number, speech content and the corresponding speaker portrait to the timeline
            addTimelibeRows(timelineRowsList, i, "Time: "+String.valueOf(startTime.get(i)) + " s", "Speaker "+speakerName+ ": "+utterences.get(i), pictureId);
        }

        // Create the timeline adapter
        ArrayAdapter<TimelineRow> myAdapter = new TimelineViewAdapter(this, 0, timelineRowsList,
                //if true, list will be sorted by date
                false);

        // Get the ListView and bind it with the timeline adapter
        ListView myListView = findViewById(R.id.timeline_listView);
        myListView.setAdapter(myAdapter);
    }

    // Method to add a row to the timeline
    private void addTimelibeRows(ArrayList<TimelineRow> timelineRowsList, int id, String title, String content, int picId){
        // Create new timeline row and give an id to it
        TimelineRow timelineRow = new TimelineRow(id);
        // Set the title of the row
        timelineRow.setTitle(title);
        // Set the description of the row
        timelineRow.setDescription(content);
        // Set the speaker portrait of the row
        timelineRow.setImage(BitmapFactory.decodeResource(getResources(), picId));
        // Set the line color of a row
        timelineRow.setBellowLineColor(Color.argb(255, 0, 0, 0));
        // Set the line size of a row
        timelineRow.setBellowLineSize(6);
        // Set the image size of a row
        timelineRow.setImageSize(40);
        // Set the background color of a row
        timelineRow.setBackgroundColor(Color.argb(255, 0, 0, 0));
        // set the background size image
        timelineRow.setBackgroundSize(60);
        // Set the DateColor
        timelineRow.setDateColor(Color.argb(255, 0, 0, 0));
        // Set the Speech content color
        timelineRow.setTitleColor(Color.argb(255, 0, 0, 0));
        // Set the color of the description of the row
        timelineRow.setDescriptionColor(Color.argb(255, 0, 0, 0));
        // Add the row to the timeline
        timelineRowsList.add(timelineRow);
    }
    
    // Check if the start time of the speech in the specified range.
    private static boolean isThisUtterence(long[] startEnd, long startTime){
        if (startTime>= startEnd[0]&&startTime<=startEnd[1]){
            return true;
        }else {
            return false;
        }

    }
}
