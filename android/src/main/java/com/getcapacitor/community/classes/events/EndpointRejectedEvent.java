package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointRejectedEvent extends EndpointEvent {

    public EndpointRejectedEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
