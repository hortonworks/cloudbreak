package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPARepairAction implements Action<FreeIPATestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPARepairAction.class);

    private final InstanceMetadataType instanceMetadataType;

    public FreeIPARepairAction(InstanceMetadataType instanceMetadataType) {
        this.instanceMetadataType = instanceMetadataType;
    }

    public FreeIPATestDto action(TestContext testContext, FreeIPATestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getRequest().getEnvironmentCrn()));
        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(true);
        request.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        List<String> instanceIds = testDto.getResponse().getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Collection::stream)
                .filter(im -> instanceMetadataType.equals(im.getInstanceType()))
                .map(InstanceMetaDataResponse::getInstanceId)
                .collect(Collectors.toList());
        request.setInstanceIds(instanceIds);
        Log.whenJson(LOGGER, format(" FreeIPA repair request: %n"), request);
        client.getFreeIpaClient()
                .getFreeIpaV1Endpoint()
                .repairInstances(request);
        return testDto;
    }
}
