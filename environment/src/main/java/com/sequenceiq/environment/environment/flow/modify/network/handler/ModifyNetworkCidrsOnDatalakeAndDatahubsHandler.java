package com.sequenceiq.environment.environment.flow.modify.network.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINISH_MODIFY_NETWORK_CIDRS_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyNetworkCidrsOnDatalakeAndDatahubsHandler extends ExceptionCatcherEventHandler<EnvNetworkCidrsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyNetworkCidrsOnDatalakeAndDatahubsHandler.class);

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvNetworkCidrsModificationEvent> event) {
        Long environmentId = event.getData().getResourceId();
        String environmentName = event.getData().getResourceName();
        String environmentCrn = event.getData().getResourceCrn();
        List<String> networkCidrs = event.getData().getNetworkCidrs();
        try {
            stackService.updateNetworkCidrsForEnvironment(environmentCrn, networkCidrs);
        } catch (Exception e) {
            LOGGER.error("Modify network CIDRs on Data Lake and Data Hubs failed.", e);
            return new EnvNetworkCidrsModificationFailureEvent(environmentId, environmentName, environmentCrn,
                    NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_FAILED, e);
        }
        return EnvNetworkCidrsModificationEvent.builder()
                .withSelector(FINISH_MODIFY_NETWORK_CIDRS_EVENT.name())
                .withResourceId(environmentId)
                .withResourceName(environmentName)
                .withResourceCrn(environmentCrn)
                .withNetworkCidrs(networkCidrs)
                .build();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvNetworkCidrsModificationEvent> event) {
        LOGGER.error("Modify network CIDRs on Data Lake and Data Hubs failed.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvNetworkCidrsModificationFailureEvent(resourceId, resourceName, resourceCrn,
                NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_FAILED, e);
    }
}
