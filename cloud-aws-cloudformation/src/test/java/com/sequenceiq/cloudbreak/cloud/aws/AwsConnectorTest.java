package com.sequenceiq.cloudbreak.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
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
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;

@ExtendWith(MockitoExtension.class)
class AwsConnectorTest {

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private AwsInstanceConnector awsInstanceConnector;

    @Mock
    private AwsMetadataCollector awsMetadataCollector;

    @Mock
    private AwsCredentialConnector awsCredentialConnector;

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private AwsPlatformResources awsPlatformResources;

    @Mock
    private AwsCloudFormationSetup awsSetup;

    @Mock
    private AwsTagValidator awsTagValidator;

    @Mock
    private AwsStackValidator awsStackValidator;

    @Mock
    private AwsAuthenticator awsAuthenticator;

    @Mock
    private AwsConstants awsConstants;

    @Mock
    private AwsNetworkConnector awsNetworkConnector;

    @Mock
    private AwsIdentityService awsIdentityService;

    @Mock
    private AwsObjectStorageConnector awsObjectStorageConnector;

    @Mock
    private AwsNoSqlConnector awsNoSqlConnector;

    @Mock
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Mock
    private AwsStorageValidator awsStorageValidator;

    @Mock
    private AwsResourceVolumeConnector resourceVolumeConnector;

    @Mock
    private List<ConsumptionCalculator> consumptionCalculators;

    @Mock
    private AwsSecretsManagerConnector awsSecretsManagerConnector;

    @Mock
    private AwsEncryptionResources awsEncryptionResources;

    @InjectMocks
    private AwsConnector underTest;

    @Test
    void encryptionResourcesTest() {
        assertThat(underTest.encryptionResources()).isSameAs(awsEncryptionResources);
    }

    @Test
    void secretConnectorTest() {
        assertThat(underTest.secretConnector()).isSameAs(awsSecretsManagerConnector);
    }

}