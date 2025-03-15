package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointInitiatedEvent extends EndpointEvent {

    public EndpointInitiatedEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
