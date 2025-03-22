package com.getcapacitor.community.classes.events;

import android.util.Base64;
import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.classes.Endpoint;

public class PayloadReceivedEvent extends EndpointEvent {

    @NonNull
    byte[] payload;

    public PayloadReceivedEvent(@NonNull Endpoint endpoint, @NonNull byte[] payload) {
        super(endpoint);
        this.payload = payload;
    }

    @NonNull
    public JSObject toJSObject() {
        JSObject result = super.toJSObject();

        result.put("payload", Base64.encodeToString(payload, Base64.NO_WRAP));

        return result;
    }
}
