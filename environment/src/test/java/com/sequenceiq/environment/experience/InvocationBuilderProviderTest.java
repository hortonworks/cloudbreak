package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@ExtendWith(MockitoExtension.class)
class InvocationBuilderProviderTest {

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Invocation.Builder mockBuilder;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private InvocationBuilderProvider underTest;

    @BeforeEach
    void setUp() {
        when(mockWebTarget.request()).thenReturn(mockBuilder);
        when(mockBuilder.accept(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.header(anyString(), any())).thenReturn(mockBuilder);
        when(mockBuilder.header(anyString(), anyString())).thenReturn(mockBuilder);
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldAcceptRequestCall() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockWebTarget, times(1)).request();
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetApplicationJsonAsAcceptedFormat() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).accept(anyString());
        verify(mockBuilder, times(1)).accept(APPLICATION_JSON);
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetCrnHeader() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(ACTOR_CRN_HEADER), any());
        verify(mockBuilder, times(1)).header(ACTOR_CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    void testCreateInvocationBuilderWebTargetShouldSetRequestIdHeader() {
        underTest.createInvocationBuilder(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(REQUEST_ID_HEADER), anyString());
    }

    @Test
    void testCreateInvocationBuilderForInternalActorShouldAcceptRequestCall() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.createInvocationBuilderForInternalActor(mockWebTarget);

        verify(mockWebTarget, times(1)).request();
    }

    @Test
    void testCreateInvocationBuilderForInternalActorShouldSetApplicationJsonAsAcceptedFormat() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.createInvocationBuilderForInternalActor(mockWebTarget);

        verify(mockBuilder, times(1)).accept(anyString());
        verify(mockBuilder, times(1)).accept(APPLICATION_JSON);
    }

    @Test
    void testCreateInvocationBuilderForInternalActorShouldSetCrnHeader() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.createInvocationBuilderForInternalActor(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(ACTOR_CRN_HEADER), any());
        verify(mockBuilder, times(1)).header(ACTOR_CRN_HEADER, "crn");
    }

    @Test
    void testCreateInvocationBuilderForInternalActorShouldSetRequestIdHeader() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.createInvocationBuilderForInternalActor(mockWebTarget);

        verify(mockBuilder, times(1)).header(eq(REQUEST_ID_HEADER), anyString());
    }

}
