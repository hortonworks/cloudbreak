package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;

@Service
public class ArnService {

    private static final String SERVICE_IAM = "iam";

    public boolean isInstanceProfileArn(String resourceArn) {
        return hasServiceAndResourceType(resourceArn, SERVICE_IAM, "instance-profile");
    }

    private boolean hasServiceAndResourceType(String resourceArn, String service, String resourceType) {
        Arn arn = Arn.fromString(resourceArn);
        ArnResource arnResource = arn.resource();
        return hasService(arn, service) && hasResourceType(arnResource, resourceType);
    }

    private boolean hasService(Arn arn, String service) {
        return service.equals(arn.service());
    }

    private boolean hasResourceType(ArnResource arnResource, String resourceType) {
        return arnResource.resourceType()
                .map(resourceType::equals)
                .orElse(false);
    }

    public boolean isRoleArn(String resourceArn) {
        return hasServiceAndResourceType(resourceArn, SERVICE_IAM, "role");
    }

    public boolean isEc2InstanceArn(String resourceArn) {
        return hasServiceAndResourceType(resourceArn, "ec2", "instance");
    }

    public boolean isSecretsManagerSecretArn(String resourceArn) {
        return hasServiceAndResourceType(resourceArn, "secretsmanager", "secret");
    }

}
