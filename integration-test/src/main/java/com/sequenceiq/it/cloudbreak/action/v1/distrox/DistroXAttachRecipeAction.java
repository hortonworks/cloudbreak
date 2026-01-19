package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXAttachRecipeAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAttachRecipeAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        InstanceGroupV1Request instanceGroupV1Request = testDto.getRequest().getInstanceGroups().iterator().next();
        instanceGroupV1Request.getRecipeNames().forEach(recipeName -> {
            AttachRecipeV4Request request = new AttachRecipeV4Request();
            request.setRecipeName(recipeName);
            request.setHostGroupName(instanceGroupV1Request.getName());
            AttachRecipeV4Response response = client.getDefaultClient(testContext).distroXV1Endpoint().attachRecipeByName(testDto.getName(), request);
            try {
                Log.whenJson(LOGGER, " attach recipe to stack response:\n", response);
            } catch (IOException ignored) {

            }
        });
        return testDto;
    }
}
