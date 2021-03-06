package kpmm.htl.weatherstation.model;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import kpmm.htl.weatherstation.receivers.AlarmReceiver;

import static android.content.Context.ALARM_SERVICE;


/**
 * Created by ak47tehaxor on 12.01.17.
 */

public class Model extends Observable {
    private static final String PATH = "weatherstationconfig.txt";
    private static final boolean TEST = true;

    private boolean goingOutside = false;
    private boolean notifications = false;
    private byte monday = (byte) 0;
    private byte tuesday = (byte) 0;
    private byte wednesday = (byte) 0;
    private byte thursday = (byte) 0;
    private byte friday = (byte) 0;
    public static final long[] millisOfDay = {31800000, 35100000, 39000000, 42300000, 45600000, 48900000, 52200000, 55500000, 58800000};

    private final String IPADRESS = "http://192.168.2.213:8080";
    private final String LAST = "/last";
    private final String ALL = "/all";
    private final String FROM_NOW = "/from_now/";
    private final long DELAY = 5 * 60 * 1000;
    private final int WEEKSECONDS= 60*60*24*7;

    private static Model instance;

    private List<Measurement> measurementList = new LinkedList<>();
    private Context context;
    private boolean success = true;

    public static Model getInstance() {
        return instance == null ? (instance = new Model()) : instance;
    }

    public void setContext(Context context) {
        this.context = context;
        requestAllMeasurements();
        //saveData();
        loadData();


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timeOfDay = calendar.getTimeInMillis();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        for(int i=0;i<millisOfDay.length;i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            long timeToSet=timeOfDay+millisOfDay[i]<System.currentTimeMillis()?timeOfDay+millisOfDay[i]+1000*60*60*24:timeOfDay+millisOfDay[i];
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timeToSet, AlarmManager.INTERVAL_DAY, pendingIntent);
        }


    }

    private Model() {
        System.out.println(GregorianCalendar.getInstance().get(Calendar.DATE));
    }

    private void receiveDataArrayFromREST(String url) {
        if (this.context == null)
            return;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonArrayRequest request = new JsonArrayRequest(JsonArrayRequest.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        Measurement measurement = new Measurement(
                                (float) jsonObject.getDouble("ambient_temperature"),
                                (float) jsonObject.getDouble("ground_temperature"),
                                (float) jsonObject.getDouble("air_quality"),
                                (float) jsonObject.getDouble("air_pressure"),
                                (float) jsonObject.getDouble("humidity"),
                                (float) jsonObject.getDouble("wind_speed"),
                                (float) jsonObject.getDouble("wind_gust_speed"),
                                (float) jsonObject.getDouble("rainfall"),
                                Timestamp.valueOf(jsonObject.getString("created")));
                        measurementList.add(0, measurement);
                    }
                } catch (JSONException ex) {
                    System.out.println(ex.getMessage());
                    success = false;
                    setChanged();
                    notifyObservers();
                }
                success = true;
                Collections.sort(measurementList);
                setChanged();
                notifyObservers();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (TEST) {
                    try {
                        JSONArray response = new JSONArray("[{\"ground_temperature\": 22.0, \"wind_speed\": 40.0, \"rainfall\": 60.0, \"created\": \"2017-01-11 16:54:21\", \"ambient_temperature\": 20.0, \"air_quality\": 20.0, \"air_pressure\": 5.2, \"humidity\": 60.1, \"wind_gust_speed\": 10.0}, {\"ground_temperature\": 21.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 16:54:43\", \"ambient_temperature\": 30.0, \"air_quality\": 30.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 17:02:27\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 17:03:24\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 17:03:27\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 17:05:33\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-11 17:05:42\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 14:44:37\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 14:45:36\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 18:24:16\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 18:28:14\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 18:34:51\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}, {\"ground_temperature\": 31.0, \"wind_speed\": 10.0, \"rainfall\": 70.0, \"created\": \"2017-01-12 18:45:32\", \"ambient_temperature\": 10.0, \"air_quality\": 40.0, \"air_pressure\": 8.2, \"humidity\": 10.1, \"wind_gust_speed\": 1.0}]");
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject jsonObject = response.getJSONObject(i);
                            Measurement measurement = new Measurement(
                                    (float) jsonObject.getDouble("ambient_temperature"),
                                    (float) jsonObject.getDouble("ground_temperature"),
                                    (float) jsonObject.getDouble("air_quality"),
                                    (float) jsonObject.getDouble("air_pressure"),
                                    (float) jsonObject.getDouble("humidity"),
                                    (float) jsonObject.getDouble("wind_speed"),
                                    (float) jsonObject.getDouble("wind_gust_speed"),
                                    (float) jsonObject.getDouble("rainfall"),
                                    Timestamp.valueOf(jsonObject.getString("created")));
                            measurementList.add(0, measurement);
                        }
                        success = true;
                        Collections.sort(measurementList);
                        setChanged();
                        notifyObservers();
                    } catch (JSONException ex) {
                        System.out.println(ex.getMessage());
                    }
                } else {
                    success = false;
                    setChanged();
                    notifyObservers();
                }
            }
        });

        requestQueue.add(request);
        requestQueue.start();
    }

    private void receiveDataObjectFromREST(String url) {
        if (this.context == null)
            return;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Measurement measurement = new Measurement(
                            (float) response.getDouble("ambient_temperature"),
                            (float) response.getDouble("ground_temperature"),
                            (float) response.getDouble("air_quality"),
                            (float) response.getDouble("air_pressure"),
                            (float) response.getDouble("humidity"),
                            (float) response.getDouble("wind_speed"),
                            (float) response.getDouble("wind_gust_speed"),
                            (float) response.getDouble("rainfall"),
                            Timestamp.valueOf(response.getString("created")));
                    measurementList.add(0, measurement);
                    success = true;
                    Collections.sort(measurementList);
                    setChanged();
                    notifyObservers();
                } catch (JSONException ex) {
                    System.out.println(ex.getMessage());
                    success = false;
                    setChanged();
                    notifyObservers();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                success = false;
                setChanged();
                notifyObservers();
            }
        });
        requestQueue.add(jsonObjectRequest);
        requestQueue.start();
    }

    public void requestLastMeasurement() {
        receiveDataObjectFromREST(IPADRESS + LAST);
    }

    public void requestAllMeasurements() {
        //receiveDataArrayFromREST(IPADRESS + ALL);
        receiveDataArrayFromREST(IPADRESS+FROM_NOW+WEEKSECONDS);
    }

    public void requestMeasurementFromNow() {

    }

    public Measurement getLastMeasurement() {
        return measurementList.isEmpty() ? null : measurementList.get(0);
    }

    public Measurement getMeasurementFromNow(int minutes) {
        long millis = System.currentTimeMillis() - minutes * 60 * 1000;
        Measurement last = null;
        for (Measurement measurement : measurementList) {
            long difference = millis - measurement.getTime().getTime();
            if (difference < 0)
                last = measurement;
            else
                return last == null ? measurement : last.getTime().getTime() - millis < difference ? last : measurement;
        }
        return last;
    }

    public List<Measurement> getMeasurementList() {
        return measurementList;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isGoingOutside() {
        return goingOutside;
    }

    public void setGoingOutside(boolean goingOutside) {
        this.goingOutside = goingOutside;
        saveData();
    }

    public byte getMonday() {
        return monday;
    }

    public void setMonday(byte monday) {
        this.monday = monday;
        saveData();
    }

    public byte getTuesday() {
        return tuesday;
    }

    public void setTuesday(byte tuesday) {
        this.tuesday = tuesday;
        saveData();
    }

    public byte getWednesday() {
        return wednesday;
    }

    public void setWednesday(byte wednesday) {
        this.wednesday = wednesday;
        saveData();
    }

    public byte getThursday() {
        return thursday;
    }

    public void setThursday(byte thursday) {
        this.thursday = thursday;
        saveData();
    }

    public byte getFriday() {
        return friday;
    }

    public void setFriday(byte friday) {
        this.friday = friday;
        saveData();
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
        saveData();
    }

    private void saveData() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(context.getFilesDir().getPath() + "/" + PATH));
            bufferedWriter.write((goingOutside ? 1 : 0) + ";" + monday + ";" + tuesday + ";" + wednesday + ";" + thursday + ";" + friday + ";" + (notifications ? 1 : 0));
            bufferedWriter.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void loadData() {
        try {
            if (!new File(context.getFilesDir().getPath() + "/" + PATH).exists()) {
                saveData();
                return;
            }

            BufferedReader bufferedReader = new BufferedReader(new FileReader(context.getFilesDir().getPath() + "/" + PATH));
            String str = bufferedReader.readLine();
            System.out.println(str);
            bufferedReader.close();
            String[] split = str.split(";");
            goingOutside = split[0].charAt(0) == '1';
            monday = Byte.parseByte(split[1]);
            tuesday = Byte.parseByte(split[2]);
            wednesday = Byte.parseByte(split[3]);
            thursday = Byte.parseByte(split[4]);
            friday = Byte.parseByte(split[5]);
            notifications = split[6].charAt(0) == '1';
        } catch (IOException ex) {
            System.out.println("load Error");
        }
    }
}
