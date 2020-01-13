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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerParcelActivationListenerTaskTest {

    private static final BigDecimal COMMAND_ID = new BigDecimal(100);

    private static final String STACK_NAME = "stack_name";

    private static final String CDH = "CDH";

    private static final String CDH_VERSION = "7.0.0-1.cdh7.0.0.p0.1376867";

    private static final String CDSW = "CDSW";

    private static final String CDSW_VERSION = "2.0.0.p1.1410896";

    private static final String PARCEL_TEMPLATE = "{\"name\":\"%s\",\"version\":\"%s\","
            + "\"parcel\":\"https://archive.cloudera.com/cdh7/7.0.0/parcels/\"}";

    private static final String CDH_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDH, CDH_VERSION);

    private static final String CDSW_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDSW, CDSW_VERSION);

    private static final String CM_ATTRIBUTES = "{\"predefined\":false,\"version\":\"7.0.0\","
            + "\"baseUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/\","
            + "\"gpgKeyUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/RPM-GPG-KEY-cloudera\"}";

    private static final String ACTIVATED = "ACTIVATED";

    private static final String ACTIVATING = "ACTIVATING";

    private static final String SUMMARY = "summary";

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ApiClient apiClientMock;

    @Mock
    private ParcelsResourceApi parcelsResourcesApi;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    private ClouderaManagerParcelActivationListenerTask underTest;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerParcelActivationListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService);
        stack = new Stack();
        cluster = new Cluster();

        ClusterComponent cdhComponent = createClusterComponent(CDH_ATTRIBUTES, ComponentType.CDH_PRODUCT_DETAILS);
        ClusterComponent cdswComponent = createClusterComponent(CDSW_ATTRIBUTES, ComponentType.CDH_PRODUCT_DETAILS);
        ClusterComponent cmComponent = createClusterComponent(CM_ATTRIBUTES, ComponentType.CM_REPO_DETAILS);

        cluster.setComponents(Set.of(cdhComponent, cdswComponent, cmComponent));
        stack.setCluster(cluster);
        stack.setName(STACK_NAME);
    }

    private ClusterComponent createClusterComponent(String attributeString, ComponentType componentType) {
        Json attributes = new Json(attributeString);
        return new ClusterComponent(componentType, attributes, cluster);
    }

    @Test
    void checkStatusOneActivating() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClientMock)).thenReturn(parcelsResourcesApi);
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = new ApiParcel().product(CDH).version(CDH_VERSION).stage(ACTIVATED);
        ApiParcel apiParcel2 = new ApiParcel().product(CDSW).version(CDSW_VERSION).stage(ACTIVATING);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusBothActivating() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = new ApiParcel().product(CDH).version(CDH_VERSION).stage(ACTIVATING);
        ApiParcel apiParcel2 = new ApiParcel().product(CDSW).version(CDSW_VERSION).stage(ACTIVATING);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusMissing() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = new ApiParcel().product(CDH).version(CDH_VERSION).stage(ACTIVATED);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusActivated() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = new ApiParcel().product(CDH).version(CDH_VERSION).stage(ACTIVATED);
        ApiParcel apiParcel2 = new ApiParcel().product(CDSW).version(CDSW_VERSION).stage(ACTIVATED);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertTrue(underTest.checkStatus(clouderaManagerPollerObject));
    }

    @Test
    void checkStatusJsonParseException() throws ApiException {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                new Json("{"),
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerPollerObject));
        assertEquals("Cannot deserialize the component: class com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct",
                cloudbreakServiceException.getMessage());
        verify(parcelsResourcesApi, never()).readParcels(eq(STACK_NAME), eq(SUMMARY));
    }

    @Test
    void checkStatusNullJson() throws ApiException {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                null,
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerPollerObject));
        assertEquals("Cluster component attribute json cannot be null.",
                cloudbreakServiceException.getMessage());
        verify(parcelsResourcesApi, never()).readParcels(eq(STACK_NAME), eq(SUMMARY));
    }
}
