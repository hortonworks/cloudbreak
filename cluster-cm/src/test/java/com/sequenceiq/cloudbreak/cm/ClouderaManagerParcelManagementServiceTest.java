package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.AVAILABLE_REMOTELY;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.DISTRIBUTED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.DOWNLOADED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.UNKNOWN;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDH;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.FLINK;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.SPARK3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerParcelManagementServiceTest {

    private static final String STACK_NAME = "stack-name";

    private static final String VIEW = "summary";

    private static Stack stack;

    @InjectMocks
    private ClouderaManagerParcelManagementService underTest;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private ParcelsResourceApi parcelsResourceApi;

    @Mock
    private ParcelResourceApi parcelResourceApi;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ApiClient apiClient;

    @Mock
    private CloudbreakEventService eventService;

    @BeforeAll
    static void before() {
        stack = createStack();
    }

    private static Stack createStack() {
        Stack stack = new Stack();
        stack.setName("stack-name");
        return stack;
    }

    @Test
    void testGetParcelsInStatusShouldReturnAllParcelsInTheRequiredStatus() throws ApiException {
        when(parcelsResourceApi.readParcels(STACK_NAME, VIEW)).thenReturn(new ApiParcelList()
                .addItemsItem(createApiParcel(CDH, "7.2.7", ACTIVATED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.12", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.15", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(SPARK3, "5.4.1", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(FLINK, "1.2.2", DOWNLOADED.name())));

        Set<ParcelInfo> actual = underTest.getParcelsInStatus(parcelsResourceApi, STACK_NAME, DISTRIBUTED);

        assertEquals(3, actual.size());
        assertParcel(actual, CDH, "7.2.12", DISTRIBUTED);
        assertParcel(actual, CDH, "7.2.15", DISTRIBUTED);
        assertParcel(actual, SPARK3, "5.4.1", DISTRIBUTED);
    }

    @Test
    void testGetParcelsInStatusShouldReturnEmptySetWhenThereAreNoParcelInTheRequiredStatus() throws ApiException {
        when(parcelsResourceApi.readParcels(STACK_NAME, VIEW)).thenReturn(new ApiParcelList()
                .addItemsItem(createApiParcel(CDH, "7.2.7", ACTIVATED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.12", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.15", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(SPARK3, "5.4.1", DISTRIBUTED.name())));

        Set<ParcelInfo> actual = underTest.getParcelsInStatus(parcelsResourceApi, STACK_NAME, DOWNLOADED);

        assertEquals(Collections.emptySet(), actual);
    }

    @Test
    void testGetParcelsInStatusShouldThrowExceptionWhenUnableToReadTheParcelsFromCm() throws ApiException {
        when(parcelsResourceApi.readParcels(STACK_NAME, VIEW)).thenThrow(new ApiException("Failed to read parcels"));
        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.getParcelsInStatus(parcelsResourceApi, STACK_NAME, DOWNLOADED));
    }

    @Test
    void testGetAllParcelsShouldReturnAllParcelsFromCm() throws ApiException {
        when(parcelsResourceApi.readParcels(STACK_NAME, VIEW)).thenReturn(new ApiParcelList()
                .addItemsItem(createApiParcel(CDH, "7.2.7", ACTIVATED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.12", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(CDH, "7.2.15", DISTRIBUTED.name()))
                .addItemsItem(createApiParcel(SPARK3, "5.4.1", AVAILABLE_REMOTELY.name()))
                .addItemsItem(createApiParcel(FLINK, "1.2.2", DOWNLOADED.name())));

        Set<ParcelInfo> actual = underTest.getAllParcels(parcelsResourceApi, STACK_NAME);

        assertEquals(5, actual.size());
        assertParcel(actual, CDH, "7.2.7", ACTIVATED);
        assertParcel(actual, CDH, "7.2.12", DISTRIBUTED);
        assertParcel(actual, CDH, "7.2.15", DISTRIBUTED);
        assertParcel(actual, SPARK3, "5.4.1", AVAILABLE_REMOTELY);
        assertParcel(actual, FLINK, "1.2.2", DOWNLOADED);
    }

    @Test
    void testGetAllParcelsShouldReturnUnknownWhenUnknownStatus() throws ApiException {
        when(parcelsResourceApi.readParcels(STACK_NAME, VIEW)).thenReturn(new ApiParcelList()
                .addItemsItem(createApiParcel(CDH, "7.2.7", "anUnknownStatus")));

        Set<ParcelInfo> actual = underTest.getAllParcels(parcelsResourceApi, STACK_NAME);

        assertEquals(1, actual.size());
        assertParcel(actual, CDH, "7.2.7", UNKNOWN);
    }

    @Test
    void testSetParcelReposShouldSetTheRequiredParcelBaseUrlsInCm() throws ApiException {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withParcel("https://cdh.parcel");
        ClouderaManagerProduct flinkProduct = new ClouderaManagerProduct().withParcel("https://flink.parcel");
        ArgumentCaptor<ApiConfigList> apiConfigListCaptor = ArgumentCaptor.forClass(ApiConfigList.class);
        when(clouderaManagerResourceApi.updateConfig(apiConfigListCaptor.capture(), eq("Updated configurations."))).thenReturn(new ApiConfigList());

        underTest.setParcelRepos(Set.of(cdhProduct, flinkProduct), clouderaManagerResourceApi);

        ApiConfigList apiConfigList = apiConfigListCaptor.getValue();
        assertTrue(apiConfigList.getItems().stream().anyMatch(apiConfig ->
                apiConfig.getName().equals("remote_parcel_repo_urls") &&
                        apiConfig.getValue().contains(cdhProduct.getParcel()) &&
                        apiConfig.getValue().contains(flinkProduct.getParcel())));
    }

    @Test
    void testRefreshParcelReposShouldCallCmApi() throws ApiException {
        ApiCommand apiCommand = new ApiCommand().id(BigDecimal.ONE);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(apiCommand);

        underTest.refreshParcelRepos(clouderaManagerResourceApi, stack, apiClient);

        verify(clouderaManagerResourceApi).refreshParcelRepos();
        verify(clouderaManagerPollingServiceProvider).startPollingCmParcelRepositoryRefresh(stack, apiClient, apiCommand.getId());
    }

    @Test
    void testDownloadParcelsShouldCallCmApiToDownloadParcels() throws CloudbreakException, ApiException {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName(CDH).withVersion("7.2.15").withParcel("http://parcel");
        ClouderaManagerProduct flinkProduct = new ClouderaManagerProduct().withName(FLINK).withVersion("1.2.3")
                .withParcel("https://archive.cloudera.com/");
        ApiCommand cdhDownloadCommand = new ApiCommand().id(BigDecimal.ONE);
        ApiCommand flinkDownloadCommand = new ApiCommand().id(BigDecimal.TEN);
        ExtendedPollingResult pollingResult = createPollingResult();
        when(parcelResourceApi.startDownloadCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion())).thenReturn(cdhDownloadCommand);
        when(parcelResourceApi.startDownloadCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion())).thenReturn(flinkDownloadCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDownload(eq(stack), eq(apiClient), eq(cdhDownloadCommand.getId()),
                any(ParcelResource.class))).thenReturn(pollingResult);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDownload(eq(stack), eq(apiClient), eq(flinkDownloadCommand.getId()),
                any(ParcelResource.class))).thenReturn(pollingResult);
        when(parcelsResourceApi.readParcels(stack.getName(), "summary")).thenReturn(
                new ApiParcelList().items(List.of(createApiParcel(CDH, "7.2.15", AVAILABLE_REMOTELY), createApiParcel(FLINK, "1.2.3", AVAILABLE_REMOTELY))));

        underTest.downloadParcels(Set.of(cdhProduct, flinkProduct), parcelResourceApi, parcelsResourceApi, stack, apiClient);

        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_PARCEL, List.of(CDH));
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_PARCEL, List.of(FLINK));
        verify(parcelResourceApi).startDownloadCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion());
        verify(parcelResourceApi).startDownloadCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeParcelDownload(eq(stack), eq(apiClient), eq(cdhDownloadCommand.getId()),
                any(ParcelResource.class));
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeParcelDownload(eq(stack), eq(apiClient), eq(flinkDownloadCommand.getId()),
                any(ParcelResource.class));
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for CDP Runtime Parcel to be downloaded",
                "The upgrade process encountered issues due to the degradation of build-cache.vpc.cloudera.com or the build cache, "
                        + "resulting in the inability to download the parcels within 120 minutes. "
                        + "If this issue persists, please contact our Release Engineering team for assistance.");
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for CDP Runtime Parcel to be downloaded",
                "The upgrade process encountered issues due to the degradation of archive.cloudera.com or the build cache, "
                        + "resulting in the inability to download the parcels within 120 minutes. "
                        + "If this issue persists, please contact our Support team for assistance.");
    }

    @Test
    void testDistributeParcelsShouldCallCmApiToDistributeParcels() throws CloudbreakException, ApiException {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName(CDH).withVersion("7.2.15");
        ClouderaManagerProduct flinkProduct = new ClouderaManagerProduct().withName(FLINK).withVersion("1.2.3");
        ApiCommand cdhDistributeCommand = new ApiCommand().id(BigDecimal.ONE);
        ApiCommand flinkDistributeCommand = new ApiCommand().id(BigDecimal.TEN);
        ExtendedPollingResult pollingResult = createPollingResult();
        when(parcelResourceApi.startDistributionCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion())).thenReturn(cdhDistributeCommand);
        when(parcelResourceApi.startDistributionCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion())).thenReturn(flinkDistributeCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(cdhDistributeCommand.getId()),
                any(ParcelResource.class))).thenReturn(pollingResult);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(flinkDistributeCommand.getId()),
                any(ParcelResource.class))).thenReturn(pollingResult);
        when(parcelsResourceApi.readParcels(stack.getName(), "summary")).thenReturn(
                new ApiParcelList().items(List.of(createApiParcel(CDH, "7.2.15", DOWNLOADED), createApiParcel(FLINK, "1.2.3", DOWNLOADED))));

        underTest.distributeParcels(Set.of(cdhProduct, flinkProduct), parcelResourceApi, parcelsResourceApi, stack, apiClient);

        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL, List.of(CDH));
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL, List.of(FLINK));
        verify(parcelResourceApi).startDistributionCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion());
        verify(parcelResourceApi).startDistributionCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(cdhDistributeCommand.getId()),
                any(ParcelResource.class));
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(flinkDistributeCommand.getId()),
                any(ParcelResource.class));
        verify(pollingResultErrorHandler, times(2)).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for CDP Runtime Parcel to be distributed", "Timeout during the updated CDP Runtime Parcel distribution.");
    }

    @Test
    void testDistributeParcelsShouldDistributeOnlyCdhParcelWhenTheFlinkIsAlreadyActivated() throws CloudbreakException, ApiException {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName(CDH).withVersion("7.2.15");
        ClouderaManagerProduct flinkProduct = new ClouderaManagerProduct().withName(FLINK).withVersion("1.2.3");
        ApiCommand cdhDistributeCommand = new ApiCommand().id(BigDecimal.ONE);
        ApiCommand flinkDistributeCommand = new ApiCommand().id(BigDecimal.TEN);
        ExtendedPollingResult pollingResult = createPollingResult();
        when(parcelResourceApi.startDistributionCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion())).thenReturn(cdhDistributeCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(cdhDistributeCommand.getId()),
                any(ParcelResource.class))).thenReturn(pollingResult);
        when(parcelsResourceApi.readParcels(stack.getName(), "summary"))
                .thenReturn(new ApiParcelList().items(List.of(createApiParcel(CDH, "7.2.15", DOWNLOADED), createApiParcel(FLINK, "1.2.3", ACTIVATED))));

        underTest.distributeParcels(Set.of(cdhProduct, flinkProduct), parcelResourceApi, parcelsResourceApi, stack, apiClient);

        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL, List.of(CDH));
        verify(eventService, times(0))
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL, List.of(FLINK));
        verify(parcelResourceApi).startDistributionCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion());
        verify(parcelResourceApi, times(0)).startDistributionCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(cdhDistributeCommand.getId()),
                any(ParcelResource.class));
        verify(clouderaManagerPollingServiceProvider, times(0))
                .startPollingCdpRuntimeParcelDistribute(eq(stack), eq(apiClient), eq(flinkDistributeCommand.getId()), any(ParcelResource.class));
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for CDP Runtime Parcel to be distributed", "Timeout during the updated CDP Runtime Parcel distribution.");
    }

    @Test
    void testActivateParcelsShouldCallCmApiToActivateParcels() throws CloudbreakException, ApiException {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName(CDH).withVersion("7.2.15");
        ClouderaManagerProduct flinkProduct = new ClouderaManagerProduct().withName(FLINK).withVersion("1.2.3");
        ApiCommand cdhActivationCommand = new ApiCommand().id(BigDecimal.ONE);
        ApiCommand flinkActivationCommand = new ApiCommand().id(BigDecimal.TEN);
        ExtendedPollingResult pollingResult = createPollingResult();
        when(parcelResourceApi.activateCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion())).thenReturn(cdhActivationCommand);
        when(parcelResourceApi.activateCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion())).thenReturn(flinkActivationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmSingleParcelActivation(stack, apiClient, cdhActivationCommand.getId(), cdhProduct))
                .thenReturn(pollingResult);
        when(clouderaManagerPollingServiceProvider.startPollingCmSingleParcelActivation(stack, apiClient, flinkActivationCommand.getId(), flinkProduct))
                .thenReturn(pollingResult);
        when(parcelsResourceApi.readParcels(stack.getName(), "summary")).thenReturn(
                new ApiParcelList().items(List.of(createApiParcel(CDH, "7.2.15", DISTRIBUTED), createApiParcel(FLINK, "1.2.3", DISTRIBUTED))));

        underTest.activateParcels(Set.of(cdhProduct, flinkProduct), parcelResourceApi, parcelsResourceApi, stack, apiClient);

        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_ACTIVATE_PARCEL, List.of(CDH));
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_ACTIVATE_PARCEL, List.of(FLINK));
        verify(parcelResourceApi).activateCommand(stack.getName(), cdhProduct.getName(), cdhProduct.getVersion());
        verify(parcelResourceApi).activateCommand(stack.getName(), flinkProduct.getName(), flinkProduct.getVersion());
        verify(clouderaManagerPollingServiceProvider).startPollingCmSingleParcelActivation(stack, apiClient, cdhActivationCommand.getId(), cdhProduct);
        verify(clouderaManagerPollingServiceProvider).startPollingCmSingleParcelActivation(stack, apiClient, flinkActivationCommand.getId(), flinkProduct);
        verify(pollingResultErrorHandler, times(2)).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for CDP Runtime Parcel to be activated", "Timeout during the updated CDP Runtime Parcel activation.");
    }

    private ApiParcel createApiParcel(String name, String version, ParcelStatus parcelStatus) {
        return new ApiParcel().product(name).version(version).stage(parcelStatus.name());
    }

    @Test
    void testCheckParcelApiAvailabilityShouldPollCmParcelApi() throws CloudbreakException {
        ExtendedPollingResult pollingResult = createPollingResult();
        when(clouderaManagerPollingServiceProvider.startPollingParcelsApiAvailable(stack, apiClient)).thenReturn(pollingResult);

        underTest.checkParcelApiAvailability(stack, apiClient);

        verify(clouderaManagerPollingServiceProvider).startPollingParcelsApiAvailable(stack, apiClient);
        verify(pollingResultErrorHandler).handlePollingResult(pollingResult.getPollingResult(),
                "Cluster was terminated while waiting for Parcels API to be available", "Timeout during waiting for CM Parcels API to be available.");
    }

    private ExtendedPollingResult createPollingResult() {
        return new ExtendedPollingResult.ExtendedPollingResultBuilder().withPollingResult(PollingResult.SUCCESS).build();
    }

    private void assertParcel(Set<ParcelInfo> actual, String name, String version, ParcelStatus status) {
        assertTrue(actual.stream().anyMatch(parcelInfo ->
                parcelInfo.getName().equals(name)
                        && parcelInfo.getVersion().equals(version)
                        && parcelInfo.getStatus().equals(status)));
    }

    private ApiParcel createApiParcel(String name, String version, String status) {
        ApiParcel apiParcel = new ApiParcel();
        apiParcel.setProduct(name);
        apiParcel.setVersion(version);
        apiParcel.setStage(status);
        return apiParcel;
    }
}