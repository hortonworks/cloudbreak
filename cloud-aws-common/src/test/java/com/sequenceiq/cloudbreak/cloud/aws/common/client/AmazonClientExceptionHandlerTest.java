package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;

@ExtendWith(MockitoExtension.class)
class AmazonClientExceptionHandlerTest {

    private static final RuntimeException MAPPED_EXCEPTION = new RuntimeException("mappedException");

    private static final String REGION = "us-west-1";

    @Mock
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    private Ec2Client mockAmazonClient;

    @BeforeEach
    void setUp() {
        lenient().when(sdkClientExceptionMapper.map(any(), eq(REGION), any(), any())).thenReturn(MAPPED_EXCEPTION);

        Ec2Client ec2 = mock(Ec2Client.class);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(ec2);
        proxyFactory.addAspect(new AmazonClientExceptionHandler(mock(AwsCredentialView.class), REGION, sdkClientExceptionMapper));
        this.mockAmazonClient = proxyFactory.getProxy();
    }

    @Test
    void testRuntimeExceptionIsNotMapped() {
        RuntimeException thrownException = new RuntimeException("RuntimeException");
        when(mockAmazonClient.describeRouteTables()).thenThrow(thrownException);

        Assertions.assertThatThrownBy(() -> mockAmazonClient.describeRouteTables())
                .isEqualTo(thrownException);
    }

    @Test
    void testSdkClientExceptionIsMapped() {
        when(mockAmazonClient.describeRouteTables()).thenThrow(SdkClientException.builder().build());

        Assertions.assertThatThrownBy(() -> mockAmazonClient.describeRouteTables())
                .isEqualTo(MAPPED_EXCEPTION);
    }

}
