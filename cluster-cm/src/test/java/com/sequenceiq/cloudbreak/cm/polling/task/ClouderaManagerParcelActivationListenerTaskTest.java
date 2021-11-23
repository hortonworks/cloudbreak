package com.sequenceiq.cloudbreak.cm.polling.task;

import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDH;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDH_VERSION;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDSW;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDSW_VERSION;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerParcelActivationListenerTaskTest {

    private static final BigDecimal COMMAND_ID = new BigDecimal(100);

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
    private ClusterEventService clusterEventService;

    private ClouderaManagerParcelActivationListenerTask underTest;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerParcelActivationListenerTask(clouderaManagerApiPojoFactory, clusterEventService, createProducts());
        stack = new Stack();
        cluster = new Cluster();
        stack.setCluster(TestUtil.clusterComponents(cluster));
        stack.setName(STACK_NAME);
    }

    private List<ClouderaManagerProduct> createProducts() {
        return List.of(createClouderaManagerProduct(CDH, CDH_VERSION), createClouderaManagerProduct(CDSW, CDSW_VERSION));
    }

    private ClouderaManagerProduct createClouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setName(name);
        clouderaManagerProduct.setVersion(version);
        return clouderaManagerProduct;
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
}
