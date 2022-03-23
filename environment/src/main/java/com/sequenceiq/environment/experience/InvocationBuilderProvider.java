package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.logger.MDCContextFilter.REQUEST_ID_HEADER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Component
public class InvocationBuilderProvider {

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Invocation.Builder createInvocationBuilder(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
    }

    public Invocation.Builder createInvocationBuilderForInternalActor(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(CRN_HEADER, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString())
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
    }

}
