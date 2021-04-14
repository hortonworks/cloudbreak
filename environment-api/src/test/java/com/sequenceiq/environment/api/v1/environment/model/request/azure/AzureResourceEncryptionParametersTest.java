package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AzureResourceEncryptionParametersTest {

    private static final String ENCRYPTION_KEY_URL = "encryptionKeyUrl";

    private static final String ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "encryptionKeyResourceGroupName";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    void defaultConstructorTest() {
        AzureResourceEncryptionParameters underTest = new AzureResourceEncryptionParameters();
        underTest.setEncryptionKeyUrl(ENCRYPTION_KEY_URL);
        underTest.setEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME);
        underTest.setDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID);

        verifyFields(underTest);
    }

    private void verifyFields(AzureResourceEncryptionParameters underTest) {
        assertThat(underTest.getEncryptionKeyUrl()).isEqualTo(ENCRYPTION_KEY_URL);
        assertThat(underTest.getEncryptionKeyResourceGroupName()).isEqualTo(ENCRYPTION_KEY_RESOURCE_GROUP_NAME);
        assertThat(underTest.getDiskEncryptionSetId()).isEqualTo(DISK_ENCRYPTION_SET_ID);
    }

    @Test
    void builderTest() {
        AzureResourceEncryptionParameters underTest = AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyUrl(ENCRYPTION_KEY_URL)
                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                .build();

        verifyFields(underTest);
    }

    static Object[][] encryptionKeyUrlValidationTestDataProvider() {
        return new Object[][]{
                // testCaseName encryptionKeyUrl expectedValid
                {"encryptionKeyUrl=null", null, true},
                {"encryptionKeyUrl=ENCRYPTION_KEY_URL", ENCRYPTION_KEY_URL, false},
                {"encryptionKeyUrl=https://myVault.foo.bar/keys/myKey/myVersion", "https://myVault.foo.bar/keys/myKey/myVersion", false},
                {"encryptionKeyUrl=https://myVault.vault.azure.baddomain/keys/myKey/myVersion", "https://myVault.vault.azure.baddomain/keys/myKey/myVersion",
                        false},
                {"encryptionKeyUrl=https://0myInvalidVault.vault.azure.net/keys/myKey/myVersion",
                        "https://0myInvalidVault.vault.azure.net/keys/myKey/myVersion", false},
                {"encryptionKeyUrl=https://myVault.vault.azure.net/keys/myKey/myVersion", "https://myVault.vault.azure.net/keys/myKey/myVersion", true},
                {"encryptionKeyUrl=https://myVault.vault.azure.cn/keys/myKey/myVersion", "https://myVault.vault.azure.cn/keys/myKey/myVersion", true},
                {"encryptionKeyUrl=https://myVault.vault.usgovcloudapi.net/keys/myKey/myVersion",
                        "https://myVault.vault.usgovcloudapi.net/keys/myKey/myVersion", true},
                {"encryptionKeyUrl=https://myVault.vault.microsoftazure.de/keys/myKey/myVersion",
                        "https://myVault.vault.microsoftazure.de/keys/myKey/myVersion", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encryptionKeyUrlValidationTestDataProvider")
    void encryptionKeyUrlValidationTest(String testCaseName, String encryptionKeyUrl, boolean expectedValid) {
        AzureResourceEncryptionParameters underTest = AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyUrl(encryptionKeyUrl)
                .build();

        Set<ConstraintViolation<AzureResourceEncryptionParameters>> constraintViolations = localValidatorFactory.validate(underTest);

        if (expectedValid) {
            assertThat(constraintViolations
                    .stream()
                    .noneMatch(cv -> AzureResourceEncryptionParameters.ENCRYPTION_KEY_URL_INVALID_MSG.equals(cv.getMessage()))
            ).isTrue();
        } else {
            assertThat(constraintViolations
                    .stream()
                    .anyMatch(cv -> AzureResourceEncryptionParameters.ENCRYPTION_KEY_URL_INVALID_MSG.equals(cv.getMessage()))
            ).isTrue();
        }
    }

}