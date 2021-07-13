package com.sequenceiq.cloudbreak.cloud.aws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;

@ExtendWith(MockitoExtension.class)
public class AwsMethodExecutorTest {

    @InjectMocks
    private AwsMethodExecutor underTest;

    @Test
    public void testExecuteWhenNoException() {
        boolean actual = underTest.execute(() -> true, false);

        Assertions.assertTrue(actual);
    }

    @Test
    public void testExecuteWhenHasAmazonEc2ExceptionWithNotFoundErrorCodes() {
        boolean actual = underTest.execute(() -> {
            AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("");
            amazonEC2Exception.setErrorCode("Resource.NotFound");
            throw amazonEC2Exception;
        }, false);

        Assertions.assertFalse(actual);
    }

    @Test
    public void testExecuteWhenHasAmazonEc2ExceptionWithoutNotFoundErrorCodes() {
        AmazonEC2Exception actual = Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.execute(() -> {
            AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("");
            amazonEC2Exception.setErrorCode("Resource.AnyError");
            throw amazonEC2Exception;
        }, false));

        Assertions.assertEquals(actual.getErrorCode(), "Resource.AnyError");
    }
}
