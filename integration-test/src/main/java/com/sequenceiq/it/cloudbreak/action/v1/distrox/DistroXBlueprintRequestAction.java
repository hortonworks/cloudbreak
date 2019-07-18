package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static com.sequenceiq.it.cloudbreak.log.Log.log;
import static com.sequenceiq.it.cloudbreak.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

public class DistroXBlueprintRequestAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXBlueprintRequestAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, " Stack get blueprint:\n", testDto.getRequest());
        GeneratedBlueprintV4Response bp = client.getCloudbreakClient().distroXV1Endpoint().postStackForBlueprintByName(
                testDto.getName(),
                testDto.getRequest());
        testDto.withGeneratedBlueprint(bp);
        logJSON(LOGGER, " get blueprint was successfully:\n", testDto.getGeneratedBlueprint());
        return testDto;
    }
}
