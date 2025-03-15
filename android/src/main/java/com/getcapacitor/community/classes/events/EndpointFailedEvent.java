package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointFailedEvent extends EndpointEvent {

    public EndpointFailedEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
