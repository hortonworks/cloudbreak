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
import org.springframework.test.util.ReflectionTestUtils;

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
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ClouderaManagerLdapServiceTest {
    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ExternalUserMappingsResourceApi externalUserMappingsResourceApi;

    @Mock
    private AuthRolesResourceApi authRolesResourceApi;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient apiClient;

    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private ClouderaManagerLdapService underTest = new ClouderaManagerLdapService();

    private Stack stack;

    private Cluster cluster;

    private HttpClientConfig httpClientConfig;

    @Before
    public void init() throws ClouderaManagerClientInitException {
        User user = new User();
        stack = new Stack();
        stack.setCreator(user);
        cluster = new Cluster();
        cluster.setName("clusterName");
        stack.setCluster(cluster);
        httpClientConfig = new HttpClientConfig("apiAddress");
        MockitoAnnotations.initMocks(this);
        String cmUser = cluster.getCloudbreakClusterManagerUser();
        String cmPassword = cluster.getCloudbreakClusterManagerPassword();
        when(clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), cmUser, cmPassword, httpClientConfig)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getExternalUserMappingsResourceApi(apiClient)).thenReturn(externalUserMappingsResourceApi);
        when(clouderaManagerApiFactory.getAuthRolesResourceApi(apiClient)).thenReturn(authRolesResourceApi);
    }

    @Test
    public void testSetupLdapWithoutGroupMapping() throws ApiException, ClouderaManagerClientInitException {
        // GIVEN
        LdapView ldapConfig = getLdapConfig();
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList());
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig, ldapConfig, null);
        // THEN
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    @Test
    public void testSetupLdapWithFullAdminGroupMapping() throws ApiException, ClouderaManagerClientInitException {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "adminRole", "ROLE_ADMIN");
        ReflectionTestUtils.setField(underTest, "limitedAdminRole", "NO_ROLE_LIMITED_CLUSTER_ADMIN");
        ReflectionTestUtils.setField(underTest, "userRole", "ROLE_USER");
        ReflectionTestUtils.setField(underTest, "dashboardUserRole", "ROLE_DASHBOARD_USER");
        LdapView ldapConfig = getLdapConfig();
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(TestConstants.CRN, "");
        ApiAuthRoleMetadataList apiAuthRoleMetadataList = new ApiAuthRoleMetadataList().addItemsItem(
                new ApiAuthRoleMetadata().displayName("ROLE_LIMITED_CLUSTER_ADMIN").uuid("uuid").role("ROLE_LIMITED_CLUSTER_ADMIN"));
        apiAuthRoleMetadataList.addItemsItem(
                new ApiAuthRoleMetadata().displayName("ROLE_ADMIN").uuid("uuid").role("ROLE_ADMIN"));
        apiAuthRoleMetadataList.addItemsItem(
                new ApiAuthRoleMetadata().displayName("ROLE_DASHBOARD_USER").uuid("uuid").role("ROLE_DASHBOARD_USER"));
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(apiAuthRoleMetadataList);
        when(virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN)).thenReturn("virtualGroup");
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig, ldapConfig, virtualGroupRequest);
        // THEN
        ArgumentCaptor<ApiExternalUserMappingList> apiExternalUserMappingListArgumentCaptor = ArgumentCaptor.forClass(ApiExternalUserMappingList.class);
        verify(externalUserMappingsResourceApi).createExternalUserMappings(apiExternalUserMappingListArgumentCaptor.capture());
        ApiExternalUserMapping apiExternalUserMapping = apiExternalUserMappingListArgumentCaptor.getValue().getItems().get(0);
        ApiAuthRoleRef authRole = apiExternalUserMapping.getAuthRoles().get(0);
        assertEquals("ROLE_ADMIN", authRole.getDisplayName());
        assertEquals("uuid", authRole.getUuid());
        assertEquals("virtualGroup", apiExternalUserMapping.getName());
    }

    @Test
    public void testSetupLdapWithLimitedAdminGroupMapping() throws ApiException, ClouderaManagerClientInitException {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "adminRole", "ROLE_ADMIN");
        ReflectionTestUtils.setField(underTest, "limitedAdminRole", "ROLE_LIMITED_CLUSTER_ADMIN");
        ReflectionTestUtils.setField(underTest, "userRole", "ROLE_USER");
        ReflectionTestUtils.setField(underTest, "dashboardUserRole", "ROLE_DASHBOARD_USER");
        LdapView ldapConfig = getLdapConfig();
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(TestConstants.CRN, "");
        ApiAuthRoleMetadataList apiAuthRoleMetadataList = new ApiAuthRoleMetadataList().addItemsItem(
                new ApiAuthRoleMetadata().displayName("ROLE_LIMITED_CLUSTER_ADMIN").uuid("uuid").role("ROLE_LIMITED_CLUSTER_ADMIN"));
        apiAuthRoleMetadataList.addItemsItem(
                new ApiAuthRoleMetadata().displayName("ROLE_ADMIN").uuid("uuid").role("ROLE_ADMIN"));
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(apiAuthRoleMetadataList);
        when(virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN)).thenReturn("virtualGroup");
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig, ldapConfig, virtualGroupRequest);
        // THEN
        ArgumentCaptor<ApiExternalUserMappingList> apiExternalUserMappingListArgumentCaptor = ArgumentCaptor.forClass(ApiExternalUserMappingList.class);
        verify(externalUserMappingsResourceApi).createExternalUserMappings(apiExternalUserMappingListArgumentCaptor.capture());
        ApiExternalUserMapping apiExternalUserMapping = apiExternalUserMappingListArgumentCaptor.getValue().getItems().get(0);
        ApiAuthRoleRef authRole = apiExternalUserMapping.getAuthRoles().get(0);
        assertEquals("ROLE_LIMITED_CLUSTER_ADMIN", authRole.getDisplayName());
        assertEquals("uuid", authRole.getUuid());
        assertEquals("virtualGroup", apiExternalUserMapping.getName());
    }

    @Test
    public void testSetupLdapWithNoRoleAdmin() throws ApiException, ClouderaManagerClientInitException {
        // GIVEN
        ReflectionTestUtils.setField(underTest, "adminRole", "ROLE_CONFIGURATOR");
        ReflectionTestUtils.setField(underTest, "limitedAdminRole", "ROLE_CONFIGURATOR_2");
        ReflectionTestUtils.setField(underTest, "userRole", "ROLE_USER");
        ReflectionTestUtils.setField(underTest, "dashboardUserRole", "ROLE_DASHBOARD_USER");
        LdapView ldapConfig = getLdapConfig();
        when(authRolesResourceApi.readAuthRolesMetadata(null)).thenReturn(new ApiAuthRoleMetadataList().addItemsItem(
                new ApiAuthRoleMetadata().displayName("role").uuid("uuid").role("NO_ROLE_ADMIN")));
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig, ldapConfig, null);
        // THEN
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    @Test
    public void testSetupLdapWithoutLdap() throws ApiException, ClouderaManagerClientInitException {
        // GIVEN
        // WHEN
        underTest.setupLdap(stack, cluster, httpClientConfig, null, null);
        // THEN
        verify(clouderaManagerResourceApi, never()).updateConfig(any(), anyString());
        verify(authRolesResourceApi, never()).readAuthRolesMetadata(anyString());
        verify(externalUserMappingsResourceApi, never()).createExternalUserMappings(any(ApiExternalUserMappingList.class));
    }

    private static LdapView getLdapConfig() {
        return LdapView.LdapViewBuilder.aLdapView()
                .withUserSearchBase("cn=users,dc=example,dc=org")
                .withUserDnPattern("cn={0},cn=users,dc=example,dc=org")
                .withGroupSearchBase("cn=groups,dc=example,dc=org")
                .withBindDn("cn=admin,dc=example,dc=org")
                .withBindPassword("admin")
                .withServerHost("localhost")
                .withUserNameAttribute("cn=admin,dc=example,dc=org")
                .withDomain("ad.hdc.com")
                .withServerPort(389)
                .withProtocol("ldap")
                .withDirectoryType(DirectoryType.LDAP)
                .withUserObjectClass("person")
                .withGroupObjectClass("groupOfNames")
                .withGroupNameAttribute("cn")
                .withGroupMemberAttribute("member")
                .withAdminGroup("ambariadmins")
                .withCertificate("-----BEGIN CERTIFICATE-----certificate-----END CERTIFICATE-----")
                .withConnectionURL("ldap://localhost:389")
                .build();
    }
}
