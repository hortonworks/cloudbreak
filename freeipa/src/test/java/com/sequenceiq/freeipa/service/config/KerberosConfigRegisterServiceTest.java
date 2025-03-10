package com.sequenceiq.freeipa.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.BalancedDnsAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
class KerberosConfigRegisterServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String LOAD_BALANCER_IP = "2.2.2.2";

    @InjectMocks
    private KerberosConfigRegisterService underTest;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Mock
    private BalancedDnsAvailabilityChecker balancedDnsAvailabilityChecker;

    @Mock
    private FreeIpaLoadBalancerService loadBalancerService;

    @Test
    void testRegister() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");
        stack.setName("name");
        stack.setAppVersion("2.20.0");
        stack.setId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        instanceMetaData.setPrivateIp("1.1.1.1");
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        stack.setInstanceGroups(Collections.singleton(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("testdomain.local");
        freeIpa.setAdminPassword("asdf");
        when(freeIpaService.findByStackId(anyLong())).thenReturn(freeIpa);
        when(balancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack)).thenReturn(true);
        when(underTest.getEnvironmentCrnByStackId(1L)).thenReturn(ENVIRONMENT_CRN);
        when(loadBalancerService.findByStackId(stack.getId())).thenReturn(Optional.empty());

        underTest.register(1L);

        ArgumentCaptor<KerberosConfig> kerberosConfigArgumentCaptor = ArgumentCaptor.forClass(KerberosConfig.class);
        ArgumentCaptor<String> accountIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(kerberosConfigService).createKerberosConfig(kerberosConfigArgumentCaptor.capture(), accountIdArgumentCaptor.capture());
        assertEquals(stack.getAccountId(), accountIdArgumentCaptor.getValue());
        KerberosConfig kerberosConfig = kerberosConfigArgumentCaptor.getValue();
        assertEquals(stack.getName(), kerberosConfig.getName());
        assertEquals(stack.getEnvironmentCrn(), kerberosConfig.getEnvironmentCrn());
        assertEquals("kdc.testdomain.local", kerberosConfig.getUrl());
        assertEquals("kerberos.testdomain.local", kerberosConfig.getAdminUrl());
        assertEquals(instanceMetaData.getPrivateIp(), kerberosConfig.getNameServers());
        assertEquals(freeIpa.getAdminPassword(), kerberosConfig.getPassword());
        assertEquals(freeIpa.getDomain(), kerberosConfig.getDomain());
        assertEquals(freeIpa.getDomain().toUpperCase(Locale.ROOT), kerberosConfig.getRealm());
        assertEquals(KerberosType.FREEIPA, kerberosConfig.getType());
        assertEquals(KerberosConfigRegisterService.FREEIPA_DEFAULT_ADMIN, kerberosConfig.getPrincipal());
    }

    @Test
    void testRegisterWithLb() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");
        stack.setName("name");
        stack.setAppVersion("2.20.0");
        stack.setId(1L);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        instanceMetaData.setPrivateIp("1.1.1.1");
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        stack.setInstanceGroups(Collections.singleton(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("testdomain.local");
        freeIpa.setAdminPassword("asdf");
        when(freeIpaService.findByStackId(anyLong())).thenReturn(freeIpa);
        when(balancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack)).thenReturn(true);
        when(underTest.getEnvironmentCrnByStackId(1L)).thenReturn(ENVIRONMENT_CRN);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setIp(Set.of(LOAD_BALANCER_IP));
        when(loadBalancerService.findByStackId(stack.getId())).thenReturn(Optional.of(loadBalancer));

        underTest.register(1L);

        ArgumentCaptor<KerberosConfig> kerberosConfigArgumentCaptor = ArgumentCaptor.forClass(KerberosConfig.class);
        ArgumentCaptor<String> accountIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(kerberosConfigService).createKerberosConfig(kerberosConfigArgumentCaptor.capture(), accountIdArgumentCaptor.capture());
        assertEquals(stack.getAccountId(), accountIdArgumentCaptor.getValue());
        KerberosConfig kerberosConfig = kerberosConfigArgumentCaptor.getValue();
        assertEquals(stack.getName(), kerberosConfig.getName());
        assertEquals(stack.getEnvironmentCrn(), kerberosConfig.getEnvironmentCrn());
        assertEquals("kdc.testdomain.local", kerberosConfig.getUrl());
        assertEquals("kerberos.testdomain.local", kerberosConfig.getAdminUrl());
        assertEquals(LOAD_BALANCER_IP, kerberosConfig.getNameServers());
        assertEquals(freeIpa.getAdminPassword(), kerberosConfig.getPassword());
        assertEquals(freeIpa.getDomain(), kerberosConfig.getDomain());
        assertEquals(freeIpa.getDomain().toUpperCase(Locale.ROOT), kerberosConfig.getRealm());
        assertEquals(KerberosType.FREEIPA, kerberosConfig.getType());
        assertEquals(KerberosConfigRegisterService.FREEIPA_DEFAULT_ADMIN, kerberosConfig.getPrincipal());
    }

    @Test
    void testAlreadyExists() {
        when(underTest.getEnvironmentCrnByStackId(1L)).thenReturn(ENVIRONMENT_CRN);
        when(kerberosConfigService.doesEnvironmentLevelKerberosConfigExists(Crn.safeFromString(ENVIRONMENT_CRN))).thenReturn(Boolean.TRUE);

        underTest.register(1L);

        verify(kerberosConfigService, never()).createKerberosConfig(any(), any());
        verify(kerberosConfigService, never()).createKerberosConfig(any());
    }

    @Test
    void delete() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");

        underTest.delete(stack);

        verify(kerberosConfigService).deleteAllInEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
    }
}