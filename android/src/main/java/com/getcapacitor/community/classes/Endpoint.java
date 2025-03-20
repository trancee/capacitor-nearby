package com.getcapacitor.community.classes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record Endpoint(@NonNull String endpointID, @Nullable byte[] endpointInfo) {
    public Endpoint(@NonNull String endpointID) {
        this(endpointID, null);
    }

    @NonNull
    public String toString() {
        return endpointID;
    }
}
