package com.sequenceiq.environment.environment.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.DefaultAvailabilityZoneService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchApplyException;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentNetworkAzBackfillPatchServiceTest {

    @Mock
    private DefaultAvailabilityZoneService defaultAvailabilityZoneService;

    @Mock
    private NetworkService networkService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudRegions cloudRegions;

    @InjectMocks
    private GcpEnvironmentNetworkAzBackfillPatchService underTest;

    private Environment environment;

    private GcpNetwork network;

    private GcpNetwork currentNetwork;

    @BeforeEach
    void setUp() {
        network = new GcpNetwork();
        currentNetwork = new GcpNetwork();
        environment = new Environment();
        environment.setId(1L);
        environment.setResourceCrn("crn:cdp:environments:us-west-1:accountId:environment:envId");
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setCloudPlatform(CloudPlatform.GCP.name());
        environment.setNetwork(network);
    }

    @Test
    void getStackPatchTypeReturnsGcpEnvironmentNetworkAzBackfill() {
        assertEquals(StackPatchType.GCP_ENVIRONMENT_NETWORK_AZ_BACKFILL, underTest.getStackPatchType());
    }

    @Test
    void isAffectedWhenTheZonesNeedToBePopulated() {
        when(defaultAvailabilityZoneService.isPopulationNeeded(environment, network)).thenReturn(true);

        assertTrue(underTest.isAffected(environment));
    }

    @Test
    void isNotAffectedWhenTheZonesDoNotNeedToBePopulated() {
        when(defaultAvailabilityZoneService.isPopulationNeeded(environment, network)).thenReturn(false);

        assertFalse(underTest.isAffected(environment));
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = "GCP", mode = EnumSource.Mode.EXCLUDE)
    void isNotAffectedWhenNotGcp(CloudPlatform cloudPlatform) {
        environment.setCloudPlatform(cloudPlatform.name());

        assertFalse(underTest.isAffected(environment));

        verifyNoInteractions(defaultAvailabilityZoneService);
    }

    @Test
    void doApplyPopulatesAndSavesTheNetworkReReadInTheTransactionAfterTheProviderCall() throws Exception {
        givenTransactionRunsCallback();
        when(defaultAvailabilityZoneService.getRegions(environment)).thenReturn(cloudRegions);
        when(networkService.findByEnvironment(1L)).thenReturn(Optional.of(currentNetwork));
        when(defaultAvailabilityZoneService.isPopulationNeeded(environment, currentNetwork)).thenReturn(false);

        assertTrue(underTest.doApply(environment));

        InOrder inOrder = inOrder(defaultAvailabilityZoneService, transactionService, networkService);
        inOrder.verify(defaultAvailabilityZoneService).getRegions(environment);
        inOrder.verify(transactionService).required(any(Supplier.class));
        inOrder.verify(networkService).findByEnvironment(1L);
        inOrder.verify(defaultAvailabilityZoneService).populateIfMissing(environment, currentNetwork, cloudRegions);
        inOrder.verify(networkService).save(currentNetwork);
        verify(networkService, never()).save(network);
    }

    @Test
    void doApplyFailsWhenTheTransactionFails() throws Exception {
        when(defaultAvailabilityZoneService.getRegions(environment)).thenReturn(cloudRegions);
        when(transactionService.required(any(Supplier.class)))
                .thenThrow(new TransactionExecutionException("tx failed", new IllegalStateException("tx failed")));

        assertThrows(TransactionRuntimeExecutionException.class, () -> underTest.doApply(environment));
    }

    @Test
    void doApplyIsSkippedWhenTheEnvironmentIsNotAvailable() throws Exception {
        environment.setStatus(EnvironmentStatus.ENV_STOPPED);

        assertFalse(underTest.doApply(environment));

        verifyNoInteractions(defaultAvailabilityZoneService, networkService);
    }

    @Test
    void doApplyFailsWhenNoZoneWasPopulated() throws Exception {
        givenTransactionRunsCallback();
        when(defaultAvailabilityZoneService.getRegions(environment)).thenReturn(cloudRegions);
        when(networkService.findByEnvironment(1L)).thenReturn(Optional.of(currentNetwork));
        when(defaultAvailabilityZoneService.isPopulationNeeded(environment, currentNetwork)).thenReturn(true);

        assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(environment));

        verify(defaultAvailabilityZoneService).populateIfMissing(environment, currentNetwork, cloudRegions);
        verify(networkService, never()).save(any());
    }

    @Test
    void shouldNotCheckForFailedRetryableFlow() {
        assertFalse(underTest.shouldCheckForFailedRetryableFlow());
    }

    private void givenTransactionRunsCallback() throws TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }
}
