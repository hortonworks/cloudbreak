package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(SpringExtension.class)
class EnvironmentResourceDeletionServiceTest {

    private static final Long WORKSPACE_ID = 0L;

    private static final String ENVIRONMENT_CRN = "someEnvCrn";

    @MockBean
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @MockBean
    private ThreadBasedUserCrnProvider userCrnProvider;

    @MockBean
    private EventSender eventSender;

    @MockBean
    private DistroXV1Endpoint distroXEndpoint;

    @MockBean
    private DatalakeV4Endpoint datalakeEndpoint;

    @MockBean
    private ClusterTemplateV4Endpoint clusterTemplateV4Endpoint;

    @MockBean
    private ExperienceConnectorService experienceConnectorService;

    @Mock
    private ClusterTemplateViewV4Responses clusterTemplateViewV4Responses;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @MockBean
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Inject
    private EnvironmentResourceDeletionService environmentResourceDeletionServiceUnderTest;

    private EnvironmentView environment;

    @BeforeEach
    void setup() {
        environment = new EnvironmentView();
        environment.setId(1L);
        environment.setName(ENVIRONMENT_NAME);
        environment.setResourceCrn(ENVIRONMENT_CRN);
    }

    @Test
    void getAttachedSdxClusterNames() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        environmentResourceDeletionServiceUnderTest.getAttachedSdxClusterCrns(environment);
        verify(platformAwareSdxConnector).listSdxCrns(eq(ENVIRONMENT_CRN));
    }

    @Test
    void getDatalakeClusterNames() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(datalakeEndpoint.list(isNull(), anyString())).thenReturn(new StackViewV4Responses());
        environmentResourceDeletionServiceUnderTest.getDatalakeClusterNames(environment);
        verify(datalakeEndpoint).list(isNull(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void getAttachedDistroXClusterNames() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(distroXEndpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses());
        environmentResourceDeletionServiceUnderTest.getAttachedDistroXClusterNames(environment);
        verify(distroXEndpoint).list(isNull(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsOnCloudbreakThrowsWebApplicationExceptionThenItShouldBeCatchedAndEnvironmentServiceExceptionShouldBeThrown() {
        WebApplicationException exception = mock(WebApplicationException.class);

        ClusterTemplateViewV4Response templateViewV4ResponseUserManaged = new ClusterTemplateViewV4Response();
        templateViewV4ResponseUserManaged.setName("name");
        templateViewV4ResponseUserManaged.setStatus(ResourceStatus.USER_MANAGED);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(clusterTemplateViewV4Responses.getResponses())
                .thenReturn(Set.of(templateViewV4ResponseUserManaged));
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);
        doThrow(exception).when(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
        Response response = mock(Response.class);
        when(exception.getResponse()).thenReturn(response);
        when(response.readEntity(any(Class.class))).thenReturn("error");

        assertThrows(EnvironmentServiceException.class,
                () -> environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN));

        verify(clusterTemplateV4Endpoint).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsOnCloudbreakThrowsProcessingExceptionThenItShouldBeCatchedAndEnvironmentServiceExceptionShouldBeThrown() {
        doThrow(ProcessingException.class).when(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setName("name");
        templateViewV4Response.setStatus(ResourceStatus.USER_MANAGED);
        when(clusterTemplateViewV4Responses.getResponses()).thenReturn(Set.of(templateViewV4Response));
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThrows(EnvironmentServiceException.class,
                () -> environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN));

        verify(clusterTemplateV4Endpoint, times(1)).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint, times(1)).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Configuration
    @Import(EnvironmentResourceDeletionService.class)
    static class Config {
    }

}
