package com.sequenceiq.remoteenvironment.scheduled;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;
import com.sequenceiq.remoteenvironment.service.PrivateEnvironmentBaseClusterService;
import com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane.PrivateControlPlaneRemoteEnvironmentConnector;

@ExtendWith(MockitoExtension.class)
class PrivateEnvironmentBaseClusterRegistrarJobTest {

    @Mock
    private PrivateControlPlaneRemoteEnvironmentConnector remoteEnvironmentService;

    @Mock
    private PrivateControlPlaneService privateControlPlaneService;

    @Mock
    private PrivateEnvironmentBaseClusterService privateEnvironmentBaseClusterService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private PrivateEnvironmentBaseClusterRegistrarJob underTest;

    @Test
    void testQueryAndRegisterNoControlPlanesIsAvailable() throws JobExecutionException {
        when(privateControlPlaneService.findAll()).thenReturn(List.of());

        underTest.executeTracedJob(jobExecutionContext);

        verify(privateControlPlaneService).findAll();
        verifyNoInteractions(remoteEnvironmentService);
        verifyNoInteractions(privateEnvironmentBaseClusterService);
    }

    @Test
    void testQueryAndRegisterNoEnvironmentIsAvailableWithinControlPlanes() throws JobExecutionException {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlaneService.findAll()).thenReturn(List.of(privateControlPlane));

        underTest.executeTracedJob(jobExecutionContext);

        verify(privateControlPlaneService).findAll();
        verify(remoteEnvironmentService).listRemoteEnvironmentsInternal(privateControlPlane);
        verify(remoteEnvironmentService, times(0)).describeRemoteEnvironmentInternal(any(), any());
        verifyNoInteractions(privateEnvironmentBaseClusterService);
    }

    @Test
    void testQueryAndRegisterGetEnvironmentDetailsThrowsExceptionWhenControlPlanesAndEnvironmentListReturned() {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlaneService.findAll()).thenReturn(List.of(privateControlPlane));
        SimpleRemoteEnvironmentResponse environmentResponse = mock(SimpleRemoteEnvironmentResponse.class);
        when(remoteEnvironmentService.listRemoteEnvironmentsInternal(privateControlPlane)).thenReturn(List.of(environmentResponse));
        when(remoteEnvironmentService.describeRemoteEnvironmentInternal(any(), any())).thenThrow(new RuntimeException("something went wrong"));

        JobExecutionException jobExecutionException = Assertions.assertThrows(JobExecutionException.class,
                () -> underTest.executeTracedJob(jobExecutionContext));

        Assertions.assertEquals("Could not query and update private control planes.", jobExecutionException.getMessage());
        verify(privateControlPlaneService).findAll();
        verify(remoteEnvironmentService).listRemoteEnvironmentsInternal(privateControlPlane);
        verify(remoteEnvironmentService).describeRemoteEnvironmentInternal(any(), any());
        verifyNoInteractions(privateEnvironmentBaseClusterService);
    }

    @Test
    void testQueryAndRegisterBaseClustersOfAllEnvironmentsOfAllControlPlanes() throws JobExecutionException {
        PrivateControlPlane privateControlPlane = mockPrivateControlPlane("cp1");
        PrivateControlPlane privateControlPlane2 = mockPrivateControlPlane("cp2");
        when(privateControlPlaneService.findAll()).thenReturn(List.of(privateControlPlane, privateControlPlane2));

        underTest.executeTracedJob(jobExecutionContext);

        verify(privateControlPlaneService).findAll();
        verify(remoteEnvironmentService).listRemoteEnvironmentsInternal(privateControlPlane);
        verify(remoteEnvironmentService).listRemoteEnvironmentsInternal(privateControlPlane2);
        verify(remoteEnvironmentService, times(4)).describeRemoteEnvironmentInternal(any(), any());
        verify(privateEnvironmentBaseClusterService, times(4)).registerBaseCluster(any(), any(), any());
    }

    private PrivateControlPlane mockPrivateControlPlane(String controlPlaneName) {
        String controlPlaneCrn = "crn_" + controlPlaneName;
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getName()).thenReturn(controlPlaneName);
        when(privateControlPlane.getResourceCrn()).thenReturn(controlPlaneCrn);
        SimpleRemoteEnvironmentResponse environmentResponse = mockSimpleEnvironmentResponse(controlPlaneName, "_env1");
        SimpleRemoteEnvironmentResponse environmentResponse2 = mockSimpleEnvironmentResponse(controlPlaneName, "_env2");
        when(remoteEnvironmentService.listRemoteEnvironmentsInternal(privateControlPlane))
                .thenReturn(List.of(environmentResponse, environmentResponse2));
        DescribeEnvironmentResponse envResponse1 = mock(DescribeEnvironmentResponse.class);
        when(remoteEnvironmentService.describeRemoteEnvironmentInternal(privateControlPlane, "crn_" + controlPlaneName + "_env1")).thenReturn(envResponse1);
        DescribeEnvironmentResponse envResponse2 = mock(DescribeEnvironmentResponse.class);
        when(remoteEnvironmentService.describeRemoteEnvironmentInternal(privateControlPlane, "crn_" + controlPlaneName + "_env2")).thenReturn(envResponse2);
        when(privateEnvironmentBaseClusterService.registerBaseCluster(envResponse1, controlPlaneCrn, controlPlaneName)).thenReturn("privCrn1");
        when(privateEnvironmentBaseClusterService.registerBaseCluster(envResponse2, controlPlaneCrn, controlPlaneName)).thenReturn("");
        return privateControlPlane;
    }

    private SimpleRemoteEnvironmentResponse mockSimpleEnvironmentResponse(String controlPlaneName, String envNameSuffix) {
        SimpleRemoteEnvironmentResponse environmentResponse = mock(SimpleRemoteEnvironmentResponse.class);
        when(environmentResponse.getCrn()).thenReturn("crn_" + controlPlaneName + envNameSuffix);
        return environmentResponse;
    }
}