package com.getcapacitor.community.classes.options;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.getcapacitor.PluginCall;
import com.getcapacitor.community.NearbyConfig;

public abstract class EndpointOptions {

    @Nullable
    private String endpointID;

    @Nullable
    private byte[] endpointInfo;

    public EndpointOptions(PluginCall call, @Nullable NearbyConfig config) {
        String endpointID = call.getString("endpointID");
        this.setEndpointID(endpointID);

        if (config != null) {
            @Nullable
            byte[] endpointInfo = config.getEndpointInfo();

            @Nullable
            String value = call.getString("endpointInfo");
            if (value != null && !value.isEmpty()) {
                try {
                    endpointInfo = Base64.decode(value, Base64.NO_WRAP);
                } catch (IllegalArgumentException ignored) {
                    endpointInfo = value.getBytes();
                }
            }

            this.setEndpointInfo(endpointInfo);
        }
    }

    public EndpointOptions(PluginCall call) {
        this(call, null);
    }

    public void setEndpointID(@Nullable String endpointID) {
        this.endpointID = endpointID;
    }

    public void setEndpointInfo(@Nullable byte[] endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    @Nullable
    public String getEndpointID() {
        return endpointID;
    }

    @Nullable
    public byte[] getEndpointInfo() {
        return endpointInfo;
    }
}
