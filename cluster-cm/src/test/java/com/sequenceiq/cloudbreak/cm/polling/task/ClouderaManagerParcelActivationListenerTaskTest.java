package com.sequenceiq.cloudbreak.cm.polling.task;

import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDH;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDSW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerParcelActivationListenerTaskTest {

    private static final Integer COMMAND_ID = 100;

    private static final String STACK_NAME = "stack_name";

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
        stack.setCluster(TestUtil.clusterComponents(cluster));
        stack.setName(STACK_NAME);
    }

    @Test
    void checkStatusOneActivating() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClientMock)).thenReturn(parcelsResourcesApi);
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = TestUtil.apiParcel(CDH, ACTIVATED);
        ApiParcel apiParcel2 = TestUtil.apiParcel(CDSW, ACTIVATING);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerCommandPollerObject));
    }

    @Test
    void checkStatusBothActivating() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = TestUtil.apiParcel(CDH, ACTIVATING);
        ApiParcel apiParcel2 = TestUtil.apiParcel(CDSW, ACTIVATING);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerCommandPollerObject));
    }

    @Test
    void checkStatusMissing() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = TestUtil.apiParcel(CDH, ACTIVATED);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertFalse(underTest.checkStatus(clouderaManagerCommandPollerObject));
    }

    @Test
    void checkStatusActivated() throws ApiException {
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(eq(apiClientMock))).thenReturn(parcelsResourcesApi);
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ApiParcel apiParcel1 = TestUtil.apiParcel(CDH, ACTIVATED);
        ApiParcel apiParcel2 = TestUtil.apiParcel(CDSW, ACTIVATED);
        ApiParcelList apiParcelList = new ApiParcelList().items(List.of(apiParcel1, apiParcel2));
        when(parcelsResourcesApi.readParcels(eq(STACK_NAME), eq(SUMMARY))).thenReturn(apiParcelList);
        assertTrue(underTest.checkStatus(clouderaManagerCommandPollerObject));
    }

    @Test
    void checkStatusJsonParseException() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                new Json("{"),
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerCommandPollerObject));
        assertEquals("Cannot deserialize the component: class com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct",
                cloudbreakServiceException.getMessage());
        verify(parcelsResourcesApi, never()).readParcels(eq(STACK_NAME), eq(SUMMARY));
    }

    @Test
    void checkStatusNullJson() throws ApiException {
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClientMock, COMMAND_ID);

        ClusterComponent cdhComponent = new ClusterComponent(
                ComponentType.CDH_PRODUCT_DETAILS,
                null,
                cluster);
        cluster.setComponents(Set.of(cdhComponent));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.checkStatus(clouderaManagerCommandPollerObject));
        assertEquals("Cluster component attribute json cannot be null.",
                cloudbreakServiceException.getMessage());
        verify(parcelsResourcesApi, never()).readParcels(eq(STACK_NAME), eq(SUMMARY));
    }
}
