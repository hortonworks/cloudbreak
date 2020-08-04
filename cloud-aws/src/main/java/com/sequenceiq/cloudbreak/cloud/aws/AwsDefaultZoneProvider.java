package com.sequenceiq.cloudbreak.cloud.aws;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsDefaultZoneProvider {

    @Value("${cb.aws.zone.parameter.default:us-east-1}")
    private String awsZoneParameterDefault;

    @Value("${cb.aws.gov.zone.parameter.default:us-gov-west-1}")
    private String awsGovZoneParameterDefault;

    public String getDefaultZone(CloudCredential credential) {
        return getDefaultZone(new AwsCredentialView(credential));
    }

    public String getDefaultZone(AwsCredentialView awsCredentialView) {
        return awsCredentialView.isGovernmentCloudEnabled() ? awsGovZoneParameterDefault : getCredentialOrGlobalDefault(awsCredentialView);
    }

    private String getCredentialOrGlobalDefault(AwsCredentialView credentialView) {
        String credentialDefaultRegion = credentialView.getDefaultRegion();
        if (StringUtils.isNoneEmpty(credentialDefaultRegion)) {
            return credentialDefaultRegion;
        }
        return awsZoneParameterDefault;
    }

}
