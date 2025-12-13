package com.sequenceiq.cloudbreak.cm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class CmMgmtServiceConfigLocationServiceTest {

    private static final String VOLUME_PREFIX = "/hadoopfs/fs1/";

    private static final String EVENTSERVER = "EVENTSERVER";

    private static final String HOSTMONITOR = "HOSTMONITOR";

    private static final String REPORTSMANAGER = "REPORTSMANAGER";

    private static final String SERVICEMONITOR = "SERVICEMONITOR";

    private static final String TEST_CONFIG_NAME = "test_config_name";

    private static final String TEST_ROLE = "test_role";

    private static final String TEST_CONFIG_VALUE = "test_config_value";

    @Spy
    private CmMgmtVolumePathBuilder volumePathBuilder;

    @InjectMocks
    private CmMgmtServiceConfigLocationService underTest;

    @Test
    public void testSetConfigLocationsShouldSetLocationsWhenThereAreNoExistingConfig() {
        Stack stack = createStack();
        ApiRoleList apiRoleList = createApiRoleListWithoutConfig();

        underTest.setConfigs(stack, apiRoleList);

        assertEquals(VOLUME_PREFIX + "cloudera-scm-eventserver", getApiConfig(EVENTSERVER, "eventserver_index_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-host-monitor", getApiConfig(HOSTMONITOR, "firehose_storage_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-scm-headlamp", getApiConfig(REPORTSMANAGER, "headlamp_scratch_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-service-monitor", getApiConfig(SERVICEMONITOR, "firehose_storage_dir", apiRoleList).getValue());
    }

    @Test
    public void testSetConfigLocationsShouldSetLocationsWhenThereAreExistingConfig() {
        Stack stack = createStack();
        ApiRoleList apiRoleList = createApiRoleListWithOtherConfig();

        underTest.setConfigs(stack, apiRoleList);

        assertEquals(VOLUME_PREFIX + "cloudera-scm-eventserver", getApiConfig(EVENTSERVER, "eventserver_index_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-host-monitor", getApiConfig(HOSTMONITOR, "firehose_storage_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-scm-headlamp", getApiConfig(REPORTSMANAGER, "headlamp_scratch_dir", apiRoleList).getValue());
        assertEquals(VOLUME_PREFIX + "cloudera-service-monitor", getApiConfig(SERVICEMONITOR, "firehose_storage_dir", apiRoleList).getValue());
        assertEquals(TEST_CONFIG_VALUE, getApiConfig(TEST_ROLE, TEST_CONFIG_NAME, apiRoleList).getValue());
    }

    private ApiConfig getApiConfig(String roleName, String configName, ApiRoleList apiRoleList) {
        return apiRoleList.getItems().stream()
                .filter(apiRole -> apiRole.getName().equals(roleName))
                .findFirst().get()
                .getConfig().getItems()
                .stream()
                .filter(apiConfig -> apiConfig.getName().equals(configName))
                .findFirst().get();
    }

    private ApiRoleList createApiRoleListWithoutConfig() {
        ApiRoleList apiRoleList = new ApiRoleList();
        apiRoleList.setItems(createApiRolesWithoutConfig(List.of(EVENTSERVER, HOSTMONITOR, REPORTSMANAGER, SERVICEMONITOR)));
        return apiRoleList;
    }

    private ApiRoleList createApiRoleListWithOtherConfig() {
        ApiRoleList apiRoleList = new ApiRoleList();
        apiRoleList.setItems(createApiRolesWithoutConfig(List.of(EVENTSERVER, HOSTMONITOR, REPORTSMANAGER, SERVICEMONITOR)));
        apiRoleList.addItemsItem(createApiRoleWithConfig());
        return apiRoleList;
    }

    private List<ApiRole> createApiRolesWithoutConfig(List<String> names) {
        return names.stream().map(this::createApiRole).collect(Collectors.toList());
    }

    private ApiRole createApiRole(String name) {
        ApiRole apiRole = new ApiRole();
        apiRole.setName(name);
        return apiRole;
    }

    private ApiRole createApiRoleWithConfig() {
        ApiRole apiRole = new ApiRole();
        apiRole.setName(TEST_ROLE);
        ApiConfigList apiConfigList = new ApiConfigList();
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(TEST_CONFIG_NAME);
        apiConfig.setValue(TEST_CONFIG_VALUE);
        apiConfigList.addItemsItem(apiConfig);
        apiRole.setConfig(apiConfigList);
        return apiRole;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setPlatformVariant(CloudConstants.AWS);
        stack.setResources(createResources());
        return stack;
    }

    private Set<Resource> createResources() {
        Resource resource = new Resource();
        resource.setResourceName("volume1");
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        return Set.of(resource);
    }
}