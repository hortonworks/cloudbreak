package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.AUTH_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.OPT_IN_REQUIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.UNAUTHORIZED_OPERATION;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsDefaultRegionSelectionFailed;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

@Service
public class AwsDefaultRegionSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDefaultRegionSelector.class);

    private static final Set<String> UNAUTHORIZED_OPERATIONS = Set.of(AUTH_FAILURE, UNAUTHORIZED_OPERATION, OPT_IN_REQUIRED);

    @Inject
    private AwsPlatformResources platformResources;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsDefaultZoneProvider defaultZoneProvider;

    public String determineDefaultRegion(CloudCredential cloudCredential) {
        String result = null;
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        String originalDefaultRegion = defaultZoneProvider.getDefaultZone(cloudCredential);
        Set<Region> enabledRegions = platformResources.getEnabledRegions();
        LOGGER.debug("Try to describe regions by using the global default region '{}' in EC2.", originalDefaultRegion);
        boolean globalDefaultRegionViable = describeRegionsViaEc2Region(awsCredential, originalDefaultRegion);
        if (!globalDefaultRegionViable && CollectionUtils.isNotEmpty(enabledRegions)) {
            LOGGER.info("Regions could not be described by using the global default region '{}' in EC2. Starting to describe regions with other regions",
                    originalDefaultRegion);
            String regionSelected = enabledRegions.stream()
                    .filter(r -> describeRegionsViaEc2Region(awsCredential, r.getRegionName()))
                    .findFirst()
                    .orElseThrow(() -> {
                        List<String> usedRegions = enabledRegions.stream().map(Region::getRegionName).collect(Collectors.toList());
                        usedRegions.add(originalDefaultRegion);
                        String regions = String.join(",", usedRegions);
                        String msg = String.format("Failed to describe available EC2 regions by configuring SDK to use the following regions: '%s'", regions);
                        LOGGER.warn(msg);
                        return new AwsDefaultRegionSelectionFailed(msg);
                    })
                    .getRegionName();
            if (!originalDefaultRegion.equals(regionSelected)) {
                LOGGER.info("The default region for credential needs to be changed from '{}' to '{}'", originalDefaultRegion, regionSelected);
                result = regionSelected;
            }
        } else if (!globalDefaultRegionViable) {
            String msg = String.format("Failed to describe available EC2 regions in region '%s'", originalDefaultRegion);
            LOGGER.warn(msg);
            throw new AwsDefaultRegionSelectionFailed(msg);
        }
        return result;
    }

    private boolean describeRegionsViaEc2Region(AwsCredentialView awsCredential, String region) {
        boolean regionIsViable = false;
        try {
            LOGGER.debug("Describing regions on EC2 API in '{}'", region);
            AmazonEc2Client access = awsClient.createAccessWithMinimalRetries(awsCredential, region);
            DescribeRegionsRequest describeRegionsRequest = DescribeRegionsRequest.builder().build();
            access.describeRegions(describeRegionsRequest);
            regionIsViable = true;
        } catch (Ec2Exception ec2Exception) {
            String errorMessage = String.format("Unable to describe regions via using EC2 region '%s' APIs, due to: '%s'", region, ec2Exception.getMessage());
            LOGGER.debug(errorMessage, ec2Exception);
            String errorCode = ec2Exception.awsErrorDetails().errorCode();
            if (!UNAUTHORIZED_OPERATIONS.contains(errorCode)) {
                throw ec2Exception;
            }
        } catch (RuntimeException e) {
            String errorMessage = String.format("Unable to describe EC2 regions from AWS: %s", e.getMessage());
            LOGGER.warn(errorMessage, e);
            throw e;
        }
        return regionIsViable;
    }

}
