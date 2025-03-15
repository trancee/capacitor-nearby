package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointConnectedEvent extends EndpointEvent {

    public EndpointConnectedEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
