package com.sequenceiq.freeipa.service.config;

import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.ADMIN_GROUP;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.BIND_DN;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.GROUP_MEMBER_ATTRIBUTE;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.GROUP_NAME_ATTRIBUTE;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.GROUP_OBJECT_CLASS;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.GROUP_SEARCH_BASE;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.PROTOCOL;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.SERVER_PORT;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.USER_DN_PATTERN;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.USER_NAME_ATTRIBUTE;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.USER_OBJECT_CLASS;
import static com.sequenceiq.freeipa.service.config.LdapConfigRegisterService.USER_SEARCH_BASE;
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
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class LdapConfigRegisterServiceTest {

    @InjectMocks
    private LdapConfigRegisterService underTest;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Mock
    private LdapConfigService ldapConfigService;

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

        ArgumentCaptor<LdapConfig> ldapConfigArgumentCaptor = ArgumentCaptor.forClass(LdapConfig.class);
        ArgumentCaptor<String> accountIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(ldapConfigService).createLdapConfig(ldapConfigArgumentCaptor.capture(), accountIdArgumentCaptor.capture());

        assertEquals(stack.getAccountId(), accountIdArgumentCaptor.getValue());

        LdapConfig ldapConfig = ldapConfigArgumentCaptor.getValue();
        assertEquals(ldapConfig.getName(), stack.getName());
        assertEquals(ldapConfig.getEnvironmentCrn(), stack.getEnvironmentCrn());
        assertEquals(ldapConfig.getAdminGroup(), ADMIN_GROUP);
        String domainComponent = ",dc=testdomain,dc=local";
        assertEquals(ldapConfig.getBindDn(), BIND_DN + domainComponent);
        assertEquals(ldapConfig.getUserSearchBase(), USER_SEARCH_BASE + domainComponent);
        assertEquals(ldapConfig.getGroupSearchBase(), GROUP_SEARCH_BASE + domainComponent);
        assertEquals(ldapConfig.getUserDnPattern(), USER_DN_PATTERN + domainComponent);
        assertEquals(ldapConfig.getServerHost(), instanceMetaData.getDiscoveryFQDN());
        assertEquals(ldapConfig.getProtocol(), PROTOCOL);
        assertEquals(ldapConfig.getServerPort(), SERVER_PORT);
        assertEquals(ldapConfig.getDomain(), freeIpa.getDomain());
        assertEquals(ldapConfig.getBindPassword(), freeIpa.getAdminPassword());
        assertEquals(ldapConfig.getDirectoryType(), DirectoryType.LDAP);
        assertEquals(ldapConfig.getUserNameAttribute(), USER_NAME_ATTRIBUTE);
        assertEquals(ldapConfig.getUserObjectClass(), USER_OBJECT_CLASS);
        assertEquals(ldapConfig.getGroupMemberAttribute(), GROUP_MEMBER_ATTRIBUTE);
        assertEquals(ldapConfig.getGroupNameAttribute(), GROUP_NAME_ATTRIBUTE);
        assertEquals(ldapConfig.getGroupObjectClass(), GROUP_OBJECT_CLASS);
    }

    @Test
    void testDelete() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("env");
        stack.setAccountId("acc");

        underTest.delete(stack);

        verify(ldapConfigService).deleteAllInEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
    }
}