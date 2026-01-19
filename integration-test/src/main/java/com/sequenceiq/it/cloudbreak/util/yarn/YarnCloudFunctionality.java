package com.sequenceiq.it.cloudbreak.util.yarn;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class YarnCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnCloudFunctionality.class);

    @Inject
    private ServerProperties serverProperties;

    @Override
    public List<String> listInstancesVolumeIds(String clusterName, List<String> instanceIds) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Set<String>> listInstanceVolumeIds(String clusterName, String instanceId) {
        return Collections.emptyMap();
    }

    @Override
    public List<String> listInstanceTypes(String clusterName, List<String> instanceIds) {
        return Collections.emptyList();
    }

    @Override
    public List<String> listVolumeEncryptionKeyIds(String clusterName, String resourceGroupName, List<String> instanceIds) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public void deleteInstances(String clusterName, List<String> instanceIds) {
    }

    @Override
    public void stopInstances(String clusterName, List<String> instanceIds) {
    }

    @Override
    public void cloudStorageInitialize() {
    }

    @Override
    public void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent) {

    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getInstanceSubnetMap(List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return null;
    }

    public String getFreeIpaS3LogsUrl(String crn) {
        if (StringUtils.isNotBlank(serverProperties.getCbVersion())) {
            return format("https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-freeipa-logs/%s/&showversions=false", serverProperties.getCbVersion());
        } else {
            Log.error(LOGGER, format(" CB Version is not available for freeipa '%s'. So cannot determine the version at " +
                    "cluster logs path: https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-freeipa-logs/&showversions=false ", crn));
            return null;
        }
    }

    @Override
    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return null;
    }

    public String getDataLakeS3LogsUrl(String crn) {
        if (StringUtils.isNotBlank(serverProperties.getCbVersion())) {
            return format("https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-sdx-logs/%s/&showversions=false", serverProperties.getCbVersion());
        } else {
            Log.error(LOGGER, format(" CB Version is not available for datalake '%s'. So cannot determine the version at " +
                    "cluster logs path: https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-sdx-logs/&showversions=false ", crn));
            return null;
        }
    }

    @Override
    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return null;
    }

    public String getDataHubS3LogsUrl(String crn) {
        if (StringUtils.isNotBlank(serverProperties.getCbVersion())) {
            return format("https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-distrox-logs/%s/&showversions=false", serverProperties.getCbVersion());
        } else {
            Log.error(LOGGER, format(" CB Version is not available for datahub '%s'. So cannot determine the version at " +
                    "cluster logs path: https://s3.console.aws.amazon.com/s3/buckets/" +
                    "jenkins-e2e-artifacts?region=eu-central-1&prefix=ycloud-distrox-logs/&showversions=false ", crn));
            return null;
        }
    }

    @Override
    public void checkMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {

    }

    @Override
    public Set<String> getVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        return Collections.emptySet();
    }

    @Override
    public void verifyEnaDriver(StackV4Response stackV4Response, CloudbreakClient cloudbreakClient, TestContext testContext) {
    }

    @Override
    public Map<String, String> getLaunchTemplateUserData(String name) {
        return Collections.emptyMap();
    }

    @Override
    public Boolean isCloudFormationExistForStack(String name) {
        return false;
    }

    @Override
    public Boolean isFreeipaCfStackExistForEnvironment(String environmentCrn) {
        return false;
    }
}
