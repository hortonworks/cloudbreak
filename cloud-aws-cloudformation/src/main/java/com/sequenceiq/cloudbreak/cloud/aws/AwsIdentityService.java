package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.Arn;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsIdentityService implements IdentityService {

    @Inject
    private LegacyAwsClient awsClient;

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
        GetCallerIdentityResult callerIdentity = stsService.getCallerIdentity(new GetCallerIdentityRequest());
        return callerIdentity.getAccount();
    }
}
