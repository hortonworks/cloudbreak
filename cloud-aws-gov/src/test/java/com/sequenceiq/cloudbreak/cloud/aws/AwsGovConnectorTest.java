package com.sequenceiq.cloudbreak.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsGatewaySubnetMultiAzValidator;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsGovSecretEncryptionValidator;

@ExtendWith(MockitoExtension.class)
class AwsGovConnectorTest {

    @Mock
    private AwsTagValidator awsTagValidator;

    @Mock
    private AwsGatewaySubnetMultiAzValidator awsGatewaySubnetMultiAzValidator;

    @Mock
    private AwsStorageValidator awsStorageValidator;

    @Mock
    private AwsGovSecretEncryptionValidator awsGovSecretEncryptionValidator;

    @InjectMocks
    private AwsGovConnector underTest;

    @Test
    void testValidatorsImage() {
        assertThat(underTest.validators(ValidatorType.IMAGE)).containsExactly();
    }

    @Test
    void testValidatorsAll() {
        assertThat(underTest.validators(ValidatorType.ALL))
                .containsExactly(awsTagValidator, awsGatewaySubnetMultiAzValidator, awsStorageValidator, awsGovSecretEncryptionValidator);
    }
}
