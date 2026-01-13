package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class InvocationBuilderProvider {

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Invocation.Builder createInvocationBuilder(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(ACTOR_CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                .header(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }

    public Invocation.Builder createInvocationBuilderForInternalActor(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(ACTOR_CRN_HEADER, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString())
                .header(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }

}
