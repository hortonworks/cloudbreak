package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.UnableToDeleteClusterDefinitionException;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

@ExtendWith(SpringExtension.class)
class EnvironmentResourceDeletionServiceTest {

    private static final Long WORKSPACE_ID = 0L;

    private static final String ENVIRONMENT_CRN = "someEnvCrn";

    @MockBean
    private SdxEndpoint sdxEndpoint;

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

    @Inject
    private EnvironmentResourceDeletionService environmentResourceDeletionServiceUnderTest;

    private Environment environment;

    @BeforeEach
    void setup() {
        environment = new Environment();
        environment.setId(1L);
        environment.setCreator(CRN);
        environment.setName(ENVIRONMENT_NAME);
        environment.setResourceCrn(ENVIRONMENT_CRN);
    }

    @Test
    void getAttachedSdxClusterNames() {
        environmentResourceDeletionServiceUnderTest.getAttachedSdxClusterCrns(environment);
        verify(sdxEndpoint).list(eq(ENVIRONMENT_NAME));
    }

    @Test
    void getDatalakeClusterNames() {
        when(datalakeEndpoint.list(isNull(), anyString())).thenReturn(new StackViewV4Responses());
        environmentResourceDeletionServiceUnderTest.getDatalakeClusterNames(environment);
        verify(datalakeEndpoint).list(isNull(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void getAttachedDistroXClusterNames() {
        when(distroXEndpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses());
        environmentResourceDeletionServiceUnderTest.getAttachedDistroXClusterNames(environment);
        verify(distroXEndpoint).list(isNull(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsOnCloudbreakThrowsWebApplicationExceptionThenItShouldBeCatchedAndEnvironmentServiceExceptionShouldBeThrown() {
        WebApplicationException exception = Mockito.mock(WebApplicationException.class);
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setName("name");
        when(clusterTemplateViewV4Responses.getResponses()).thenReturn(Set.of(templateViewV4Response));
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);
        doThrow(exception).when(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
        Response response = Mockito.mock(Response.class);
        when(exception.getResponse()).thenReturn(response);
        when(response.readEntity(any(Class.class))).thenReturn("error");

        Assertions.assertThrows(EnvironmentServiceException.class,
                () -> environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN));

        verify(clusterTemplateV4Endpoint).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsOnCloudbreakThrowsProcessingExceptionThenItShouldBeCatchedAndEnvironmentServiceExceptionShouldBeThrown() {
        doThrow(ProcessingException.class).when(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setName("name");
        when(clusterTemplateViewV4Responses.getResponses()).thenReturn(Set.of(templateViewV4Response));
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);

        Assertions.assertThrows(EnvironmentServiceException.class,
                () -> environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN));

        verify(clusterTemplateV4Endpoint).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsThrowsUnableToDeleteClusterDefinitionExceptionThenItShouldBeCatchedAndEnvironmentServiceExceptionShouldBeThrown() {
        doThrow(UnableToDeleteClusterDefinitionException.class).when(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(),
                eq(ENVIRONMENT_CRN));
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setName("name");
        when(clusterTemplateViewV4Responses.getResponses()).thenReturn(Set.of(templateViewV4Response));
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);

        Assertions.assertThrows(EnvironmentServiceException.class,
                () -> environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN));

        verify(clusterTemplateV4Endpoint).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Test
    void testWhenDeleteClusterDefinitionsWhenNamesEmpty() {
        when(clusterTemplateViewV4Responses.getResponses()).thenReturn(Collections.emptySet());
        when(clusterTemplateV4Endpoint.listByEnv(anyLong(), anyString())).thenReturn(clusterTemplateViewV4Responses);

        environmentResourceDeletionServiceUnderTest.deleteClusterDefinitionsOnCloudbreak(ENVIRONMENT_CRN);

        verify(clusterTemplateV4Endpoint, never()).deleteMultiple(anyLong(), any(), any(), anyString());
        verify(clusterTemplateV4Endpoint, never()).deleteMultiple(eq(WORKSPACE_ID), any(), any(), eq(ENVIRONMENT_CRN));
    }

    @Configuration
    @Import(EnvironmentResourceDeletionService.class)
    static class Config {
    }

}
