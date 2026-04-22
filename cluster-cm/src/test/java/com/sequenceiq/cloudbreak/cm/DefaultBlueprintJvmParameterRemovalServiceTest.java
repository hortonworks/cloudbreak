package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@ExtendWith(MockitoExtension.class)
class DefaultBlueprintJvmParameterRemovalServiceTest {

    private static final String ACCOUNT_ID = "acc-id";

    private static final String CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:someone";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private DefaultBlueprintJvmParameterRemovalService underTest;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ApiClusterTemplate template;

    @BeforeEach
    void setUp() {
        Blueprint blueprint = new Blueprint();
        blueprint.setStatus(ResourceStatus.DEFAULT);
        when(stack.getBlueprint()).thenReturn(blueprint);
        lenient().when(stack.getResourceCrn()).thenReturn(CRN);
        template = new ApiClusterTemplate();
        template.setCdhVersion("7.3.2");
    }

    @Test
    void testDoNotRemoveWhenNotDefaultBlueprint() {
        enableEntitlement();
        stack.getBlueprint().setStatus(ResourceStatus.USER_MANAGED);
        ApiClusterTemplateRoleConfigGroup rcg = setupTemplateWithConfigs(List.of(createConfig("atlas_max_heap_size")));

        underTest.removeJvmPropertiesIfNeeded(stack, template);

        assertEquals(1, rcg.getConfigs().size());
    }

    @Test
    void testDoNotRemoveWhenVersionIsOlder() {
        enableEntitlement();
        template.setCdhVersion("7.3.1");
        ApiClusterTemplateRoleConfigGroup rcg = setupTemplateWithConfigs(List.of(createConfig("atlas_max_heap_size")));

        underTest.removeJvmPropertiesIfNeeded(stack, template);

        assertEquals(1, rcg.getConfigs().size());
    }

    @Test
    void testRemoveJvmPropertiesWhenAllConditionsMet() {
        enableEntitlement();
        ApiClusterTemplateConfig removableConfig = createConfig("atlas_max_heap_size");
        ApiClusterTemplateConfig persistentConfig = createConfig("other_config");
        ApiClusterTemplateRoleConfigGroup rcg = setupTemplateWithConfigs(List.of(removableConfig, persistentConfig));

        underTest.removeJvmPropertiesIfNeeded(stack, template);

        assertEquals(1, rcg.getConfigs().size());
        assertEquals("other_config", rcg.getConfigs().get(0).getName());
    }

    @Test
    void testDoNotRemoveWhenEntitlementDisabled() {
        disableEntitlement();
        ApiClusterTemplateRoleConfigGroup rcg = setupTemplateWithConfigs(List.of(createConfig("atlas_max_heap_size")));

        underTest.removeJvmPropertiesIfNeeded(stack, template);

        assertEquals(1, rcg.getConfigs().size());
    }

    @Test
    void testAllRemovablePropertiesAreFiltered() {
        enableEntitlement();

        List<ApiClusterTemplateConfig> configs = new ArrayList<>(List.of(
                createConfig("atlas_max_heap_size"),
                createConfig("broker_max_heap_size"),
                createConfig("datanode_java_heapsize"),
                createConfig("dfs_datanode_max_locked_memory"),
                createConfig("hbase_regionserver_java_heapsize"),
                createConfig("hive_metastore_java_heapsize"),
                createConfig("hive_metastore_server_max_message_size"),
                createConfig("java.arg.2"),
                createConfig("java.arg.3"),
                createConfig("namenode_java_heapsize"),
                createConfig("solr_java_heapsize"),
                createConfig("solr_java_direct_memory_size"),
                createConfig("zookeeper_server_java_heapsize"),
                createConfig("keep_me")
        ));

        ApiClusterTemplateRoleConfigGroup rcg = setupTemplateWithConfigs(configs);

        underTest.removeJvmPropertiesIfNeeded(stack, template);

        assertEquals(1, rcg.getConfigs().size());
        assertEquals("keep_me", rcg.getConfigs().get(0).getName());
    }

    private ApiClusterTemplateConfig createConfig(String name) {
        return new ApiClusterTemplateConfig().name(name).value("value");
    }

    private ApiClusterTemplateRoleConfigGroup setupTemplateWithConfigs(List<ApiClusterTemplateConfig> configs) {
        ApiClusterTemplateRoleConfigGroup rcg = new ApiClusterTemplateRoleConfigGroup();
        rcg.setConfigs(new ArrayList<>(configs));

        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setRoleConfigGroups(List.of(rcg));
        template.setServices(List.of(service));

        return rcg;
    }

    private void enableEntitlement() {
        lenient().when(entitlementService.isBlueprintJvmParameterRemovalEnabled(ACCOUNT_ID)).thenReturn(true);
    }

    private void disableEntitlement() {
        lenient().when(entitlementService.isBlueprintJvmParameterRemovalEnabled(ACCOUNT_ID)).thenReturn(false);
    }
}