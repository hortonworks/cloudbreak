package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class ActorService extends ActorBase {

    private final String actorServiceName;

    public ActorService(Builder builder) {
        this.actorServiceName = builder.actorServiceName;
    }

    public String getActorServiceName() {
        return actorServiceName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ActorService{" +
                "actorServiceName='" + actorServiceName + '\'' +
                '}';
    }

    public static class Builder {

        private String actorServiceName;

        public Builder withActorServiceName(String actorServiceName) {
            this.actorServiceName = actorServiceName;
            return this;
        }

        public ActorService build() {
            checkArgument(Crn.Service.fromString(actorServiceName) != null, "Actor service name must be a valid service name as represented in a CRN");
            return new ActorService(this);
        }
    }
}
