package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.common.api.type.EncryptionType;

class AwsInstanceTemplateV4ParametersTest {

    private static final String ENCRYPTION_KEY = "encryptionKey";

    private AwsInstanceTemplateV4Parameters underTest;

    @BeforeEach
    void setUp() {
        underTest = new AwsInstanceTemplateV4Parameters();
    }

    static Object[][] asMapTestWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName encryption expectedVolumeEncryptionKeyType expectedEbsEncryptionEnabled
                {"encryption=null", null, null, null},
                {"encryption=(null, null)", createAwsEncryptionV4Parameters(null, null), null, false},
                {"encryption=(null, \"\")", createAwsEncryptionV4Parameters(null, ""), null, false},
                {"encryption=(null, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(null, ENCRYPTION_KEY), null, false},
                {"encryption=(EncryptionType.NONE, null)", createAwsEncryptionV4Parameters(EncryptionType.NONE, null), EncryptionType.NONE, false},
                {"encryption=(EncryptionType.NONE, \"\")", createAwsEncryptionV4Parameters(EncryptionType.NONE, ""), EncryptionType.NONE, false},
                {"encryption=(EncryptionType.NONE, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.NONE, ENCRYPTION_KEY), EncryptionType.NONE,
                        false},
                {"encryption=(EncryptionType.DEFAULT, null)", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, null), EncryptionType.DEFAULT, true},
                {"encryption=(EncryptionType.DEFAULT, \"\")", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, ""), EncryptionType.DEFAULT, true},
                {"encryption=(EncryptionType.DEFAULT, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, ENCRYPTION_KEY),
                        EncryptionType.DEFAULT, true},
                {"encryption=(EncryptionType.CUSTOM, null)", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, null), EncryptionType.CUSTOM, true},
                {"encryption=(EncryptionType.CUSTOM, \"\")", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ""), EncryptionType.CUSTOM, true},
                {"encryption=(EncryptionType.CUSTOM, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY),
                        EncryptionType.CUSTOM, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asMapTestWhenEncryptionDataProvider")
    void asMapTestWhenEncryption(String testCaseName, AwsEncryptionV4Parameters encryption, EncryptionType expectedVolumeEncryptionKeyType,
            Boolean expectedEbsEncryptionEnabled) {
        underTest.setEncryption(encryption);

        Map<String, Object> result = underTest.asMap();

        assertThat(result).isNotNull();
        assertThat(result.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(expectedVolumeEncryptionKeyType);
        assertThat(result.get(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(expectedEbsEncryptionEnabled);
    }

    static Object[][] asSecretMapTestDataProvider() {
        return new Object[][]{
                // testCaseName encryption expectedVolumeEncryptionKeyId
                {"encryption=null", null, null},
                {"encryption=(null, null)", createAwsEncryptionV4Parameters(null, null), null},
                {"encryption=(null, \"\")", createAwsEncryptionV4Parameters(null, ""), ""},
                {"encryption=(null, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(null, ENCRYPTION_KEY), ENCRYPTION_KEY},
                {"encryption=(EncryptionType.NONE, null)", createAwsEncryptionV4Parameters(EncryptionType.NONE, null), null},
                {"encryption=(EncryptionType.NONE, \"\")", createAwsEncryptionV4Parameters(EncryptionType.NONE, ""), ""},
                {"encryption=(EncryptionType.NONE, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.NONE, ENCRYPTION_KEY), ENCRYPTION_KEY},
                {"encryption=(EncryptionType.DEFAULT, null)", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, null), null},
                {"encryption=(EncryptionType.DEFAULT, \"\")", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, ""), ""},
                {"encryption=(EncryptionType.DEFAULT, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.DEFAULT, ENCRYPTION_KEY),
                        ENCRYPTION_KEY},
                {"encryption=(EncryptionType.CUSTOM, null)", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, null), null},
                {"encryption=(EncryptionType.CUSTOM, \"\")", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ""), ""},
                {"encryption=(EncryptionType.CUSTOM, ENCRYPTION_KEY)", createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY), ENCRYPTION_KEY},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asSecretMapTestDataProvider")
    void asSecretMapTest(String testCaseName, AwsEncryptionV4Parameters encryption, String expectedVolumeEncryptionKeyId) {
        underTest.setEncryption(encryption);

        Map<String, Object> result = underTest.asSecretMap();
        assertThat(result).isNotNull();
        assertThat(result.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isEqualTo(expectedVolumeEncryptionKeyId);
    }

    static Object[][] parseTestWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName parameters expectedVolumeEncryptionKeyType expectedVolumeEncryptionKeyId
                {"parameters=null", null, null, null},
                {"parameters={}", Map.of(), null, null},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"NONE\"}", Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "NONE")),
                        EncryptionType.NONE, null},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"NONE\", VOLUME_ENCRYPTION_KEY_ID=\"\"}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "NONE"), entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "")),
                        EncryptionType.NONE, ""},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"NONE\", VOLUME_ENCRYPTION_KEY_ID=ENCRYPTION_KEY}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "NONE"),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY)),
                        EncryptionType.NONE, ENCRYPTION_KEY},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"DEFAULT\"}", Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "DEFAULT")),
                        EncryptionType.DEFAULT, null},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"DEFAULT\", VOLUME_ENCRYPTION_KEY_ID=\"\"}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "DEFAULT"), entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "")),
                        EncryptionType.DEFAULT, ""},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"DEFAULT\", VOLUME_ENCRYPTION_KEY_ID=ENCRYPTION_KEY}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "DEFAULT"),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY)),
                        EncryptionType.DEFAULT, ENCRYPTION_KEY},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"CUSTOM\"}", Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "CUSTOM")),
                        EncryptionType.CUSTOM, null},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"CUSTOM\", VOLUME_ENCRYPTION_KEY_ID=\"\"}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "CUSTOM"),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "")),
                        EncryptionType.CUSTOM, ""},
                {"parameters={VOLUME_ENCRYPTION_KEY_TYPE=\"CUSTOM\", VOLUME_ENCRYPTION_KEY_ID=ENCRYPTION_KEY}",
                        Map.ofEntries(entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "CUSTOM"),
                                entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY)),
                        EncryptionType.CUSTOM, ENCRYPTION_KEY},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parseTestWhenEncryptionDataProvider")
    void parseTestWhenEncryption(String testCaseName, Map<String, Object> parameters, EncryptionType expectedVolumeEncryptionKeyType,
            String expectedVolumeEncryptionKeyId) {
        underTest.parse(parameters);

        AwsEncryptionV4Parameters encryption = underTest.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedVolumeEncryptionKeyType);
        assertThat(encryption.getKey()).isEqualTo(expectedVolumeEncryptionKeyId);
    }

    private static AwsEncryptionV4Parameters createAwsEncryptionV4Parameters(EncryptionType type, String key) {
        AwsEncryptionV4Parameters result = new AwsEncryptionV4Parameters();
        result.setType(type);
        result.setKey(key);
        return result;
    }

}