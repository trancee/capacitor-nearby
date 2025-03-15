package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointDisconnectedEvent extends EndpointEvent {

    public EndpointDisconnectedEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
