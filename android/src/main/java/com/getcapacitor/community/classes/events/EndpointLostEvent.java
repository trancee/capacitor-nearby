package com.getcapacitor.community.classes.events;

import androidx.annotation.NonNull;
import com.getcapacitor.community.classes.Endpoint;

public class EndpointLostEvent extends EndpointEvent {

    public EndpointLostEvent(@NonNull Endpoint endpoint) {
        super(endpoint);
    }
}
