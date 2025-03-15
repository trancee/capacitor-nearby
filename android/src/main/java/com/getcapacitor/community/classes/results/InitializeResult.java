package com.getcapacitor.community.classes.results;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.interfaces.Result;

public class InitializeResult implements Result {

    @NonNull
    private final EndpointID endpointID;

    public InitializeResult(@NonNull EndpointID endpointID) {
        this.endpointID = endpointID;
    }

    @Override
    public JSObject toJSObject() {
        JSObject result = new JSObject();

        result.put("endpointID", endpointID);

        return result;
    }
}
