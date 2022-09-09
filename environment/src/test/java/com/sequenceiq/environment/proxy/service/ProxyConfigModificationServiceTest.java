package com.sequenceiq.environment.proxy.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class ProxyConfigModificationServiceTest {

    private static final String ACCOUNT_ID = "account-id";

    private static final String ENV_CRN = "env-crn";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

    @InjectMocks
    private ProxyConfigModificationService underTest;

    @Mock
    private Environment environment;

    @Mock
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        lenient().when(environment.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(environment.getResourceCrn()).thenReturn(ENV_CRN);

        lenient().when(entitlementService.isEditProxyConfigEnabled(ACCOUNT_ID)).thenReturn(true);
        setFreeipaStatus(Status.AVAILABLE);
        setSdxStatus(SdxClusterStatusResponse.RUNNING);
        setDistroxStatuses(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);
    }

    @Test
    void modifyValidateWithoutEntitlement() {
        when(entitlementService.isEditProxyConfigEnabled(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> underTest.modify(environment, proxyConfig))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Proxy config editing is not enabled in your account");
    }

    @Test
    void modifyValidateWithNonAvailableFreeipa() {
        setFreeipaStatus(Status.STOPPED);

        assertThatThrownBy(() -> underTest.modify(environment, proxyConfig))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Proxy config editing is not supported when FreeIpa is not available");
    }

    @Test
    void modifyValidateWithNonRunningSdx() {
        setSdxStatus(SdxClusterStatusResponse.STOPPED);

        assertThatThrownBy(() -> underTest.modify(environment, proxyConfig))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Proxy config editing is not supported when Data Lake is not running");
    }

    @Test
    void modifyValidateWithNonAvailableDistrox() {
        setDistroxStatuses(
                com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE,
                com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED);

        assertThatThrownBy(() -> underTest.modify(environment, proxyConfig))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Proxy config editing is not supported when not all Data Hubs are available");
    }

    @Test
    void modifyValidateSuccess() {
        assertThatThrownBy(() -> underTest.modify(environment, proxyConfig))
                .isInstanceOf(NotImplementedException.class)
                .hasMessage("Editing the proxy configuration is not supported yet");
    }

    private void setFreeipaStatus(Status status) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setStatus(status);
        lenient().when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
    }

    private void setSdxStatus(SdxClusterStatusResponse status) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setStatus(status);
        lenient().when(sdxService.list(ENV_CRN)).thenReturn(List.of(sdxClusterResponse));
    }

    private void setDistroxStatuses(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status... statuses) {
        List<StackViewV4Response> stackViewV4ResponseList = Arrays.stream(statuses)
                .map(status -> {
                    StackViewV4Response stackViewV4Response = new StackViewV4Response();
                    stackViewV4Response.setStatus(status);
                    return stackViewV4Response;
                })
                .collect(Collectors.toList());
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        stackViewV4Responses.setResponses(stackViewV4ResponseList);

        lenient().when(datahubService.list(ENV_CRN)).thenReturn(stackViewV4Responses);
    }

}