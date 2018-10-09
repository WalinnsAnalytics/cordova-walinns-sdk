package com.walinns.hybrid;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by walinnsinnovation on 30/12/17.
 */

public class WAMessingService extends FirebaseMessagingService {


    private static final WALog logger = WALog.getLogger();
    private static final String TAG = WAMessingService.class.getSimpleName();
    public String notification_clicked = "NA";
    WAPref waPref;
    static String URL="https://wa.track.app.walinns.com/";
    JSONObject hashMap;
    private NotificationUtils notificationUtils;
    private MyThread mythread;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        if (remoteMessage == null)
            return;

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification clicked Body: " + remoteMessage.getNotification().getBody());
              hashMap= new JSONObject();
              waPref = new WAPref(this.getApplicationContext());
            try {

                hashMap.put("event_name", remoteMessage.getNotification().getBody() + " received");
                hashMap.put("device_id","594c1e64588f53a3");
                hashMap.put("date_time", WAUtils.getCurrentUTC());
                hashMap.put("event_type","default_event" );
                logger.e("WalinnTrackerClient date_time_event default",hashMap.toString());


                mythread  = new MyThread();
                mythread.start();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            waPref.save(WAPref.noify_clicked,remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getBody());
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Data Payload: " + remoteMessage.getData().toString());
            waPref = new WAPref(this.getApplicationContext());
            notification_clicked = "received";

            try {
                Map<String, String> data = remoteMessage.getData();

                handleDataMessage(data);
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
        }else {


        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNotification(String message) {
        if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
            // app is in foreground, broadcast the push message

            Intent pushNotification = new Intent(WAConfig.PUSH_NOTIFICATION);
            pushNotification.putExtra("message", message);
            pushNotification.setAction("pushNotification");
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

            // play notification sound
            NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
            notificationUtils.playNotificationSound();

        }else{
            // If the app is in background, firebase itself handles the notification
            System.out.println("App Status :" + "background" + message);
//            Intent pushNotification = new Intent(WAConfig.PUSH_NOTIFICATION);
//            pushNotification.putExtra("message", message);
//

            Intent pushNotification = new Intent(WAConfig.PUSH_NOTIFICATION);
            pushNotification.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pushNotification.putExtra("message", message);




             LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

            // play notification sound
            NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
            notificationUtils.playNotificationSound();

        }
    }


    private void handleDataMessage(Map<String, String> data) {
        Log.e(TAG, "push json: " + data.toString());

        try {

            String title = data.get("title");
            String message = data.get("message");
            String imageUrl = data.get("image");
            String timestamp = data.get ("timestamp");
            String ui_type = data.get("ui_type");
            String btn_1_name = data.get("btn_1_name");
            String deep_link = data.get("deep_link");
            String btn_2_name = data.get("btn_2_name");
            String bg_color = data.get("bg_color");
            String btn_1_color = data.get("btn_1_color");
            String btn_2_color = data.get("btn_2_color");
            String external_link = data.get("external_link");

            Log.e(TAG, "title: " + title);
            Log.e(TAG, "message: " + message);
            Log.e(TAG, "imageUrl: " + imageUrl);
            Log.e(TAG, "timestamp: " + timestamp);
            Log.e(TAG, "ui_type: " + ui_type);
            waPref.save(WAPref.noify_clicked,title);
            hashMap= new JSONObject();
            try {

                hashMap.put("event_name", title + " received");
                hashMap.put("device_id","594c1e64588f53a3");
                hashMap.put("date_time", WAUtils.getCurrentUTC());
                hashMap.put("event_type","default_event" );
                logger.e("WalinnTrackerClient date_time_event default",hashMap.toString());


                mythread  = new MyThread();
                mythread.start();

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {

                Intent intent = new Intent(getApplicationContext(), InAppNotification.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("title",title);
                intent.putExtra("message",message);
                intent.putExtra("imageUrl",imageUrl);
                intent.putExtra("ui_type",ui_type);
                intent.putExtra("btn_1_name",btn_1_name);
                intent.putExtra("deep_link",deep_link);
                intent.putExtra("btn_2_name",btn_2_name);
                intent.putExtra("bg_color",bg_color);
                intent.putExtra( "btn_1_color",btn_1_color);
                intent.putExtra("btn_2_color",btn_2_color);
                intent.putExtra("external_link",external_link);
                getApplicationContext().startActivity(intent);


            } else {
                // app is in background, show the notification in notification tray
                System.out.println("App Status msg:" + "background" + "....."+ data.toString());

                Intent resultIntent = new Intent(WAConfig.PUSH_NOTIFICATION);
                resultIntent.putExtra("message", message);

                // check for image attachment
                if (TextUtils.isEmpty(imageUrl)) {
                    showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent,ui_type,deep_link,external_link,btn_1_name,btn_2_name);
                } else {
                    // image is present, show notification with image
                    showNotificationMessageWithBigImage(getApplicationContext(), title, message, timestamp, resultIntent, imageUrl,ui_type,deep_link,external_link,btn_1_name,btn_2_name);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    /**
     * Showing notification with text only
     */
    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent,ui_type,deep_link,external_link,btn1_name,btn2_name);
    }

    /**
     * Showing notification with text and image
     */
    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl,ui_type,deep_link,external_link,btn1_name,btn2_name);
    }

    private void Connection(JSONObject mjsonObject){

        try{
            logger.e("WalinnsTrackerClient","Request_data Notification"+ mjsonObject.toString());
            java.net.URL url = new URL(URL+"events");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("CONTENT_TYPE", "application/json");
            conn.addRequestProperty("Authorization", waPref.getValue(WAPref.project_token));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(mjsonObject));

            writer.flush();
            writer.close();
            os.close();

            logger.e("Http_connection_request_data",waPref.getValue(WAPref.project_token));

            int responseCode=conn.getResponseCode();


            if (responseCode == HttpsURLConnection.HTTP_OK) {

                logger.e("WalinnsTrackerClient Notification","life_cycle_method_detected mURL");


                logger.e("WalinnsTrackerHttpConnection Notification", conn.getResponseMessage());

            }else {
                logger.e("WalinnsTrackerHttpConnection Notification","Fail");

            }
        }
        catch(Exception e){
            logger.e("WalinnsTrackerHttpConnection Notification","Exce"+e);

        }
    }
    private String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }


    class MyThread extends Thread{
        static final long DELAY = 5000;
        @Override
        public void run(){
            //while(isRunning){
                Log.d(TAG,"Running");
                try {

                    Connection(hashMap);
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    //isRunning = false;
                    e.printStackTrace();
                }
           // }
        }

    }

}
