package com.sequenceiq.maintenance.dispatcher;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

/**
 * Builds outbound Jersey {@link Invocation.Builder} instances for submitter dispatch HTTP.
 * <p>
 * Same internal-actor headers as {@code com.sequenceiq.environment.experience.InvocationBuilderProvider
 * #createInvocationBuilderForInternalActor} (not shared across modules); differs only by
 * {@code maintenance.dispatcher.submitter.internal-auth-enabled}.
 */
@Component
public class MaintenanceSubmitterOutboundRequestBuilder {

    private final RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory;

    private final boolean internalAuthEnabled;

    @Inject
    public MaintenanceSubmitterOutboundRequestBuilder(
            RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory,
            @Value("${maintenance.dispatcher.submitter.internal-auth-enabled:true}") boolean internalAuthEnabled) {
        this.internalCrnGeneratorFactory = internalCrnGeneratorFactory;
        this.internalAuthEnabled = internalAuthEnabled;
    }

    public Invocation.Builder prepareJsonPost(WebTarget webTarget) {
        Invocation.Builder builder = webTarget.request().accept(APPLICATION_JSON);
        if (internalAuthEnabled) {
            builder.header(ACTOR_CRN_HEADER, internalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
        }
        builder.header(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
        return builder;
    }
}
