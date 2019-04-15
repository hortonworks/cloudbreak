package com.sequenceiq.it.cloudbreak.newway.action.v4.clusterdefinition;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;

public class ClusterDefinitionListAction implements Action<ClusterDefinitionTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionListAction.class);

    @Override
    public ClusterDefinitionTestDto action(TestContext testContext, ClusterDefinitionTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, format(" Cluster definition list by workspace request:%n"), testDto.getRequest());
        testDto.setViewResponses(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint()
                        .list(client.getWorkspaceId()).getResponses());
        logJSON(LOGGER, format(" Cluster definition list has executed successfully:%n"), testDto.getViewResponses());

        return testDto;
    }

}