package com.sequenceiq.cloudbreak.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsIdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsNoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSecretsManagerConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.encryption.AwsEncryptionSdkCryptoConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsGatewaySubnetMultiAzValidator;

@ExtendWith(MockitoExtension.class)
class AwsNativeConnectorTest {

    @Mock
    private AwsConstants awsConstants;

    @Mock
    private AwsCredentialConnector awsCredentialConnector;

    @Mock
    private AwsPlatformResources awsPlatformResources;

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private AwsIdentityService awsIdentityService;

    @Mock
    private AwsObjectStorageConnector awsObjectStorageConnector;

    @Mock
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Mock
    private AwsNoSqlConnector awsNoSqlConnector;

    @Mock
    private AwsAuthenticator awsAuthenticator;

    @Mock
    private AwsInstanceConnector awsInstanceConnector;

    @Mock
    private AwsNativeResourceVolumeConnector resourceVolumeConnector;

    @Mock
    private AwsNativeSetup awsNativeSetup;

    @Mock
    private AwsTagValidator awsTagValidator;

    @Mock
    private AwsGatewaySubnetMultiAzValidator awsGatewaySubnetMultiAzValidator;

    @Mock
    private AwsNativeMetadataCollector nativeMetadataCollector;

    @Mock
    private AwsNativeResourceConnector awsNativeResourceConnector;

    @Mock
    private AwsStorageValidator awsStorageValidator;

    @Mock
    private AwsEncryptionSdkCryptoConnector awsEncryptionSdkCryptoConnector;

    @Mock
    private AwsSecretsManagerConnector awsSecretsManagerConnector;

    @Mock
    private AwsEncryptionResources awsEncryptionResources;

    @InjectMocks
    private AwsNativeConnector underTest;

    @Test
    void encryptionResourcesTest() {
        assertThat(underTest.encryptionResources()).isSameAs(awsEncryptionResources);
    }

    @Test
    void cryptoConnectorTest() {
        assertThat(underTest.cryptoConnector()).isSameAs(awsEncryptionSdkCryptoConnector);
    }

    @Test
    void secretConnectorTest() {
        assertThat(underTest.secretConnector()).isSameAs(awsSecretsManagerConnector);
    }

}