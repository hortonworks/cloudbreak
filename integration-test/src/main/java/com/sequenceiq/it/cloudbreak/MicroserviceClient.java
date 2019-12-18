package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class MicroserviceClient extends Entity {

    private CloudbreakUser acting;

    protected  MicroserviceClient(String newId) {
        super(newId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + acting + "]";
    }

    public void setActing(CloudbreakUser acting) {
        this.acting = acting;
    }

    public CloudbreakUser getActing() {
        return acting;
    }
}
