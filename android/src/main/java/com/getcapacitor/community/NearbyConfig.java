package com.getcapacitor.community;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NearbyConfig {

    @NonNull
    String endpointID;

    @Nullable
    String endpointName;

    @Nullable
    byte[] endpointInfo;

    @Nullable
    String serviceID;

    public NearbyConfig(@Nullable String endpointName, @Nullable byte[] endpointInfo, @Nullable String serviceID) {
        this.setEndpointName(endpointName);
        this.setEndpointInfo(endpointInfo);

        this.setServiceID(serviceID);
    }

    public void setEndpointName(@Nullable String endpointName) {
        this.endpointName = endpointName;

        endpointID = EndpointID.fromString(endpointName);
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

    @NonNull
    String getEndpointID() {
        return endpointID;
    }
}
