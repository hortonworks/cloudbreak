package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class ActorCrn extends ActorBase {

    private final String actorCrn;

    public ActorCrn(Builder builder) {
        this.actorCrn = builder.actorCrn;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ActorCrn{" +
                "actorCrn='" + actorCrn + '\'' +
                '}';
    }

    public static class Builder {

        private String actorCrn;

        public Builder withActorCrn(String actorCrn) {
            this.actorCrn = actorCrn;
            return this;
        }

        public ActorCrn build() {
            checkArgument(Crn.isCrn(actorCrn), "Actor user must be a valid CRN");
            return new ActorCrn(this);
        }
    }
}
