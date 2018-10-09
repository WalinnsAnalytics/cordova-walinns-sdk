package com.walinns.hybrid;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by walinnsinnovation on 30/12/17.
 */

public class WalinnsAPIClient extends Activity {
    private static final WALog logger = WALog.getLogger();
    private WADeviceInfo deviceInfo;
    protected static Context context, mContext;
    protected String deviceId;
    protected String project_token;
    WAWorkerThread logThread;
    WAWorkerThread httpThread;
    protected String instanceName;
    protected WAPref shared_pref;
    protected WALifeCycle mWalinnsactivitylifecycle;
    public JSONObject device_hashMap;
    public static boolean flag_once = false;
    WAProfile waProfile;


    public WalinnsAPIClient(Context context) {
        this((String) null);
        mContext = context;
        this.shared_pref = new WAPref(mContext);
        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);

    }

    public WalinnsAPIClient(String instance) {
        this.logThread = new WAWorkerThread("logThread");
        this.httpThread = new WAWorkerThread("httpThread");
        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);

        this.instanceName = WAUtils.normalizeInstanceName(instance);
        this.logThread.start();
        this.httpThread.start();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public WalinnsAPIClient initialize(Context context, String project_token) {
        this.mContext = context;
        new APIClient(mContext);
        this.shared_pref = new WAPref(context);
        shared_pref.save(WAPref.project_token, project_token);
        mContext.startService(new Intent(mContext, WAIntentService.class));
        logger.d("WalinnsTrackerClient Token:", project_token);
        logger.e("Notification clicked or not init ", shared_pref.getValue(WAPref.noify_clicked));
        if (getIntent() != null) {
            if (getIntent().getStringExtra("message") != null) {
                logger.d("Push notification text:", getIntent().getStringExtra("message"));
            } else {
                logger.d("Push notification text:", "first else detected");
            }
        } else {
            logger.d("Push notification text:", "else detected");
        }
        return this.initialize(context, project_token, (String) null);
    }

    private synchronized WalinnsAPIClient initialize(final Context context, String apiKey, final String userId) {
        // connect to app and dashboard
        if (context == null && mContext == null) {
            logger.e("WalinnsTrackerClient", "Argument context cannot be null in initialize()");
            return this;
        } else if (WAUtils.isEmptyString(apiKey)) {
            logger.e("WalinnsTrackerClient", "Argument apiKey cannot be null or blank in initialize()");
            return this;
        } else {
            String v1 = "NA", v2 = "NA", f1 = "NA", l1 = "NA";
            this.context = context.getApplicationContext();
            this.project_token = apiKey;
            this.shared_pref = new WAPref(this.context);
            if (shared_pref.getValue(WAPref.gender) != null) {
                logger.e("WalinnsTrackerClientt gender", "Argument" + shared_pref.getValue(WAPref.gender));
                v1 = shared_pref.getValue(WAPref.gender);
            }
            if (shared_pref.getValue(WAPref.age) != null) {
                logger.e("WalinnsTrackerClientt age", "Argument" + shared_pref.getValue(WAPref.age));

                v2 = shared_pref.getValue(WAPref.age);
            }
            if (!shared_pref.getValue(WAPref.first_name).isEmpty()) {
                f1 = shared_pref.getValue(WAPref.first_name);
            }
            if (!shared_pref.getValue(WAPref.last_name).isEmpty()) {
                l1 = shared_pref.getValue(WAPref.last_name);
            }
            deviceCall(v1, v2, f1, l1, "NA", "default", "NA");

        }
        return this;
    }


    private WADeviceInfo.CachedInfo initializeDeviceInfo() {

        this.deviceInfo = new WADeviceInfo(mContext);
        this.deviceId = Settings.Secure.getString(this.context.getContentResolver(), "android_id");
        shared_pref.save(WAPref.device_id, deviceId);
        logger.e("WalinnsTrackerClient", deviceId + "..." + deviceInfo.toString());
        this.deviceInfo.prefetch();
        return this.deviceInfo.prefetch();
    }

    protected void runOnLogThread(Runnable r) {
        if (Thread.currentThread() != this.logThread) {
            this.logThread.post(r);
        } else {
            r.run();

        }

    }

    protected long getCurrentTimeMillis() {
        logger.e("Current session", String.valueOf(System.currentTimeMillis()));
        return System.currentTimeMillis();
    }

    public void track(String eventType /*view name like Button*/, String event_name/*Button name like submit*/) {
        this.logEvent(eventType, event_name);
    }

    private void logEvent(String eventType, String event_name) {
        // if(this.validateLogEvent(eventType)) {
        this.logEventAsync(eventType, event_name, this.getCurrentTimeMillis());
        //}
    }

    protected boolean validateLogEvent(String eventType) {
        if (TextUtils.isEmpty(eventType)) {
            logger.e("WalinnsTrackerClient", "Argument eventType cannot be null or blank in eventTrack()");
            return false;
        } else {
            return this.contextAndApiKeySet("logEvent()");
        }
    }

    protected synchronized boolean contextAndApiKeySet(String methodName) {
        if (this.context == null && mContext == null) {
            logger.e("WalinnsTrackerClient", "context cannot be null, set context with initialize() before calling " + methodName);
            return false;
        } else if (TextUtils.isEmpty(this.project_token)) {
            logger.e("WalinnsTrackerClient", "apiKey cannot be null or empty, set apiKey with initialize() before calling " + methodName);
            return false;
        } else {
            return true;
        }
    }

    protected void logEventAsync(final String eventType, final String event_name, final long timestamp) {
        if (event_name != null) {
            this.runOnLogThread(new Runnable() {
                public void run() {
                    WalinnsAPIClient.this.logEvent(eventType, event_name, timestamp);
                }
            });
        }


    }

    private void logEvent(String eventType, String event_name, long timestamp) {
        //deviceId = shared_pref.getValue(WAPref.device_id);
        deviceId = "594c1e64588f53a3";
        logger.e("walinnstrackerclient device_id", deviceId);
        JSONObject hashMap = new JSONObject();
        try {

            hashMap.put("event_name", event_name);
            hashMap.put("device_id", deviceId);
            hashMap.put("date_time", WAUtils.getCurrentUTC());
            hashMap.put("event_type", eventType);
            logger.e("WalinnTrackerClient date_time_event default", hashMap.toString());
            new APIClient(mContext, "events", hashMap, "events");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void install_refferer() {

        JSONObject install = new JSONObject();
        final SharedPreferences referralInfo = mContext.getSharedPreferences("WAInstall_refferer", Context.MODE_PRIVATE);

        String lanSettings = referralInfo.getString("utm_source", null);
        String device_id = Settings.Secure.getString(mContext.getContentResolver(), "android_id");
        try {
            if (referralInfo.getString("utm_source", null) != null) {
                track("UTM_Visited", referralInfo.getString("utm_source", null));
                install.put("medium", referralInfo.getString("utm_source", null));
            }
            if (referralInfo.getString("referrer", null) != null) {
                install.put("refferer_url", referralInfo.getString("referrer", null));
            }
//            if(referralInfo.getString("utm_term", null) != null){
//                install.put("utm_term", referralInfo.getString("utm_term", null));
//            }
            if (referralInfo.getString("utm_campaign", null) != null) {
                install.put("campign_name", referralInfo.getString("utm_campaign", null));
            }
            if (referralInfo.getString("utm_content", null) != null) {
                install.put("campign_source", referralInfo.getString("utm_content", null));
            }
            if (referralInfo.getString("utm_medium", null) != null) {
                if (referralInfo.getString("utm_medium", null).equals("organic")) {
                    install.put("channel", referralInfo.getString("utm_medium", null));

                } else if (referralInfo.getString("utm_medium", null).equals("(not%20set)")) {
                    install.put("channel", "Direct");
                } else {
                    install.put("channel", referralInfo.getString("utm_medium", null));
                }
            }


            install.put("device_id", device_id);
            install.put("date_time", WAUtils.getCurrentUTC());

            new APIClient(mContext, "refferrer", install, "refferrer");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(14)
    private void registerWalinnsActivityLifecycleCallbacks(String token) {
        if (Build.VERSION.SDK_INT >= 14) {
            if (this.mContext != null) {
                logger.e("WalinnsTrackerClient", "life_cycle_method" + "inside_if");
                Application app = (Application) this.mContext.getApplicationContext();
                this.mWalinnsactivitylifecycle = new WALifeCycle(this, WAConfig.getInstance(mContext), mContext, token);
                app.registerActivityLifecycleCallbacks(this.mWalinnsactivitylifecycle);
                mContext.startService(new Intent(mContext, WAService.class)); //start service which is MyService.java

            } else {
                logger.i("WalinnsTrackerClient", "Context is not an Application, Walinns will not automatically show in-app notifications or A/B test experiments. We won\'t be able to automatically flush on an app background.");
            }
        }

    }

    protected void track_(String eventName) {//
        logger.e("WalinnsTrackerClient gesture tracker", eventName);
    }

    protected void track_(String eventName, JSONObject properties, boolean isAutomaticEvent) {
        try {
            // logger.e("WalinnsTrackerClient  tracker_session", eventName + Utils.convertUtctoCurrent(properties.getString("$start_time"),properties.getString("$end_time")));
            if (isAutomaticEvent) {

                final JSONObject hashMapp = new JSONObject();
                hashMapp.put("device_id", shared_pref.getValue(WAPref.device_id));
                if (!WAUtils.convertUtctoCurrent(properties.getString("$start_time"), properties.getString("$end_time")).isEmpty()) {
                    hashMapp.put("session_length", WAUtils.convertUtctoCurrent(properties.getString("$start_time"), properties.getString("$end_time")));

                } else {
                    hashMapp.put("session_length", properties.getString("$ae_session_length"));
                }
                hashMapp.put("start_time", properties.getString("$start_time"));
                hashMapp.put("end_time", properties.getString("$end_time"));

                this.runOnLogThread(new Runnable() {
                    public void run() {
                        // Call<ResponseBody> call = apiService.session(hashMapp);
                        //  call.enqueue(callResponse);
                        new APIClient(mContext, "session", hashMapp, "session");
                    }
                });

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    protected void track_(String value, boolean status) {
        logger.e("WalinnsTrackerClient active_status", String.valueOf(status) + shared_pref.getValue(WAPref.device_id));
        JSONObject hash = null;
        try {
            if (status) {
                hash = new JSONObject();
                hash.put("active_status", "yes");
                hash.put("date_time", value);
                hash.put("device_id", shared_pref.getValue(WAPref.device_id));


                //track("default_event","App Launch");
                // track("default_event","App Screen Viewed");

            } else {

                hash = new JSONObject();
                hash.put("active_status", "no");
                hash.put("date_time", value);
                hash.put("device_id", shared_pref.getValue(WAPref.device_id));
                shared_pref.save(WAPref.app_launch_called, "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JSONObject finalHash = hash;
        this.runOnLogThread(new Runnable() {
            public void run() {

                new APIClient(mContext, "fetchAppUserDetail", finalHash, "fetchAppUserDetail");
            }
        });
    }

    private Thread.UncaughtExceptionHandler handleAppCrash =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, final Throwable ex) {
                    logger.e("WalinnsTrackerClient crash report", ex.getMessage() + thread.getName());
                    shared_pref.save(WAPref.crash_report, ex.toString());
                    runOnLogThread(new Runnable() {
                        @Override
                        public void run() {

                            sendCreash();

                        }
                    });

                }
            };

    private void sendCreash() {
        runOnLogThread(new Runnable() {
            public void run() {
                final JSONObject hash;
                hash = new JSONObject();
                if (!WAUtils.isEmptyString(shared_pref.getValue(WAPref.crash_report))) {
                    try {
                        hash.put("reason", shared_pref.getValue(WAPref.crash_report));
                        hash.put("device_id", shared_pref.getValue(WAPref.device_id));
                        hash.put("date_time", WAUtils.getCurrentUTC());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    new APIClient(mContext, "crashReport", hash, "crashReport");
                }
            }
        });
    }

    public void track(final String screen_name) {
        runOnLogThread(new Runnable() {
            public void run() {
                final JSONObject hash;
                hash = new JSONObject();
                if (!WAUtils.isEmptyString(screen_name)) {
                    try {
                        hash.put("screen_name", screen_name);
                        hash.put("date_time", WAUtils.getCurrentUTC());
                        hash.put("device_id", shared_pref.getValue(WAPref.device_id));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    logger.e("WalinnsTrackerClient", "ScreenView value is empty" + "Please enter valid name");
                }

                new APIClient(mContext, "screenView", hash, "screenView");

            }
        });
    }

    protected void sendpush() {
        String pushtoken = null;
        if (!shared_pref.getValue(WAPref.push_token).isEmpty()) {
            pushtoken = shared_pref.getValue(WAPref.push_token);
        } else {
            pushtoken = "null";
        }

        logger.d("WalinnsTracker push token", pushtoken);
        logger.d("WalinnsTracker package name", mContext.getPackageName());
        final String finalPushtoken = pushtoken;
        runOnLogThread(new Runnable() {
            public void run() {
                final JSONObject hash;
                hash = new JSONObject();
                if (!WAUtils.isEmptyString(finalPushtoken)) {
                    try {
                        hash.put("push_token", finalPushtoken);
                        hash.put("package_name", mContext.getPackageName());
                        hash.put("device_id", shared_pref.getValue(WAPref.device_id));
                        hash.put("date_time", WAUtils.getCurrentUTC());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    new APIClient(mContext, "uninstallcount", hash, "uninstallcount");
                }
            }
        });
    }

    protected void sendNotificationEvent() {

        runOnLogThread(new Runnable() {
            public void run() {
                if (shared_pref.getValue(WAPref.noify_clicked) != null) {
                    deviceId = shared_pref.getValue(WAPref.device_id);
                    logger.e("walinnstrackerclient device_id *************", shared_pref.getValue(WAPref.noify_clicked));
                    JSONObject hashMap = new JSONObject();
                    try {

                        hashMap.put("event_name", shared_pref.getValue(WAPref.noify_clicked));
                        hashMap.put("device_id", deviceId);
                        hashMap.put("date_time", WAUtils.getCurrentUTC());


                        hashMap.put("event_type", "default_event");
                        logger.e("WalinnTrackerClient date_time_event default", hashMap.toString());
                        new APIClient(mContext, "events", hashMap, "notify_events");


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    protected void lifeCycle(String token) {
        logger.e("WalinnsTrackerClient", "life_cycle_method_detected");
        registerWalinnsActivityLifecycleCallbacks(token);
    }


    public void pushProfile(JSONObject jsonObject) {
        waProfile = new WAProfile();
        if (jsonObject != null) {
            logger.e("WalinnsTrackerClient Profile :", jsonObject.toString());
            try {
                if (jsonObject.has("gender")) {
                    waProfile.setGender(jsonObject.getString("gender"));
                } else {
                    waProfile.setGender("NA");
                }

                if (jsonObject.has("birthday")) {
                    SimpleDateFormat df = new SimpleDateFormat("dd/mm/yyyy");
                    Date birthdate = df.parse(jsonObject.getString("birthday"));
                    logger.e("WalinnsTrackerClient Profile birthdate :", "Age: " + WAUtils.calculateAge(birthdate));
                    waProfile.setAge(String.valueOf(WAUtils.calculateAge(birthdate)));
                } else {
                    waProfile.setAge("NA");
                }

                if (jsonObject.has("first_name")) {
                    waProfile.setFirst_name(jsonObject.getString("first_name"));
                } else {
                    waProfile.setFirst_name(" ");
                }

                if (jsonObject.has("last_name")) {
                    waProfile.setLast_name(jsonObject.getString("last_name"));
                } else {
                    waProfile.setLast_name(" ");
                }

                if (jsonObject.has("id")) {
                    try {
                        URL profile_pic = new URL("https://graph.facebook.com/" + jsonObject.getString("id") + "/picture?width=200&height=150");
                        Log.i("profile_pic", profile_pic + "");
                        waProfile.setProfile_pic(profile_pic.toString());

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("email")) {
                    waProfile.setEmail(jsonObject.getString("email"));

                }


            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            logger.e("WalinnsTrackerClient Profile birthdate :", "Age: " + waProfile.gender + "..." + waProfile.age);
            shared_pref.save(WAPref.gender, waProfile.getGender());
            shared_pref.save(WAPref.age, waProfile.getAge());
            shared_pref.save(WAPref.first_name, waProfile.getFirst_name());
            shared_pref.save(WAPref.last_name, waProfile.getLast_name());
            shared_pref.save(WAPref.profile_pic, waProfile.getProfile_pic());
            shared_pref.save(WAPref.email, waProfile.getEmail());
            deviceCall(waProfile.getGender(), waProfile.getAge(), waProfile.getFirst_name(), waProfile.getLast_name(), waProfile.getProfile_pic(), "fb", waProfile.getEmail());
        }
    }

    public void pushProfile(String acess_token) {
        if (acess_token != null) {
            logger.e("WalinnsTrackerClient Profile token:", acess_token);
            waProfile = new WAProfile();
            URL url = null;
            try {
                url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + acess_token);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                int sc = con.getResponseCode();
                if (sc == 200) {
                    InputStream is = con.getInputStream();
                    String response = readResponse(is);
                    is.close();
                    JSONObject jsonObject = new JSONObject(response);
                    System.out.println("Google login profile picture data :" + jsonObject.toString());
                    if (jsonObject.has("gender")) {
                        waProfile.setGender(jsonObject.getString("gender"));
                    } else {
                        waProfile.setGender("NA");
                    }
                    if (jsonObject.has("birthday")) {
                        SimpleDateFormat df = new SimpleDateFormat("dd/mm/yyyy");
                        Date birthdate = df.parse(jsonObject.getString("birthday"));
                        logger.e("WalinnsTrackerClient Profile birthdate :", "Age: " + WAUtils.calculateAge(birthdate));
                        waProfile.setAge(String.valueOf(WAUtils.calculateAge(birthdate)));
                    } else {
                        waProfile.setAge("NA");
                    }
                    if (jsonObject.has("given_name")) {
                        waProfile.setFirst_name(jsonObject.getString("given_name"));
                    } else {
                        waProfile.setLast_name("");
                    }

                    if (jsonObject.has("family_name")) {
                        waProfile.setLast_name(jsonObject.getString("family_name"));
                    } else {
                        waProfile.setLast_name("");
                    }

                    if (jsonObject.has("picture")) {

                        waProfile.setProfile_pic(jsonObject.getString("picture"));
                    } else {
                        waProfile.setProfile_pic("NA");
                    }

                    shared_pref.save(WAPref.gender, waProfile.getGender());
                    shared_pref.save(WAPref.age, waProfile.getAge());
                    shared_pref.save(WAPref.first_name, waProfile.getFirst_name());
                    shared_pref.save(WAPref.last_name, waProfile.getLast_name());
                    shared_pref.save(WAPref.profile_pic, waProfile.getProfile_pic());
                    deviceCall(waProfile.getGender(), waProfile.getAge(), waProfile.getFirst_name(), waProfile.getLast_name(), waProfile.getProfile_pic(), "google", "NA");
                    return;
                } else if (sc == 401) {

                    logger.e("Server auth error, please try again.", null);

                    return;
                } else {
                    logger.e("Server returned the following error code: " + sc, null);
                    return;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    protected void deviceCall(final String v1, final String v2, final String f1, final String l1, final String profile_picture, final String flag, final String email_new) {
        this.runOnLogThread(new Runnable() {
            @Override
            public void run() {
                WADeviceInfo.CachedInfo cachedInfo = initializeDeviceInfo();

                logger.e("Notification clicked or not", shared_pref.getValue(WAPref.noify_clicked));
                device_hashMap = new JSONObject();
                try {
                    device_hashMap.put("device_id", deviceId);
                    device_hashMap.put("device_model", cachedInfo.brand);
                    device_hashMap.put("device_manufacture", cachedInfo.manufacturer);
                    device_hashMap.put("os_name", cachedInfo.osName);
                    device_hashMap.put("os_version", cachedInfo.osVersion);
                    device_hashMap.put("app_version", cachedInfo.app_version);
                    device_hashMap.put("connectivity", cachedInfo.connectivty);
                    device_hashMap.put("carrier", cachedInfo.carrier);
                    device_hashMap.put("play_service", String.valueOf(cachedInfo.playservice));
                    device_hashMap.put("bluetooth", String.valueOf(cachedInfo.bluetooth));
                    device_hashMap.put("screen_dpi", cachedInfo.screen_dpi);
                    device_hashMap.put("screen_height", cachedInfo.screen_height);
                    device_hashMap.put("screen_width", cachedInfo.screen_width);
                    if (flag.equals("fb") || flag.equals("google")) {
                        device_hashMap.put("gender", v1);
                        device_hashMap.put("age", v2);
                    } else {
                        device_hashMap.put("gender", "NA");
                        device_hashMap.put("age", "NA");
                    }
                    device_hashMap.put("language", cachedInfo.language);
                    device_hashMap.put("country", cachedInfo.country);
                    device_hashMap.put("date_time", WAUtils.getCurrentUTC());
                    device_hashMap.put("sdk_version", cachedInfo.sdk_version);
                    device_hashMap.put("notify_status", cachedInfo.notify_status);
                    device_hashMap.put("app_language", cachedInfo.app_language);
                    device_hashMap.put("device_type", cachedInfo.device_type);
                    device_hashMap.put("profile_pic", profile_picture);
                    //  device_hashMap.put("Notification viewd or not",shared_pref.getValue(WAPref.noify_clicked));

                    if (cachedInfo.city == null) {
                        device_hashMap.put("city", "NA");
                    } else {
                        device_hashMap.put("city", cachedInfo.city);

                    }
                    if (cachedInfo.state == null) {
                        device_hashMap.put("state", "NA");

                    } else {
                        device_hashMap.put("state", cachedInfo.state);

                    }
                    if (flag.equals("user_profile")) {
                        device_hashMap.put("First_name", f1);
                        device_hashMap.put("Last_name", l1);
                        device_hashMap.put("email", v1);
                        device_hashMap.put("phone_number", v2);
                    } else {
                        device_hashMap.put("First_name", f1);
                        device_hashMap.put("Last_name", l1);
                        device_hashMap.put("email", v1);
                        device_hashMap.put("phone_number", v2);
                    }
                    if (email_new != null && !email_new.isEmpty()) {
                        device_hashMap.put("email", email_new);
                    }
                    new APIClient(mContext, "devices", device_hashMap, "devices");

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }


    public void pushUserProfile(JSONObject jsonObject) {

        UserProfile userProfile = new UserProfile();

        try {
            if (jsonObject.has("First_name")) {
                String name = jsonObject.getString("First_name");
                userProfile.setFirst_name(name);
            }
            if (jsonObject.has("Last_name")) {
                String last_name = jsonObject.getString("Last_name");
                userProfile.setLast_name(last_name);
            }
            if (jsonObject.has("email")) {
                String email = jsonObject.getString("email");
                userProfile.setEmail(email);
            }
            if (jsonObject.has("phone_number")) {
                String phone = jsonObject.getString("phone_number");
                userProfile.setPhone_no(phone);
            }

            shared_pref.save(WAPref.email, userProfile.getEmail());
            shared_pref.save(WAPref.phone, userProfile.getPhone_no());
            shared_pref.save(WAPref.first_name, userProfile.getFirst_name());
            shared_pref.save(WAPref.last_name, userProfile.getLast_name());
            deviceCall(userProfile.getEmail(), userProfile.getPhone_no(), userProfile.getFirst_name(), userProfile.getLast_name(), "NA", "user_profile", "NA");


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
