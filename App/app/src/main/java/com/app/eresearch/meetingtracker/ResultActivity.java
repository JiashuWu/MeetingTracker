package com.app.eresearch.meetingtracker;

import android.app.Activity;
import android.os.Bundle;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Hashtable;
import static com.app.eresearch.meetingtracker.JsonReader.readJson;


// Result activity shows the diarization result from the server in the form of the pie chart.
// Reference source: https://github.com/PhilJay/MPAndroidChart
public class ResultActivity extends Activity {

    String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        PieChart pieChart = findViewById(R.id.piechart);
        
        // get the diarization result
        DataPasser myDP = (DataPasser) getApplication();
        result = myDP.getCurrentResult();
        Hashtable<long[],Integer> finalTimeline = readJson(result);
        // this hashtable record the speaker id and the total duration of each speaker
        Hashtable<Integer,Long> totalDuration = new Hashtable<>();
        
        for (long[] startEnd:
             finalTimeline.keySet()) {
            long duration = startEnd[1]-startEnd[0];
            int speaker = finalTimeline.get(startEnd);
            // if the speaker already in the hashtable then add the new duration to the previous duration
            // Otherwise, just record the speaker and the duration
            if (totalDuration.keySet().contains(speaker)){
                totalDuration.put(speaker,totalDuration.get(speaker)+duration);
            }else {
                totalDuration.put(speaker,duration);
            }
        }
        
        // visualise legend
        ArrayList durations = new ArrayList();
        // speakers list, stores speaker id
        ArrayList speakers = new ArrayList();
        // add speaker id and each duration time
        for (int no:
                totalDuration.keySet()) {
            durations.add(new Entry(totalDuration.get(no)/1000, no));
            speakers.add("\""+no+"\"");
        }
        // plot pit chart
        PieDataSet dataSet = new PieDataSet(durations, "Duration(s) each speaker");
        PieData data = new PieData(speakers, dataSet);
        pieChart.setData(data);
        // set color
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        // animation
        pieChart.animateXY(3000, 3000);
    }
}
