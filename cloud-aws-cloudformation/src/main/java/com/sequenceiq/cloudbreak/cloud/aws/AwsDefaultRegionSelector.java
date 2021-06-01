package com.sequenceiq.cloudbreak.cloud.aws;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Service
public class AwsDefaultRegionSelector {
    static final String EC2_AUTH_FAILURE_ERROR_CODE = "AuthFailure";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDefaultRegionSelector.class);

    @Inject
    private AwsPlatformResources platformResources;

    @Inject
    private AwsClient awsClient;

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
            DescribeRegionsRequest describeRegionsRequest = new DescribeRegionsRequest();
            access.describeRegions(describeRegionsRequest);
            regionIsViable = true;
        } catch (AmazonEC2Exception ec2Exception) {
            String errorMessage = String.format("Unable to describe regions via using EC2 region '%s' APIs, due to: '%s'", region, ec2Exception.getMessage());
            LOGGER.debug(errorMessage, ec2Exception);
            if (!EC2_AUTH_FAILURE_ERROR_CODE.equals(ec2Exception.getErrorCode())) {
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
