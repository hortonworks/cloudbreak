package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesRequest;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ExecutePreTerminationRecipesHandler extends ExceptionCatcherEventHandler<ExecutePreTerminationRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePreTerminationRecipesHandler.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExecutePreTerminationRecipesRequest> event) {
        boolean forced = event.getData().getForced();
        if (!forced) {
            return new StackFailureEvent(StackTerminationEvent.TERMINATION_FAILED_EVENT.event(), resourceId, e, ERROR);
        } else {
            return new ExecutePreTerminationRecipesFinished(event.getData().getResourceId(), forced);
        }
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExecutePreTerminationRecipesRequest> event) {
        ExecutePreTerminationRecipesRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            boolean hasPreterminationRecipe = freeIpaRecipeService.hasRecipeType(stackId, RecipeType.PRE_TERMINATION);
            if (hasPreterminationRecipe) {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(stackId);
                Map<String, List<RecipeModel>> recipeMap = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName)
                        .collect(Collectors.toMap(instanceGroup -> instanceGroup, instanceGroup -> recipes));
                Set<InstanceMetaData> availableInstances = stack.getNotDeletedInstanceMetaDataSet().stream()
                        .filter(instanceMetaData -> !InstanceStatus.STOPPED.equals(instanceMetaData.getInstanceStatus()))
                        .collect(Collectors.toSet());
                if (runPreTerminationRecipesIfAnyNodeAvailable(availableInstances)) {
                    List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, availableInstances);
                    hostOrchestrator.uploadRecipes(gatewayConfigs, recipeMap, StackBasedExitCriteriaModel.nonCancellableModel());
                    Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(availableInstances);
                    LOGGER.info("Executing pre-termination recipes on nodes: {}", allNodes);
                    GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                    hostOrchestrator.preTerminationRecipes(primaryGatewayConfig, allNodes,
                            StackBasedExitCriteriaModel.nonCancellableModel(), request.getForced());
                } else {
                    LOGGER.info("Every instances in deleted or stopped state");
                }
            } else {
                LOGGER.info("We have no pre-termination recipes for this stack");
            }
        } catch (CloudbreakOrchestratorFailedException | CloudbreakOrchestratorTimeoutException e) {
            LOGGER.error("Pre-termination recipe execution failed", e);
            if (request.getForced()) {
                LOGGER.info("Force flag is true, don't care about pre-termination recipe fail");
                return new ExecutePreTerminationRecipesFinished(stackId, request.getForced());
            }
            return new StackFailureEvent(EventSelectorUtil.failureSelector(TerminateStackResult.class), stackId, e, ERROR);
        }
        return new ExecutePreTerminationRecipesFinished(stackId, request.getForced());
    }

    private boolean runPreTerminationRecipesIfAnyNodeAvailable(Set<InstanceMetaData> instanceMetaDatas) {
        return !instanceMetaDatas.isEmpty();
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePreTerminationRecipesRequest.class);
    }

}
