package com.sequenceiq.remoteenvironment.scheduled.archiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.remotecluster.client.GrpcRemoteClusterClient;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@ExtendWith(MockitoExtension.class)
class PrivateControlPlaneQueryJobTest {
    @Mock
    private GrpcRemoteClusterClient grpcRemoteClusterClient;

    @Mock
    private PrivateControlPlaneService privateControlPlaneService;

    @InjectMocks
    private PrivateControlPlaneQueryJob privateControlPlaneQueryJob;

    @Test
    public void testQueryPrivateControlPlaneConfigs() throws JobExecutionException {
        List<PvcControlPlaneConfiguration> remoteControlPlanes = new ArrayList<>();
        remoteControlPlanes.add(getPvcControlPlaneConfiguration("CRN1", "NAME1", "URL1"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration("CRN2", "NAME2", "URL22"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration("CRN3", "NAME3", "URL3"));
        remoteControlPlanes.add(getPvcControlPlaneConfiguration("CRN4", "NAME4", "URL4"));

        List<PrivateControlPlane> controlPlanesInOurDatabase = new ArrayList<>();
        controlPlanesInOurDatabase.add(getPrivateControlPlane("CRN1", "NAME1", "URL1"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane("CRN2", "NAME2", "URL2"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane("CRN3", "NAME3", "URL3"));
        controlPlanesInOurDatabase.add(getPrivateControlPlane("CRN5", "NAME5", "URL5"));

        when(grpcRemoteClusterClient.listAllPrivateControlPlanes()).thenReturn(remoteControlPlanes);
        when(privateControlPlaneService.findAll()).thenReturn(controlPlanesInOurDatabase);
        when(privateControlPlaneService.pureSave(any())).thenReturn(new PrivateControlPlane());
        doNothing().when(privateControlPlaneService).deleteByResourceCrns(any());


        privateControlPlaneQueryJob.queryPrivateControlPlaneConfigs();

        verify(privateControlPlaneService, times(1)).deleteByResourceCrns(any());
        verify(privateControlPlaneService, times(1)).register(any());
        verify(privateControlPlaneService, times(1)).pureSave(any());
    }

    private static PrivateControlPlane getPrivateControlPlane(String crn, String name, String url) {
        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setUrl(url);
        privateControlPlane.setName(name);
        privateControlPlane.setResourceCrn(crn);
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