package com.sequenceiq.freeipa.service.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class KerberosConfigUpdateServiceTest {

    @InjectMocks
    private KerberosConfigUpdateService underTest;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private StackService stackService;

    @Test
    void testUpdateNameservers() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");
        stack.setName("name");
        stack.setAppVersion("2.20.0");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("fqdn");
        instanceMetaData1.setPrivateIp("1.1.1.1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("fqdn");
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData1, instanceMetaData2));
        stack.setInstanceGroups(Collections.singleton(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        KerberosConfig kerberosConfig1 = mock(KerberosConfig.class);
        KerberosConfig kerberosConfig2 = mock(KerberosConfig.class);
        List<KerberosConfig> kerberosConfigs = List.of(kerberosConfig1, kerberosConfig2);
        when(kerberosConfigService.findAllInEnvironment(any())).thenReturn(kerberosConfigs);

        underTest.updateNameservers(1L);

        ArgumentCaptor<String> nameServersCaptor = ArgumentCaptor.forClass(String.class);
        verify(kerberosConfig1).setNameServers(nameServersCaptor.capture());
        verify(kerberosConfig2).setNameServers(nameServersCaptor.capture());

        List.of(nameServersCaptor.getValue(), nameServersCaptor.getValue()).forEach(actualNameServersValue -> {
            assertTrue("1.1.1.1,2.2.2.2".equals(actualNameServersValue) || "2.2.2.2,1.1.1.1".equals(actualNameServersValue));
        });

        verify(kerberosConfigService).saveAll(eq(kerberosConfigs));
    }
}