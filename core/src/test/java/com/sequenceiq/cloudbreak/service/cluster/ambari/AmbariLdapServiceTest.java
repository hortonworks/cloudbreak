package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.DirectoryType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(MockitoJUnitRunner.class)
public class AmbariLdapServiceTest {

    @InjectMocks
    private AmbariLdapService ambariLdapService;

    @Mock
    private AmbariClientFactory ambariClientFactory;

    @Mock
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    private Cluster cluster;

    private Stack stack;

    private LdapConfig ldapConfig;

    private AmbariClient ambariClient;

    @Before
    public void initTest() {
        Gateway gateway = new Gateway();

        cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);
        ldapConfig = new LdapConfig();
        ldapConfig.setProtocol("ldaps");
        ldapConfig.setName("ldap-name");
        ldapConfig.setAdminGroup("admingroup");
        ldapConfig.setBindDn("bindDn");
        ldapConfig.setBindPassword("bindPasswd");
        ldapConfig.setDescription("descr");
        ldapConfig.setDirectoryType(DirectoryType.ACTIVE_DIRECTORY);
        ldapConfig.setDomain("domain");
        ldapConfig.setGroupMemberAttribute("groupMemberAttr");
        ldapConfig.setGroupNameAttribute("groupNameAttr");
        ldapConfig.setGroupObjectClass("groupObjectClass");
        ldapConfig.setGroupSearchBase("groupSearchBase");
        ldapConfig.setUserNameAttribute("userNameAttr");
        ldapConfig.setUserDnPattern("userDnPattern");
        ldapConfig.setUserObjectClass("userObjectClass");
        ldapConfig.setUserSearchBase("userSearchBase");
        ldapConfig.setServerPort(1234);
        ldapConfig.setServerHost("host");
        cluster.setLdapConfig(ldapConfig);

        stack = new Stack();
        stack.setName("stack0");
        stack.setId(2L);
        stack.setCluster(cluster);

        ambariClient = mock(AmbariClient.class);
        when(ambariClientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);
    }

    @Test
    public void setupLdap() {
        AmbariRepo ambariRepo = mock(AmbariRepo.class);
        when(ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(any(), any())).thenReturn(false);
        ambariLdapService.setupLdap(stack, cluster, ambariRepo);

        verify(ambariClient).configureLdap(captor.capture());
        Map<String, Object> parameters = captor.getValue();
        assertThat(parameters, hasEntry("ambari.ldap.authentication.enabled", true));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.server.host", ldapConfig.getServerHost()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.server.port", ldapConfig.getServerPort()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.secondary.server.host", ldapConfig.getServerHost()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.secondary.server.port", ldapConfig.getServerPort()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.use_ssl", true));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.dn_attr", ldapConfig.getUserDnPattern()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.object_class", ldapConfig.getUserObjectClass()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.object_class", ldapConfig.getGroupObjectClass()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.name_attr", ldapConfig.getUserNameAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.name_attr", ldapConfig.getGroupNameAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.search_base", ldapConfig.getUserSearchBase()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.search_base", ldapConfig.getGroupSearchBase()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.member_attr", ldapConfig.getGroupMemberAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.bind_password", "/etc/ambari-server/conf/ldap-password.dat"));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.referrals", "follow"));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.anonymous_bind", false));
        assertThat(parameters, hasEntry("ambari.ldap.advance.collision_behavior", "convert"));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.force_lowercase_usernames", false));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.pagination_enabled", true));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.group_mapping_rules", ldapConfig.getAdminGroup()));
    }

    @Test
    public void setupLdapWithAmbari2720() {
        AmbariRepo ambariRepo = mock(AmbariRepo.class);
        when(ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(any(), any())).thenReturn(true);
        ambariLdapService.setupLdap(stack, cluster, ambariRepo);

        verify(ambariClient).configureLdap(captor.capture());
        Map<String, Object> parameters = captor.getValue();
        assertThat(parameters, hasEntry("ambari.ldap.authentication.enabled", true));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.server.host", ldapConfig.getServerHost()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.server.port", ldapConfig.getServerPort()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.secondary.server.host", ldapConfig.getServerHost()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.secondary.server.port", ldapConfig.getServerPort()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.use_ssl", true));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.dn_attr", ldapConfig.getUserDnPattern()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.object_class", ldapConfig.getUserObjectClass()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.object_class", ldapConfig.getGroupObjectClass()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.name_attr", ldapConfig.getUserNameAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.name_attr", ldapConfig.getGroupNameAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.user.search_base", ldapConfig.getUserSearchBase()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.search_base", ldapConfig.getGroupSearchBase()));
        assertThat(parameters, hasEntry("ambari.ldap.attributes.group.member_attr", ldapConfig.getGroupMemberAttribute()));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.bind_password", "/etc/ambari-server/conf/ldap-password.dat"));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.referrals", "follow"));
        assertThat(parameters, hasEntry("ambari.ldap.connectivity.anonymous_bind", false));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.collision_behavior", "convert"));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.force_lowercase_usernames", false));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.pagination_enabled", true));
        assertThat(parameters, hasEntry("ambari.ldap.advanced.group_mapping_rules", ldapConfig.getAdminGroup()));
    }

    @Test
    public void syncLdap() {
        ambariLdapService.syncLdap(stack, cluster);
        verify(ambariClient, times(1)).syncLdap();
    }
}