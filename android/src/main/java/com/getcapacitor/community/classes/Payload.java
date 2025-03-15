package com.getcapacitor.community.classes;

import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record Payload(@NonNull Long payloadID, @Nullable byte[] data) {
    public String getPayload() {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
