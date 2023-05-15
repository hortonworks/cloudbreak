package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseResponse;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.operation.OperationService;

@Service
public class RedbeamsUpgradeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private NetworkBuilderService networkBuilderService;

    @Inject
    private OperationService operationService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    public UpgradeDatabaseResponse validateUpgradeDatabaseServer(String crn, UpgradeDatabaseRequest upgradeDatabaseRequest) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());

        MajorVersion currentVersion = dbStack.getMajorVersion();
        TargetMajorVersion targetVersion = upgradeDatabaseRequest.getTargetMajorVersion();

        LOGGER.debug("Validate upgrade called for: {}, with target version: {}, current version is: {}", dbStack, targetVersion, currentVersion);
        Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
        String accountId = dbStack.getAccountId();
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(dbStack.getId())
                .withName(dbStack.getName())
                .withCrn(dbStack.getResourceCrn())
                .withPlatform(dbStack.getCloudPlatform())
                .withVariant(dbStack.getPlatformVariant())
                .withLocation(location)
                .withUserName(dbStack.getUserName())
                .withAccountId(accountId)
                .build();
        Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);

        ResourceConnector resourceConnector = connector.resources();
        try {
            resourceConnector.validateUpgradeDatabaseServer(ac, databaseStack, persistenceNotifier, targetVersion);
            return new UpgradeDatabaseResponse(dbStack.getMajorVersion());
        } catch (Exception ex) {
            LOGGER.warn("RDS upgrade validation failed on provider side", ex);
            return new UpgradeDatabaseResponse(ex.getMessage(), dbStack.getMajorVersion());
        }
    }

    public UpgradeDatabaseResponse upgradeDatabaseServer(String crn, UpgradeDatabaseRequest upgradeDatabaseRequest) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());

        MajorVersion currentVersion = dbStack.getMajorVersion();
        TargetMajorVersion targetVersion = upgradeDatabaseRequest.getTargetMajorVersion();

        LOGGER.debug("Upgrade called for: {}, with target version: {}, current version is: {}", dbStack, targetVersion, currentVersion);

        if (dbStack.getStatus().isUpgradeInProgress()) {
            OperationView operationView = operationService.getOperationProgressByResourceCrn(crn, false);
            if (operationView.getProgressStatus().isRunning()) {
                return handleRunningFlow(crn, currentVersion, operationView);
            } else {
                LOGGER.warn("[INVESTIGATE] DatabaseServer with crn {} has {} status but no running flows so re-triggering the upgrade flow now", crn,
                        dbStack.getStatus());
            }
        } else if (!isUpgradeNeeded(currentVersion, targetVersion)) {
            return handleAlreadyUpgraded(crn, currentVersion);
        }

        networkBuilderService.updateNetworkSubnets(dbStack);
        dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.UPGRADE_REQUESTED);
        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                targetVersion);
        FlowIdentifier flowId = flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest);
        return new UpgradeDatabaseResponse(flowId, currentVersion);
    }

    private UpgradeDatabaseResponse handleAlreadyUpgraded(String crn, MajorVersion currentVersion) {
        String message = String.format("DatabaseServer with crn %s is already on version %s, upgrade is not necessary.",
                crn, currentVersion.getVersion());
        LOGGER.debug(message);
        return new UpgradeDatabaseResponse(message, currentVersion);
    }

    private UpgradeDatabaseResponse handleRunningFlow(String crn, MajorVersion currentVersion, OperationView operationView) {
        String message = String.format("DatabaseServer with crn %s is already being upgraded.", crn);
        LOGGER.debug(message);
        String operationId = operationView.getOperationId();
        List<FlowProgressResponse> operations = operationView.getOperations();
        FlowProgressResponse lastFlowProgress = operations.get(operations.size() - 1);
        FlowIdentifier identifier = StringUtils.isBlank(lastFlowProgress.getFlowChainId()) ?
                new FlowIdentifier(FlowType.FLOW, operationId) :
                new FlowIdentifier(FlowType.FLOW_CHAIN, operationId);
        return new UpgradeDatabaseResponse(message, identifier, currentVersion);
    }

    private boolean isUpgradeNeeded(MajorVersion currentVersion, TargetMajorVersion targetMajorVersion) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        boolean upgradeNeeded = majorVersionComparator.compare(currentVersion, targetMajorVersion.convertToMajorVersion()) < 0;
        LOGGER.debug("Comparing current and target versions. Current version is {}, and target version is {}, isUpgradeNeeded: {}", currentVersion,
                targetMajorVersion, upgradeNeeded);
        return upgradeNeeded;
    }

}
