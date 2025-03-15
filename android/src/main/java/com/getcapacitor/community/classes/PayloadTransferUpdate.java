package com.getcapacitor.community.classes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record PayloadTransferUpdate(@NonNull Long payloadID, int status, @Nullable Long bytesTransferred, @Nullable Long totalBytes) {
    public String getStatus() {
        return switch (status) {
            case 1 -> "success";
            case 0 -> "canceled";
            case -1 -> "failure";
            case 2 -> "progress";
            default -> "unknown";
        };
    }
}
