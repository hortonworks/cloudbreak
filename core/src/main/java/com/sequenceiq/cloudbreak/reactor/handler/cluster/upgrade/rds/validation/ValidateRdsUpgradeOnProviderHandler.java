package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;

import reactor.bus.Event;

@Component
public class ValidateRdsUpgradeOnProviderHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradeOnCloudProviderRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateRdsUpgradeOnProviderHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradeOnCloudProviderRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradeOnCloudProviderRequest> event) {
        LOGGER.error("Validating RDS upgrade on cloudprovider side has failed has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradeOnCloudProviderRequest> event) {
        ValidateRdsUpgradeOnCloudProviderRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.info("Validating RDS upgrade on cloudprovider side");
            ClusterView clusterView = stackDtoService.getClusterViewByStackId(stackId);
            UpgradeDatabaseServerV4Request upgradeRequest = new UpgradeDatabaseServerV4Request();
            upgradeRequest.setUpgradeTargetMajorVersion(targetMajorVersionToUpgradeTargetVersionConverter.convert(request.getVersion()));
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> databaseServerV4Endpoint.validateUpgrade(clusterView.getDatabaseServerCrn(), upgradeRequest));
            return new ValidateRdsUpgradeOnCloudProviderResult(stackId);
        } catch (Exception e) {
            LOGGER.warn("Validating RDS upgrade on cloudprovider side has failed", e);
            return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
        }
    }
}
