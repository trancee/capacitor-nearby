package com.getcapacitor.community.classes.events;

import android.util.Base64;
import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointEvent {

    @NonNull
    Endpoint endpoint;

    public EndpointEvent(@NonNull Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @NonNull
    public JSObject toJSObject() {
        JSObject result = new JSObject();

        result.put("endpointID", endpoint.endpointID());

        byte[] endpointInfo = endpoint.endpointInfo();
        if (endpointInfo != null && endpointInfo.length > 0) {
            result.put("endpointInfo", Base64.encodeToString(endpointInfo, Base64.NO_WRAP));
        }

        return result;
    }
}
