package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;

/**
 * Platform resources.
 */
public interface PlatformResources {

    /**
     * Return the networks in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement (vpcId = will query only that vpdId)
     * @return the {@link CloudNetworks} contains every vpc per region
     */
    CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the sshkeys in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudSshKeys} contains every sshkey per region
     */
    CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the securitygroup in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudSecurityGroups} contains every securitygroup per region
     */
    CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the regions
     * @param region specific region (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudRegions} contains every region per region
     */
    CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the securitygroup in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudVmTypes} contains every vmtype per region
     */
    CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the gateways
     * @param region specific region (if null then the method will query every gateway)
     * @param filters the filter statement
     * @return the {@link CloudGateWay} contains every gateway per region
     */
    CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the ip pool
     * @param region specific region (if null then the method will query ip pool)
     * @param filters the filter statement
     * @return the {@link CloudIpPool} contains every ip pool per region
     */
    CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    /**
     * Return the accessConfigs
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudAccessConfigs} contains every accessrole
     */
    CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception;

    default boolean regionMatch(Region actualRegion, Region expectedRegion) {
        if (expectedRegion != null && !Strings.isNullOrEmpty(expectedRegion.value())) {
            return actualRegion.value().equals(expectedRegion.value());
        } else {
            return true;
        }
    }

    default boolean regionMatch(String actualRegion, Region expectedRegion) {
        if (expectedRegion != null && !Strings.isNullOrEmpty(expectedRegion.value())) {
            return actualRegion.equals(expectedRegion.value());
        } else {
            return true;
        }
    }
}
