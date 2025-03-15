package com.getcapacitor.community.classes.options;

import androidx.annotation.Nullable;
import com.getcapacitor.PluginCall;
import com.getcapacitor.community.NearbyConfig;

public abstract class EndpointOptions {

    @Nullable
    private String endpointID;

    @Nullable
    private String endpointInfo;

    public EndpointOptions(PluginCall call, @Nullable NearbyConfig config) {
        String endpointID = call.getString("endpointID");
        this.setEndpointID(endpointID);

        String endpointInfo = call.getString("endpointInfo", (config == null) ? null : config.getEndpointInfo());
        this.setEndpointInfo(endpointInfo);
    }

    public EndpointOptions(PluginCall call) {
        this(call, null);
    }

    public void setEndpointID(@Nullable String endpointID) {
        this.endpointID = endpointID;
    }

    public void setEndpointInfo(@Nullable String endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    @Nullable
    public String getEndpointID() {
        return endpointID;
    }

    @Nullable
    public String getEndpointInfo() {
        return endpointInfo;
    }
}
