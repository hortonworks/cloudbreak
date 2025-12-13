package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class StackRuntimeVersionValidatorTest {

    private static final String DATA_HUB_VERSION = "7.2.0";

    private static final String ENVIRONMENT_CRN = "environment-crn";

    private static final String UUID = java.util.UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + UUID + ":user:" + UUID;

    private static final String SDX_VERSION = "7.2.0";

    @InjectMocks
    private StackRuntimeVersionValidator underTest;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Test
    void testValidationWhenEntitlementOn() {
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new StackV4Request(), mock(Image.class), StackType.WORKLOAD));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verifyNoInteractions(sdxClientService);
    }

    @Test
    void testValidationWhenDataHubVersionIsNotPresent() {
        StackV4Request request = createStackRequestWithoutCm();
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(request, mock(Image.class), StackType.WORKLOAD));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verifyNoInteractions(sdxClientService);
    }

    @Test
    void testValidationWhenImageContainsVersionAndVersionsAreEquals() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequestWithoutCm(), createImage(DATA_HUB_VERSION), StackType.WORKLOAD));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenRequestContainsVersionAndVersionsAreEquals() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenImageContainsVersionAndVersionsAreNotEquals() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validate(createStackRequestWithoutCm(), createImage("7.2.2"), StackType.WORKLOAD)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenStackContainsVersionAndVersionsAreNotEquals() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse());

        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest("7.2.2"), mock(Image.class), StackType.WORKLOAD)));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenDataLakeVersionIsNotPresent() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse(null, SdxClusterStatusResponse.RUNNING));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD));

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenDataLakeIsNotRunning() {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(
                createSdxClusterResponse(null, SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS));

        assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                        () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD)),
                "Datalake myDatalake is not available yet, thus we cannot check runtime version!");

        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenCbHasDlStackButCdhProductMissing() throws IllegalAccessException {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDatalakeStack(Status.AVAILABLE)));
        when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.empty());
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse(null, SdxClusterStatusResponse.RUNNING));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD));

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
        verify(runtimeVersionService).getRuntimeVersion(anyLong());
        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenCbHasDlStack() throws IllegalAccessException {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDatalakeStack(Status.AVAILABLE)));
        when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.of(DATA_HUB_VERSION));
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD));

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
        verify(runtimeVersionService).getRuntimeVersion(anyLong());
        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService, never()).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    void testValidationWhenCbHasDlStackButNotRunning() throws IllegalAccessException {
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDatalakeStack(Status.CREATE_IN_PROGRESS)));
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(any())).thenReturn(false);
        when(sdxClientService.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(createSdxClusterResponse(null, SdxClusterStatusResponse.RUNNING));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(createStackRequest(DATA_HUB_VERSION), mock(Image.class), StackType.WORKLOAD));

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
        verify(runtimeVersionService, never()).getRuntimeVersion(anyLong());
        verify(entitlementService).isDifferentDataHubAndDataLakeVersionAllowed(any());
        verify(sdxClientService).getByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    private StackView createDatalakeStack(Status status) throws IllegalAccessException {
        StackView stack = new StackView();
        stack.setType(StackType.DATALAKE);
        ClusterView cluster = new ClusterView();
        cluster.setId(1L);
        FieldUtils.writeField(stack, "environmentCrn", ENVIRONMENT_CRN, true);
        stack.setName("dlStack");
        FieldUtils.writeField(stack, "cluster", cluster, true);
        StackStatusView stackStatusView = new StackStatusView();
        stackStatusView.setStatus(status);
        FieldUtils.writeField(stack, "stackStatus", stackStatusView, true);
        return stack;
    }

    private ClouderaManagerProduct getCdhProduct(String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName("CDH");
        product.setVersion(version + "-something");
        return product;
    }

    private StackV4Request createStackRequest(String dataHubVersion) {
        ClouderaManagerRepositoryV4Request clouderaManagerRepositoryV4Request = new ClouderaManagerRepositoryV4Request();
        clouderaManagerRepositoryV4Request.setVersion("do-not-use-this-version");
        ClouderaManagerV4Request clouderaManagerV4Request = new ClouderaManagerV4Request();
        clouderaManagerV4Request.setProducts(List.of(new ClouderaManagerProductV4Request()
                .withName("CDH")
                .withVersion(dataHubVersion)));
        clouderaManagerV4Request.setRepository(clouderaManagerRepositoryV4Request);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setCm(clouderaManagerV4Request);
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(clusterV4Request);
        stackV4Request.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stackV4Request;
    }

    private Image createImage(String dataHubVersion) {
        ImageStackDetails stackDetails = new ImageStackDetails(dataHubVersion, null, null);
        return Image.builder()
                .withStackDetails(stackDetails)
                .build();
    }

    private StackV4Request createStackRequestWithoutCm() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(clusterV4Request);
        stackV4Request.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stackV4Request;
    }

    private List<SdxClusterResponse> createSdxClusterResponse(String version, SdxClusterStatusResponse status) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setRuntime(version);
        sdxClusterResponse.setStatus(status);
        sdxClusterResponse.setName("myDatalake");
        return Collections.singletonList(sdxClusterResponse);
    }

    private List<SdxClusterResponse> createSdxClusterResponse() {
        return createSdxClusterResponse(SDX_VERSION, SdxClusterStatusResponse.RUNNING);
    }

}