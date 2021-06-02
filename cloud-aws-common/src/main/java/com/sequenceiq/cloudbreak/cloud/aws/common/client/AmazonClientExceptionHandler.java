package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

@Aspect
public class AmazonClientExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonClientExceptionHandler.class);

    private final AwsCredentialView awsCredentialView;

    private final String region;

    private final SdkClientExceptionMapper sdkClientExceptionMapper;

    public AmazonClientExceptionHandler(AwsCredentialView awsCredentialView, String region, SdkClientExceptionMapper sdkClientExceptionMapper) {
        this.awsCredentialView = awsCredentialView;
        this.region = region;
        this.sdkClientExceptionMapper = sdkClientExceptionMapper;
    }

    @Around("execution(* *(..))")
    public Object mapException(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            return proceedingJoinPoint.proceed();
        } catch (SdkClientException e) {
            LOGGER.error("Cannot execute `{}`: {}", proceedingJoinPoint.getSignature().toString(), e.getMessage(), e);
            throw sdkClientExceptionMapper.map(awsCredentialView, region, e, proceedingJoinPoint.getSignature());
        } catch (Throwable e) {
            throw (RuntimeException) e;
        }
    }

}
