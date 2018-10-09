package com.walinns.hybrid;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by walinnsinnovation on 08/05/18.
 */

public class WAListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";
    private NotificationUtils notificationUtils;
    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
         Map<String, String> data_h = new HashMap<>();
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
         if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        String title = data.getString("title");
        String imageUrl = data.getString("image");
        String timestamp= data.getString("timestamp");
        data_h.put("title",title);
        data_h.put("message",message);
        data_h.put("image",imageUrl);
        data_h.put("timestamp",timestamp);
        handleDataMessage(data_h);

    }


    private void handleDataMessage(Map<String, String> data) {
        Log.e(TAG, "push json: " + data.toString());

        try {


            String title = data.get("title");
            String message = data.get("message");
            String imageUrl = data.get("image");
            boolean isBackground = false;
            String timestamp = data.get ("timestamp");
            String ui_type = data.get("ui_type");
            String btn_1_name= data.get("btn_1_name");
            String deep_link = data.get("deep_link");
            String btn_2_name =data.get("btn_2_name");
            String bg_color = data.get("bg_color");
            String btn_1_color = data.get("btn_1_color");
            String btn_2_color = data.get("btn_2_color");
            String external_link = data.get("external_link");

            Log.e(TAG, "title: " + title);
            Log.e(TAG, "message: " + message);
            Log.e(TAG, "isBackground: " + isBackground);
            // Log.e(TAG, "payload: " + payload.toString());
            Log.e(TAG, "imageUrl: " + imageUrl);
            Log.e(TAG, "timestamp: " + timestamp);


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
    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
