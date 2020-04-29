package com.sequenceiq.cloudbreak.audit.util;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;

@Component
public class ActorUtil {

    public String getActorCrn(ActorBase actorBase) {
        String actorCrn;
        if (actorBase instanceof ActorCrn) {
            ActorCrn actor = (ActorCrn) actorBase;
            actorCrn = actor.getActorCrn();
        } else if (actorBase instanceof ActorService) {
            actorCrn = INTERNAL_ACTOR_CRN;
        } else {
            throw new IllegalArgumentException("Actor has an invalid class: " + actorBase.getClass().getName());
        }
        return actorCrn;
    }
}
