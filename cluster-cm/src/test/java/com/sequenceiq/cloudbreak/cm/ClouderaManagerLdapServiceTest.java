package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadata;
import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiAuthRoleRef;
import com.cloudera.api.swagger.model.ApiExternalUserMapping;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ClouderaManagerLdapServiceTest {
    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ExternalUserMappingsResourceApi externalUserMappingsResourceApi;

    @Mock
    private AuthRolesResourceApi authRolesResourceApi;

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private ClouderaManagerLicenseService licenseService;

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private ClouderaManagerLdapService underTest = new ClouderaManagerLdapService();

    private Stack stack;

    private Cluster cluster;

    private HttpClientConfig httpClientConfig;

    @Before
    public void init() {
        User user = new User();
        stack = new Stack();
        stack.setCreator(user);
        cluster = new Cluster();
        cluster.setName("clusterName");
        stack.setCluster(cluster);
        httpClientConfig = new HttpClientConfig("apiAddress");
        MockitoAnnotations.initMocks(this);
        when(clouderaManagerClientFactory.getClient(stack, cluster, httpClientConfig)).thenReturn(apiClient);
        when(clouderaManagerClientFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerClientFactory.getExternalUserMappingsResourceApi(apiClient)).thenReturn(externalUserMappingsResourceApi);
        when(clouderaManagerClientFactory.getAuthRolesResourceApi(apiClient)).thenReturn(authRolesResourceApi);
    }

    @Test
    public void testSetupLdapWithoutGroupMapping() throws ApiException {
        // GIVEN
        LdapConfig ldapConfig = getLdapConfig();
        cluster.setLdapConfig(ldapConfig);
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList());
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig);
        // THEN
        verify(licenseService).beginTrialIfNeeded(stack.getCreator(), apiClient);
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
        underTest.setupLdap(stack, cluster, httpClientConfig);
        // THEN
        verify(licenseService).beginTrialIfNeeded(stack.getCreator(), apiClient);
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
        underTest.setupLdap(stack, cluster, httpClientConfig);
        // THEN
        verify(licenseService).beginTrialIfNeeded(stack.getCreator(), apiClient);
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    @Test
    public void testSetupLdapWithoutLdap() throws ApiException {
        // GIVEN
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig);
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
