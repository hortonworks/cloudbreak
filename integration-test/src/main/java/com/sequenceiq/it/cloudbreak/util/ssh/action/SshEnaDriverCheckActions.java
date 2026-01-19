package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Component
public class SshEnaDriverCheckActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshEnaDriverCheckActions.class);

    @Inject
    private AwsCloudFunctionality awsCloudFunctionality;

    @Inject
    private SshJClient sshJClient;

    private void checkEnaDriver(String instanceIp) {
        String modinfoEnaCmd = "/usr/sbin/modinfo ena";
        String wrongResult = "modinfo: ERROR: Module ena not found.";
        Pair<Integer, String> result = sshJClient.executeCommand(instanceIp, modinfoEnaCmd);
        if (result.getValue().startsWith(wrongResult)) {
            LOGGER.error(format("ENA driver is not available at '%s' instance!", instanceIp));
            throw new TestFailException(format("ENA driver is not available at '%s' instance!", instanceIp));
        } else {
            LOGGER.info(format("ENA driver is available at '%s' instance: [%s]", instanceIp, result.getValue()));
            Log.then(LOGGER, format(" ENA driver is available at '%s' instance: [%s] ", instanceIp, result.getValue()));
        }
    }

    private void checkEnaSupport(String instanceId) {
        Map<String, Boolean> enaSupportResult = awsCloudFunctionality.enaSupport(List.of(instanceId));
        boolean actual = enaSupportResult.values().stream().allMatch(it -> it);
        if (enaSupportResult.isEmpty() || !actual) {
            LOGGER.error(format("ENA is not supported at '%s' instance: [%s]", instanceId, enaSupportResult));
            throw new TestFailException(format("ENA is not supported at '%s' instance: [%s] ", instanceId, enaSupportResult));
        } else {
            LOGGER.info(format("ENA is supported at '%s' instance: [%s]", instanceId, enaSupportResult));
            Log.then(LOGGER, format(" ENA is supported at '%s' instance: [%s] ", instanceId, enaSupportResult));
        }
    }

    /**
     * We need to test the ENA driver in case of AWS.
     * @param stackV4Response the stacj response object, we run the validation against this stack
     * @param client a cpnnection to the server
     */
    public void checkEnaDriverOnAws(StackV4Response stackV4Response, CloudbreakClient client, TestContext testContext) {
        if (stackV4Response.getCloudPlatform() == CloudPlatform.AWS) {
            InstanceMetaDataV4Response metadata = getInstanceMetadata(stackV4Response.getName(), client, "master", testContext);
            checkEnaDriver(metadata.getPrivateIp());
            checkEnaSupport(metadata.getInstanceId());
        } else {
            LOGGER.warn(format("ENA driver is only available at AWS. So validation on '%s' provider is not possible!",
                    stackV4Response.getCloudPlatform()));
        }
    }

    private InstanceMetaDataV4Response getInstanceMetadata(String name, CloudbreakClient cloudbreakClient, String group, TestContext testContext) {
        InstanceMetaDataV4Response instanceMetaDataResponse = cloudbreakClient.getDefaultClient(testContext).distroXV1Endpoint()
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
