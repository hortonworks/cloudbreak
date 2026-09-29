package com.sequenceiq.it.cloudbreak.preflight;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.client.EC2Client;
import com.sequenceiq.it.cloudbreak.util.aws.servicequotas.client.AwsServiceQuotasClient;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;
import software.amazon.awssdk.services.servicequotas.model.GetServiceQuotaRequest;
import software.amazon.awssdk.services.servicequotas.model.ServiceQuota;

@Component
public class AwsSecurityGroupQuotaCheck implements PreflightCheck {

    private static final String VPC_SERVICE_CODE = "vpc";

    private static final String SECURITY_GROUPS_PER_REGION_QUOTA_CODE = "L-E79EC296";

    @Value("${integrationtest.preflight.aws.minQuotaHeadroom:25}")
    private int minQuotaHeadroom;

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Inject
    @Qualifier("EC2Client")
    private EC2Client ec2ClientFactory;

    @Inject
    private AwsServiceQuotasClient serviceQuotasClientFactory;

    @Override
    public PreflightCheckResult check() {
        if (CloudPlatform.AWS != endpointResolver.activeCloudPlatform()) {
            return PreflightCheckResult.skip(getName(), "not an AWS run");
        }
        try {
            long currentSecurityGroupCount = countSecurityGroups();
            double limit = fetchSecurityGroupQuotaLimit();
            if (isHeadroomBelowThreshold(limit, currentSecurityGroupCount, minQuotaHeadroom)) {
                return PreflightCheckResult.fail(getName(), buildFailureMessage(limit, currentSecurityGroupCount, minQuotaHeadroom));
            }
            return PreflightCheckResult.pass(getName(), "security group quota OK: " + currentSecurityGroupCount + "/" + limit);
        } catch (AwsServiceException e) {
            return PreflightCheckResult.fail(getName(), "could not check AWS security group quota: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "aws-security-group-quota";
    }

    private long countSecurityGroups() {
        try (Ec2Client ec2Client = ec2ClientFactory.buildEC2Client()) {
            return ec2Client.describeSecurityGroupsPaginator().securityGroups().stream().count();
        }
    }

    private double fetchSecurityGroupQuotaLimit() {
        ServiceQuota quota;
        try (ServiceQuotasClient serviceQuotasClient = serviceQuotasClientFactory.buildServiceQuotasClient()) {
            GetServiceQuotaRequest request = GetServiceQuotaRequest.builder()
                    .serviceCode(VPC_SERVICE_CODE)
                    .quotaCode(SECURITY_GROUPS_PER_REGION_QUOTA_CODE)
                    .build();
            quota = serviceQuotasClient.getServiceQuota(request).quota();
        }
        return quota.value();
    }

    static boolean isHeadroomBelowThreshold(double limit, long currentCount, int minHeadroom) {
        return limit - currentCount < minHeadroom;
    }

    private static String buildFailureMessage(double limit, long currentCount, int minHeadroom) {
        return "AWS VPC security group quota headroom below " + minHeadroom + ": " + currentCount + "/" + limit + " used";
    }
}
