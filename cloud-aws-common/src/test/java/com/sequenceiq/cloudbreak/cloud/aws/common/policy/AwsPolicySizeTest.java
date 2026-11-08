package com.sequenceiq.cloudbreak.cloud.aws.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Customers create these documented policies as IAM customer managed policies, which AWS rejects above
 * 6,144 characters. Whitespace does not count towards that quota.
 */
class AwsPolicySizeTest {

    private static final int MANAGED_POLICY_MAX_LENGTH = 6144;

    @ParameterizedTest
    @ValueSource(strings = {
            "definitions/aws-environment-minimal-policy.json",
            "definitions/aws-gov-environment-minimal-policy.json",
            "definitions/aws-cb-policy.json",
            "definitions/aws-gov-cb-policy.json",
            "definitions/cdp/aws-cdp-tag-update-policy.json"
    })
    void policyFitsIntoManagedPolicySizeQuota(String policyFile) throws IOException {
        String policy = readClasspath(policyFile).replaceAll("\\s", "");

        assertThat(policy.length())
                .as("non-whitespace length of %s", policyFile)
                .isLessThanOrEqualTo(MANAGED_POLICY_MAX_LENGTH);
    }

    private String readClasspath(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("classpath resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
