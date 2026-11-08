package com.sequenceiq.cloudbreak.cloud.gcp.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * The environment tag-modification flow is preceded by a permission pre-flight that checks
 * {@link GcpEnvironmentTagUpdatePermissionValidator#REQUIRED_PERMISSIONS} against the credential's project. A
 * credential following the documented minimal custom role must therefore pass that check, otherwise tag propagation
 * is rejected up front with HTTP 400 even though the customer granted exactly what Cloudera asked for.
 *
 * <p>The default credential binds predefined roles, whose content is not part of this repository, so only the
 * minimal custom role is checked here.</p>
 */
class GcpTagUpdatePermissionCoverageTest {

    private static final String MINIMAL_POLICY = "definitions/gcp-environment-minimal-policy.json";

    @Test
    void minimalPolicyGrantsEveryTagUpdatePermission() throws IOException {
        Set<String> granted = new HashSet<>();
        JsonUtil.readTree(readClasspath(MINIMAL_POLICY)).get("includedPermissions").forEach(node -> granted.add(node.asText()));

        assertThat(granted)
                .as("permissions of %s", MINIMAL_POLICY)
                .containsAll(GcpEnvironmentTagUpdatePermissionValidator.REQUIRED_PERMISSIONS);
    }

    private String readClasspath(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("classpath resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
