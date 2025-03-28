package com.getcapacitor.community.classes.options;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.getcapacitor.JSArray;
import com.getcapacitor.PluginCall;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

public class SendPayloadOptions {

    @Nullable
    private List<String> endpointIDs;

    @Nullable
    private byte[] payload;

    public SendPayloadOptions(PluginCall call) throws JSONException {
        List<String> endpointIDs = new ArrayList<>();

        @Nullable
        String endpointID = call.getString("endpointID");
        if (endpointID != null && !endpointID.isEmpty()) {
            endpointIDs.add(endpointID);
        }

        @Nullable
        JSArray endpointArray = call.getArray("endpointIDs");
        if (endpointArray != null) {
            for (var item : endpointArray.toList()) {
                if (endpointID != null && !endpointID.isEmpty()) {
                    endpointIDs.add((String) item);
                }
            }
        }

        this.setEndpointIDs(endpointIDs);

        @Nullable
        String payload = call.getString("payload");
        this.setPayload(payload);
    }

    public void setEndpointIDs(@Nullable List<String> endpointIDs) {
        this.endpointIDs = endpointIDs;
    }

    public void setPayload(@Nullable byte[] payload) {
        this.payload = payload;
    }

    public void setPayload(@Nullable String payload) {
        this.payload = (payload == null) ? null : Base64.decode(payload, Base64.NO_WRAP);
    }

    @Nullable
    public List<String> getEndpointIDs() {
        return endpointIDs;
    }

    @Nullable
    public byte[] getPayload() {
        return payload;
    }
}
