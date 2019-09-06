package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBulkCommandList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleNameList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerRoleRefreshServiceTest {

    private static final String STACK_NAME = "myStack";

    private static final String SUMMARY = "SUMMARY";

    private static final String SERVICE_1_NAME = "Service1";

    private static final String SERVICE_2_NAME = "Service2";

    private static final String SERVICE_3_NAME = "Service3";

    private static final String ROLE_2_NAME = "Role2";

    private static final String ROLE_1_NAME = "Role1";

    private static final BigDecimal COMMAND_ID_1 = BigDecimal.valueOf(1);

    private static final BigDecimal COMMAND_ID_2 = BigDecimal.valueOf(2);

    @InjectMocks
    private ClouderaManagerRoleRefreshService underTest;

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private RoleCommandsResourceApi roleCommandsResourceApi;

    private RolesResourceApi rolesResourceApi;

    private ServicesResourceApi servicesResourceApi;

    private ApiClient apiClient;

    @Before
    public void before() {
        roleCommandsResourceApi = mock(RoleCommandsResourceApi.class);
        rolesResourceApi = mock(RolesResourceApi.class);
        servicesResourceApi = mock(ServicesResourceApi.class);
        apiClient = mock(ApiClient.class);
        when(clouderaManagerClientFactory.getRoleCommandsResourceApi(apiClient)).thenReturn(roleCommandsResourceApi);
        when(clouderaManagerClientFactory.getRolesResourceApi(apiClient)).thenReturn(rolesResourceApi);
        when(clouderaManagerClientFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
    }

    @Test
    public void testRestartClusterRolesShouldUpdateTheRoles() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        List<ApiService> apiServices = createApiServices();
        ApiServiceList apiServiceList = createApiServiceList(apiServices);
        ApiRoleList apiRoleList = createApiRoleList();
        ApiBulkCommandList apiBulkCommandList = createApiBulkCommandList();

        when(servicesResourceApi.readServices(STACK_NAME, SUMMARY)).thenReturn(apiServiceList);
        when(rolesResourceApi.readRoles(STACK_NAME, SERVICE_1_NAME, null, null)).thenReturn(apiRoleList);
        when(rolesResourceApi.readRoles(STACK_NAME, SERVICE_2_NAME, null, null)).thenReturn(apiRoleList);
        when(rolesResourceApi.readRoles(STACK_NAME, SERVICE_3_NAME, null, null)).thenReturn(apiRoleList);
        when(roleCommandsResourceApi.refreshCommand(eq(STACK_NAME), eq(SERVICE_1_NAME), any(ApiRoleNameList.class))).thenReturn(apiBulkCommandList);
        when(roleCommandsResourceApi.refreshCommand(eq(STACK_NAME), eq(SERVICE_2_NAME), any(ApiRoleNameList.class))).thenReturn(apiBulkCommandList);
        when(roleCommandsResourceApi.refreshCommand(eq(STACK_NAME), eq(SERVICE_3_NAME), any(ApiRoleNameList.class))).thenReturn(apiBulkCommandList);
        when(clouderaManagerPollingServiceProvider.refreshClusterPollingService(stack, apiClient, COMMAND_ID_1)).thenReturn(PollingResult.SUCCESS);
        when(clouderaManagerPollingServiceProvider.refreshClusterPollingService(stack, apiClient, COMMAND_ID_2)).thenReturn(PollingResult.SUCCESS);

        underTest.refreshClusterRoles(apiClient, stack);

        verify(servicesResourceApi).readServices(STACK_NAME, SUMMARY);
        verify(rolesResourceApi).readRoles(STACK_NAME, SERVICE_1_NAME, null, null);
        verify(rolesResourceApi).readRoles(STACK_NAME, SERVICE_2_NAME, null, null);
        verify(rolesResourceApi).readRoles(STACK_NAME, SERVICE_3_NAME, null, null);
        verify(roleCommandsResourceApi).refreshCommand(eq(STACK_NAME), eq(SERVICE_1_NAME), any(ApiRoleNameList.class));
        verify(roleCommandsResourceApi).refreshCommand(eq(STACK_NAME), eq(SERVICE_2_NAME), any(ApiRoleNameList.class));
        verify(roleCommandsResourceApi).refreshCommand(eq(STACK_NAME), eq(SERVICE_3_NAME), any(ApiRoleNameList.class));
        verify(clouderaManagerPollingServiceProvider, times(3)).refreshClusterPollingService(stack, apiClient, COMMAND_ID_1);
        verify(clouderaManagerPollingServiceProvider, times(3)).refreshClusterPollingService(stack, apiClient, COMMAND_ID_2);
    }

    private List<ApiCommand> createApiCommandList() {
        return List.of(createApiCommand(COMMAND_ID_1), createApiCommand(COMMAND_ID_2));
    }

    private ApiCommand createApiCommand(BigDecimal id) {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(id);
        return apiCommand;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        return stack;
    }

    private ApiServiceList createApiServiceList(List<ApiService> apiServices) {
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);
        return apiServiceList;
    }

    private List<ApiService> createApiServices() {
        List<ApiService> services = new ArrayList<>();
        services.add(createApiService(SERVICE_1_NAME));
        services.add(createApiService(SERVICE_2_NAME));
        services.add(createApiService(SERVICE_3_NAME));
        return services;
    }

    private ApiService createApiService(String name) {
        ApiService apiService = new ApiService();
        apiService.setName(name);
        return apiService;
    }

    private ApiRoleList createApiRoleList() {
        ApiRoleList apiRoleList = new ApiRoleList();
        apiRoleList.setItems(List.of(createApiRole(ROLE_1_NAME), createApiRole(ROLE_2_NAME)));
        return apiRoleList;
    }

    private ApiRole createApiRole(String name) {
        ApiRole apiRole = new ApiRole();
        apiRole.setName(name);
        return apiRole;
    }

    private ApiBulkCommandList createApiBulkCommandList() {
        ApiBulkCommandList apiBulkCommandList = new ApiBulkCommandList();
        apiBulkCommandList.setItems(createApiCommandList());
        return apiBulkCommandList;
    }
}