package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.classes.Endpoint;
import com.getcapacitor.community.classes.Payload;

public class PayloadReceivedEvent extends EndpointEvent {

    @NonNull
    Payload payload;

    public PayloadReceivedEvent(@NonNull Endpoint endpoint, @NonNull Payload payload) {
        super(endpoint);
        this.payload = payload;
    }

    @NonNull
    public JSObject toJSObject() {
        JSObject result = super.toJSObject();

        result.put("payloadID", payload.payloadID());
        result.put("payload", payload.getPayload());

        return result;
    }
}
