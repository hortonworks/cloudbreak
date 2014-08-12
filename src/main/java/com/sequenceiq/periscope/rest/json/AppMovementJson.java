package com.sequenceiq.periscope.rest.json;

public class AppMovementJson implements Json {

    private boolean allowed;

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
