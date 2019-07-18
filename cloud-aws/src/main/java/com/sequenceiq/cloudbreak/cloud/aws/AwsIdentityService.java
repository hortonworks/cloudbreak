package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.util.Arn;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsIdentityService implements IdentityService {

    @Override
    public String getAccountId(CloudCredential cloudCredential) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        if (awsCredentialView.getKeyBased() != null) {
            return getAccountIDUsingAccessKey(awsCredentialView.getAccessKey(), awsCredentialView.getSecretKey());
        }
        return Arn.of(awsCredentialView.getRoleArn()).getAccountId();
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

    private String getAccountIDUsingAccessKey(String accessKey, String secretKey) {
        AWSSecurityTokenService stsService = AWSSecurityTokenServiceClientBuilder.standard().withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))).build();

        GetCallerIdentityResult callerIdentity = stsService.getCallerIdentity(new GetCallerIdentityRequest());
        return callerIdentity.getAccount();
    }
}
