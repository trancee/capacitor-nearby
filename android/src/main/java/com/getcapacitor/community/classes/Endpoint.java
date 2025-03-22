package com.getcapacitor.community.classes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record Endpoint(@NonNull String endpointID, @Nullable String endpointName, @Nullable byte[] endpointInfo) {
    public Endpoint(@NonNull String endpointID, @Nullable String endpointName) {
        this(endpointID, endpointName, null);
    }
    public Endpoint(@NonNull String endpointID) {
        this(endpointID, null, null);
    }

    @NonNull
    public String toString() {
        return endpointID;
    }
}
