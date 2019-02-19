package com.sequenceiq.it.cloudbreak.newway.action.stack;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakTest.SECONDARY_REFRESH_TOKEN;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.actor.Actor;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackNodeUnhealthyAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackNodeUnhealthyAction.class);

    private final String hostgroup;

    private final int nodeCount;

    public StackNodeUnhealthyAction(String hostgroup, int nodeCount) {
        this.hostgroup = hostgroup;
        this.nodeCount = nodeCount;
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Stack unhealthy request:\n", entity.getRequest());
        FailureReportV4Request failureReport = new FailureReportV4Request();
        failureReport.setFailedNodes(getNodes(getInstanceGroupResponse(entity)));
        CloudbreakClient autoscaleClient = testContext.as(Actor::secondUser).getCloudbreakClient(SECONDARY_REFRESH_TOKEN);
        autoscaleClient.getCloudbreakClient().autoscaleEndpoint().failureReport(Objects.requireNonNull(entity.getResponse().getId()), failureReport);
            logJSON(LOGGER, " Stack unhealthy was successful:\n", entity.getResponse());
            log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
            return entity;

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