package com.sequenceiq.cloudbreak.cloud.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;

class UserdataSecretsUtilTest {

    private static final String TEST_USERDATA_WITH_SECRET_SECTION = """
            export NOT_SECRET1="not_a_secret1"
            ###SECRETS-START
            export SECRET1="secret1"
            export SECRET2="secret2"
            ###SECRETS-END
            export NOT_SECRET2="not_a_secret2"
            """;

    private static final String TEST_USERDATA_WITHOUT_SECRET_SECTION = """
            export NOT_SECRET1="not_a_secret1"
            export SECRET1="secret1"
            export SECRET2="secret2"
            export NOT_SECRET2="not_a_secret2"
            """;

    private static final String TEST_SECRET_ID = "testsecretid";

    @Test
    void testReplaceSecretsWithSecretId() {
        String expected = """
                export NOT_SECRET1="not_a_secret1"
                export USERDATA_SECRET_ID="testsecretid"
                export NOT_SECRET2="not_a_secret2"
                """;

        String actual = UserdataSecretsUtil.replaceSecretsWithSecretId(TEST_USERDATA_WITH_SECRET_SECTION, TEST_SECRET_ID);

        assertEquals(expected, actual);
    }

    @Test
    void testReplaceSecretsWithSecretIdWhenNoSecretSection() {
        assertThrows(UserdataSecretsException.class,
                () -> UserdataSecretsUtil.replaceSecretsWithSecretId(TEST_USERDATA_WITHOUT_SECRET_SECTION, TEST_SECRET_ID));
    }

    @Test
    void testGetSecretsSection() {
        String expected = """
            export SECRET1="secret1"
            export SECRET2="secret2"
            """;

        String actual = UserdataSecretsUtil.getSecretsSection(TEST_USERDATA_WITH_SECRET_SECTION);
    }

    @Test
    void testGetSecretsSectionWhenNoSecretSection() {
        assertThrows(UserdataSecretsException.class,
                () -> UserdataSecretsUtil.getSecretsSection(TEST_USERDATA_WITHOUT_SECRET_SECTION));
    }
}
