package com.sequenceiq.remoteenvironment.scheduled.archiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.scheduled.PrivateControlPlaneQueryJob;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@ExtendWith(MockitoExtension.class)
class PrivateControlPlaneQueryJobTest {
    @Mock
    private RemoteClusterServiceClient grpcRemoteClusterClient;

    @Mock
    private PrivateControlPlaneService privateControlPlaneService;

    @InjectMocks
    private PrivateControlPlaneQueryJob privateControlPlaneQueryJob;

    @Test
    public void testQueryPrivateControlPlaneConfigs() throws JobExecutionException {
        List<PvcControlPlaneConfiguration> remoteControlPlanes = new ArrayList<>();
        String invalidCrn = "crn:cdp:hybrid:us-west-1:73d:pvcInvalidControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad4";
        remoteControlPlanes.add(getPvcControlPlaneConfiguration(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad1",
                "NAME1", "URL1"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad2",
                "NAME2", "URL22"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad3",
                "NAME3", "URL3"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad4",
                "NAME4", "URL4"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration(
                invalidCrn,
                "NAME5", "URL5"));

        List<PrivateControlPlane> controlPlanesInOurDatabase = new ArrayList<>();
        controlPlanesInOurDatabase.add(getPrivateControlPlane(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad1",
                "NAME1", "URL1"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad2",
                "NAME2", "URL2"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad3",
                "NAME3", "URL3"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane(
                "crn:cdp:hybrid:us-west-1:73d:pvcControlPlane:b24e0aff-2cc5-40d0-b5fa-89bd85152ad5",
                "NAME5", "URL5"));

        when(grpcRemoteClusterClient.listAllPrivateControlPlanes()).thenReturn(remoteControlPlanes);
        when(privateControlPlaneService.findAll()).thenReturn(controlPlanesInOurDatabase);
        when(privateControlPlaneService.pureSave(any())).thenReturn(new PrivateControlPlane());
        doNothing().when(privateControlPlaneService).deleteByResourceCrns(any());


        privateControlPlaneQueryJob.queryPrivateControlPlaneConfigs();

        ArgumentCaptor<PrivateControlPlaneRegistrationRequests> requestCaptor = ArgumentCaptor.forClass(PrivateControlPlaneRegistrationRequests.class);
        verify(privateControlPlaneService, times(1)).register(requestCaptor.capture());
        verify(privateControlPlaneService, times(1)).deleteByResourceCrns(any());
        verify(privateControlPlaneService, times(1)).pureSave(any());

        Assertions.assertThat(requestCaptor)
                .isNotNull()
                .matches(rc -> rc.getValue().getItems().stream()
                        .noneMatch(pvcRequest -> invalidCrn.equals(pvcRequest.getCrn())), "should not contain invalid CRN")
                .matches(rc -> rc.getValue().getItems().size() == 1);
    }

    private static PrivateControlPlane getPrivateControlPlane(String crn, String name, String url) {
        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setUrl(url);
        privateControlPlane.setName(name);
        privateControlPlane.setResourceCrn(crn);
        privateControlPlane.setPrivateCloudAccountId("account");
        return privateControlPlane;
    }

    private PvcControlPlaneConfiguration getPvcControlPlaneConfiguration(String crn, String name, String url) {
        return PvcControlPlaneConfiguration.newBuilder()
                .setName(name)
                .setPvcCrn(crn)
                .setBaseUrl(url)
                .build();
    }
}