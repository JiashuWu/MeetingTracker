package com.app.eresearch.meetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Resource;
import com.anychart.enums.AvailabilityPeriod;
import com.anychart.enums.TimeTrackingMode;
import com.anychart.scales.calendar.Availability;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisualizationActivity extends AppCompatActivity {

    private String meetingDiarizationString = "";
    private ArrayList<HashMap<String, Object>> meetingDiarizationArrayList;
    private ArrayList<HashMap<String, Object>> fullMeetingDiarizationArrayList;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization_land);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        meetingDiarizationString = getIntent().getStringExtra("meetingDiarizationString");
        meetingDiarizationArrayList = Utils.convertDiarizationStringArrayListToHashMapArrayList(Utils.convertDiarizationStringToArrayList(meetingDiarizationString));
        fullMeetingDiarizationArrayList = Utils.generateFullDiarizationHashMap(meetingDiarizationArrayList);

        AnyChartView anyChartView = findViewById(R.id.visualization_activity_chartview_land);

        Cartesian cartesian = AnyChart.line();
        cartesian.animation(true);
        cartesian.padding(10d, 20d, 5d, 20d);
        cartesian.crosshair().enabled(true);
        cartesian.crosshair().yLabel(true).yStroke((Stroke) null, null, null, (String) null, (String) null);
        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("Meeting Participation Analysis");
        cartesian.yAxis(0).title("Speaker Index");
        cartesian.xAxis(0).title("Second (s)");
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = new ArrayList<>();
        for (int i = 0 ; i < fullMeetingDiarizationArrayList.size() ; i++) {
            HashMap<String, Object> hashMap = fullMeetingDiarizationArrayList.get(i);
            seriesData.add(new CustomDataEntry(String.valueOf(hashMap.get("startingTime")), (int) hashMap.get("speakerTag")));
        }

        Set set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name("Participation Analysis");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);
        anyChartView.setChart(cartesian);
    }

    private class CustomDataEntry extends ValueDataEntry {
        CustomDataEntry(String x, Number value) { super(x, value); }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        VisualizationActivity.this.finish();
        return true;
    }
}
