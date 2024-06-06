package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;

@Service
public class ArnService {

    private static final String SERVICE_IAM = "iam";

    private static final String SERVICE_EC2 = "ec2";

    private static final String RESOURCE_INSTANCE = "instance";

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
        return hasServiceAndResourceType(resourceArn, SERVICE_EC2, RESOURCE_INSTANCE);
    }

    public boolean isSecretsManagerSecretArn(String resourceArn) {
        return hasServiceAndResourceType(resourceArn, "secretsmanager", "secret");
    }

    public String buildEc2InstanceArn(String partition, String region, String accountId, String instanceId) {
        return Arn.builder()
                .partition(partition)
                .service(SERVICE_EC2)
                .region(region)
                .accountId(accountId)
                .resource(RESOURCE_INSTANCE + '/' + instanceId)
                .build()
                .toString();
    }

}
