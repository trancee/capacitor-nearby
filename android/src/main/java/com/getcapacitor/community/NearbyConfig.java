package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NearbyConfig {

    @NonNull
    final String endpointID;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    String serviceID;

    public NearbyConfig(@Nullable byte[] endpointInfo, @Nullable String serviceID) {
        this.endpointInfo = endpointInfo;
        this.serviceID = serviceID;

        endpointID = EndpointID.fromBytes(endpointInfo);
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

    @NonNull
    String getEndpointID() {
        return endpointID;
    }
}
