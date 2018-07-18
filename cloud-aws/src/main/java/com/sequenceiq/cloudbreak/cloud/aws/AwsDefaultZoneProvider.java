package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsDefaultZoneProvider {

    @Value("${cb.aws.zone.parameter.default:eu-west-1}")
    private String awsZoneParameterDefault;

    @Value("${cb.aws.gov.zone.parameter.default:us-gov-west-1}")
    private String awsGovZoneParameterDefault;

    public String getDefultZone(CloudCredential credential) {
        return new AwsCredentialView(credential).isGovernmentCloudEnabled() ? awsGovZoneParameterDefault : awsZoneParameterDefault;
    }

}
