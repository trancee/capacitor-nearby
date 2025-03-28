package com.getcapacitor.community.classes.options;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.getcapacitor.PluginCall;
import com.getcapacitor.community.NearbyConfig;

public class InitializeOptions {

    @Nullable
    private String endpointName;

    @Nullable
    private byte[] endpointInfo;

    @Nullable
    private String serviceID;

    public InitializeOptions(PluginCall call, NearbyConfig config) {
        @Nullable
        String endpointName = call.getString("endpointName");
        if (endpointName != null && !endpointName.isEmpty()) {
            config.setEndpointName(endpointName);
        }
        this.setEndpointName(endpointName);

        @Nullable
        byte[] endpointInfo = null;

        @Nullable
        String value = call.getString("endpointInfo");
        if (value != null && !value.isEmpty()) {
            try {
                endpointInfo = Base64.decode(value, Base64.NO_WRAP);
            } catch (IllegalArgumentException ignored) {
                endpointInfo = value.getBytes();
            }

            config.setEndpointInfo(endpointInfo);
        }
        this.setEndpointInfo(endpointInfo);

        String serviceID = call.getString("serviceID");
        if (serviceID != null && !serviceID.isEmpty()) {
            config.setServiceID(serviceID);
        }
        this.setServiceID(serviceID);
    }

    public void setEndpointName(@Nullable String endpointName) {
        this.endpointName = endpointName;
    }

    public void setEndpointInfo(@Nullable byte[] endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public void setServiceID(@Nullable String serviceID) {
        this.serviceID = serviceID;
    }

    @Nullable
    public String getEndpointName() {
        return endpointName;
    }

    @Nullable
    public byte[] getEndpointInfo() {
        return endpointInfo;
    }

    @Nullable
    public String getServiceID() {
        return serviceID;
    }
}
