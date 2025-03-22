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

    public void success(@NonNull Result result) {
        call.resolve(result.toJSObject());
    }

    public void error(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = UNKNOWN_ERROR;
        }
        Throwable cause;
        if ((cause = exception.getCause()) != null) {
            message = cause.getMessage() + ": " + message;
        }

        call.reject(message, exception);
    }
}
