package com.app.eresearch.meetingtracker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.EventLog;
import android.util.Log;
import android.util.Patterns;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class Utils {

    public static boolean checkEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean checkPasswordEmpty(String password) {
        return !password.isEmpty();
    }

    public static boolean checkPhone(String phone) {
        return phone.matches("[0-9]+") && phone.length() >= 6;
    }

    public static boolean checkSamePassword(String password, String confirmedPassword) {
        return confirmedPassword.equals(password);
    }

    public static String calculatePasswordStrength(String password){

        // Total score of the password
        int passwordScore = 0;

        if (password.length() < 8) {
            return "Weak";
        }
        else if (password.length() >= 10) {
            passwordScore += 2;
        }
        else {
            passwordScore += 1;
        }

        // If it contains at least one digit, add 2 to total score
        if (password.matches("(?=.*[0-9]).*")) {
            passwordScore += 2;
        }

        // If it contains at least one lower case letter, add 2 to total score
        if (password.matches("(?=.*[a-z]).*")) {
            passwordScore += 2;
        }

        // If it contains at least one upper case letter, add 2 to total score
        if (password.matches("(?=.*[A-Z]).*")) {
            passwordScore += 2;
        }

        // If it contains at least one special character, add 2 to total score
        if (password.matches("(?=.*[~!@#$%^&*()_-]).*")) {
            passwordScore += 2;
        }

        // Return results
        if (passwordScore <= 4) {
            return "Weak";
        }
        else if (passwordScore <= 7) {
            return "Moderate";
        }
        else {
            return "Strong";
        }
    }

    public static String generateDate(String dateString) {
        dateString = dateString.replace(",", "");
        return dateString;
    }

    public static String generateTime(int hourOfDay, int minute) {
        String HOUR = "";
        String MINUTE = "";
        if (hourOfDay <= 9) {
            HOUR = "0" + String.valueOf(hourOfDay);
        }
        else {
            HOUR = String.valueOf(hourOfDay);
        }

        if (minute <= 9) {
            MINUTE = "0" + String.valueOf(minute);
        }
        else {
            MINUTE = String.valueOf(minute);
        }
        return HOUR + ":" + MINUTE;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            switch (packageName.toLowerCase()) {
                case "outlook": packageName = "com.microsoft.office.outlook"; break;
                case "gmail": packageName = "com.google.android.gm"; break;
                case "chrome": packageName = "com.android.chrome"; break;
            }
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException nnfe) {
            return false;
        }
    }

    public static boolean validateAttendeeArrayList (ArrayList <String> attendeeArrayList) {
        if (attendeeArrayList.size() == 0) {
            return false;
        }
        for (int i = 0 ; i < attendeeArrayList.size() ; i++) {
            if (!checkEmail(attendeeArrayList.get(i))) { return false; }
        }
        return true;
    }

    public static boolean validateMessageTopic (String messageTopic) {
        return messageTopic.isEmpty();
    }

    public static boolean validateMeetingDate (String meetingDate) {
        if (meetingDate.equals("")) { return false; }
        return true;
    }

    public static boolean validateMeetingTime (String meetingStartingTime, String meetingEndingTime) {
        if (meetingStartingTime.equals("")) { return false; }
        if (meetingEndingTime.equals("")) { return false; }
        if (meetingEndingTime.compareToIgnoreCase(meetingStartingTime) <=0) { return false; }
        return true;
    }

    public static boolean validateSchedulingActions (boolean saveToCalendar, boolean sendViaEmail) {
        return saveToCalendar || sendViaEmail;
    }

    public static boolean validateSchedulingEmailMedium (String emailMedium) {
        return emailMedium.isEmpty();
    }

    public static String emailComposer (String attendeeEmail, String meetingTopic, String meetingNotes,
                                        String meetingDate, String meetingStartingTime, String meetingEndingTime, String timeZoneDisplayName, boolean hasQRCode) {
        if (meetingNotes.equals("") || meetingNotes.equalsIgnoreCase("null")) {
            meetingNotes = "No notes. ";
        }
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = "";
        if (currentHour >= 0 && currentHour <= 12) {
            greeting = "Good Morning";
        }
        else if (currentHour > 12 && currentHour <= 17) {
            greeting = "Good Afternoon";
        }
        else if (currentHour > 17 && currentHour <= 18) {
            greeting = "Good Evening";
        }
        else if (currentHour > 18) {
            greeting = "Good Night";
        }

        String content = greeting + ", " + "\n\n";
        content += "This is a notification that you are invited to attend a meeting with details as follows: " + "\n\n";
        content += "   -  Meeting topic: " + meetingTopic + "\n";
        if (meetingNotes.isEmpty()) {
            content += "   -  Meeting Description: No description" + "\n";
        }
        else {
            content += "   -  Meeting Description: " + meetingNotes + "\n";
        }
        content += "   -  Meeting Date: " + meetingDate + "\n";
        content += "   -  Meeting Starting Time: " + meetingStartingTime + " (" + timeZoneDisplayName + ")" + "\n";
        content += "   -  Meeting Ending Time: " + meetingEndingTime + " (" + timeZoneDisplayName + ")" + "\n\n";
        if (hasQRCode) {
            content += "A meeting QR Code has been attached, if you have the MeetingTracker app, you can scan this QR Code to add this meeting into your meeting schedule. " + "\n\n";
        }
        content += "Please make sure to join the meeting on time. Thank you. " + "\n\n";
        content += "Kind regards" + "\n\n";
        return content;
    }

    public static void generateCalendarFile () {
        // TODO
    }

    public static Intent addEventToSystemCalendar (HashMap<String, Object> eventHashMap) {

        Calendar startingTime = (Calendar) eventHashMap.get("startingTime");
        Calendar endingTime = (Calendar) eventHashMap.get("endingTime");
        ArrayList<String> attendeeEmailArrayList = (ArrayList<String>) eventHashMap.get("attendeeEmail");

        String attendeeEmails = String.join(",", attendeeEmailArrayList);

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startingTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endingTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.TITLE, String.valueOf(eventHashMap.get("topic")))
                .putExtra(CalendarContract.Events.DESCRIPTION, String.valueOf(eventHashMap.get("notes")))
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, String.valueOf(eventHashMap.get("timezoneId")))
                .putExtra(CalendarContract.Events.CALENDAR_ID, 1) // System default calendar
                .putExtra(Intent.EXTRA_EMAIL, attendeeEmails);
        return intent;
    }

    public static String convertAttendeeArrayListToString (ArrayList<String> attendeeArrayList) {
        return String.join(",", attendeeArrayList);
    }

    public static ArrayList<String> convertAttendeeStringToArrayList (String attendeeString) {
        String str[] = attendeeString.split(",");
        return new ArrayList<String>(Arrays.asList(str));
    }

    public static String convertCalendarToString (Calendar calendar) {
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy MMMM dd HH:mm:ss zzzz");
        return dateFormat.format(date);
    }

    public static String convertCalendarToTime(Calendar calendar) {
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        return dateFormat.format(date);
    }

    public static Calendar convertStringToCalendar (String calendar) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        Calendar cal = Calendar.getInstance();
        try {
            Date date = simpleDateFormat.parse(calendar);
            cal.setTime(date);
        }
        catch (ParseException pe) {
            pe.printStackTrace();
        }
        return cal;
    }

    public static String convertDiarizationArrayListToString (ArrayList<String> diarizationArrayList) {
        return String.join(",", diarizationArrayList);
    }

    public static ArrayList<String> convertDiarizationStringToArrayList (String diarizationString) {
        String str[] = diarizationString.split(",");
        return new ArrayList<String>(Arrays.asList(str));
    }

    public static ArrayList<HashMap<String, Object>> convertDiarizationStringArrayListToHashMapArrayList (ArrayList<String> diarizationStringArrayList) {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        for (int i = 0 ; i < diarizationStringArrayList.size() ; i++) {
            result.add(Utils.convertDiarizationStringToHashMap(diarizationStringArrayList.get(i)));
        }
        return result;
    }

    public static HashMap<String, Object> convertDiarizationStringToHashMap (String diarizationString) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("speakerTag", Integer.parseInt(diarizationString.split("#")[0]));
        result.put("startingTime", Double.parseDouble(diarizationString.split("#")[1]));
        result.put("endingTime", Double.parseDouble(diarizationString.split("#")[2]));
        return result;
    }

    public static String convertDiarizationHashMapToString (HashMap<String, Object> diarizationHashMap) {
        return diarizationHashMap.get("speakerTag") +
                "#" + String.valueOf(diarizationHashMap.get("startingTime")) +
                "#" + String.valueOf(diarizationHashMap.get("endingTime"));
    }

    public static ArrayList<Double> generateDoubleList (Double startInclusive, Double endExclusive, Double interval) {
        ArrayList<Double> doubleList = new ArrayList<Double>() {};
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        while (startInclusive < endExclusive) {
            doubleList.add(Double.parseDouble(decimalFormat.format(startInclusive)));
            startInclusive += interval;
        }
        return doubleList;
    }

    public static ArrayList<HashMap<String, Object>> generateFullDiarizationHashMap (ArrayList<HashMap<String, Object>> meetingDiarizationHashMap) {
        ArrayList<HashMap<String, Object>> fullHashMap = new ArrayList<>();
        for (int i = 0 ; i < meetingDiarizationHashMap.size() ; i++) {
            int speakerTag = (int) meetingDiarizationHashMap.get(i).get("speakerTag");
            double startingTime = (double) meetingDiarizationHashMap.get(i).get("startingTime");
            double endingTime = (double) meetingDiarizationHashMap.get(i).get("endingTime");
            ArrayList<Double> startingTimeList = Utils.generateDoubleList(startingTime, endingTime, 0.1);
            for (int j = 0 ; j < startingTimeList.size() ; j++) {
                HashMap<String, Object> newHashMap = new HashMap<>();
                newHashMap.put("speakerTag", speakerTag);
                newHashMap.put("startingTime", startingTimeList.get(j));
                newHashMap.put("endingTime", endingTime);
                fullHashMap.add(newHashMap);
            }
        }
        return fullHashMap;
    }

    public static String encodingMeetingString (HashMap<String, Object> eventHashMap) {
        return new Gson().toJson(eventHashMap);
    }

    public static HashMap<String, Object> decodingMeetingString (String jsonString) {
        HashMap<String, Object> eventHashMap = new HashMap<>();
        try {
            JSONObject json = new JSONObject(jsonString);
            eventHashMap.put("topic", json.getString("topic"));
            eventHashMap.put("notes", json.getString("notes"));
            eventHashMap.put("date", json.getString("date"));
            eventHashMap.put("timezoneId", json.getString("timezoneId"));
            eventHashMap.put("meetingDiarizationString", json.getString("meetingDiarizationString"));
            JSONObject startingTimeJSON = json.getJSONObject("startingTime");
            JSONObject endingTimeJSON = json.getJSONObject("endingTime");
            JSONArray attendeeEmails = json.getJSONArray("attendeeEmail");
            ArrayList<String> attendeeEmailArrayList = new ArrayList<>();
            for (int i = 0 ; i < attendeeEmails.length() ; i++) {
                attendeeEmailArrayList.add(attendeeEmails.getString(i));
            }
            eventHashMap.put("attendeeEmail", attendeeEmailArrayList);
            eventHashMap.put("attendeeEmailString", String.join(", ", attendeeEmailArrayList));
            Calendar startingTimeCalendar = Calendar.getInstance();
            Calendar endingTimeCalendar = Calendar.getInstance();
            startingTimeCalendar.set(
                    startingTimeJSON.getInt("year"),
                    startingTimeJSON.getInt("month"),
                    startingTimeJSON.getInt("dayOfMonth"),
                    startingTimeJSON.getInt("hourOfDay"),
                    startingTimeJSON.getInt("minute"), 0);
            endingTimeCalendar.set(
                    endingTimeJSON.getInt("year"),
                    endingTimeJSON.getInt("month"),
                    endingTimeJSON.getInt("dayOfMonth"),
                    endingTimeJSON.getInt("hourOfDay"),
                    endingTimeJSON.getInt("minute"), 0);
            eventHashMap.put("startingTime", startingTimeCalendar);
            eventHashMap.put("endingTime", endingTimeCalendar);
            eventHashMap.put("successfullyRecognized", true);
        }
        catch (Exception e) {
            eventHashMap.put("successfullyRecognized", false);
            e.printStackTrace();
        }
        return eventHashMap;
    }

    public static HashMap<String, Integer> convertMeetingDate (String meetingDate) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        String month = meetingDate.split(" ")[0];
        String day = meetingDate.split(" ")[1];
        String year = meetingDate.split(" ")[2];
        hashMap.put("day", Integer.parseInt(day));
        hashMap.put("year", Integer.parseInt(year));
        switch (month) {
            case "Jan": hashMap.put("month", 1); break;
            case "Feb": hashMap.put("month", 2); break;
            case "Mar": hashMap.put("month", 3); break;
            case "Apr": hashMap.put("month", 4); break;
            case "May": hashMap.put("month", 5); break;
            case "Jun": hashMap.put("month", 6); break;
            case "Jul": hashMap.put("month", 7); break;
            case "Aug": hashMap.put("month", 8); break;
            case "Sep": hashMap.put("month", 9); break;
            case "Oct": hashMap.put("month", 10); break;
            case "Nov": hashMap.put("month", 11); break;
            case "Dec": hashMap.put("month", 12); break;
        }
        return hashMap;
    }

    public static String convertMeetingDateToDatabaseDate (String meetingDate) {
        String month = meetingDate.split(" ")[0];
        String day = meetingDate.split(" ")[1];
        String year = meetingDate.split(" ")[2];
        day = StringUtils.leftPad(day, 2, "0");
        switch (month) {
            case "Jan": month = "01"; break;
            case "Feb": month = "02"; break;
            case "Mar": month = "03"; break;
            case "Apr": month = "04"; break;
            case "May": month = "05"; break;
            case "Jun": month = "06"; break;
            case "Jul": month = "07"; break;
            case "Aug": month = "08"; break;
            case "Sep": month = "09"; break;
            case "Oct": month = "10"; break;
            case "Nov": month = "11"; break;
            case "Dec": month = "12"; break;
        }
        return year + " " + month + " " + day;
    }

    public static String convertDatabaseDateToMeetingDate (String databaseDate) {
        String year = databaseDate.split(" ")[0];
        String month = databaseDate.split(" ")[1];
        String day = databaseDate.split(" ")[2];

        switch (month) {
            case "01": month = "Jan"; break;
            case "02": month = "Feb"; break;
            case "03": month = "Mar"; break;
            case "04": month = "Apr"; break;
            case "05": month = "May"; break;
            case "06": month = "Jun"; break;
            case "07": month = "Jul"; break;
            case "08": month = "Aug"; break;
            case "09": month = "Sep"; break;
            case "10": month = "Oct"; break;
            case "11": month = "Nov"; break;
            case "12": month = "Dec"; break;
        }

        return month + " " + day.replaceFirst("^0+(?!$)", "") + " " + year;
    }

    public static String convertMeetingDateTimeToDatabaseDateTime (String meetingDate, String meetingStartingTime, String meetingEndingTime) {
        return convertMeetingDateToDatabaseDate(meetingDate) + "@" + meetingStartingTime + "@" + meetingEndingTime;
    }

    public static HashMap<String, String> convertDatabaseDateTimeToMeetingDateTime (String databaseDateTime) {
        HashMap <String, String> result = new HashMap<>();
        String date = databaseDateTime.split("@")[0];
        String startingTime = databaseDateTime.split("@")[1];
        String endingTime = databaseDateTime.split("@")[2];
        result.put("meetingDate", convertDatabaseDateToMeetingDate(date));
        result.put("meetingStartingTime", startingTime);
        result.put("meetingEndingTime", endingTime);
        return result;
    }

    public static HashMap<String, Integer> convertMeetingTime (String meetingTime) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        String hour = meetingTime.split(":")[0];
        String minute = meetingTime.split(":")[1];
        hashMap.put("hour", Integer.parseInt(hour));
        hashMap.put("minute", Integer.parseInt(minute));
        return hashMap;
    }

    public static Bitmap CreateImage(String message, String type) throws WriterException
    {
        int size = 660;
        int size_width = 660;
        int size_height = 264;

        BitMatrix bitMatrix = null;
        // BitMatrix bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, size, size);
        switch (type)
        {
            case "QR Code": bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, size, size);break;
            case "Barcode": bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.CODE_128, size_width, size_height);break;
            case "Data Matrix": bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.DATA_MATRIX, size, size);break;
            case "PDF 417": bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.PDF_417, size_width, size_height);break;
            case "Barcode-39":bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.CODE_39, size_width, size_height);break;
            case "Barcode-93":bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.CODE_93, size_width, size_height);break;
            case "AZTEC": bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.AZTEC, size, size);break;
            default: bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, size, size);break;
        }
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int [] pixels = new int [width * height];
        for (int i = 0 ; i < height ; i++)
        {
            for (int j = 0 ; j < width ; j++)
            {
                if (bitMatrix.get(j, i))
                {
                    pixels[i * width + j] = 0xff000000;
                }
                else
                {
                    pixels[i * width + j] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static void saveBitmap (Bitmap bitmap, String fileName, String bitName)
    {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);

        String fileNameWithDate = fileName + "_at_" + String.valueOf(year) + "_" + String.valueOf(month) + "_" + String.valueOf(day) + "_" + String.valueOf(hour) + "_" + String.valueOf(minute) + "_" + String.valueOf(second) + "_"  + String.valueOf(millisecond) + bitName;
        File createFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"test");
        if(!createFolder.exists())
            createFolder.mkdir();
        File saveImage = new File(createFolder,fileNameWithDate);
        try {
            OutputStream outputStream = new FileOutputStream(saveImage);
            bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String reformatDateTimeString(String meetingDateTime) {
        String meetingDate = meetingDateTime.split("@")[0];
        String meetingStartingTime = meetingDateTime.split("@")[1];
        String meetingEndingTime = meetingDateTime.split("@")[2];
        return "Meeting Start Time: " + convertDatabaseDateToMeetingDate(meetingDate) + " " + meetingStartingTime + "\n"
                + "Meeting End Time: " + convertDatabaseDateToMeetingDate(meetingDate) + " " + meetingEndingTime;
    }

    public static int retrieveColourfulInitialIcon(char initialLetter) {
        int randomIndex = (int)(Math.random() * 12);
        int result = 0;
        if (initialLetter == 'a') {
            switch (randomIndex) {
                case 0: result = R.drawable.a_b; break;
                case 1: result = R.drawable.a_c; break;
                case 2: result = R.drawable.a_e; break;
                case 3: result = R.drawable.a_f; break;
                case 4: result = R.drawable.a_g; break;
                case 5: result = R.drawable.a_l; break;
                case 6: result = R.drawable.a_o; break;
                case 7: result = R.drawable.a_p; break;
                case 8: result = R.drawable.a_r; break;
                case 9: result = R.drawable.a_x; break;
                case 10: result = R.drawable.a_y; break;
                case 11: result = R.drawable.a_z; break;
                default: result = R.drawable.a_b;
            }
        }
        else if (initialLetter == 'b') {
            switch (randomIndex) {
                case 0: result = R.drawable.b_b; break;
                case 1: result = R.drawable.b_c; break;
                case 2: result = R.drawable.b_e; break;
                case 3: result = R.drawable.b_f; break;
                case 4: result = R.drawable.b_g; break;
                case 5: result = R.drawable.b_l; break;
                case 6: result = R.drawable.b_o; break;
                case 7: result = R.drawable.b_p; break;
                case 8: result = R.drawable.b_r; break;
                case 9: result = R.drawable.b_x; break;
                case 10: result = R.drawable.b_y; break;
                case 11: result = R.drawable.b_z; break;
                default: result = R.drawable.b_b;
            }
        }
        else if (initialLetter == 'c') {
            switch (randomIndex) {
                case 0: result = R.drawable.c_b; break;
                case 1: result = R.drawable.c_c; break;
                case 2: result = R.drawable.c_e; break;
                case 3: result = R.drawable.c_f; break;
                case 4: result = R.drawable.c_g; break;
                case 5: result = R.drawable.c_l; break;
                case 6: result = R.drawable.c_o; break;
                case 7: result = R.drawable.c_p; break;
                case 8: result = R.drawable.c_r; break;
                case 9: result = R.drawable.c_x; break;
                case 10: result = R.drawable.c_y; break;
                case 11: result = R.drawable.c_z; break;
                default: result = R.drawable.c_b;
            }
        }
        else if (initialLetter == 'd') {
            switch (randomIndex) {
                case 0: result = R.drawable.d_b; break;
                case 1: result = R.drawable.d_c; break;
                case 2: result = R.drawable.d_e; break;
                case 3: result = R.drawable.d_f; break;
                case 4: result = R.drawable.d_g; break;
                case 5: result = R.drawable.d_l; break;
                case 6: result = R.drawable.d_o; break;
                case 7: result = R.drawable.d_p; break;
                case 8: result = R.drawable.d_r; break;
                case 9: result = R.drawable.d_x; break;
                case 10: result = R.drawable.d_y; break;
                case 11: result = R.drawable.d_z; break;
                default: result = R.drawable.d_b;
            }
        }
        else if (initialLetter == 'e') {
            switch (randomIndex) {
                case 0: result = R.drawable.e_b; break;
                case 1: result = R.drawable.e_c; break;
                case 2: result = R.drawable.e_e; break;
                case 3: result = R.drawable.e_f; break;
                case 4: result = R.drawable.e_g; break;
                case 5: result = R.drawable.e_l; break;
                case 6: result = R.drawable.e_o; break;
                case 7: result = R.drawable.e_p; break;
                case 8: result = R.drawable.e_r; break;
                case 9: result = R.drawable.e_x; break;
                case 10: result = R.drawable.e_y; break;
                case 11: result = R.drawable.e_z; break;
                default: result = R.drawable.e_b;
            }
        }
        else if (initialLetter == 'f') {
            switch (randomIndex) {
                case 0: result = R.drawable.f_b; break;
                case 1: result = R.drawable.f_c; break;
                case 2: result = R.drawable.f_e; break;
                case 3: result = R.drawable.f_f; break;
                case 4: result = R.drawable.f_g; break;
                case 5: result = R.drawable.f_l; break;
                case 6: result = R.drawable.f_o; break;
                case 7: result = R.drawable.f_p; break;
                case 8: result = R.drawable.f_r; break;
                case 9: result = R.drawable.f_x; break;
                case 10: result = R.drawable.f_y; break;
                case 11: result = R.drawable.f_z; break;
                default: result = R.drawable.f_b;
            }
        }
        else if (initialLetter == 'g') {
            switch (randomIndex) {
                case 0: result = R.drawable.g_b; break;
                case 1: result = R.drawable.g_c; break;
                case 2: result = R.drawable.g_e; break;
                case 3: result = R.drawable.g_f; break;
                case 4: result = R.drawable.g_g; break;
                case 5: result = R.drawable.g_l; break;
                case 6: result = R.drawable.g_o; break;
                case 7: result = R.drawable.g_p; break;
                case 8: result = R.drawable.g_r; break;
                case 9: result = R.drawable.g_x; break;
                case 10: result = R.drawable.g_y; break;
                case 11: result = R.drawable.g_z; break;
                default: result = R.drawable.g_b;
            }
        }
        else if (initialLetter == 'h') {
            switch (randomIndex) {
                case 0: result = R.drawable.h_b; break;
                case 1: result = R.drawable.h_c; break;
                case 2: result = R.drawable.h_e; break;
                case 3: result = R.drawable.h_f; break;
                case 4: result = R.drawable.h_g; break;
                case 5: result = R.drawable.h_l; break;
                case 6: result = R.drawable.h_o; break;
                case 7: result = R.drawable.h_p; break;
                case 8: result = R.drawable.h_r; break;
                case 9: result = R.drawable.h_x; break;
                case 10: result = R.drawable.h_y; break;
                case 11: result = R.drawable.h_z; break;
                default: result = R.drawable.h_b;
            }
        }
        else if (initialLetter == 'i') {
            switch (randomIndex) {
                case 0: result = R.drawable.i_b; break;
                case 1: result = R.drawable.i_c; break;
                case 2: result = R.drawable.i_e; break;
                case 3: result = R.drawable.i_f; break;
                case 4: result = R.drawable.i_g; break;
                case 5: result = R.drawable.i_l; break;
                case 6: result = R.drawable.i_o; break;
                case 7: result = R.drawable.i_p; break;
                case 8: result = R.drawable.i_r; break;
                case 9: result = R.drawable.i_x; break;
                case 10: result = R.drawable.i_y; break;
                case 11: result = R.drawable.i_z; break;
                default: result = R.drawable.i_b;
            }
        }
        else if (initialLetter == 'j') {
            switch (randomIndex) {
                case 0: result = R.drawable.j_b; break;
                case 1: result = R.drawable.j_c; break;
                case 2: result = R.drawable.j_e; break;
                case 3: result = R.drawable.j_f; break;
                case 4: result = R.drawable.j_g; break;
                case 5: result = R.drawable.j_l; break;
                case 6: result = R.drawable.j_o; break;
                case 7: result = R.drawable.j_p; break;
                case 8: result = R.drawable.j_r; break;
                case 9: result = R.drawable.j_x; break;
                case 10: result = R.drawable.j_y; break;
                case 11: result = R.drawable.j_z; break;
                default: result = R.drawable.j_b;
            }
        }
        else if (initialLetter == 'k') {
            switch (randomIndex) {
                case 0: result = R.drawable.k_b; break;
                case 1: result = R.drawable.k_c; break;
                case 2: result = R.drawable.k_e; break;
                case 3: result = R.drawable.k_f; break;
                case 4: result = R.drawable.k_g; break;
                case 5: result = R.drawable.k_l; break;
                case 6: result = R.drawable.k_o; break;
                case 7: result = R.drawable.k_p; break;
                case 8: result = R.drawable.k_r; break;
                case 9: result = R.drawable.k_x; break;
                case 10: result = R.drawable.k_y; break;
                case 11: result = R.drawable.k_z; break;
                default: result = R.drawable.k_b;
            }
        }
        else if (initialLetter == 'l') {
            switch (randomIndex) {
                case 0: result = R.drawable.l_b; break;
                case 1: result = R.drawable.l_c; break;
                case 2: result = R.drawable.l_e; break;
                case 3: result = R.drawable.l_f; break;
                case 4: result = R.drawable.l_g; break;
                case 5: result = R.drawable.l_l; break;
                case 6: result = R.drawable.l_o; break;
                case 7: result = R.drawable.l_p; break;
                case 8: result = R.drawable.l_r; break;
                case 9: result = R.drawable.l_x; break;
                case 10: result = R.drawable.l_y; break;
                case 11: result = R.drawable.l_z; break;
                default: result = R.drawable.l_b;
            }
        }
        else if (initialLetter == 'm') {
            switch (randomIndex) {
                case 0: result = R.drawable.m_b; break;
                case 1: result = R.drawable.m_c; break;
                case 2: result = R.drawable.m_e; break;
                case 3: result = R.drawable.m_f; break;
                case 4: result = R.drawable.m_g; break;
                case 5: result = R.drawable.m_l; break;
                case 6: result = R.drawable.m_o; break;
                case 7: result = R.drawable.m_p; break;
                case 8: result = R.drawable.m_r; break;
                case 9: result = R.drawable.m_x; break;
                case 10: result = R.drawable.m_y; break;
                case 11: result = R.drawable.m_z; break;
                default: result = R.drawable.m_b;
            }
        }
        else if (initialLetter == 'n') {
            switch (randomIndex) {
                case 0: result = R.drawable.n_b; break;
                case 1: result = R.drawable.n_c; break;
                case 2: result = R.drawable.n_e; break;
                case 3: result = R.drawable.n_f; break;
                case 4: result = R.drawable.n_g; break;
                case 5: result = R.drawable.n_l; break;
                case 6: result = R.drawable.n_o; break;
                case 7: result = R.drawable.n_p; break;
                case 8: result = R.drawable.n_r; break;
                case 9: result = R.drawable.n_x; break;
                case 10: result = R.drawable.n_y; break;
                case 11: result = R.drawable.n_z; break;
                default: result = R.drawable.n_b;
            }
        }
        else if (initialLetter == 'o') {
            switch (randomIndex) {
                case 0: result = R.drawable.o_b; break;
                case 1: result = R.drawable.o_c; break;
                case 2: result = R.drawable.o_e; break;
                case 3: result = R.drawable.o_f; break;
                case 4: result = R.drawable.o_g; break;
                case 5: result = R.drawable.o_l; break;
                case 6: result = R.drawable.o_o; break;
                case 7: result = R.drawable.o_p; break;
                case 8: result = R.drawable.o_r; break;
                case 9: result = R.drawable.o_x; break;
                case 10: result = R.drawable.o_y; break;
                case 11: result = R.drawable.o_z; break;
                default: result = R.drawable.o_b;
            }
        }
        else if (initialLetter == 'p') {
            switch (randomIndex) {
                case 0: result = R.drawable.p_b; break;
                case 1: result = R.drawable.p_c; break;
                case 2: result = R.drawable.p_e; break;
                case 3: result = R.drawable.p_f; break;
                case 4: result = R.drawable.p_g; break;
                case 5: result = R.drawable.p_l; break;
                case 6: result = R.drawable.p_o; break;
                case 7: result = R.drawable.p_p; break;
                case 8: result = R.drawable.p_r; break;
                case 9: result = R.drawable.p_x; break;
                case 10: result = R.drawable.p_y; break;
                case 11: result = R.drawable.p_z; break;
                default: result = R.drawable.p_b;
            }
        }
        else if (initialLetter == 'q') {
            switch (randomIndex) {
                case 0: result = R.drawable.q_b; break;
                case 1: result = R.drawable.q_c; break;
                case 2: result = R.drawable.q_e; break;
                case 3: result = R.drawable.q_f; break;
                case 4: result = R.drawable.q_g; break;
                case 5: result = R.drawable.q_l; break;
                case 6: result = R.drawable.q_o; break;
                case 7: result = R.drawable.q_p; break;
                case 8: result = R.drawable.q_r; break;
                case 9: result = R.drawable.q_x; break;
                case 10: result = R.drawable.q_y; break;
                case 11: result = R.drawable.q_z; break;
                default: result = R.drawable.q_b;
            }
        }
        else if (initialLetter == 'r') {
            switch (randomIndex) {
                case 0: result = R.drawable.r_b; break;
                case 1: result = R.drawable.r_c; break;
                case 2: result = R.drawable.r_e; break;
                case 3: result = R.drawable.r_f; break;
                case 4: result = R.drawable.r_g; break;
                case 5: result = R.drawable.r_l; break;
                case 6: result = R.drawable.r_o; break;
                case 7: result = R.drawable.r_p; break;
                case 8: result = R.drawable.r_r; break;
                case 9: result = R.drawable.r_x; break;
                case 10: result = R.drawable.r_y; break;
                case 11: result = R.drawable.r_z; break;
                default: result = R.drawable.r_b;
            }
        }
        else if (initialLetter == 's') {
            switch (randomIndex) {
                case 0: result = R.drawable.s_b; break;
                case 1: result = R.drawable.s_c; break;
                case 2: result = R.drawable.s_e; break;
                case 3: result = R.drawable.s_f; break;
                case 4: result = R.drawable.s_g; break;
                case 5: result = R.drawable.s_l; break;
                case 6: result = R.drawable.s_o; break;
                case 7: result = R.drawable.s_p; break;
                case 8: result = R.drawable.s_r; break;
                case 9: result = R.drawable.s_x; break;
                case 10: result = R.drawable.s_y; break;
                case 11: result = R.drawable.s_z; break;
                default: result = R.drawable.s_b;
            }
        }
        else if (initialLetter == 't') {
            switch (randomIndex) {
                case 0: result = R.drawable.t_b; break;
                case 1: result = R.drawable.t_c; break;
                case 2: result = R.drawable.t_e; break;
                case 3: result = R.drawable.t_f; break;
                case 4: result = R.drawable.t_g; break;
                case 5: result = R.drawable.t_l; break;
                case 6: result = R.drawable.t_o; break;
                case 7: result = R.drawable.t_p; break;
                case 8: result = R.drawable.t_r; break;
                case 9: result = R.drawable.t_x; break;
                case 10: result = R.drawable.t_y; break;
                case 11: result = R.drawable.t_z; break;
                default: result = R.drawable.t_b;
            }
        }
        else if (initialLetter == 'u') {
            switch (randomIndex) {
                case 0: result = R.drawable.u_b; break;
                case 1: result = R.drawable.u_c; break;
                case 2: result = R.drawable.u_e; break;
                case 3: result = R.drawable.u_f; break;
                case 4: result = R.drawable.u_g; break;
                case 5: result = R.drawable.u_l; break;
                case 6: result = R.drawable.u_o; break;
                case 7: result = R.drawable.u_p; break;
                case 8: result = R.drawable.u_r; break;
                case 9: result = R.drawable.u_x; break;
                case 10: result = R.drawable.u_y; break;
                case 11: result = R.drawable.u_z; break;
                default: result = R.drawable.u_b;
            }
        }
        else if (initialLetter == 'v') {
            switch (randomIndex) {
                case 0: result = R.drawable.v_b; break;
                case 1: result = R.drawable.v_c; break;
                case 2: result = R.drawable.v_e; break;
                case 3: result = R.drawable.v_f; break;
                case 4: result = R.drawable.v_g; break;
                case 5: result = R.drawable.v_l; break;
                case 6: result = R.drawable.v_o; break;
                case 7: result = R.drawable.v_p; break;
                case 8: result = R.drawable.v_r; break;
                case 9: result = R.drawable.v_x; break;
                case 10: result = R.drawable.v_y; break;
                case 11: result = R.drawable.v_z; break;
                default: result = R.drawable.v_b;
            }
        }
        else if (initialLetter == 'w') {
            switch (randomIndex) {
                case 0: result = R.drawable.w_b; break;
                case 1: result = R.drawable.w_c; break;
                case 2: result = R.drawable.w_e; break;
                case 3: result = R.drawable.w_f; break;
                case 4: result = R.drawable.w_g; break;
                case 5: result = R.drawable.w_l; break;
                case 6: result = R.drawable.w_o; break;
                case 7: result = R.drawable.w_p; break;
                case 8: result = R.drawable.w_r; break;
                case 9: result = R.drawable.w_x; break;
                case 10: result = R.drawable.w_y; break;
                case 11: result = R.drawable.w_z; break;
                default: result = R.drawable.w_b;
            }
        }
        else if (initialLetter == 'x') {
            switch (randomIndex) {
                case 0: result = R.drawable.x_b; break;
                case 1: result = R.drawable.x_c; break;
                case 2: result = R.drawable.x_e; break;
                case 3: result = R.drawable.x_f; break;
                case 4: result = R.drawable.x_g; break;
                case 5: result = R.drawable.x_l; break;
                case 6: result = R.drawable.x_o; break;
                case 7: result = R.drawable.x_p; break;
                case 8: result = R.drawable.x_r; break;
                case 9: result = R.drawable.x_x; break;
                case 10: result = R.drawable.x_y; break;
                case 11: result = R.drawable.x_z; break;
                default: result = R.drawable.x_b;
            }
        }
        else if (initialLetter == 'y') {
            switch (randomIndex) {
                case 0: result = R.drawable.y_b; break;
                case 1: result = R.drawable.y_c; break;
                case 2: result = R.drawable.y_e; break;
                case 3: result = R.drawable.y_f; break;
                case 4: result = R.drawable.y_g; break;
                case 5: result = R.drawable.y_l; break;
                case 6: result = R.drawable.y_o; break;
                case 7: result = R.drawable.y_p; break;
                case 8: result = R.drawable.y_r; break;
                case 9: result = R.drawable.y_x; break;
                case 10: result = R.drawable.y_y; break;
                case 11: result = R.drawable.y_z; break;
                default: result = R.drawable.y_b;
            }
        }
        else if (initialLetter == 'z') {
            switch (randomIndex) {
                case 0: result = R.drawable.z_b; break;
                case 1: result = R.drawable.z_c; break;
                case 2: result = R.drawable.z_e; break;
                case 3: result = R.drawable.z_f; break;
                case 4: result = R.drawable.z_g; break;
                case 5: result = R.drawable.z_l; break;
                case 6: result = R.drawable.z_o; break;
                case 7: result = R.drawable.z_p; break;
                case 8: result = R.drawable.z_r; break;
                case 9: result = R.drawable.z_x; break;
                case 10: result = R.drawable.z_y; break;
                case 11: result = R.drawable.z_z; break;
                default: result = R.drawable.z_b;
            }
        }
        else if (initialLetter == '0') {
            switch (randomIndex) {
                case 0: result = R.drawable.n0_b; break;
                case 1: result = R.drawable.n0_c; break;
                case 2: result = R.drawable.n0_e; break;
                case 3: result = R.drawable.n0_f; break;
                case 4: result = R.drawable.n0_g; break;
                case 5: result = R.drawable.n0_l; break;
                case 6: result = R.drawable.n0_o; break;
                case 7: result = R.drawable.n0_p; break;
                case 8: result = R.drawable.n0_r; break;
                case 9: result = R.drawable.n0_x; break;
                case 10: result = R.drawable.n0_y; break;
                case 11: result = R.drawable.n0_z; break;
                default: result = R.drawable.n0_b;
            }
        }
        else if (initialLetter == '1') {
            switch (randomIndex) {
                case 0: result = R.drawable.n1_b; break;
                case 1: result = R.drawable.n1_c; break;
                case 2: result = R.drawable.n1_e; break;
                case 3: result = R.drawable.n1_f; break;
                case 4: result = R.drawable.n1_g; break;
                case 5: result = R.drawable.n1_l; break;
                case 6: result = R.drawable.n1_o; break;
                case 7: result = R.drawable.n1_p; break;
                case 8: result = R.drawable.n1_r; break;
                case 9: result = R.drawable.n1_x; break;
                case 10: result = R.drawable.n1_y; break;
                case 11: result = R.drawable.n1_z; break;
                default: result = R.drawable.n1_b;
            }
        }
        else if (initialLetter == '2') {
            switch (randomIndex) {
                case 0: result = R.drawable.n2_b; break;
                case 1: result = R.drawable.n2_c; break;
                case 2: result = R.drawable.n2_e; break;
                case 3: result = R.drawable.n2_f; break;
                case 4: result = R.drawable.n2_g; break;
                case 5: result = R.drawable.n2_l; break;
                case 6: result = R.drawable.n2_o; break;
                case 7: result = R.drawable.n2_p; break;
                case 8: result = R.drawable.n2_r; break;
                case 9: result = R.drawable.n2_x; break;
                case 10: result = R.drawable.n2_y; break;
                case 11: result = R.drawable.n2_z; break;
                default: result = R.drawable.n2_b;
            }
        }
        else if (initialLetter == '3') {
            switch (randomIndex) {
                case 0: result = R.drawable.n3_b; break;
                case 1: result = R.drawable.n3_c; break;
                case 2: result = R.drawable.n3_e; break;
                case 3: result = R.drawable.n3_f; break;
                case 4: result = R.drawable.n3_g; break;
                case 5: result = R.drawable.n3_l; break;
                case 6: result = R.drawable.n3_o; break;
                case 7: result = R.drawable.n3_p; break;
                case 8: result = R.drawable.n3_r; break;
                case 9: result = R.drawable.n3_x; break;
                case 10: result = R.drawable.n3_y; break;
                case 11: result = R.drawable.n3_z; break;
                default: result = R.drawable.n3_b;
            }
        }
        else if (initialLetter == '4') {
            switch (randomIndex) {
                case 0: result = R.drawable.n4_b; break;
                case 1: result = R.drawable.n4_c; break;
                case 2: result = R.drawable.n4_e; break;
                case 3: result = R.drawable.n4_f; break;
                case 4: result = R.drawable.n4_g; break;
                case 5: result = R.drawable.n4_l; break;
                case 6: result = R.drawable.n4_o; break;
                case 7: result = R.drawable.n4_p; break;
                case 8: result = R.drawable.n4_r; break;
                case 9: result = R.drawable.n4_x; break;
                case 10: result = R.drawable.n4_y; break;
                case 11: result = R.drawable.n4_z; break;
                default: result = R.drawable.n4_b;
            }
        }
        else if (initialLetter == '5') {
            switch (randomIndex) {
                case 0: result = R.drawable.n5_b; break;
                case 1: result = R.drawable.n5_c; break;
                case 2: result = R.drawable.n5_e; break;
                case 3: result = R.drawable.n5_f; break;
                case 4: result = R.drawable.n5_g; break;
                case 5: result = R.drawable.n5_l; break;
                case 6: result = R.drawable.n5_o; break;
                case 7: result = R.drawable.n5_p; break;
                case 8: result = R.drawable.n5_r; break;
                case 9: result = R.drawable.n5_x; break;
                case 10: result = R.drawable.n5_y; break;
                case 11: result = R.drawable.n5_z; break;
                default: result = R.drawable.n5_b;
            }
        }
        else if (initialLetter == '6') {
            switch (randomIndex) {
                case 0: result = R.drawable.n6_b; break;
                case 1: result = R.drawable.n6_c; break;
                case 2: result = R.drawable.n6_e; break;
                case 3: result = R.drawable.n6_f; break;
                case 4: result = R.drawable.n6_g; break;
                case 5: result = R.drawable.n6_l; break;
                case 6: result = R.drawable.n6_o; break;
                case 7: result = R.drawable.n6_p; break;
                case 8: result = R.drawable.n6_r; break;
                case 9: result = R.drawable.n6_x; break;
                case 10: result = R.drawable.n6_y; break;
                case 11: result = R.drawable.n6_z; break;
                default: result = R.drawable.n6_b;
            }
        }
        else if (initialLetter == '7') {
            switch (randomIndex) {
                case 0: result = R.drawable.n7_b; break;
                case 1: result = R.drawable.n7_c; break;
                case 2: result = R.drawable.n7_e; break;
                case 3: result = R.drawable.n7_f; break;
                case 4: result = R.drawable.n7_g; break;
                case 5: result = R.drawable.n7_l; break;
                case 6: result = R.drawable.n7_o; break;
                case 7: result = R.drawable.n7_p; break;
                case 8: result = R.drawable.n7_r; break;
                case 9: result = R.drawable.n7_x; break;
                case 10: result = R.drawable.n7_y; break;
                case 11: result = R.drawable.n7_z; break;
                default: result = R.drawable.n7_b;
            }
        }
        else if (initialLetter == '8') {
            switch (randomIndex) {
                case 0: result = R.drawable.n8_b; break;
                case 1: result = R.drawable.n8_c; break;
                case 2: result = R.drawable.n8_e; break;
                case 3: result = R.drawable.n8_f; break;
                case 4: result = R.drawable.n8_g; break;
                case 5: result = R.drawable.n8_l; break;
                case 6: result = R.drawable.n8_o; break;
                case 7: result = R.drawable.n8_p; break;
                case 8: result = R.drawable.n8_r; break;
                case 9: result = R.drawable.n8_x; break;
                case 10: result = R.drawable.n8_y; break;
                case 11: result = R.drawable.n8_z; break;
                default: result = R.drawable.n8_b;
            }
        }
        else {
            switch (randomIndex) {
                case 0: result = R.drawable.n9_b; break;
                case 1: result = R.drawable.n9_c; break;
                case 2: result = R.drawable.n9_e; break;
                case 3: result = R.drawable.n9_f; break;
                case 4: result = R.drawable.n9_g; break;
                case 5: result = R.drawable.n9_l; break;
                case 6: result = R.drawable.n9_o; break;
                case 7: result = R.drawable.n9_p; break;
                case 8: result = R.drawable.n9_r; break;
                case 9: result = R.drawable.n9_x; break;
                case 10: result = R.drawable.n9_y; break;
                case 11: result = R.drawable.n9_z; break;
                default: result = R.drawable.n9_b;
            }
        }
        return result;
    }

}
