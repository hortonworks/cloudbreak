package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.logger.MDCContextFilter.REQUEST_ID_HEADER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class InvocationBuilderProvider {

    public Invocation.Builder createInvocationBuilder(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(CRN_HEADER, "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:4898cf22-7c43-418b-90d5-1b12a542150e")
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
    }

    public Invocation.Builder createInvocationBuilderForInternalActor(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(CRN_HEADER, ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN)
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
    }

}
