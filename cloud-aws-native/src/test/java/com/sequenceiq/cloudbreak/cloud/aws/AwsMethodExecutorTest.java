package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

@ExtendWith(MockitoExtension.class)
public class AwsMethodExecutorTest {

    @InjectMocks
    private AwsMethodExecutor underTest;

    @Test
    public void testExecuteWhenNoException() {
        boolean actual = underTest.execute(() -> true, false);

        assertTrue(actual);
    }

    @Test
    public void testExecuteWhenHasAmazonEc2ExceptionWithNotFoundErrorCodes() {
        boolean actual = underTest.execute(() -> {
            Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                    .message("")
                    .awsErrorDetails(AwsErrorDetails.builder().errorCode("Resource.NotFound").build())
                    .build();
            throw amazonEC2Exception;
        }, false);

        assertFalse(actual);
    }

    @Test
    public void testExecuteWhenHasAmazonEc2ExceptionWithoutNotFoundErrorCodes() {
        Ec2Exception actual = assertThrows(Ec2Exception.class, () -> underTest.execute(() -> {
            Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                    .message("")
                    .awsErrorDetails(AwsErrorDetails.builder().errorCode("Resource.AnyError").build())
                    .build();
            throw amazonEC2Exception;
        }, false));

        assertEquals(actual.awsErrorDetails().errorCode(), "Resource.AnyError");
    }
}
