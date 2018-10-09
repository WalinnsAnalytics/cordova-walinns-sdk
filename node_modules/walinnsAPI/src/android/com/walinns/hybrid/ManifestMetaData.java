package com.walinns.hybrid;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by walinnsinnovation on 05/02/18.
 */

final class ManifestMetaData {
    private static HashMap<String, String> cachedMeta = new HashMap<String, String>();

    /**
     * Returns the value of the meta data key specified by name.
     *
     * @param context The Android context
     * @param name    The name of the meta data key
     * @return The value of the meta data key, if found, else null
     */
    static String getMetaData(Context context, String name) {

        String meta = cachedMeta.get(name);
        if (meta != null) {
            return meta;
        }
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;
            if (metaData == null) {
                return null;
            } else {
                Object o = metaData.get(name);
                if (o == null) {
                    return null;
                }

                cachedMeta.put(name, o.toString());
                return o.toString();
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
