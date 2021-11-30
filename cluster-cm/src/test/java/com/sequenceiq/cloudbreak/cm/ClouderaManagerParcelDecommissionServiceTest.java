package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerParcelDecommissionServiceTest {

    private static final String STACK_NAME = "stackname";

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
    void testDeactivateUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        // WHEN
        ParcelOperationStatus actual = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedComponents, productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).deactivateCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).deactivateCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).deactivateCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, actual.getSuccessful().size());
        assertEquals(0, actual.getFailed().size());
        assertEquals("version3", actual.getSuccessful().get("product3"));
    }

    @Test
    public void testDeactivateUnusedComponentsWhenDeactivationFailsOnParcel() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> activatedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(activatedParcels, ParcelStatus.ACTIVATED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        when(parcelResourceApi.deactivateCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        // WHEN and THEN
        ParcelOperationStatus operationStatus = underTest.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedComponents,
                productsFromImage);

        verify(parcelResourceApi, times(1)).deactivateCommand(STACK_NAME, "product3", "version3");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
    }

    @Test
    public void testUndistributeUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getSuccessful().get("product3"));
    }

    @Test
    public void testUndistributeUnusedComponentsAndUndistributionFails() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DISTRIBUTED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        when(parcelResourceApi.startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        // WHEN
        ParcelOperationStatus operationStatus = underTest.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).startRemovalOfDistributionCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).startRemovalOfDistributionCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
    }

    @Test
    public void testRemoveUnusedComponents() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(0, operationStatus.getFailed().size());
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals("version3", operationStatus.getSuccessful().get("product3"));
    }

    @Test
    public void testRemoveUnusedComponentsWhenRemovalFails() throws Exception {
        // GIVEN
        Set<String> productsFromImage = Set.of("product1", "product2", "product3");
        Set<String> usedComponents = Set.of("product1", "product2");
        Map<String, String> distributedParcels = Map.of("product1", "version1", "product3", "version3", "customParcel", "customParcelVersion");
        ApiParcelList parcelList = createApiParcelList(distributedParcels, ParcelStatus.DOWNLOADED);
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(parcelResourceApi.removeDownloadCommand(STACK_NAME, "product3", "version3")).thenThrow(new ApiException());
        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedComponents,
                productsFromImage);
        // THEN
        verify(parcelResourceApi, times(1)).removeDownloadCommand(STACK_NAME, "product3", "version3");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product1", "version1");
        verify(parcelResourceApi, times(0)).removeDownloadCommand(STACK_NAME, "product2", "version2");
        verifyNoMoreInteractions(parcelResourceApi);
        assertEquals(1, operationStatus.getFailed().size());
        assertEquals(0, operationStatus.getSuccessful().size());
        assertEquals("version3", operationStatus.getFailed().get("product3"));
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
