package com.sequenceiq.freeipa.service.crossrealm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2ActiveDirectoryRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2MitRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2Request;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupEvent;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.ActiveDirectoryTrustService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class TrustSetupServiceTest {

    private static final String ACCOUNT_ID = "acc";

    private static final String ENV_CRN = "env-crn";

    private static final String REMOTE_ENV_CRN = "remote-env-crn";

    private static final String REALM = "realm.com";

    private static final String FQDN = "fqdn";

    private static final String IP = "1.1.1.1";

    private static final String DNS_IP = "8.8.8.8";

    private static final String OPERATION_ID = "operationId";

    private static final String LB_FQDN = "lb.fqdn";

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private TrustSetupService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private ActiveDirectoryTrustService adTrustProvider;

    private Stack stack;

    private FreeIpa freeIpa;

    private CrossRealmTrust crossRealmTrust;

    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        lenient().when(stackService.getFreeIpaStackWithMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);

        freeIpa = new FreeIpa();
        lenient().when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        crossRealmTrust = new CrossRealmTrust();
        lenient().when(crossRealmTrustService.getByStackId(stack.getId())).thenReturn(crossRealmTrust);

        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        lenient().when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);

        OperationStatus operationStatus = new OperationStatus();
        operationStatus.setStatus(OperationState.RUNNING);
        lenient().when(operationConverter.convert(operation)).thenReturn(operationStatus);

        loadBalancer = new LoadBalancer();
        loadBalancer.setFqdn(LB_FQDN);

        lenient().when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);
    }

    @Test
    void setupTrustV1WithExistingCrossRealmTrust() {
        PrepareCrossRealmTrustRequest request = getPrepareCrossRealmTrustV1Request();
        when(crossRealmTrustService.getByStackIdIfExists(stack.getId())).thenReturn(Optional.of(crossRealmTrust));

        PrepareCrossRealmTrustResponse result = underTest.setupTrust(ACCOUNT_ID, request);

        verifyCrossRealmTrustSetupV1(crossRealmTrust);
    }

    @Test
    void setupTrustV1WithoutExistingCrossRealmTrust() {
        PrepareCrossRealmTrustRequest request = getPrepareCrossRealmTrustV1Request();
        when(crossRealmTrustService.getByStackIdIfExists(stack.getId())).thenReturn(Optional.empty());
        ArgumentCaptor<CrossRealmTrust> captor = ArgumentCaptor.forClass(CrossRealmTrust.class);
        when(crossRealmTrustService.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        PrepareCrossRealmTrustResponse result = underTest.setupTrust(ACCOUNT_ID, request);

        verifyCrossRealmTrustSetupV1(captor.getValue());
    }

    private PrepareCrossRealmTrustRequest getPrepareCrossRealmTrustV1Request() {
        PrepareCrossRealmTrustRequest request = new PrepareCrossRealmTrustRequest();
        request.setFqdn(FQDN);
        request.setIp(IP);
        request.setRealm(REALM);
        request.setEnvironmentCrn(ENV_CRN);
        request.setRemoteEnvironmentCrn(REMOTE_ENV_CRN);
        return request;
    }

    private void verifyCrossRealmTrustSetupV1(CrossRealmTrust crossRealmTrust) {
        verifyCrossRealmTrustSetup(crossRealmTrust, KdcType.ACTIVE_DIRECTORY, IP);
    }

    @Test
    void setupTrustV2WithActiveDirectoryAndExistingCrossRealmTrust() {
        PrepareCrossRealmTrustV2Request request = getPrepareCrossRealmTrustV2Request(KdcType.ACTIVE_DIRECTORY);
        when(crossRealmTrustService.getByStackIdIfExists(stack.getId())).thenReturn(Optional.of(crossRealmTrust));

        PrepareCrossRealmTrustResponse result = underTest.setupTrust(ACCOUNT_ID, request);

        verifyCrossRealmTrustSetupV2(crossRealmTrust, KdcType.ACTIVE_DIRECTORY);
    }

    @Test
    void setupTrustV2WithMitAndWithoutExistingCrossRealmTrust() {
        PrepareCrossRealmTrustV2Request request = getPrepareCrossRealmTrustV2Request(KdcType.MIT);
        when(crossRealmTrustService.getByStackIdIfExists(stack.getId())).thenReturn(Optional.empty());
        ArgumentCaptor<CrossRealmTrust> captor = ArgumentCaptor.forClass(CrossRealmTrust.class);
        when(crossRealmTrustService.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        PrepareCrossRealmTrustResponse result = underTest.setupTrust(ACCOUNT_ID, request);

        verifyCrossRealmTrustSetupV2(captor.getValue(), KdcType.MIT);
    }

    private PrepareCrossRealmTrustV2Request getPrepareCrossRealmTrustV2Request(KdcType kdcType) {
        PrepareCrossRealmTrustV2Request request = new PrepareCrossRealmTrustV2Request();
        request.setEnvironmentCrn(ENV_CRN);
        request.setRemoteEnvironmentCrn(REMOTE_ENV_CRN);
        request.setDnsServerIps(List.of(DNS_IP));
        PrepareCrossRealmTrustV2KdcServerRequest server = new PrepareCrossRealmTrustV2KdcServerRequest();
        server.setIp(IP);
        server.setFqdn(FQDN);
        switch (kdcType) {
            case ACTIVE_DIRECTORY -> {
                PrepareCrossRealmTrustV2ActiveDirectoryRequest ad = new PrepareCrossRealmTrustV2ActiveDirectoryRequest();
                ad.setRealm(REALM);
                ad.setServers(List.of(server));
                request.setAd(ad);
            }
            case MIT -> {
                PrepareCrossRealmTrustV2MitRequest mit = new PrepareCrossRealmTrustV2MitRequest();
                mit.setRealm(REALM);
                mit.setServers(List.of(server));
                request.setMit(mit);
            }
            default -> throw new NotImplementedException("New KDC type: " + kdcType);
        }
        return request;
    }

    private void verifyCrossRealmTrustSetupV2(CrossRealmTrust crossRealmTrust, KdcType kdcType) {
        verifyCrossRealmTrustSetup(crossRealmTrust, kdcType, DNS_IP);
    }

    private void verifyCrossRealmTrustSetup(CrossRealmTrust crossRealmTrust, KdcType kdcType, String dnsIp) {
        assertThat(crossRealmTrust)
                .returns(FQDN, CrossRealmTrust::getKdcFqdn)
                .returns(IP, CrossRealmTrust::getKdcIp)
                .returns(dnsIp, CrossRealmTrust::getDnsIp)
                .returns(kdcType, CrossRealmTrust::getKdcType)
                .returns(REALM, CrossRealmTrust::getKdcRealm)
                .returns(ENV_CRN, CrossRealmTrust::getEnvironmentCrn)
                .returns(REMOTE_ENV_CRN, CrossRealmTrust::getRemoteEnvironmentCrn);
        verify(crossRealmTrustService).save(crossRealmTrust);
        verify(operationService).startOperation(ACCOUNT_ID, OperationType.TRUST_SETUP, Set.of(ENV_CRN), Set.of());
        verify(flowManager).notify(any(), any(FreeIpaTrustSetupEvent.class));
    }

    @Test
    void returnsTrustSetupCommandsResponseWhenStatusIsAllowed() {
        TrustSetupCommandsResponse expectedResponse = mock(TrustSetupCommandsResponse.class);
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_ACTIVE);
        when(crossRealmTrustService.getTrustProvider(STACK_ID)).thenReturn(adTrustProvider);
        when(adTrustProvider.buildTrustSetupCommandsResponse(TrustCommandType.SETUP, ENV_CRN, stack, freeIpa, crossRealmTrust, loadBalancer))
                .thenReturn(expectedResponse);

        TrustSetupCommandsResponse response = underTest.getTrustCommands(ACCOUNT_ID, ENV_CRN, TrustCommandType.SETUP);

        assertSame(expectedResponse, response);
    }

    @Test
    void throwsBadRequestExceptionWhenTrustStatusIsNotAllowed() {
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_SETUP_REQUIRED);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> underTest.getTrustCommands(ACCOUNT_ID, ENV_CRN, TrustCommandType.SETUP));
        assertTrue(ex.getMessage().contains("trust is not in state, where trust setup commands can be generated"));
    }

    @Test
    void returnsTrustValidationCommandsResponse() {
        TrustSetupCommandsResponse expectedResponse = mock(TrustSetupCommandsResponse.class);
        crossRealmTrust.setTrustStatus(TrustStatus.TRUST_ACTIVE);
        when(crossRealmTrustService.getTrustProvider(STACK_ID)).thenReturn(adTrustProvider);
        when(adTrustProvider.buildTrustValidationCommandsResponse(ENV_CRN, stack, freeIpa, crossRealmTrust, loadBalancer))
                .thenReturn(expectedResponse);

        TrustSetupCommandsResponse response = underTest.getTrustCommands(ACCOUNT_ID, ENV_CRN, TrustCommandType.VALIDATION);

        assertSame(expectedResponse, response);
    }
}