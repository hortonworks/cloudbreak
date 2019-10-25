package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;

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
     * Return the virtual machines in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudVmTypes} contains every vmtype per region
     */
    CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the virtual machines in the defined region
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudVmTypes} contains every vmtype per region
     */
    default CloudVmTypes virtualMachinesForDistroX(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return virtualMachines(cloudCredential, region, filters);
    }

    /**
     * Return the gateways
     * @param region specific region (if null then the method will query every gateway)
     * @param filters the filter statement
     * @return the {@link CloudGateWays} contains every gateway per region
     */
    CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the ip pool
     * @param region specific region (if null then the method will query ip pool)
     * @param filters the filter statement
     * @return the {@link CloudIpPools} contains every ip pool per region
     */
    CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the accessConfigs
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudAccessConfigs} contains every accessrole
     */
    CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the encryptionKeys
     * @param region region of the resources (if null then the method will query every region)
     * @param filters the filter statement
     * @return the {@link CloudEncryptionKeys} contains every encryption key
     */
    CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    /**
     * Return the No SQL table list in the requested region
     * @param cloudCredential credentials to connect to the cloud provider
     * @param region region of the resources
     * @param filters not used (reserved)
     * @return the {@link CloudNoSqlTables} contains every No SQL table for the specified region
     */
    CloudNoSqlTables noSqlTables(CloudCredential cloudCredential, Region region, Map<String, String> filters);

    default boolean regionMatch(Region actualRegion, Region expectedRegion) {
        return expectedRegion == null || Strings.isNullOrEmpty(expectedRegion.value()) || actualRegion.value().equals(expectedRegion.value());
    }

    default boolean regionMatch(String actualRegion, Region expectedRegion) {
        return expectedRegion == null || Strings.isNullOrEmpty(expectedRegion.value()) || actualRegion.equals(expectedRegion.value());
    }
}
