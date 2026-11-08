package com.sequenceiq.cloudbreak.cloud.aws.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * The environment tag-modification flow is preceded by a permission pre-flight that simulates
 * {@code cdp/aws-cdp-tag-update-policy.json} against the credential's role. A credential following one of the
 * documented policies must therefore pass that simulation, otherwise tag propagation is rejected up front with
 * HTTP 400 even though the customer granted exactly what Cloudera asked for.
 *
 * <p>Coverage is checked the way {@code AwsCredentialVerifier} simulates: an action counts as granted only if the
 * credential policy allows it in a statement whose condition keys are all supplied by the required statement, since
 * the simulation passes context entries only for the conditions of the policy it verifies.</p>
 */
class AwsTagUpdatePolicyCoverageTest {

    private static final String TAG_UPDATE_POLICY = "definitions/cdp/aws-cdp-tag-update-policy.json";

    private static final List<String> CREDENTIAL_POLICY_FILES = List.of(
            "definitions/aws-environment-minimal-policy.json",
            "definitions/aws-cb-policy.json",
            "definitions/aws-gov-cb-policy.json",
            "definitions/aws-gov-environment-minimal-policy.json"
    );

    @ParameterizedTest
    @MethodSource("credentialPolicyFiles")
    void credentialPolicyGrantsEveryTagUpdateAction(String credentialPolicyFile) throws IOException {
        List<Statement> required = readStatements(TAG_UPDATE_POLICY);
        List<Statement> granted = readStatements(credentialPolicyFile);

        List<String> missing = new ArrayList<>();
        for (Statement requiredStatement : required) {
            requiredStatement.actions().stream()
                    .filter(action -> !isGranted(action, requiredStatement.conditionKeys(), granted))
                    .forEach(missing::add);
        }

        assertThat(missing)
                .as("actions of %s not granted by %s", TAG_UPDATE_POLICY, credentialPolicyFile)
                .isEmpty();
    }

    private static Stream<String> credentialPolicyFiles() {
        return CREDENTIAL_POLICY_FILES.stream();
    }

    private boolean isGranted(String action, Set<String> simulatedConditionKeys, List<Statement> granted) {
        return granted.stream()
                .filter(statement -> simulatedConditionKeys.containsAll(statement.conditionKeys()))
                .flatMap(statement -> statement.actions().stream())
                .anyMatch(grantedAction -> matches(grantedAction, action));
    }

    private boolean matches(String grantedAction, String action) {
        String pattern = Pattern.quote(grantedAction.toLowerCase(Locale.ROOT)).replace("*", "\\E.*\\Q");
        return action.toLowerCase(Locale.ROOT).matches(pattern);
    }

    private List<Statement> readStatements(String policyFile) throws IOException {
        List<Statement> statements = new ArrayList<>();
        for (JsonNode statement : JsonUtil.readTree(readClasspath(policyFile)).get("Statement")) {
            statements.add(new Statement(textValues(statement.get("Action")), conditionKeys(statement.get("Condition"))));
        }
        return statements;
    }

    private Set<String> conditionKeys(JsonNode condition) {
        Set<String> keys = new HashSet<>();
        if (condition != null) {
            condition.forEach(operator -> operator.fieldNames().forEachRemaining(keys::add));
        }
        return keys;
    }

    private Set<String> textValues(JsonNode node) {
        Set<String> values = new HashSet<>();
        if (node != null) {
            if (node.isArray()) {
                node.forEach(element -> values.add(element.asText()));
            } else {
                values.add(node.asText());
            }
        }
        return values;
    }

    private String readClasspath(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("classpath resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Statement(Set<String> actions, Set<String> conditionKeys) {
    }
}
