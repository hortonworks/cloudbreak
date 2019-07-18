package com.sequenceiq.it.cloudbreak.dto.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.withoutLogError;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class DistroXTestDto extends DistroXTestDtoBase<DistroXTestDto> implements Purgable<StackV4Response, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    @Inject
    private DistroXTestClient distroXTestClient;

    public DistroXTestDto(TestContext testContext) {
        super(new DistroXV1Request(), testContext);
    }

    @Override
    public DistroXTestDtoBase<DistroXTestDto> valid() {
        return super.valid().withEnvironment(EnvironmentTestDto.class);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(distroXTestClient.delete(), withoutLogError());
        await(STACK_DELETED);
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        DistroXV1Endpoint distroXV1Endpoint = client.getCloudbreakClient().distroXV1Endpoint();
        return distroXV1Endpoint.list(null, null).getResponses().stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    StackV4Response stackResponse = new StackV4Response();
                    stackResponse.setName(s.getName());
                    return stackResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(StackV4Response entity) {
        return entity.getName().startsWith(resourceProperyProvider().prefix());
    }

    @Override
    public void delete(TestContext testContext, StackV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().distroXV1Endpoint().deleteByName(entity.getName(), true);
            testContext.await(this, STACK_DELETED, key("wait-purge-distrox-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<CloudbreakClient> client() {
        return CloudbreakClient.class;
    }

    @Override
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(distroXTestClient.refresh(), key("refresh-distrox-" + getName()));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    public DistroXTestDto withGeneratedBlueprint(GeneratedBlueprintV4Response generatedBlueprint) {
        this.generatedBlueprint = generatedBlueprint;
        return this;
    }

    public GeneratedBlueprintV4Response getGeneratedBlueprint() {
        return generatedBlueprint;
    }
}
