package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerUpgradeParcelDownloadListenerTaskTest {

    private ClouderaManagerUpgradeParcelDownloadListenerTask underTest;

    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private ParcelResource parcelResource;

    @Mock
    private ApiClient apiClient;

    @BeforeEach
    public void init() {
        clouderaManagerApiPojoFactory = mock(ClouderaManagerApiPojoFactory.class);
        parcelResource = mock(ParcelResource.class);
        underTest = new ClouderaManagerUpgradeParcelDownloadListenerTask(clouderaManagerApiPojoFactory, null, parcelResource);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsActivating() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.ACTIVATING, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsActivated() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.ACTIVATED, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDistributing() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.DISTRIBUTING, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDistributed() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.DISTRIBUTED, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDownloaded() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.DOWNLOADED, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDownloading() throws ApiException {
        testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus.DOWNLOADING, false);
    }

    private void testDoStatusCheckWithParcelStatusShouldReturnTrue(ParcelStatus parcelStatus, boolean expected) throws ApiException {
        ApiClient apiClient = mock(ApiClient.class);
        ParcelResourceApi parcelResourceApi = mock(ParcelResourceApi.class);
        when(clouderaManagerApiPojoFactory.getParcelResourceApi(apiClient)).thenReturn(parcelResourceApi);
        when(parcelResource.getProduct()).thenReturn("CDH");
        when(parcelResource.getClusterName()).thenReturn("clusterName");
        when(parcelResource.getVersion()).thenReturn("7.2.8");
        ApiParcel apiParcel = mock(ApiParcel.class);
        when(parcelResourceApi.readParcel("clusterName", "CDH", "7.2.8")).thenReturn(apiParcel);
        when(apiParcel.getStage()).thenReturn(parcelStatus.name());
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(null, apiClient, BigDecimal.ONE);
        boolean result = underTest.doStatusCheck(pollerObject, null);

        Assertions.assertEquals(expected, result);
    }

}