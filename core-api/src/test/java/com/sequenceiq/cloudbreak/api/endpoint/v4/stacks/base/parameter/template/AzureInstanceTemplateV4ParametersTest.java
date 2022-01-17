package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

class AzureInstanceTemplateV4ParametersTest {

    private static final String ENCRYPTION_KEY_URL = "encryptionKeyUrl";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private AzureInstanceTemplateV4Parameters underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureInstanceTemplateV4Parameters();
    }

    static Object[][] asMapTestWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName encryption expectedManagedDiskEncryptionWithCustomKeyEnabled
                {"encryption=null", null, null},
                {"encryption=(null, null, null)", createAzureEncryptionV4Parameters(null, null, null), false},
                {"encryption=(null, \"\", \"\")", createAzureEncryptionV4Parameters(null, "", ""), false},
                {"encryption=(null, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(null, ENCRYPTION_KEY_URL,
                        DISK_ENCRYPTION_SET_ID), false},
                {"encryption=(EncryptionType.NONE, null, null)", createAzureEncryptionV4Parameters(EncryptionType.NONE, null, null), false},
                {"encryption=(EncryptionType.NONE, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.NONE, "", ""), false},
                {"encryption=(EncryptionType.NONE, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.NONE,
                        ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), false},
                {"encryption=(EncryptionType.DEFAULT, null, null)", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, null, null), false},
                {"encryption=(EncryptionType.DEFAULT, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, "", ""), false},
                {"encryption=(EncryptionType.DEFAULT, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), false},
                {"encryption=(EncryptionType.CUSTOM, null, null)", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, null, null), true},
                {"encryption=(EncryptionType.CUSTOM, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, "", ""), true},
                {"encryption=(EncryptionType.CUSTOM, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asMapTestWhenEncryptionDataProvider")
    void asMapTestWhenEncryption(String testCaseName, AzureEncryptionV4Parameters encryption, Boolean expectedManagedDiskEncryptionWithCustomKeyEnabled) {
        underTest.setEncryption(encryption);

        Map<String, Object> result = underTest.asMap();
        assertThat(result).isNotNull();
        assertThat(result.get(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED))
                .isEqualTo(expectedManagedDiskEncryptionWithCustomKeyEnabled);
    }

    static Object[][] asSecretMapTestDataProvider() {
        return new Object[][]{
                // testCaseName encryption expectedEncryptionKeyUrl expectedDiskEncryptionSetId
                {"encryption=null", null, null, null},
                {"encryption=(null, null, null)", createAzureEncryptionV4Parameters(null, null, null), null, null},
                {"encryption=(null, \"\", \"\")", createAzureEncryptionV4Parameters(null, "", ""), "", ""},
                {"encryption=(null, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID, )", createAzureEncryptionV4Parameters(null, ENCRYPTION_KEY_URL,
                        DISK_ENCRYPTION_SET_ID), ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.NONE, null, null)", createAzureEncryptionV4Parameters(EncryptionType.NONE, null, null), null, null},
                {"encryption=(EncryptionType.NONE, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.NONE, "", ""), "", ""},
                {"encryption=(EncryptionType.NONE, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.NONE,
                        ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.DEFAULT, null, null)", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, null, null), null, null},
                {"encryption=(EncryptionType.DEFAULT, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, "", ""), "", ""},
                {"encryption=(EncryptionType.DEFAULT, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT,
                        ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.CUSTOM, null, null)", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, null, null), null, null},
                {"encryption=(EncryptionType.CUSTOM, \"\", \"\")", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, "", ""), "", ""},
                {"encryption=(EncryptionType.CUSTOM, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM,
                        ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID), ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asSecretMapTestDataProvider")
    void asSecretMapTest(String testCaseName, AzureEncryptionV4Parameters encryption, String expectedEncryptionKeyUrl, String expectedDiskEncryptionSetId) {
        underTest.setEncryption(encryption);

        Map<String, Object> result = underTest.asSecretMap();
        assertThat(result).isNotNull();
        assertThat(result.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isEqualTo(expectedEncryptionKeyUrl);
        assertThat(result.get(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).isEqualTo(expectedDiskEncryptionSetId);
    }

    static Object[][] parseTestWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName parameters expectedVolumeEncryptionKeyType expectedEncryptionKeyUrl expectedDiskEncryptionSetId
                {"parameters=null", null, null, null, null},
                {"parameters={}", Map.of(), null, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE)),
                        null, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"false\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "false")),
                        null, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"False\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "False")),
                        null, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"foo\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "foo")),
                        null, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE)),
                        EncryptionType.CUSTOM, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"true\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "true")),
                        EncryptionType.CUSTOM, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"True\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "True")),
                        EncryptionType.CUSTOM, null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE, ENCRYPTION_KEY_URL=\"\", DISK_ENCRYPTION_SET_ID=\"\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ""), entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "")), null, "", ""},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE, ENCRYPTION_KEY_URL=ENCRYPTION_KEY_URL," +
                        "DISK_ENCRYPTION_SET_ID=DISK_ENCRYPTION_SET_ID}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_URL),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, DISK_ENCRYPTION_SET_ID)), null, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE, ENCRYPTION_KEY_URL=\"\", DISK_ENCRYPTION_SET_ID=\"\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ""), entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "")),
                        EncryptionType.CUSTOM, "", ""},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE, ENCRYPTION_KEY_URL=ENCRYPTION_KEY_URL," +
                        "DISK_ENCRYPTION_SET_ID=DISK_ENCRYPTION_SET_ID}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY_URL),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, DISK_ENCRYPTION_SET_ID)),
                        EncryptionType.CUSTOM, ENCRYPTION_KEY_URL, DISK_ENCRYPTION_SET_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parseTestWhenEncryptionDataProvider")
    void parseTestWhenEncryption(String testCaseName, Map<String, Object> parameters, EncryptionType expectedVolumeEncryptionKeyType,
            String expectedEncryptionKeyUrl, String expectedDiskEncryptionSetId) {
        underTest.parse(parameters);

        AzureEncryptionV4Parameters encryption = underTest.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedVolumeEncryptionKeyType);
        assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKeyUrl);
        assertThat(encryption.getDiskEncryptionSetId()).isEqualTo(expectedDiskEncryptionSetId);
    }

    private static AzureEncryptionV4Parameters createAzureEncryptionV4Parameters(EncryptionType type, String encryptionKeyUrl, String diskEncryptionSetId) {
        AzureEncryptionV4Parameters result = new AzureEncryptionV4Parameters();
        result.setType(type);
        result.setKey(encryptionKeyUrl);
        result.setDiskEncryptionSetId(diskEncryptionSetId);
        return result;
    }

}