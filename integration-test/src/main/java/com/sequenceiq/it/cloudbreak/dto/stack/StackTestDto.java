package com.sequenceiq.it.cloudbreak.dto.stack;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class StackTestDto extends StackTestDtoBase<StackTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Searchable, Investigable, Assignable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    private AttachRecipeV4Request attachRecipeV4Request;

    @Inject
    private StackTestClient stackTestClient;

    public StackTestDto(TestContext testContext) {
        super(testContext);
    }

    @Override
    public StackTestDtoBase<StackTestDto> valid() {
        return super.valid().withEnvironmentClass(EnvironmentTestDto.class);
    }

    @Override
    public void deleteForCleanup() {
        try {
            CloudbreakClient client = getClientForCleanup();
            client.getDefaultClient(getTestContext()).stackV4Endpoint().delete(0L, getName(), true, Crn.fromString(getCrn()).getAccountId());
            awaitWithClient(STACK_DELETED, client);
        } catch (NotFoundException nfe) {
            LOGGER.info("resource not found, thus cleanup not needed.");
        } catch (NullPointerException npe) {
            LOGGER.info("there is no client");
        }
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        StackV4Endpoint stackEndpoint = client.getDefaultClient(getTestContext()).stackV4Endpoint();
        return stackEndpoint.list(client.getWorkspaceId(), null, false).getResponses().stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    StackV4Response stackResponse = new StackV4Response();
                    stackResponse.setName(s.getName());
                    return stackResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(StackV4Response entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, StackV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient(getTestContext()).stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true,
                    Crn.fromString(entity.getCrn()).getAccountId());
            testContext.await(this, STACK_DELETED, key("wait-purge-stack-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<CloudbreakClient> client() {
        return CloudbreakClient.class;
    }

    @Override
    public CloudbreakTestDto refresh() {
        return when(stackTestClient.refreshV4(), key("refresh-stack-" + getName()).withSkipOnFail(false).withLogError(false));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    public StackTestDto withGeneratedBlueprint(GeneratedBlueprintV4Response generatedBlueprint) {
        this.generatedBlueprint = generatedBlueprint;
        return this;
    }

    public GeneratedBlueprintV4Response getGeneratedBlueprint() {
        return generatedBlueprint;
    }

    public StackTestDto withAttachedRecipe(String hostGroup, String recipeName) {
        attachRecipeV4Request = new AttachRecipeV4Request();
        attachRecipeV4Request.setHostGroupName(hostGroup);
        attachRecipeV4Request.setRecipeName(recipeName);
        return this;
    }

    public AttachRecipeV4Request getAttachRecipeV4Request() {
        return attachRecipeV4Request;
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null || getResponse().getId() == null) {
            return null;
        }
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                getResponse().getId(),
                null,
                getTestContext());
        boolean hasSpotTermination = (getResponse().getInstanceGroups() == null) ? false : getResponse().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        return new Clue(
                getResponse().getName(),
                getResponse().getCrn(),
                null,
                null,
                auditEvents,
                List.of(),
                getResponse(),
                hasSpotTermination);
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public String getCrn() {
        return Optional.ofNullable(getResponse()).map(response -> getResponse().getCrn()).orElse(null);
    }
}
