package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerParcelDecommissionServiceTest {
    @InjectMocks
    private ClouderaManagerParcelDecommissionService underTest;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ParcelResourceApi parcelResourceApi;

    @Mock
    private ParcelsResourceApi parcelsResourceApi;

    @Mock
    private ApiClient apiClient;

    @Test
    public void testDeactivateUnusedComponents() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        // WHEN
        ParcelOperationStatus operationStatus = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, "stackname", usedComponents);
        // THEN
        verify(parcelResourceApi, times(1)).deactivateCommand("stackname", "product3", "version3");
        verify(parcelResourceApi, times(0)).deactivateCommand("stackname", "product1", "version1");
        verify(parcelResourceApi, times(0)).deactivateCommand("stackname", "product2", "version2");
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getSuccessful().get("product3"));
    }

    @Test
    public void testDeactivateUnusedComponentsWhenDeactivationFailsOnParcel() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        when(parcelResourceApi.deactivateCommand("stackname", "product3", "version3")).thenThrow(new ApiException());
        // WHEN and THEN
        ParcelOperationStatus operationStatus = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, "stackname", usedComponents);
        verify(parcelResourceApi, times(1)).deactivateCommand("stackname", "product3", "version3");
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
    }

    @Test
    public void testUndistributeUnusedComponents() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stackname");
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand("stackname", "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand("stackname", "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand("stackname", "product2", "version2");
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getSuccessful().get("product3"));
    }

    @Test
    public void testUndistributeUnusedComponentsAndUndistributionFails() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        when(parcelResourceApi.startRemovalOfDistributionCommand("stackname", "product3", "version3")).thenThrow(new ApiException());
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stackname");
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand("stackname", "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand("stackname", "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand("stackname", "product2", "version2");
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
    }

    @Test
    public void testRemoveUnusedComponents() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stackname");
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand("stackname", "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand("stackname", "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand("stackname", "product2", "version2");
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals("version3", operationStatus.getSuccessful().get("product3"));
    }

    @Test
    public void testRemoveUnusedComponentsWhenRemovalFails() throws Exception {
        // GIVEN
        Map<String, ClouderaManagerProduct> usedComponents = new HashMap<>();
        usedComponents.put("product1", createClouderaManagerProduct("product1", "version1"));
        usedComponents.put("prodcut2", createClouderaManagerProduct("product2", "version2"));
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels("stackname", "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stackname");
        when(parcelResourceApi.removeDownloadCommand("stackname", "product3", "version3")).thenThrow(new ApiException());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand("stackname", "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand("stackname", "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand("stackname", "product2", "version2");
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
    }

    private ClusterComponent createClusterComponent(ClouderaManagerProduct clouderaManagerProduct) {
        ClusterComponent component = new ClusterComponent();
        Json attribute = mock(Json.class);
        when(attribute.getSilent(ClouderaManagerProduct.class)).thenReturn(clouderaManagerProduct);
        component.setAttributes(attribute);
        return component;
    }

    private ClouderaManagerProduct createClouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(name);
        product.setVersion(version);
        product.setDisplayName(name);
        return product;
    }

    private ApiParcelList createApiParcelList(Map<String, String> products, ParcelStatus parcelStatus) {
        ApiParcelList parcelList = new ApiParcelList();
        List<ApiParcel> apiParcels = products.entrySet().stream().map(entry -> createApiParcel(entry.getKey(), entry.getValue(), parcelStatus))
                .collect(Collectors.toList());
        parcelList.setItems(apiParcels);
        return parcelList;
    }

    private ApiParcel createApiParcel(String product, String version, ParcelStatus parcelStatus) {
        ApiParcel parcel = new ApiParcel();
        parcel.setProduct(product);
        parcel.setVersion(version);
        parcel.setStage(parcelStatus.name());
        return parcel;
    }
}
