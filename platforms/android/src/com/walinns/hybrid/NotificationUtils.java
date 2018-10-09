package com.walinns.hybrid;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by walinnsinnovation on 18/01/18.
 */

public class NotificationUtils {

        private Context mContext;
        NotificationCompat.BigPictureStyle bigPictureStyle;
        public NotificationUtils(Context mContext) {
            this.mContext = mContext;
        }

        public void showNotificationMessage(String title, String message, String timeStamp, Intent intent,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {
             showNotificationMessage(title, message, timeStamp, intent, null,ui_type,deep_link,external_link,btn1_name,btn2_name);
        }

        public void showNotificationMessage(final String title, final String message, final String timeStamp, Intent intent, String imageUrl,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {
            // Check for empty push message
            if (TextUtils.isEmpty(message))
                return;

            int icon;
            try {
                String x = ManifestMetaData.getMetaData(mContext, WAConfig.LABEL_NOTIFICATION_ICON);
                if (x == null) throw new IllegalArgumentException();
                icon = mContext.getResources().getIdentifier(x, "drawable", mContext.getPackageName());
                if (icon == 0) throw new IllegalArgumentException();
            } catch (Throwable t) {
                ApplicationInfo ai = mContext.getApplicationInfo();

                icon = ai.icon;
            }
            final Map<String, String> newPrefs = new HashMap<String, String>();
            newPrefs.put("content",title);
            // notification icon
            final SharedPreferences referralInfo = mContext.getSharedPreferences("wa_notify", Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = referralInfo.edit();
            for (final Map.Entry<String, String> entry : newPrefs.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());

            }
            editor.apply();
            WalinnsAPI.getInstance().track("default_event",title +" received");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            mContext,
                            0,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    );

            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    mContext);

           // final Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
            //        + "://" + mContext.getPackageName() + "/raw/notification");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            if (alarmSound != null) {
//                mBuilder.setSound(alarmSound);
//            }
//

            if (!TextUtils.isEmpty(imageUrl)) {

                if (imageUrl != null && imageUrl.length() > 4 && Patterns.WEB_URL.matcher(imageUrl).matches()) {

                    Bitmap bitmap = getBitmapFromURL(imageUrl);

                    if (bitmap != null) {
                        showBigNotification(bitmap, mBuilder, icon, title, message, timeStamp, resultPendingIntent, alarmSound,ui_type,deep_link,external_link,btn1_name,btn2_name);
                    } else {
                        showSmallNotification(mBuilder, icon, title, message, timeStamp, resultPendingIntent, alarmSound,ui_type,deep_link,external_link,btn1_name,btn2_name);
                    }
                }
            } else {
                showSmallNotification(mBuilder, icon, title, message, timeStamp, resultPendingIntent, alarmSound,ui_type,deep_link,external_link,btn1_name,btn2_name);
                playNotificationSound();
            }
        }


        private void showSmallNotification(NotificationCompat.Builder mBuilder, int icon, String title, String message, String timeStamp, PendingIntent resultPendingIntent, Uri alarmSound,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {


            if(ui_type!=null&&!ui_type.isEmpty()&&ui_type.equals("text")){
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.addLine(message);
                Notification notification;
                NotificationCompat.Action action = null,action1=null;
                Intent resume = null;
                PendingIntent pendingIntent = null;

                if(deep_link!=null&&!deep_link.isEmpty()|| external_link!=null&&!external_link.isEmpty()){
                    if(deep_link.startsWith("https://")||deep_link.startsWith("http://"))
                    {
                        resume = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(deep_link));
                        // mContext.startActivity(resume);
                    }else if(external_link.startsWith("https://")||external_link.startsWith("http://")){
                        resume = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(external_link));
                    }else {
                        String resumeName = mContext.getPackageName()+"."+deep_link;

                        try {
                            Class newClass = Class.forName(resumeName);
                            resume = new Intent(mContext, newClass);

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();

                        }
                    }
                }


                if(isCallable(resume)){
                    pendingIntent = PendingIntent.getActivity(mContext, 1, resume, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                action = new NotificationCompat.Action.Builder(0, btn1_name, pendingIntent).build();
                action1 = new NotificationCompat.Action.Builder(0, btn2_name, pendingIntent).build();
                notification = mBuilder.setSmallIcon(icon).setTicker(title).setWhen(0)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentIntent(resultPendingIntent)
                        .setSound(alarmSound)
                        .setStyle(inboxStyle)
                        .setWhen(getTimeMilliSec(timeStamp))
                        .setSmallIcon(icon)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                        .setContentText(message)
                        .addAction(action)
                        .addAction(action1)
                        .build();
                notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(WAConfig.NOTIFICATION_ID, notification);

            }else {



                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.addLine(message);
                Notification notification;
                notification = mBuilder.setSmallIcon(icon).setTicker(title).setWhen(0)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentIntent(resultPendingIntent)
                        .setSound(alarmSound)
                        .setStyle(inboxStyle)
                        .setWhen(getTimeMilliSec(timeStamp))
                        .setSmallIcon(icon)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                        .setContentText(message)
                        .build();
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(WAConfig.NOTIFICATION_ID, notification);
            }
        }

        private void showBigNotification(final Bitmap bitmap, NotificationCompat.Builder mBuilder, int icon, final String title, final String message, String timeStamp, PendingIntent resultPendingIntent, Uri alarmSound,String ui_type,String deep_link,String external_link,String btn1_name,String btn2_name) {

            bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.setBigContentTitle(title);
            bigPictureStyle.setSummaryText(Html.fromHtml(message).toString());
            bigPictureStyle.bigPicture(bitmap);
            Notification notification;
            NotificationCompat.Action action = null,action1=null;
            Intent resume = null;
            PendingIntent pendingIntent = null;

            System.out.println("Notification clicked or not "+ title);

            if(ui_type!=null&&!ui_type.isEmpty()&&ui_type.equals("banner")||ui_type.equals("text")){
                if(deep_link!=null&&!deep_link.isEmpty() || external_link!=null&&!external_link.isEmpty()){
                    if(deep_link.startsWith("https://")||deep_link.startsWith("http://"))
                    {
                        resume = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(deep_link));
                        // mContext.startActivity(resume);
                    }else if(external_link.startsWith("https://")||external_link.startsWith("http://")){
                        resume = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(external_link));
                    }else {
                        String resumeName = mContext.getPackageName()+"."+deep_link;

                        try {
                            Class newClass = Class.forName(resumeName);
                            resume = new Intent(mContext, newClass);

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();

                        }
                    }
                }

                if(isCallable(resume)){
                    pendingIntent = PendingIntent.getActivity(mContext, 1, resume, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                    action = new NotificationCompat.Action.Builder(0, btn1_name, pendingIntent).build();
                    action1 = new NotificationCompat.Action.Builder(0, btn2_name, pendingIntent).build();



                notification = mBuilder.setSmallIcon(icon).setTicker(title).setWhen(0)
                            .setAutoCancel(true)
                            .setContentTitle(title)
                            .setContentIntent(resultPendingIntent)
                            .setSound(alarmSound)
                            .setStyle(bigPictureStyle)
                            .setColor(ContextCompat.getColor(mContext, android.R.color.holo_blue_light))
                            .setWhen(getTimeMilliSec(timeStamp))
                            .setSmallIcon(icon)
                            .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                            .setContentText(message)
                            .addAction(action)
                            .addAction(action1)
                            .build();
                    notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
                    NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(WAConfig.NOTIFICATION_ID_BIG_IMAGE, notification);

            }else{
                System.out.println("Notification clicked or not @@@@@@@ "+ title);

                notification = mBuilder.setSmallIcon(icon).setTicker(title).setWhen(0)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentIntent(resultPendingIntent)
                        .setSound(alarmSound)
                        .setStyle(bigPictureStyle)
                        .setWhen(getTimeMilliSec(timeStamp))
                        .setSmallIcon(icon)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), icon))
                        .setContentText(message)
                        .build();
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(WAConfig.NOTIFICATION_ID_BIG_IMAGE, notification);



            }

        }

        /**
         * Downloading push notification image before displaying it in
         * the notification tray
         */
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

        // Playing notification sound
        public void playNotificationSound() {
            try {
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                String strUri = "android.resource://"+
                        mContext.getPackageName()+  "/" + "raw/blasters";
              //  Uri alarmSound = Uri.parse(strUri);
//                 Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
//                        + "://" + "com.walinns.walinnsapi" + "/raw/notification");
                Ringtone r = RingtoneManager.getRingtone(mContext, alarmSound);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Method checks if the app is in background or not
         */
        public static boolean isAppIsInBackground(Context context) {
            boolean isInBackground = true;
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (String activeProcess : processInfo.pkgList) {
                            if (activeProcess.equals(context.getPackageName())) {
                                isInBackground = false;
                            }
                        }
                    }
                }
            } else {
                List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                if (componentInfo.getPackageName().equals(context.getPackageName())) {
                    isInBackground = false;
                }
            }

            return isInBackground;
        }

        // Clears notification tray messages
        public static void clearNotifications(Context context) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }

        public static long getTimeMilliSec(String timeStamp) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date date = format.parse(timeStamp);
                return date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

}
