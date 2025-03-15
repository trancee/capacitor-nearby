package com.getcapacitor.community.classes.options;

import androidx.annotation.Nullable;
import com.getcapacitor.PluginCall;
import com.getcapacitor.community.NearbyConfig;

public class InitializeOptions {

    @Nullable
    private String endpointInfo;

    @Nullable
    private String serviceID;

    public InitializeOptions(PluginCall call, NearbyConfig config) {
        String endpointInfo = call.getString("endpointInfo");
        //        if (endpointName != null) {
        //            config.endpointName = (endpointName);
        //        }
        this.setEndpointInfo(endpointInfo);

        String serviceID = call.getString("serviceID");
        //        if (serviceID != null) {
        //            config.setServiceID(serviceID);
        //        }
        this.setServiceID(serviceID);
    }

    public void setEndpointInfo(@Nullable String endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public void setServiceID(@Nullable String serviceID) {
        this.serviceID = serviceID;
    }

    @Nullable
    public String getEndpointInfo() {
        return endpointInfo;
    }

    @Nullable
    public String getServiceID() {
        return serviceID;
    }
}
