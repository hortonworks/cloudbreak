package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackInstancesV4Responses;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;

@ExtendWith(MockitoExtension.class)
public class DatahubServiceTest {
    private static final String INTERNAL_ACTOR_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Mock
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @InjectMocks
    private DatahubService underTest;

    @Test
    public void testWhenWeGetBackInfoFromCoreModulThenResponseShouldNotBeNull() {
        String datahubCrn = "crn";
        StackInstancesV4Responses stackInstancesV4Responses = new StackInstancesV4Responses();

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR_CRN);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.distroXInternalV1Endpoint()).thenReturn(distroXInternalV1Endpoint);
        when(distroXInternalV1Endpoint.getInstancesByCrn(any())).thenReturn(stackInstancesV4Responses);

        StackInstancesV4Responses instancesByCrn = underTest.getInstancesByCrn(datahubCrn);
        assertNotNull(instancesByCrn);
    }

    @Test
    public void testWhenWeGetBackErrorFromCoreModulThenResponseShouldNotBeNull() {
        String datahubCrn = "crn";
        WebApplicationException webApplicationException = new WebApplicationException("test");

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR_CRN);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.distroXInternalV1Endpoint()).thenReturn(distroXInternalV1Endpoint);
        when(webApplicationExceptionHandler.handleException(any())).thenReturn(webApplicationException);
        when(distroXInternalV1Endpoint.getInstancesByCrn(any())).thenThrow(new WebApplicationException("test"));

        WebApplicationException result =
                Assertions.assertThrows(WebApplicationException.class, () -> underTest.getInstancesByCrn(datahubCrn));
        assertEquals(result, webApplicationException);
    }

}