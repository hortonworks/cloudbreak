package com.sequenceiq.cloudbreak.cloud.azure.policy;

import static com.sequenceiq.cloudbreak.cloud.policy.PolicyPermissionDescriptionsTestUtil.loadDescriptions;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.policy.PolicyPermissionDescriptionsTestUtil.PolicyDescriptions;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * Ensures Azure custom role JSON files stay aligned with their parallel description sidecars under
 * {@code src/main/resources/definitions/*-descriptions.yaml}.
 */
class AzurePolicyPermissionDescriptionsTest {

    private static final List<String> CREDENTIAL_POLICY_FILES = List.of(
            "azure-role-def.json",
            "azure-minimal-role-def.json",
            "azure-service-endpoints-role-def.json",
            "azure-private-endpoints-role-def.json"
    );

    @ParameterizedTest
    @MethodSource("credentialPolicyFiles")
    void policyPermissionsHaveMatchingDescriptions(String policyFile) throws IOException {
        String descriptionsFile = policyFile.replace(".json", "-descriptions.yaml");
        Set<String> policyPermissions = extractPermissions(readClasspath("definitions/" + policyFile));
        PolicyDescriptions descriptions = loadDescriptions(readClasspath("definitions/" + descriptionsFile));

        assertThat(descriptions.policyFile())
                .as("policyFile in %s", descriptionsFile)
                .isEqualTo(policyFile);

        assertThat(descriptions.title()).isNotBlank();
        assertThat(descriptions.summary()).isNotBlank();

        assertThat(descriptions.permissions().keySet())
                .as("permissions documented in %s", descriptionsFile)
                .containsExactlyInAnyOrderElementsOf(policyPermissions);

        descriptions.permissions().values().forEach(description ->
                assertThat(description)
                        .as("description text in %s", descriptionsFile)
                        .isNotBlank());
    }

    private static List<String> credentialPolicyFiles() {
        return CREDENTIAL_POLICY_FILES;
    }

    private static Set<String> extractPermissions(String policyJson) throws IOException {
        Set<String> permissions = new HashSet<>();
        JsonNode root = JsonUtil.readTree(policyJson);
        addArrayElements(permissions, root.get("Actions"));
        addArrayElements(permissions, root.get("DataActions"));
        return permissions;
    }

    private static void addArrayElements(Set<String> permissions, JsonNode arrayNode) {
        assertThat(arrayNode).isNotNull();
        arrayNode.forEach(node -> permissions.add(node.asText()));
    }

    private static String readClasspath(String path) throws IOException {
        try (InputStream in = AzurePolicyPermissionDescriptionsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("classpath resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
