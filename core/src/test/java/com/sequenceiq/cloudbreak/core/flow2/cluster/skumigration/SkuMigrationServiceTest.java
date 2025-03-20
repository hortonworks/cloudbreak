package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.LOAD_BALANCER_SKU_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.STANDARD_SKU_MIGRATION_PARAMETER;
import static com.sequenceiq.common.api.type.LoadBalancerSku.BASIC;
import static com.sequenceiq.common.api.type.LoadBalancerSku.STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackParametersService;

@ExtendWith(MockitoExtension.class)
class SkuMigrationServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private StackParametersService stackParametersService;

    @InjectMocks
    private SkuMigrationService skuMigrationService;

    @Mock
    private StackDto stackDto;

    @Mock
    private Network network;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getNetwork()).thenReturn(network);
        lenient().when(stackDto.getParameters()).thenReturn(new HashMap<>());
    }

    @Test
    void testUpdateSkuToStandardWithLoadBalancers() {
        Set<LoadBalancer> loadBalancers = new HashSet<>();
        LoadBalancer lb = new LoadBalancer();
        loadBalancers.add(lb);

        skuMigrationService.updateSkuToStandard(STACK_ID, loadBalancers);

        assertEquals(STANDARD, lb.getSku());
        verify(loadBalancerPersistenceService).saveAll(loadBalancers);
        verify(stackParametersService).setStackParameter(STACK_ID, LOAD_BALANCER_SKU_PARAMETER, STANDARD.name());
    }

    @Test
    void testUpdateSkuToStandardWithEmptyLoadBalancers() {
        Set<LoadBalancer> loadBalancers = new HashSet<>();

        skuMigrationService.updateSkuToStandard(STACK_ID, loadBalancers);

        verify(loadBalancerPersistenceService, times(0)).saveAll(Collections.emptySet());
        verify(stackParametersService, times(0)).setStackParameter(eq(STACK_ID), eq(LOAD_BALANCER_SKU_PARAMETER), any());
    }

    @Test
    void testSetSkuMigrationParameter() {
        skuMigrationService.setSkuMigrationParameter(STACK_ID);

        verify(stackParametersService).setStackParameter(STACK_ID, STANDARD_SKU_MIGRATION_PARAMETER, SkuMigrationService.MIGRATED);
    }

    @Test
    void testIsMigrationNecessaryWhenAlreadyMigrated() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(STANDARD_SKU_MIGRATION_PARAMETER, SkuMigrationService.MIGRATED);
        when(stackDto.getParameters()).thenReturn(parameters);
        when(stackDto.getNetwork().getAttributes()).thenReturn(new Json(Map.of(AzureConstants.NO_PUBLIC_IP, true)));
        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertFalse(result);
    }

    @Test
    void testIsMigrationNecessaryForAzureWithNonStandardSku() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setSku(BASIC);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID))
                .thenReturn(Set.of(loadBalancer));
        when(stackDto.getNetwork().getAttributes()).thenReturn(new Json(Map.of(AzureConstants.NO_PUBLIC_IP, true)));
        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertTrue(result);
    }

    @Test
    void testIsMigrationNecessaryForNonAzure() {
        when(stackDto.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertFalse(result);
    }

    @Test
    void testIsMigrationNecessaryWithZeroLoadBalancerAndNoPublicIpFalseAndNotMultiAz() {
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of());
        when(stackDto.getNetwork().getAttributes()).thenReturn(new Json(Map.of(AzureConstants.NO_PUBLIC_IP, false)));
        when(stackDto.getStack().isMultiAz()).thenReturn(false);

        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertTrue(result);
    }

    @Test
    void testIsMigrationNecessaryWithBasicLoadBalancerAndNoPublicIpFalseAndNotMultiAz() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setSku(BASIC);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(loadBalancer));
        when(stackDto.getNetwork().getAttributes()).thenReturn(new Json(Map.of(AzureConstants.NO_PUBLIC_IP, false)));
        when(stackDto.getStack().isMultiAz()).thenReturn(false);

        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertTrue(result);
    }

    @Test
    void testIsMigrationNecessaryWithMultiAzAndNoPublicIp() {
        LoadBalancer lb = new LoadBalancer();
        lb.setSku(STANDARD);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(lb));
        when(stackDto.getNetwork().getAttributes()).thenReturn(new Json(Map.of(AzureConstants.NO_PUBLIC_IP, true)));
        when(stackDto.getStack().isMultiAz()).thenReturn(true);

        boolean result = skuMigrationService.isMigrationNecessary(stackDto);

        assertFalse(result);
    }
}