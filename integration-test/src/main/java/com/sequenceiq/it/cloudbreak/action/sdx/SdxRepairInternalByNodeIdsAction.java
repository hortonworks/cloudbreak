package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

public class SdxRepairInternalByNodeIdsAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairInternalByNodeIdsAction.class);

    private final Set<String> hostGroups;

    public SdxRepairInternalByNodeIdsAction(String[] hostGroups) {
        this.hostGroups = Set.of(hostGroups);
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Starting repair by node IDs on SDX Internal: %s for host groups: %s", testDto.getName(), hostGroups));
        SdxClusterDetailResponse detail = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        List<String> nodeIds = detail.getStackV4Response().getInstanceGroups().stream()
                .filter(ig -> hostGroups.contains(ig.getName()))
                .flatMap(ig -> ig.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
        Log.when(LOGGER, format(" Collected %d node IDs for repair: %s", nodeIds.size(), nodeIds));
        SdxRepairRequest repairRequest = new SdxRepairRequest();
        repairRequest.setNodesIds(nodeIds);
        Log.whenJson(LOGGER, " SDX Internal repair by node IDs request: ", repairRequest);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .repairCluster(testDto.getName(), repairRequest);
        testDto.setFlow("SDX Internal repair by node IDs", flowIdentifier);
        testDto.setResponse(client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet()));
        Log.whenJson(LOGGER, " SDX Internal repair response: ", testDto.getResponse());
        return testDto;
    }
}
