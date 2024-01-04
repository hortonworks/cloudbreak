package com.sequenceiq.cloudbreak.cloud.aws.common;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.Arn;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

@Service
public class AwsIdentityService implements IdentityService {

    @Inject
    private CommonAwsClient awsClient;

    @Override
    public String getAccountId(String region, CloudCredential cloudCredential) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        if (awsCredentialView.getKeyBased() != null) {
            return getAccountIdUsingAccessKey(region, awsCredentialView);
        }
        return Arn.of(awsCredentialView.getRoleArn()).getAccountId();
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    private String getAccountIdUsingAccessKey(String region, AwsCredentialView awsCredentialView) {
        AmazonSecurityTokenServiceClient stsService = awsClient.createSecurityTokenService(awsCredentialView, region);
        return stsService.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
    }
}
