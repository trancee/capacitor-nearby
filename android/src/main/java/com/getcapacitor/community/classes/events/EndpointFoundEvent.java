package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointFoundEvent extends EndpointEvent {

    public EndpointFoundEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
