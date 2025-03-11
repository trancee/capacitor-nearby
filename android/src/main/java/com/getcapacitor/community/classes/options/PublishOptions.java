package com.getcapacitor.community.classes.options;

import androidx.annotation.NonNull;

public class PublishOptions {

    @NonNull
    private String name;

    public PublishOptions(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }
}
