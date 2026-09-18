package com.sequenceiq.maintenance.dispatcher;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

class MaintenanceSubmitterOutboundRequestBuilderTest {

    @Test
    void prepareJsonPostAddsInternalActorCrnWhenEnabled() {
        RegionAwareInternalCrnGeneratorFactory factory = mock(RegionAwareInternalCrnGeneratorFactory.class);
        RegionAwareInternalCrnGenerator iam = mock(RegionAwareInternalCrnGenerator.class);
        when(factory.iam()).thenReturn(iam);
        when(iam.getInternalCrnForServiceAsString()).thenReturn("crn:altus:iam:us-west-1:altus:user:maintenance");
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(builder.accept(APPLICATION_JSON)).thenReturn(builder);
        when(builder.header(ACTOR_CRN_HEADER, "crn:altus:iam:us-west-1:altus:user:maintenance")).thenReturn(builder);
        when(builder.header(eq(REQUEST_ID_HEADER), any())).thenReturn(builder);

        MaintenanceSubmitterOutboundRequestBuilder underTest = new MaintenanceSubmitterOutboundRequestBuilder(factory, true);
        underTest.prepareJsonPost(webTarget);

        verify(builder).header(ACTOR_CRN_HEADER, "crn:altus:iam:us-west-1:altus:user:maintenance");
    }

    @Test
    void prepareJsonPostSkipsActorCrnWhenInternalAuthDisabled() {
        RegionAwareInternalCrnGeneratorFactory factory = mock(RegionAwareInternalCrnGeneratorFactory.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(builder.accept(APPLICATION_JSON)).thenReturn(builder);
        when(builder.header(eq(REQUEST_ID_HEADER), any())).thenReturn(builder);

        MaintenanceSubmitterOutboundRequestBuilder underTest = new MaintenanceSubmitterOutboundRequestBuilder(factory, false);
        Invocation.Builder result = underTest.prepareJsonPost(webTarget);

        assertThat(result).isSameAs(builder);
        verify(builder, never()).header(eq(ACTOR_CRN_HEADER), any());
    }
}
