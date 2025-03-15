package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.classes.Endpoint;
import com.getcapacitor.community.classes.PayloadTransferUpdate;

public class PayloadTransferUpdateEvent extends EndpointEvent {

    @NonNull
    PayloadTransferUpdate update;

    public PayloadTransferUpdateEvent(@NonNull Endpoint endpoint, @NonNull PayloadTransferUpdate update) {
        super(endpoint);
        this.update = update;
    }

    @NonNull
    public JSObject toJSObject() {
        JSObject result = super.toJSObject();

        result.put("payloadID", update.payloadID());

        result.put("status", update.getStatus());

        result.put("bytesTransferred", update.bytesTransferred());
        result.put("totalBytes", update.totalBytes());

        return result;
    }
}
