package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;
import static com.getcapacitor.community.NearbyHelper.generateEndpointID;

import androidx.annotation.Nullable;

public class NearbyConfig {

    private static EndpointID endpointID;

    @Nullable
    String endpointInfo;

    @Nullable
    String serviceID;

    public NearbyConfig(@Nullable String endpointInfo, @Nullable String serviceID) {
        this.endpointInfo = endpointInfo;
        this.serviceID = serviceID;

        endpointID = generateEndpointID(endpointInfo);
    }

    EndpointID getEndpointID() {
        return endpointID;
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
