package com.sequenceiq.cloudbreak.cloud.aws.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * Ensures IAM policy JSON files stay aligned with their parallel description sidecars under
 * {@code src/main/resources/definitions/*-descriptions.yaml}.
 */
class AwsPolicyPermissionDescriptionsTest {

    private static final List<String> CREDENTIAL_POLICY_FILES = List.of(
            "aws-environment-minimal-policy.json",
            "aws-cb-policy.json",
            "aws-gov-cb-policy.json",
            "aws-gov-environment-minimal-policy.json"
    );

    @ParameterizedTest
    @MethodSource("credentialPolicyFiles")
    void policyActionsHaveMatchingDescriptions(String policyFile) throws IOException {
        String descriptionsFile = policyFile.replace(".json", "-descriptions.yaml");
        Set<String> policyActions = extractActions(readClasspath("definitions/" + policyFile));
        PolicyDescriptions descriptions = loadDescriptions(readClasspath("definitions/" + descriptionsFile));

        assertThat(descriptions.policyFile())
                .as("policyFile in %s", descriptionsFile)
                .isEqualTo(policyFile);

        assertThat(descriptions.title()).isNotBlank();
        assertThat(descriptions.summary()).isNotBlank();

        assertThat(descriptions.permissions().keySet())
                .as("actions documented in %s", descriptionsFile)
                .containsExactlyInAnyOrderElementsOf(policyActions);

        descriptions.permissions().values().forEach(description ->
                assertThat(description)
                        .as("description text in %s", descriptionsFile)
                        .isNotBlank());
    }

    private static Stream<String> credentialPolicyFiles() {
        return CREDENTIAL_POLICY_FILES.stream();
    }

    private static Set<String> extractActions(String policyJson) throws IOException {
        Set<String> actions = new HashSet<>();
        JsonNode statements = JsonUtil.readTree(policyJson).get("Statement");
        for (JsonNode statement : statements) {
            JsonNode actionNode = statement.get("Action");
            if (actionNode == null) {
                continue;
            }
            if (actionNode.isArray()) {
                actionNode.forEach(node -> actions.add(node.asText()));
            } else {
                actions.add(actionNode.asText());
            }
        }
        return actions;
    }

    @SuppressWarnings("unchecked")
    private static PolicyDescriptions loadDescriptions(String yamlContent) {
        Object loaded = new Yaml().load(yamlContent);
        assertThat(loaded).isInstanceOf(Map.class);
        Map<String, Object> root = (Map<String, Object>) loaded;
        Object permissions = root.get("permissions");
        assertThat(permissions).isInstanceOf(Map.class);
        Map<String, String> permissionDescriptions = ((Map<?, ?>) permissions).entrySet().stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
        return new PolicyDescriptions(
                String.valueOf(root.get("policyFile")),
                String.valueOf(root.get("title")),
                String.valueOf(root.get("summary")),
                permissionDescriptions);
    }

    private static String readClasspath(String path) throws IOException {
        try (InputStream in = AwsPolicyPermissionDescriptionsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("classpath resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record PolicyDescriptions(String policyFile, String title, String summary, Map<String, String> permissions) {
    }
}
