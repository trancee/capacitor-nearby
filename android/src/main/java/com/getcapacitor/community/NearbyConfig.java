package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;
import static com.getcapacitor.community.NearbyHelper.generateEndpointID;

import androidx.annotation.Nullable;

public class NearbyConfig {

    static EndpointID endpointID;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    String serviceID;

    public NearbyConfig(@Nullable byte[] endpointInfo, @Nullable String serviceID) {
        this.endpointInfo = endpointInfo;
        this.serviceID = serviceID;

        endpointID = generateEndpointID(endpointInfo);
    }

    public void setEndpointInfo(@Nullable byte[] endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public void setServiceID(@Nullable String serviceID) {
        this.serviceID = serviceID;
    }

    @Nullable
    public byte[] getEndpointInfo() {
        return endpointInfo;
    }

    @Nullable
    public String getServiceID() {
        return serviceID;
    }

    EndpointID getEndpointID() {
        return endpointID;
    }
}
