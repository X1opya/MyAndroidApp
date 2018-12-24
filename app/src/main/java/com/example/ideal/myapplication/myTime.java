package com.example.ideal.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class myTime extends AppCompatActivity  implements View.OnClickListener {
    final String TAG = "DBInf";
    final String FILE_NAME = "Info";
    final String PHONE = "phone";
    final String WORKING_DAYS_ID = "working days id";
    final String STATUS_USER_BY_SERVICE = "status user";

    String statusUser;
    String userId;
    boolean hasCurrentHours;

    Button[][] timeBtns;
    Button saveBtn;

    // чтобы сохранять несколько выбранных часов надо создать массив,
    // куда будем добавлять текст выбранной кнопки?
    ArrayList<String> workingHours;
    ArrayList<String> removedHours;
    ArrayList<String> currentHours;

    DBHelper dbHelper;
    SharedPreferences sPref;
    RelativeLayout mainLayout;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_time);

        statusUser = getIntent().getStringExtra(STATUS_USER_BY_SERVICE);
        userId = getUserId();

        dbHelper = new DBHelper(this);

        mainLayout = findViewById(R.id.mainMyTimeLayout);

        timeBtns = new Button[7][4];
        saveBtn = findViewById(R.id.saveMyTimeBtn);

        workingHours = new ArrayList<>();
        removedHours = new ArrayList<>();
        currentHours = new ArrayList<>();

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        addButtonsOnScreen(width,height);

        checkCurrentTimes();

        saveBtn.setOnClickListener(this);
    }

    private void checkCurrentTimes() {
        String workingDaysId = String.valueOf(getIntent().getLongExtra(WORKING_DAYS_ID, -1));
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        Cursor cursor = database.query(
                DBHelper.TABLE_WORKING_TIME,
                new String[]{DBHelper.KEY_TIME_WORKING_TIME,DBHelper.KEY_USER_ID},
                DBHelper.KEY_TIME_WORKING_DAYS_ID + " = ?",
                new String[]{workingDaysId},
                null,
                null,
                null,
                null);
        //если уже есть запись, то он не пустой
        hasCurrentHours =  isWorkingHours(cursor); // становится не empty только если есть запись
        Log.d(TAG, "size curHours " + currentHours.size());
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                String time = (String) timeBtns[i][j].getText();
                if (statusUser.equals("worker")) {
                    if (hasTimeForWorker(cursor, time)) {
                        timeBtns[i][j].setBackgroundResource(R.drawable.pressed_button);
                        timeBtns[i][j].setTag(R.string.selectedId, true);

                        if(!isFreeTime(cursor,time)){
                            timeBtns[i][j].setEnabled(false);
                            timeBtns[i][j].setTag(R.string.selectedId, true);
                        }
                    }
                } else {
                    if(!hasCurrentHours) {
                        if (!isFreeTime(cursor, time)) {
                            timeBtns[i][j].setBackgroundResource(R.drawable.disabled_button);
                            timeBtns[i][j].setEnabled(false);
                        }

                        if (hasOrder(cursor, userId, time)) {
                            timeBtns[i][j].setBackgroundResource(R.drawable.pressed_button);
                            timeBtns[i][j].setTag(R.string.selectedId, true);
                        }
                    }else {
                        if (hasOrder(cursor, userId, time)) {
                            timeBtns[i][j].setBackgroundResource(R.drawable.pressed_button);
                            timeBtns[i][j].setTag(R.string.selectedId, true);
                        } else {
                            timeBtns[i][j].setBackgroundResource(R.drawable.disabled_button);
                            timeBtns[i][j].setEnabled(false);
                        }
                    }
                }
            }
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.saveMyTimeBtn:
                if(statusUser.equals("worker")) {
                    Log.d(TAG, "Worker pressed button");
                    if (workingHours.size() > 0) {
                        addTime();
                    }
                    if (removedHours.size() > 0) {
                        deleteTime();
                    }
                    Toast.makeText(this, "Расписанеие обновлено", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.d(TAG, "User pressed button");
                    if (workingHours.size() > 0) {
                        makeOrder();
                        checkCurrentTimes();
                    }
                    Toast.makeText(this, "Запрос отправлен пользователю", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                Button btn = (Button) v;
                String btnText = btn.getText().toString();
                if(statusUser.equals("worker")){
                    if(Boolean.valueOf((btn.getTag(R.string.selectedId)).toString())) {
                        btn.setBackgroundResource(R.drawable.time_button);
                        workingHours.remove(btnText);
                        removedHours.add(btnText);
                        btn.setTag(R.string.selectedId, false);
                    } else {
                        btn.setBackgroundResource(R.drawable.pressed_button);
                        workingHours.add(btnText);
                        removedHours.remove(btnText);
                        btn.setTag(R.string.selectedId, true);
                    }
                }
                else {
                    // тоже самое только отличается в проверке на смежное время
                    if(Boolean.valueOf((btn.getTag(R.string.selectedId)).toString())) {
                        btn.setBackgroundResource(R.drawable.time_button);
                        workingHours.remove(btnText);
                        removedHours.add(btnText);
                        btn.setTag(R.string.selectedId, false);
                    } else {
                        String laterBtnText;
                        if(workingHours.size()>0){
                            laterBtnText =  workingHours.get(0);
                            updateBtn(laterBtnText);
                        }
                        workingHours.clear();
                        btn.setBackgroundResource(R.drawable.pressed_button);
                        workingHours.add(btnText);
                        btn.setTag(R.string.selectedId, true);
                        for(String sx: workingHours){
                            Log.d(TAG, sx);
                        }
                    }
                }
                break;
        }
    }

    private void updateBtn(String laterBtnText){
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                String time = (String) timeBtns[i][j].getText();

                if(time.equals(laterBtnText)){
                    timeBtns[i][j].setBackgroundResource(R.drawable.time_button);
                    timeBtns[i][j].setTag(R.string.selectedId, false);
                }
            }
        }
    }

    private void addTime(){
        long workingDaysId = getIntent().getLongExtra(WORKING_DAYS_ID, -1);

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        Cursor cursor = database.query(
                DBHelper.TABLE_WORKING_TIME,
                new String[]{DBHelper.KEY_TIME_WORKING_TIME},
                DBHelper.KEY_TIME_WORKING_DAYS_ID + " = ? ",
                new String[]{String.valueOf(workingDaysId)},
                null,
                null,
                null,
                null);

        ContentValues contentValues = new ContentValues();
        for (String time: workingHours) {
            if(!hasTimeForWorker(cursor, time)) {
                contentValues.put(DBHelper.KEY_TIME_WORKING_TIME, time);
                contentValues.put(DBHelper.KEY_USER_ID,"0");
                contentValues.put(DBHelper.KEY_TIME_WORKING_DAYS_ID, workingDaysId);

                database.insert(DBHelper.TABLE_WORKING_TIME,null,contentValues);
            }
        }
        readDB(database);
        workingHours.clear();
        cursor.close();
    }

    private boolean isWorkingHours(Cursor cursor) {
        if(cursor.moveToFirst()) {
            int indexUserId= cursor.getColumnIndex(DBHelper.KEY_USER_ID);
            do {
                if(cursor.getString(indexUserId).equals(getUserId()))
                    return true;
            } while (cursor.moveToNext());
        }
        return false;
    }

    private void makeOrder(){
        long workingDaysId = getIntent().getLongExtra(WORKING_DAYS_ID, -1);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        Cursor cursor = database.query(
                DBHelper.TABLE_WORKING_TIME,
                new String[]{DBHelper.KEY_TIME_WORKING_TIME},
                DBHelper.KEY_TIME_WORKING_DAYS_ID + " = ? ",
                new String[]{String.valueOf(workingDaysId)},
                null,
                null,
                null,
                null);

        ContentValues contentValues = new ContentValues();
        String userId  = getUserId();
        Log.d(TAG, "user id " + userId);
        for (String time: workingHours) {
            Log.d(TAG, "In has time");
            contentValues.put(DBHelper.KEY_USER_ID, userId);
            database.update(DBHelper.TABLE_WORKING_TIME, contentValues,
                    DBHelper.KEY_TIME_WORKING_TIME + " = ?",
                    new String []{time});
        }
        readDB(database);
        workingHours.clear();
        cursor.close();
    }

    private void deleteTime() {
        long workingDaysId = getIntent().getLongExtra(WORKING_DAYS_ID, -1);

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        Cursor cursor = database.query(
                DBHelper.TABLE_WORKING_TIME,
                new String[]{DBHelper.KEY_TIME_WORKING_TIME},
                DBHelper.KEY_TIME_WORKING_DAYS_ID + " = ? ",
                new String[]{String.valueOf(workingDaysId)},
                null,
                null,
                null,
                null);

        for (String time: removedHours) {
            if(hasTimeForWorker(cursor, time)) {
                database.delete(
                        DBHelper.TABLE_WORKING_TIME,
                        DBHelper.KEY_TIME_WORKING_TIME + " = ? AND "
                                + DBHelper.KEY_TIME_WORKING_DAYS_ID + " = ?",
                        new String[]{time, String.valueOf(workingDaysId)});
            }
        }

        readDB(database);
        removedHours.clear();
    }

    private  void readDB(SQLiteDatabase database){
        Cursor cursor = database.query(
                DBHelper.TABLE_WORKING_TIME,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        if(cursor.moveToFirst()){
            int indexId = cursor.getColumnIndex(DBHelper.KEY_ID);
            int indexWorkingDayId = cursor.getColumnIndex(DBHelper.KEY_TIME_WORKING_DAYS_ID);
            int indexUserId = cursor.getColumnIndex(DBHelper.KEY_USER_ID);
            int indexWorkingTime = cursor.getColumnIndex(DBHelper.KEY_TIME_WORKING_TIME);

            do{
                Log.d(TAG, cursor.getString(indexId)
                        + " "
                        + cursor.getString(indexWorkingDayId)
                        + " user id =  "
                        + cursor.getString(indexUserId)
                        + " "
                        + cursor.getString(indexWorkingTime)
                        + " "
                );
            } while (cursor.moveToNext());
        }
        else {
            Log.d(TAG, "DB is empty");
        }
        cursor.close();
        Log.d(TAG, "Done!");
    }

    private boolean hasTimeForWorker(Cursor cursor, String time) {
        if(cursor.moveToFirst()) {
            int indexTime = cursor.getColumnIndex(DBHelper.KEY_TIME_WORKING_TIME);
            do {
                if (time.equals(cursor.getString(indexTime))) {
                    return true;
                }
            } while (cursor.moveToNext());
        }
        return false;
    }

    private boolean isFreeTime(Cursor cursor, String time) {
        if(cursor.moveToFirst()) {
            int indexTime = cursor.getColumnIndex(DBHelper.KEY_TIME_WORKING_TIME);
            int indexUserId = cursor.getColumnIndex(DBHelper.KEY_USER_ID);
            do {
                if (cursor.getString(indexUserId).equals("0") && time.equals(cursor.getString(indexTime))) {
                    return true;
                }
            } while (cursor.moveToNext());
        }
        return false;
    }

    private boolean hasOrder(Cursor cursor, String userId, String time){
        if(cursor.moveToFirst()) {
            int indexUserId= cursor.getColumnIndex(DBHelper.KEY_USER_ID);
            int indexTime = cursor.getColumnIndex(DBHelper.KEY_TIME_WORKING_TIME);
            do {
                // есть id пользователя в таблице и есть время
                if (userId.equals(cursor.getString(indexUserId)) && time.equals(cursor.getString(indexTime))) {
                    return true;
                }
            } while (cursor.moveToNext());
        }
        return false;
    }
    private  String getUserId(){
        sPref = getSharedPreferences(FILE_NAME,MODE_PRIVATE);
        userId = sPref.getString(PHONE, "-");

        return userId;
    }

    private void addButtonsOnScreen(int width, int height){
        for (int i=0; i<6; i++) {
            for (int j=0; j<4; j++) {
                timeBtns[i][j]= new Button(this);
                timeBtns[i][j].setWidth(50);
                timeBtns[i][j].setHeight(30);
                timeBtns[i][j].setX(j*width/4);
                timeBtns[i][j].setY(i*height/12);
                timeBtns[i][j].setBackgroundResource(R.drawable.time_button);
                timeBtns[i][j].setTag(R.string.selectedId, false);
                timeBtns[i][j].setOnClickListener(this);
                String hour = String.valueOf((i*4+j)/2);
                String min = (j%2==0) ? "00":"30";
                timeBtns[i][j].setText(hour + ":" + min);
                if(timeBtns[i][j].getParent() != null) {
                    ((ViewGroup)timeBtns[i][j].getParent()).removeView(timeBtns[i][j]);
                }
                mainLayout.addView(timeBtns[i][j]);
            }
        }
    }
}
