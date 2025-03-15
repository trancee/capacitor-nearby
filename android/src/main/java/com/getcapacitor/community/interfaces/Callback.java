package com.getcapacitor.community.interfaces;

import androidx.annotation.NonNull;
import com.getcapacitor.PluginCall;

public abstract class Callback {

    static final String UNKNOWN_ERROR = "unknown error";

    final PluginCall call;

    protected Callback(PluginCall call) {
        this.call = call;
    }

    public void success() {
        call.resolve();
    }

    public void success(Void unused) {
        call.resolve();
    }

    public void success(@NonNull Result result) {
        call.resolve(result.toJSObject());
    }

    public void error(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = UNKNOWN_ERROR;
        }

        call.reject(message, exception);
    }
}
