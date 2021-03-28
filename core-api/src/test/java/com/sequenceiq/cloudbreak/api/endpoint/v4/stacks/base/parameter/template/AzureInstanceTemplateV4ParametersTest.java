package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

class AzureInstanceTemplateV4ParametersTest {

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
                {"encryption=(null, null)", createAzureEncryptionV4Parameters(null, null), false},
                {"encryption=(null, \"\")", createAzureEncryptionV4Parameters(null, ""), false},
                {"encryption=(null, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(null, DISK_ENCRYPTION_SET_ID), false},
                {"encryption=(EncryptionType.NONE, null)", createAzureEncryptionV4Parameters(EncryptionType.NONE, null), false},
                {"encryption=(EncryptionType.NONE, \"\")", createAzureEncryptionV4Parameters(EncryptionType.NONE, ""), false},
                {"encryption=(EncryptionType.NONE, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.NONE, DISK_ENCRYPTION_SET_ID),
                        false},
                {"encryption=(EncryptionType.DEFAULT, null)", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, null), false},
                {"encryption=(EncryptionType.DEFAULT, \"\")", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, ""), false},
                {"encryption=(EncryptionType.DEFAULT, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, DISK_ENCRYPTION_SET_ID), false},
                {"encryption=(EncryptionType.CUSTOM, null)", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, null), true},
                {"encryption=(EncryptionType.CUSTOM, \"\")", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, ""), true},
                {"encryption=(EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID), true},
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
                // testCaseName encryption expectedDiskEncryptionSetId
                {"encryption=null", null, null},
                {"encryption=(null, null)", createAzureEncryptionV4Parameters(null, null), null},
                {"encryption=(null, \"\")", createAzureEncryptionV4Parameters(null, ""), ""},
                {"encryption=(null, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(null, DISK_ENCRYPTION_SET_ID), DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.NONE, null)", createAzureEncryptionV4Parameters(EncryptionType.NONE, null), null},
                {"encryption=(EncryptionType.NONE, \"\")", createAzureEncryptionV4Parameters(EncryptionType.NONE, ""), ""},
                {"encryption=(EncryptionType.NONE, DISK_ENCRYPTION_SET_ID)", createAzureEncryptionV4Parameters(EncryptionType.NONE, DISK_ENCRYPTION_SET_ID),
                        DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.DEFAULT, null)", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, null), null},
                {"encryption=(EncryptionType.DEFAULT, \"\")", createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, ""), ""},
                {"encryption=(EncryptionType.DEFAULT, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.DEFAULT, DISK_ENCRYPTION_SET_ID), DISK_ENCRYPTION_SET_ID},
                {"encryption=(EncryptionType.CUSTOM, null)", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, null), null},
                {"encryption=(EncryptionType.CUSTOM, \"\")", createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, ""), ""},
                {"encryption=(EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID)",
                        createAzureEncryptionV4Parameters(EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID), DISK_ENCRYPTION_SET_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asSecretMapTestDataProvider")
    void asSecretMapTest(String testCaseName, AzureEncryptionV4Parameters encryption, String expectedDiskEncryptionSetId) {
        underTest.setEncryption(encryption);

        Map<String, Object> result = underTest.asSecretMap();
        assertThat(result).isNotNull();
        assertThat(result.get(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).isEqualTo(expectedDiskEncryptionSetId);
    }

    static Object[][] parseTestWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName parameters expectedVolumeEncryptionKeyType expectedDiskEncryptionSetId
                {"parameters=null", null, null, null},
                {"parameters={}", Map.of(), null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE)),
                        null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"false\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "false")),
                        null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"False\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "False")),
                        null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"foo\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "foo")),
                        null, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE)),
                        EncryptionType.CUSTOM, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"true\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "true")),
                        EncryptionType.CUSTOM, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=\"True\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, "True")),
                        EncryptionType.CUSTOM, null},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE, DISK_ENCRYPTION_SET_ID=\"\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "")),
                        null, ""},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.FALSE, DISK_ENCRYPTION_SET_ID=DISK_ENCRYPTION_SET_ID}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.FALSE),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, DISK_ENCRYPTION_SET_ID)),
                        null, DISK_ENCRYPTION_SET_ID},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE, DISK_ENCRYPTION_SET_ID=\"\"}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "")),
                        EncryptionType.CUSTOM, ""},
                {"parameters={MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED=Boolean.TRUE, DISK_ENCRYPTION_SET_ID=DISK_ENCRYPTION_SET_ID}",
                        Map.ofEntries(entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE),
                                entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, DISK_ENCRYPTION_SET_ID)),
                        EncryptionType.CUSTOM, DISK_ENCRYPTION_SET_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parseTestWhenEncryptionDataProvider")
    void parseTestWhenEncryption(String testCaseName, Map<String, Object> parameters, EncryptionType expectedVolumeEncryptionKeyType,
            String expectedDiskEncryptionSetId) {
        underTest.parse(parameters);

        AzureEncryptionV4Parameters encryption = underTest.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedVolumeEncryptionKeyType);
        assertThat(encryption.getDiskEncryptionSetId()).isEqualTo(expectedDiskEncryptionSetId);
    }

    private static AzureEncryptionV4Parameters createAzureEncryptionV4Parameters(EncryptionType type, String diskEncryptionSetId) {
        AzureEncryptionV4Parameters result = new AzureEncryptionV4Parameters();
        result.setType(type);
        result.setDiskEncryptionSetId(diskEncryptionSetId);
        return result;
    }

}