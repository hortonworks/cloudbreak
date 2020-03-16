package com.sequenceiq.freeipa.service.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class KerberosConfigRegisterServiceTest {

    @InjectMocks
    private KerberosConfigRegisterService underTest;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Test
    void testRegister() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");
        stack.setName("name");
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
        assertEquals(freeIpa.getDomain().toUpperCase(), kerberosConfig.getRealm());
        assertEquals(KerberosType.FREEIPA, kerberosConfig.getType());
        assertEquals(KerberosConfigRegisterService.FREEIPA_DEFAULT_ADMIN, kerberosConfig.getPrincipal());
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