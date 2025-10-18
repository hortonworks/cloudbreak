package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_EXISTING_UPGRADE_COMMAND_EVENT;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeExistingUpgradeCommandValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeExistingUpgradeCommandValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeExistingUpgradeCommandValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeExistingUpgradeCommandValidationHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeExistingUpgradeCommandValidationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade existing upgradeCDH command validation event.");

        ClusterUpgradeExistingUpgradeCommandValidationEvent request = event.getData();

        Image targetImage = request.getImage();
        Long stackId = request.getResourceId();
        StackDto stackDto = getStack(stackId);

        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        Optional<ClusterManagerCommand> optionalUpgradeCommand = connector.clusterStatusService().findCommand(stackDto, ClusterCommandType.UPGRADE_CLUSTER);

        if (optionalUpgradeCommand.isEmpty()) {
            LOGGER.debug("There is no existing upgradeCDH command, validation passed successfully");
            return new ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(stackId);
        } else {
            ClusterManagerCommand upgradeCommand = optionalUpgradeCommand.get();

            if (upgradeCommand.getActive() || !upgradeCommand.getSuccess() && upgradeCommand.getRetryable()) {
                LOGGER.debug("Upgrade command found with id: {}", upgradeCommand.getId());
                return validateIfExistingRuntimeMatchesTargetRuntime(stackDto.getStack(), connector, targetImage);
            } else {
                LOGGER.debug("There is no retryable upgradeCDH command, validation passed successfully");
                return new ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(stackId);
            }
        }
    }

    private StackEvent validateIfExistingRuntimeMatchesTargetRuntime(StackView stack, ClusterApi connector, Image targetImage) {
        String activeRuntimeParcelVersion = getActiveRuntimeParcelVersion(stack, connector);
        String targetRuntimeBuildNumber = getTargetRuntimeBuildNumber(targetImage);
        Long stackId = stack.getId();

        if (StringUtils.isEmpty(activeRuntimeParcelVersion)) {
            String message = "There is an existing upgradeCDH command but active parcel version could not be queried from CM, validation passed successfully";
            LOGGER.debug(message);
            return new ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(stackId);
        } else if (activeRuntimeParcelVersion.endsWith(targetRuntimeBuildNumber)) {
            String message = String.format("There is an existing upgradeCDH command with the same build number %s, validation passed successfully",
                    targetRuntimeBuildNumber);
            LOGGER.debug(message);
            return new ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent(stackId);
        } else {
            String msg = String.format("Existing upgrade command found for active runtime %s, "
                            + "upgrading to a different runtime version (%s-%s) is not allowed! "
                            + "Possible solutions: "
                            + "#1, retry the upgrade with the same target runtime. "
                            + "#2, complete the upgrade manually in Cloudera Manager. "
                            + "#3, recover the cluster",
                    activeRuntimeParcelVersion,
                    getTargetRuntimeVersion(targetImage),
                    targetRuntimeBuildNumber);
            LOGGER.debug(msg);
            return new ClusterUpgradeValidationFailureEvent(stackId, new UpgradeValidationFailedException(msg));
        }
    }

    private String getTargetRuntimeBuildNumber(Image targetImage) {
        return targetImage.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
    }

    private String getTargetRuntimeVersion(Image targetImage) {
        return targetImage.getPackageVersions().get(ImagePackageVersion.STACK.getKey());
    }

    private String getActiveRuntimeParcelVersion(StackView stack, ClusterApi connector) {
        return connector.gatherInstalledParcels(stack.getName())
                .stream()
                .filter(parcel -> parcel.getName().equals(StackType.CDH.name()))
                .findFirst()
                .map(ParcelInfo::getVersion)
                .orElse("");
    }

    private StackDto getStack(Long stackId) {
        return stackDtoService.getById(stackId);
    }

    @Override
    public String selector() {
        return VALIDATE_EXISTING_UPGRADE_COMMAND_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeExistingUpgradeCommandValidationEvent> event) {
        LOGGER.error("Cluster upgrade existing upgradeCDH command validation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
