package com.getcapacitor.community.classes;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record Endpoint(@NonNull EndpointID endpointID, @Nullable String endpointInfo) {
    public Endpoint(@NonNull EndpointID endpointID) {
        this(endpointID, null);
    }

    @NonNull
    public String toString() {
        return endpointID.name();
    }
}
