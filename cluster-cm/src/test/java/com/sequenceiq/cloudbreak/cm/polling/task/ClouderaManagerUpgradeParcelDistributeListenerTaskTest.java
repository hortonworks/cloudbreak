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

public class ClouderaManagerUpgradeParcelDistributeListenerTaskTest {

    private ClouderaManagerUpgradeParcelDistributeListenerTask underTest;

    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private ParcelResource parcelResource;

    @Mock
    private ApiClient apiClient;

    @BeforeEach
    public void init() {
        clouderaManagerApiPojoFactory = mock(ClouderaManagerApiPojoFactory.class);
        parcelResource = mock(ParcelResource.class);
        underTest = new ClouderaManagerUpgradeParcelDistributeListenerTask(clouderaManagerApiPojoFactory, null, parcelResource);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsActivating() throws ApiException {
        testDoStatusCheckWhenParcelIsInState(ParcelStatus.ACTIVATING, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsActivated() throws ApiException {
        testDoStatusCheckWhenParcelIsInState(ParcelStatus.ACTIVATED, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDistributed() throws ApiException {
        testDoStatusCheckWhenParcelIsInState(ParcelStatus.DISTRIBUTED, true);
    }

    @Test
    public void testDoStatusCheckWhenParcelIsDistributing() throws ApiException {
        testDoStatusCheckWhenParcelIsInState(ParcelStatus.DISTRIBUTING, false);
    }

    private void testDoStatusCheckWhenParcelIsInState(ParcelStatus parcelStatus, boolean expected) throws ApiException {
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
        boolean result = underTest.doStatusCheck(pollerObject);

        Assertions.assertEquals(expected, result);
    }
}