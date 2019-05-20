package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackNodeUnhealthyAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackNodeUnhealthyAction.class);

    private final String hostgroup;

    private final int nodeCount;

    public StackNodeUnhealthyAction(String hostgroup, int nodeCount) {
        this.hostgroup = hostgroup;
        this.nodeCount = nodeCount;
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack unhealthy request:\n", testDto.getRequest());
        FailureReportV4Request failureReport = new FailureReportV4Request();
        failureReport.setFailedNodes(getNodes(getInstanceGroupResponse(testDto)));
        CloudbreakClient autoscaleClient = testContext.as(Actor::secondUser).getCloudbreakClient(CloudbreakTest.SECONDARY_REFRESH_TOKEN);
        autoscaleClient.getCloudbreakClient().autoscaleEndpoint().failureReport(Objects.requireNonNull(testDto.getResponse().getId()), failureReport);
            Log.logJSON(LOGGER, " Stack unhealthy was successful:\n", testDto.getResponse());
            Log.log(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));
            return testDto;

    }

    private InstanceGroupV4Response getInstanceGroupResponse(StackTestDto entity) {
        return entity.getResponse().getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(hostgroup)).collect(Collectors.toList()).get(0);
    }

    private List<String> getNodes(InstanceGroupV4Response instanceGroup) {
        return instanceGroup.getMetadata().stream()
                .map(InstanceMetaDataV4Response::getDiscoveryFQDN).collect(Collectors.toList()).subList(0, nodeCount);
    }

}