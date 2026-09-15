package com.sequenceiq.cloudbreak.cloud.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class PolicyPermissionDescriptionsTestUtil {

    private PolicyPermissionDescriptionsTestUtil() {
    }

    @SuppressWarnings("unchecked")
    public static PolicyDescriptions loadDescriptions(String yamlContent) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object loaded = new Yaml(new SafeConstructor(options)).load(yamlContent);
        assertThat(loaded).isInstanceOf(Map.class);
        Map<String, Object> root = (Map<String, Object>) loaded;
        Object groups = root.get("permissionGroups");
        assertThat(groups).isInstanceOf(Map.class);
        Map<String, Object> permissionGroups = (Map<String, Object>) groups;
        assertThat(permissionGroups).isNotEmpty();
        Map<String, String> permissionDescriptions = new HashMap<>();
        permissionGroups.forEach((name, value) -> {
            assertThat(name).isNotBlank();
            assertThat(value).isInstanceOf(Map.class);
            Map<String, Object> group = (Map<String, Object>) value;
            assertThat(group.get("description")).isInstanceOf(String.class);
            assertThat((String) group.get("description")).as("description of group %s", name).isNotBlank();
            assertThat(group.get("permissions")).isInstanceOf(Map.class);
            Map<String, String> permissions = (Map<String, String>) group.get("permissions");
            assertThat(permissions).as("permissions in group %s", name).isNotEmpty();
            permissions.forEach((permission, description) -> {
                assertThat(permissionDescriptions).as("permission assigned to multiple groups").doesNotContainKey(permission);
                permissionDescriptions.put(permission, description);
            });
        });
        return new PolicyDescriptions(
                String.valueOf(root.get("policyFile")),
                String.valueOf(root.get("title")),
                String.valueOf(root.get("summary")),
                permissionDescriptions);
    }

    public record PolicyDescriptions(String policyFile, String title, String summary, Map<String, String> permissions) {
    }
}
