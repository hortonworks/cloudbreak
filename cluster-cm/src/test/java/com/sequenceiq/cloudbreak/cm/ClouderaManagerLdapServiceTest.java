package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class ClouderaManagerLdapServiceTest {

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ExternalUserMappingsResourceApi externalUserMappingsResourceApi;

    @Mock
    private AuthRolesResourceApi authRolesResourceApi;

    @InjectMocks
    private ClouderaManagerLdapService underTest = spy(new ClouderaManagerLdapService());

    private Stack stack;

    private Cluster cluster;

    private ApiClient client;

    @Before
    public void init() {
        stack = new Stack();
        cluster = new Cluster();
        cluster.setName("clusterName");
        stack.setCluster(cluster);
        client = new ApiClient();
        MockitoAnnotations.initMocks(this);
        when(underTest.getClouderaManagerResourceApi(any(ApiClient.class))).thenReturn(clouderaManagerResourceApi);
        when(underTest.getExternalUserMappingsResourceApi(any(ApiClient.class))).thenReturn(externalUserMappingsResourceApi);
        when(underTest.getAuthRolesResourceApi(any(ApiClient.class))).thenReturn(authRolesResourceApi);
    }

    @Test
    public void testSetupLdapWithoutGroupMapping() throws ApiException {
        // GIVEN
        LdapConfig ldapConfig = getLdapConfig();
        cluster.setLdapConfig(ldapConfig);
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList());
        // WHEN
        underTest.setupLdap(client, stack, cluster);
        // THEN
        ArgumentCaptor<ApiConfigList> apiConfigListArgumentCaptor = ArgumentCaptor.forClass(ApiConfigList.class);
        verify(clouderaManagerResourceApi).updateConfig(anyString(), apiConfigListArgumentCaptor.capture());
        Map<String, String> apiConfigMap =
                apiConfigListArgumentCaptor.getValue().getItems().stream().collect(Collectors.toMap(ApiConfig::getName, ApiConfig::getValue));
        assertEquals("LDAP", apiConfigMap.get("ldap_type"));
        assertEquals(ldapConfig.getProtocol() + "://" + ldapConfig.getServerHost() + ":" + ldapConfig.getServerPort(), apiConfigMap.get("ldap_url"));
        assertEquals(ldapConfig.getBindDn(), apiConfigMap.get("ldap_bind_dn"));
        assertEquals(ldapConfig.getBindPassword(), apiConfigMap.get("ldap_bind_pw"));
        assertEquals(ldapConfig.getDomain(), apiConfigMap.get("nt_domain"));
        assertEquals(ldapConfig.getUserNameAttribute() + "={0}", apiConfigMap.get("ldap_user_search_filter"));
        assertEquals(ldapConfig.getUserSearchBase(), apiConfigMap.get("ldap_user_search_base"));
        assertEquals(ldapConfig.getGroupMemberAttribute() + "={0}", apiConfigMap.get("ldap_group_search_filter"));
        assertEquals(ldapConfig.getGroupSearchBase(), apiConfigMap.get("ldap_group_search_base"));
        assertEquals(ldapConfig.getUserDnPattern(), apiConfigMap.get("ldap_dn_pattern"));
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    @Test
    public void testSetupLdapWithGroupMapping() throws ApiException {
        // GIVEN
        LdapConfig ldapConfig = getLdapConfig();
        cluster.setLdapConfig(ldapConfig);
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList().addItemsItem(
                new ApiAuthRoleMetadata().displayName("role").uuid("uuid").role("ROLE_ADMIN")));
        // WHEN
        underTest.setupLdap(client, stack, cluster);
        // THEN
        ArgumentCaptor<ApiExternalUserMappingList> apiExternalUserMappingListArgumentCaptor = ArgumentCaptor.forClass(ApiExternalUserMappingList.class);
        verify(externalUserMappingsResourceApi).createExternalUserMappings(apiExternalUserMappingListArgumentCaptor.capture());
        ApiExternalUserMapping apiExternalUserMapping = apiExternalUserMappingListArgumentCaptor.getValue().getItems().get(0);
        ApiAuthRoleRef authRole = apiExternalUserMapping.getAuthRoles().get(0);
        assertEquals("role", authRole.getDisplayName());
        assertEquals("uuid", authRole.getUuid());
        assertEquals(ldapConfig.getAdminGroup(), apiExternalUserMapping.getName());
    }

    @Test
    public void testSetupLdapWithNoRoleAdmin() throws ApiException {
        // GIVEN
        LdapConfig ldapConfig = getLdapConfig();
        cluster.setLdapConfig(ldapConfig);
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList().addItemsItem(
                new ApiAuthRoleMetadata().displayName("role").uuid("uuid").role("NO_ROLE_ADMIN")));
        // WHEN
        underTest.setupLdap(client, stack, cluster);
        // THEN
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    @Test
    public void testSetupLdapWithoutLdap() throws ApiException {
        // GIVEN
        // WHEN
        underTest.setupLdap(client, stack, cluster);
        // THEN
        verify(clouderaManagerResourceApi, never()).updateConfig(anyString(), any());
        verify(authRolesResourceApi, never()).readAuthRolesMetadata(anyString());
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    private LdapConfig getLdapConfig() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName("ldapcfg");
        ldapConfig.setDomain("domain");
        ldapConfig.setProtocol("ldap");
        ldapConfig.setServerHost("locahost");
        ldapConfig.setServerPort(389);
        ldapConfig.setDirectoryType(DirectoryType.LDAP);
        ldapConfig.setBindDn("binddn");
        ldapConfig.setBindPassword("bindpwd");
        ldapConfig.setUserSearchBase("usersearchbase");
        ldapConfig.setUserNameAttribute("user");
        ldapConfig.setGroupSearchBase("groupsearchbase");
        ldapConfig.setGroupMemberAttribute("member");
        ldapConfig.setAdminGroup("adminGroup");
        ldapConfig.setUserDnPattern("userDnPattern");
        return ldapConfig;
    }
}
