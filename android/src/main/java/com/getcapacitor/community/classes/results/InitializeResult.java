package com.getcapacitor.community.classes.results;

import static com.getcapacitor.community.NearbyHelper.EndpointID;

import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.interfaces.Result;

public class InitializeResult implements Result {

    @NonNull
    final String endpointID;

    public InitializeResult(@NonNull String endpointID) {
        this.endpointID = endpointID;
    }

    @Override
    public JSObject toJSObject() {
        JSObject result = new JSObject();

        result.put("endpointID", endpointID);

        return result;
    }
}
