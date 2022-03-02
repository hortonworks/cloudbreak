package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroXRemoveInstanceAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRemoveInstanceAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if (testDto.getRemovableInstanceId().isPresent()) {
            testDto.setFlow("Instance deletion", client.getDefaultClient()
                    .distroXV1Endpoint()
                    .deleteInstanceByCrn(testDto.getCrn(), false, testDto.getRemovableInstanceId().get()));
            return testDto;
        } else {
            throw new TestFailException("There is no instance id set for removal");
        }
    }
}
