package com.sequenceiq.environment.proxy.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
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
    private FreeIpaService freeIpaService;

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

    @InjectMocks
    private ProxyConfigModificationService underTest;

    @Mock
    private EnvironmentDto environment;

    @Mock
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        lenient().when(environment.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(environment.getResourceCrn()).thenReturn(ENV_CRN);

        setFreeipaStatus(Status.AVAILABLE);
        setSdxStatus(SdxClusterStatusResponse.RUNNING);
        setDistroxStatuses(com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE);
    }

    @Test
    void modifyValidateWithNonAvailableFreeipa() {
        setFreeipaStatus(Status.STOPPED);

        assertThatThrownBy(() -> underTest.validateModify(environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Modifying proxy config is not supported when FreeIpa is not available");
    }

    @Test
    void modifyValidateWithNonRunningSdx() {
        setSdxStatus(SdxClusterStatusResponse.STOPPED);

        assertThatThrownBy(() -> underTest.validateModify(environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Modifying proxy config is not supported when Data Lake is not running");
    }

    @Test
    void modifyValidateWithNonAvailableDistrox() {
        setDistroxStatuses(
                com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE,
                com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED);

        assertThatThrownBy(() -> underTest.validateModify(environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Modifying proxy config is not supported when any of the Data Hubs are not available");
    }

    @Test
    void modifyValidateSuccess() {
        assertThatCode(() -> underTest.validateModify(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotModifyNoProxies() {
        Environment environment = new Environment();
        ProxyConfig newProxyConfig = new ProxyConfig();

        boolean result = underTest.shouldModify(environment, newProxyConfig);

        assertFalse(result);
    }

    @Test
    void shouldNotModifySameProxies() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("name");
        Environment environment = new Environment();
        environment.setProxyConfig(proxyConfig);

        boolean result = underTest.shouldModify(environment, proxyConfig);

        assertFalse(result);
    }

    @Test
    void shouldModifyAddProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("name");
        Environment environment = new Environment();

        boolean result = underTest.shouldModify(environment, proxyConfig);

        assertTrue(result);
    }

    @Test
    void shouldModifyRemoveProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("name");
        Environment environment = new Environment();
        environment.setProxyConfig(proxyConfig);

        boolean result = underTest.shouldModify(environment, new ProxyConfig());

        assertTrue(result);
    }

    @Test
    void shouldModifyChangeProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("name");
        Environment environment = new Environment();
        environment.setProxyConfig(proxyConfig);

        ProxyConfig newProxyConfig = new ProxyConfig();
        proxyConfig.setName("new-name");

        boolean result = underTest.shouldModify(environment, newProxyConfig);

        assertTrue(result);
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
