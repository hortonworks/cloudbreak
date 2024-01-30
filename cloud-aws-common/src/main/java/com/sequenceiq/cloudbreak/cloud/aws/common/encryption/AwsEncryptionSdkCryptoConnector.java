package com.sequenceiq.cloudbreak.cloud.aws.common.encryption;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.sequenceiq.cloudbreak.cloud.CryptoConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cloud.model.encryption.UnexpectedKeyTypeException;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.KmsException;
import software.amazon.cryptography.materialproviders.IKeyring;
import software.amazon.cryptography.materialproviders.MaterialProviders;
import software.amazon.cryptography.materialproviders.model.CreateAwsKmsKeyringInput;
import software.amazon.cryptography.materialproviders.model.MaterialProvidersConfig;

@Service
public class AwsEncryptionSdkCryptoConnector implements CryptoConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEncryptionSdkCryptoConnector.class);

    @Inject
    private AwsCrypto awsCrypto;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private Retry retryService;

    @Override
    public byte[] encrypt(byte[] input, EncryptionKeySource keySource, CloudCredential cloudCredential,
            String regionName, Map<String, String> encryptionContext) {
        if (keySource.keyType() == EncryptionKeyType.AWS_KMS_KEY_ARN) {
            String kmsKeyArn = keySource.keyValue();
            IKeyring keyring = getKeyring(kmsKeyArn, cloudCredential, regionName);
            LOGGER.debug("Encrypting with keySource {}...", keySource);
            return retryService.testWith1SecDelayMax5Times(() -> encrypt(input, encryptionContext, keyring));
        } else {
            throw new UnexpectedKeyTypeException(String.format("Expected a key source of type %s, but got %s.",
                    EncryptionKeyType.AWS_KMS_KEY_ARN, keySource.keyType()));
        }
    }

    private byte[] encrypt(byte[] input, Map<String, String> encryptionContext, IKeyring keyring) {
        try {
            CryptoResult<byte[], ?> cryptoResult = awsCrypto.encryptData(keyring, input, encryptionContext);
            return cryptoResult.getResult();
        } catch (KmsException kmsException) {
            LOGGER.error("Failed to encrypt secret!", kmsException);
            throw kmsException;
        }
    }

    private IKeyring getKeyring(String keySource, CloudCredential cloudCredential, String regionName) {
        MaterialProviders materialProviders = MaterialProviders.builder()
                .MaterialProvidersConfig(MaterialProvidersConfig.builder().build())
                .build();
        KmsClient kmsClient = commonAwsClient.createKmsClient(new AwsCredentialView(cloudCredential), regionName);
        CreateAwsKmsKeyringInput kmsKeyringInput = CreateAwsKmsKeyringInput.builder()
                .kmsClient(kmsClient)
                .kmsKeyId(keySource)
                .build();
        IKeyring keyring = materialProviders.CreateAwsKmsKeyring(kmsKeyringInput);
        LOGGER.debug("Created keyring with key ARN: {}.", keySource);
        return keyring;
    }

    @Bean
    public AwsCrypto getAwsCrypto() {
        return AwsCrypto.standard();
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
