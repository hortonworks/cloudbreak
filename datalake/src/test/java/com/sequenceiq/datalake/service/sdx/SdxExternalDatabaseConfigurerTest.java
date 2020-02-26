package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class SdxExternalDatabaseConfigurerTest {

    @Mock
    private PlatformConfig platformConfig;

    @InjectMocks
    private SdxExternalDatabaseConfigurer underTest;

    @Test
    public void whenPlatformIsAwsWithDefaultsShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxCluster sdxCluster = new SdxCluster();

        underTest.configure(cloudPlatform, null, sdxCluster);

        assertEquals(true, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAwsWithSkipCreateShouldNotCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setCreate(false);
        SdxCluster sdxCluster = new SdxCluster();

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(false, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAwsAndCreateShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setCreate(true);
        SdxCluster sdxCluster = new SdxCluster();

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(true, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSet() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(true, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSetAndNoDbRequested() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setCreate(false);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(false, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAzureWithNotSupportedRuntime() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.0.2");

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(false, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAzureWithMinSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.1.0");

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(true, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsAzureWithNewerSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");

        underTest.configure(cloudPlatform, dbRequest, sdxCluster);

        assertEquals(true, sdxCluster.isCreateDatabase());
    }

    @Test
    public void whenPlatformIsYarnShouldNotAllowDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.YARN;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(false);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(false);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setCreate(true);
        SdxCluster sdxCluster = new SdxCluster();

        Assertions.assertThrows(BadRequestException.class, () -> underTest.configure(cloudPlatform, dbRequest, sdxCluster));

        assertEquals(true, sdxCluster.isCreateDatabase());
    }
}