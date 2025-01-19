package com.getcapacitor.community.classes.results;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.community.interfaces.Result;

import org.json.JSONException;

public class StatusResult implements Result {

    private final boolean isPublishing;
    private final boolean isSubscribing;

    @Nullable
    private final String[] uuids;

    public StatusResult(
            boolean isPublishing,
            boolean isSubscribing,

            @Nullable String[] uuids
    ) {
        this.isPublishing = isPublishing;
        this.isSubscribing = isSubscribing;

        this.uuids = uuids;
    }

    @NonNull
    public JSObject toJSObject() {
        JSObject result = new JSObject();

        result.put("isPublishing", isPublishing);
        result.put("isSubscribing", isSubscribing);

        try {
            result.put("uuids", new JSArray(uuids));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
