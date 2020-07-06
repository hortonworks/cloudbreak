package com.sequenceiq.datalake.cm;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBulkCommandList;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiClusterList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.datalake.controller.exception.RangerCloudIdentitySyncException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerRangerUtilTest {

    private static final String CLUSTER = "cluster";

    private static final String RANGER_USER_SYNC_ROLE = "ranger_user_sync_role";

    @Mock
    private ClouderaManagerProxiedClientFactory clouderaManagerProxiedClientFactory;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private RolesResourceApi rolesResourceApi;

    @Mock
    private RoleCommandsResourceApi roleCommandsResourceApi;

    @InjectMocks
    private ClouderaManagerRangerUtil underTest;

    private void setupCluster() throws ApiException {
        ApiCluster apiCluster = new ApiCluster();
        apiCluster.setName(CLUSTER);
        ApiClusterList apiClusterList = new ApiClusterList();
        apiClusterList.addItemsItem(apiCluster);
        when(clustersResourceApi.readClusters(null, null)).thenReturn(apiClusterList);
    }

    private void setupRangerUserSyncRole() throws ApiException {
        ApiRole apiRole = new ApiRole();
        apiRole.setType("RANGER_USERSYNC");
        apiRole.setName(RANGER_USER_SYNC_ROLE);
        ApiRoleList roleList = new ApiRoleList();
        roleList.addItemsItem(apiRole);
        when(rolesResourceApi.readRoles(eq(CLUSTER), any(), any(), any())).thenReturn(roleList);
    }

    private void setupRangerUserSyncRoleConig(boolean supportsCloudIdentityConfig) throws ApiException {
        ApiConfigList apiConfigList = new ApiConfigList();
        if (supportsCloudIdentityConfig) {
            ApiConfig apiConfig = new ApiConfig();
            apiConfig.setName("ranger_usersync_azure_user_mapping");
            apiConfigList.addItemsItem(apiConfig);
        } else {
            apiConfigList.setItems(List.of());
        }
        when(rolesResourceApi.readRoleConfig(eq(CLUSTER), eq(RANGER_USER_SYNC_ROLE), anyString(), eq("full"))).thenReturn(apiConfigList);
    }

    private void setupRoleRefreshResponse(boolean success) throws ApiException {
        ApiCommand response = new ApiCommand();
        response.setActive(Boolean.FALSE);
        response.setSuccess(success);
        ApiBulkCommandList apiBulkCommandList = new ApiBulkCommandList();
        apiBulkCommandList.addItemsItem(response);
        when(roleCommandsResourceApi.refreshCommand(eq(CLUSTER), anyString(), any())).thenReturn(apiBulkCommandList);
    }

    @Test
    public void testSetAzureCloudIdentityMapping() throws ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(any())).thenReturn(rolesResourceApi);
        when(clouderaManagerApiFactory.getRoleCommandsResourceApi(any())).thenReturn(roleCommandsResourceApi);
        setupCluster();
        setupRangerUserSyncRole();
        setupRangerUserSyncRoleConig(true);
        setupRoleRefreshResponse(true);

        underTest.setAzureCloudIdentityMapping("stackCrn", Map.of("user01", "val01", "user02", "val02"), Map.of("group01", "val"));

        ArgumentCaptor<ApiConfigList> apiConfigListCaptor = ArgumentCaptor.forClass(ApiConfigList.class);
        verify(rolesResourceApi, times(1)).updateRoleConfig(eq(CLUSTER), eq(RANGER_USER_SYNC_ROLE), anyString(), anyString(), apiConfigListCaptor.capture());

        ApiConfig expectedAzureUserMappingConfig = new ApiConfig();
        expectedAzureUserMappingConfig.setName("ranger_usersync_azure_user_mapping");
        expectedAzureUserMappingConfig.setValue("user01=val01;user02=val02");
        ApiConfig expectedAzureGroupMappingConfig = new ApiConfig();
        expectedAzureGroupMappingConfig.setName("ranger_usersync_azure_group_mapping");
        expectedAzureGroupMappingConfig.setValue("group01=val");

        ApiConfigList apiConfigList = apiConfigListCaptor.getValue();
        assertThat(apiConfigList.getItems(), hasItems(expectedAzureUserMappingConfig, expectedAzureGroupMappingConfig));
    }

    @Test
    public void testSetAzureCloudIdentityMappingUnsuccessfulRefresh() throws ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(any())).thenReturn(rolesResourceApi);
        when(clouderaManagerApiFactory.getRoleCommandsResourceApi(any())).thenReturn(roleCommandsResourceApi);
        setupCluster();
        setupRangerUserSyncRole();
        setupRangerUserSyncRoleConig(true);
        setupRoleRefreshResponse(false);

        assertThrows(RangerCloudIdentitySyncException.class, () -> underTest.setAzureCloudIdentityMapping("stackCrn",
                Map.of("user01", "val01", "user02", "val02"), Map.of("group01", "val")));
    }

    @Test
    public void testSetAzureCloudIdentityMappingUnsupported() throws ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(any())).thenReturn(rolesResourceApi);
        setupCluster();
        setupRangerUserSyncRole();
        setupRangerUserSyncRoleConig(false);

        underTest.setAzureCloudIdentityMapping("stackCrn", Map.of("user01", "val01", "user02", "val02"), Map.of("group01", "val"));

        verify(rolesResourceApi, never()).updateRoleConfig(any(), any(), any(), any(), any());
    }

}