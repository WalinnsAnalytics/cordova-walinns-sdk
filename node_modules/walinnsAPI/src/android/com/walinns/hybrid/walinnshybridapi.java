package com.walinns.hybrid;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;


/**
 * This class echoes a string called from JavaScript.
 */
public class walinnshybridapi extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0) + "-" + args.getString(1);
            this.coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("trackEvent")) {
            this.trackEvent(args.getString(0), args.getString(1), callbackContext);
            return true;
        } else if (action.equals("trackScreen")) {
            this.trackScreen(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Error");
        }
    }


    private void trackEvent(String type, String eventName, CallbackContext callbackContext) {
        if (type != null && type.length() > 0) {
            callbackContext.success(type + " - " + eventName);
            WalinnsAPI.getInstance().track(type, eventName);
        } else {
            callbackContext.error("Error");
        }
    }

    private void trackScreen(String screenName, CallbackContext callbackContext) {
        if (screenName != null && screenName.length() > 0) {
            WalinnsAPI.getInstance().track(screenName);
            callbackContext.success(screenName);
        } else {
            callbackContext.error("Error");
        }
    }
}
