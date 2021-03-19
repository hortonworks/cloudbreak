package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Component
public class SshEnaDriverCheckActions extends SshJClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshEnaDriverCheckActions.class);

    @Inject
    private AwsCloudFunctionality awsCloudFunctionality;

    private void checkEnaDriver(String instanceIP) {
        String modinfoEnaCmd = "/usr/sbin/modinfo ena";
        String wrongResult = "modinfo: ERROR: Module ena not found.";
        Pair<Integer, String> result = executeCommand(instanceIP, modinfoEnaCmd);
        if (result.getValue().startsWith(wrongResult)) {
            throw new TestFailException("Cannot find ena driver on the instance");
        }
    }

    private void checkEnaSupport(String instanceId) {
        Map<String, Boolean> enaSupportResult = awsCloudFunctionality.enaSupport(List.of(instanceId));
        boolean actual = enaSupportResult.values().stream().allMatch(it -> it);
        if (enaSupportResult.isEmpty() || !actual) {
            throw new TestFailException("Ena is not supported on the instance. Result list from aws: " + enaSupportResult);
        }
    }

    /**
     * We need to test the ENA driver in case of AWS.
     * @param stackV4Response the stacj response object, we run the validation against this stack
     * @param client a cpnnection to the server
     */
    public void checkEnaDriverOnAws(StackV4Response stackV4Response, CloudbreakClient client) {
        if (stackV4Response.getCloudPlatform() == CloudPlatform.AWS) {
            InstanceMetaDataV4Response metadata = getInstanceMetadata(stackV4Response.getName(), client, "master");
            checkEnaDriver(metadata.getPrivateIp());
            checkEnaSupport(metadata.getInstanceId());
        }
    }

    private InstanceMetaDataV4Response getInstanceMetadata(String name, CloudbreakClient cloudbreakClient, String group) {
        InstanceMetaDataV4Response instanceMetaDataResponse = cloudbreakClient.getDefaultClient().distroXV1Endpoint()
                .getByName(name, Collections.emptySet())
                .getInstanceGroups()
                .stream()
                .filter(ig -> group.equals(ig.getName()))
                .findFirst()
                .orElseThrow(() -> new TestFailException("Cannot find " + group + " for " + name))
                .getMetadata()
                .stream()
                .findFirst()
                .orElseThrow(() -> new TestFailException("Cannot find metadata in " + group + " for " + name));
        LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]",
                group, instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp());
        return instanceMetaDataResponse;
    }
}
