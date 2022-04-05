package com.sequenceiq.cloudbreak.audit.util;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Component
public class ActorUtil {

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public String getActorCrn(ActorBase actorBase) {
        String actorCrn;
        if (actorBase instanceof ActorCrn) {
            ActorCrn actor = (ActorCrn) actorBase;
            actorCrn = actor.getActorCrn();
        } else if (actorBase instanceof ActorService) {
            actorCrn = regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
        } else {
            throw new IllegalArgumentException("Actor has an invalid class: " + actorBase.getClass().getName());
        }
        return actorCrn;
    }
}
