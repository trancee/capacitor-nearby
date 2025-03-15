package com.getcapacitor.community.classes.results;

import com.getcapacitor.JSObject;
import com.getcapacitor.community.interfaces.Result;

public class StatusResult implements Result {

    private final boolean isAdvertising;
    private final boolean isDiscovering;

    public StatusResult(boolean isAdvertising, boolean isDiscovering) {
        this.isAdvertising = isAdvertising;
        this.isDiscovering = isDiscovering;
    }

    @Override
    public JSObject toJSObject() {
        JSObject result = new JSObject();

        result.put("isAdvertising", isAdvertising);
        result.put("isDiscovering", isDiscovering);

        return result;
    }
}
