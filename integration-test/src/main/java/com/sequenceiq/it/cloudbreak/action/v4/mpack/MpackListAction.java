package com.sequenceiq.it.cloudbreak.action.v4.mpack;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class MpackListAction implements Action<MPackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackListAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.log(LOGGER, "ManagementPack get all");

        Collection<ManagementPackV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .managementPackV4Endpoint()
                .listByWorkspace(cloudbreakClient.getWorkspaceId()).getResponses();

        testDto.setResponses(new HashSet<>(responses));
        Log.logJSON(LOGGER, " ManagementPacks got successfully:\n", testDto.getResponses());

        return testDto;
    }
}
