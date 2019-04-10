package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerDeployClientConfigListenerTaskTest {

    private static final BigDecimal COMMAND_ID = new BigDecimal(100);

    private static final String CDH_ATTRIBUTES = "{\"name\":\"CDH\",\"version\":\"6.1.1-1.cdh6.1.1.p0.875250\","
            + "\"parcel\":\"https://archive.cloudera.com/cdh6/6.1.1/parcels/\"}";

    private static final String CM_ATTRIBUTES = "{\"predefined\":false,\"version\":\"6.1.1\","
            + "\"baseUrl\":\"https://archive.cloudera.com/cm6/6.1.1/redhat7/yum/\","
            + "\"gpgKeyUrl\":\"https://archive.cloudera.com/cm6/6.1.1/redhat7/yum/RPM-GPG-KEY-cloudera\"}";

    private static final String STACK_NAME = "stack_name";

    @InjectMocks
    private ClouderaManagerDeployClientConfigListenerTask underTest;

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private ApiClient apiClientMock;

    @Mock
    private ParcelResourceApi parcelResourceApi;

    @Spy
    private Stack stack = new Stack();

    @Spy
    private Cluster cluster = new Cluster();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        Json cdhAttributes = new Json(CDH_ATTRIBUTES);
        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                cdhAttributes,
                cluster);

        Json cmAttributes = new Json(CM_ATTRIBUTES);
        ClusterComponent cmComponent = new ClusterComponent(
                ComponentType.CM_REPO_DETAILS,
                cmAttributes,
                cluster);
        cluster.setComponents(Set.of(cdhComponent, cmComponent));
        stack.setCluster(cluster);
        stack.setName(STACK_NAME);

        when(clouderaManagerClientFactory.getParcelResourceApi(eq(apiClientMock))).thenReturn(parcelResourceApi);
    }

    @Test
    void checkStatusActivating() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel = new ApiParcel().stage("ACTIVATING");
        when(parcelResourceApi.readParcel(eq(STACK_NAME), eq("CDH"), eq("6.1.1-1.cdh6.1.1.p0.875250"))).thenReturn(apiParcel);
        assertFalse(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusException() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        when(parcelResourceApi.readParcel(eq(STACK_NAME), eq("CDH"), eq("6.1.1-1.cdh6.1.1.p0.875250")))
                .thenThrow(new ApiException("Error"));
        assertFalse(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatus() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel = new ApiParcel().stage("ACTIVATED");
        when(parcelResourceApi.readParcel(eq(STACK_NAME), eq("CDH"), eq("6.1.1-1.cdh6.1.1.p0.875250"))).thenReturn(apiParcel);
        assertTrue(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusJsonParseException() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                new Json("{"),
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerPollerObject));
        assertEquals("Cannot deserialize the compnent: class com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct",
                cloudbreakServiceException.getMessage());
        verify(parcelResourceApi, never()).readParcel(eq(STACK_NAME), eq("CDH"), eq("6.1.1-1.cdh6.1.1.p0.875250"));
    }

    @Test
    void checkStatusNullJson() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                null,
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerPollerObject));
        assertEquals("Cluster component attribute json cannot be null.",
                cloudbreakServiceException.getMessage());
        verify(parcelResourceApi, never()).readParcel(eq(STACK_NAME), eq("CDH"), eq("6.1.1-1.cdh6.1.1.p0.875250"));
    }
}