package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

@Component
public class AwsEnvironmentVariableChecker {

    public boolean isAwsSecretAccessKeyAvailable(AwsCredentialView awsCredentialView) {
        return !isEmpty(getAwsSecretAccessKey(awsCredentialView));
    }

    public String getAwsSecretAccessKey(AwsCredentialView awsCredentialView) {
        return System.getenv(getAwsSecretAccessKeyString(awsCredentialView));
    }

    public String getAwsSecretAccessKeyString(AwsCredentialView awsCredentialView) {
        return awsCredentialView.isGovernmentCloudEnabled() ? "AWS_GOV_SECRET_ACCESS_KEY" : "AWS_SECRET_ACCESS_KEY";
    }

    public boolean isAwsAccessKeyAvailable(AwsCredentialView awsCredentialView) {
        return !isEmpty(System.getenv(getAwsAccessKeyString(awsCredentialView)));
    }

    public String getAwsAccessKey(AwsCredentialView awsCredentialView) {
        return System.getenv(getAwsAccessKeyString(awsCredentialView));
    }

    public String getAwsAccessKeyString(AwsCredentialView awsCredentialView) {
        return awsCredentialView.isGovernmentCloudEnabled() ? "AWS_GOV_ACCESS_KEY_ID" : "AWS_ACCESS_KEY_ID";
    }

}
