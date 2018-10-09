package com.walinns.hybrid;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import static com.walinns.hybrid.WalinnsAPIClient.flag_once;


/**
 * Created by walinnsinnovation on 30/12/17.
 */

public class APIClient {
    static WAPref sharedPref;
    private static final WALog logger = WALog.getLogger();
    public Context mContext;
    public String mUrl,mType;
    public JSONObject mjsonObject;
    static String URL="https://wa.track.app.walinns.com/";
    public APIClient(Context context){
        sharedPref=new WAPref(context);
    }
    public APIClient(Context context,String url,JSONObject jsonObject , String type){
        this.mContext=context;
        this.mUrl=url;
        this.mjsonObject=jsonObject;
        this.mType = type ;
        sharedPref = new WAPref(mContext);
        Connection();

    }

    private void Connection(){
        try{
            logger.e("WalinnsTrackerClient","Request_data"+ mjsonObject.toString());
            java.net.URL url = new URL(URL+mUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("CONTENT_TYPE", "application/json");
            conn.addRequestProperty("Authorization", sharedPref.getValue( WAPref.project_token));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(mjsonObject));

            writer.flush();
            writer.close();
            os.close();

            logger.e("Http_connection_request_data",sharedPref.getValue( WAPref.project_token));

            int responseCode=conn.getResponseCode();


            if (responseCode == HttpsURLConnection.HTTP_OK) {
                if(mUrl.equals("devices")&&!flag_once){
                    logger.e("WalinnsTrackerClient","life_cycle_method_detected"+mUrl);
                 //   sharedPref.clear(WAPref.noify_clicked);
                    flag_once = true;
                     WalinnsAPIClient walinnsTrackerClient=new WalinnsAPIClient(mContext);
                    walinnsTrackerClient.lifeCycle(sharedPref.getValue( WAPref.project_token));
                }
                logger.e("WalinnsTrackerClient","life_cycle_method_detected mURL"+mType);

                if(mType.equals("notify_events")){
                    logger.e("WAClient Lifecycle WalinnsTrackerHttpConnection notify_events", mUrl);

//                    final SharedPreferences referralInfo = mContext.getSharedPreferences("wa_notify", Context.MODE_PRIVATE);
//                    final SharedPreferences.Editor editor = referralInfo.edit();
//                    editor.putString("content", "").commit();

                }
                logger.e("WalinnsTrackerHttpConnection", conn.getResponseMessage());

            }else {
                logger.e("WalinnsTrackerHttpConnection","Fail");

            }
        }
        catch(Exception e){
            logger.e("WalinnsTrackerHttpConnection","Exce"+e);

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




}
