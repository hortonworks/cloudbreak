package com.sequenceiq.environment.environment.flow.hybrid.setup.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINISH_TRUST_SETUP_EVENT;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2ActiveDirectoryRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2MitRequest;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2Request;

@Component
public class EnvironmentCrossRealmTrustSetupHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    private final EnvironmentService environmentService;

    protected EnvironmentCrossRealmTrustSetupHandler(
        FreeIpaService freeIpaService,
        FreeIpaPollerService freePollerIpaService,
        EnvironmentService environmentService
    ) {
        this.freeIpaService = freeIpaService;
        freeIpaPollerService = freePollerIpaService;
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return TRUST_SETUP_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFailedEvent(event.getData(), e, TRUST_SETUP_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustSetupHandler.accept");
        EnvironmentCrossRealmTrustSetupEvent data = event.getData();
        try {
            environmentService.updateRemoteEnvironmentCrn(
                    data.getAccountId(),
                    data.getResourceCrn(),
                    data.getRemoteEnvironmentCrn());

            Optional<DescribeFreeIpaResponse> describe = freeIpaService.describe(data.getResourceCrn());
            if (describe.isPresent()) {
                DescribeFreeIpaResponse freeIpa = describe.get();
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, cross realm trust setup interrupted.");
                } else if (!freeIpa.getStatus().isCrossRealmPreparable()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to cross realm trust setup. Current state is: " +
                            freeIpa.getStatus().name());
                } else {
                    LOGGER.info("FreeIPA will be cross realm trust setup.");
                    freeIpaPollerService.waitForCrossRealmTrustSetup(
                            data.getResourceId(),
                            data.getResourceCrn(),
                            getPrepareCrossRealmTrustRequest(data));
                }
            }
            LOGGER.debug("FINISH_TRUST_SETUP_EVENT event sent");
            return data.toBuilder()
                    .withSelector(FINISH_TRUST_SETUP_EVENT.selector())
                    .build();
        } catch (Exception e) {
            LOGGER.debug("TRUST_SETUP_FAILED event sent");
            return new EnvironmentCrossRealmTrustSetupFailedEvent(data, e, TRUST_SETUP_FAILED);
        }
    }

    private PrepareCrossRealmTrustV2Request getPrepareCrossRealmTrustRequest(EnvironmentCrossRealmTrustSetupEvent data) {
        PrepareCrossRealmTrustV2Request prepareCrossRealmTrustRequest = new PrepareCrossRealmTrustV2Request();
        switch (data.getKdcType()) {
            case ACTIVE_DIRECTORY -> {
                PrepareCrossRealmTrustV2ActiveDirectoryRequest ad = new PrepareCrossRealmTrustV2ActiveDirectoryRequest();
                ad.setRealm(data.getKdcRealm());
                PrepareCrossRealmTrustV2KdcServerRequest server = new PrepareCrossRealmTrustV2KdcServerRequest();
                server.setFqdn(data.getKdcFqdn());
                server.setIp(data.getKdcIp());
                ad.setServers(List.of(server));
                prepareCrossRealmTrustRequest.setAd(ad);
            }
            case MIT -> {
                PrepareCrossRealmTrustV2MitRequest mit = new PrepareCrossRealmTrustV2MitRequest();
                mit.setRealm(data.getKdcRealm());
                PrepareCrossRealmTrustV2KdcServerRequest server = new PrepareCrossRealmTrustV2KdcServerRequest();
                server.setFqdn(data.getKdcFqdn());
                server.setIp(data.getKdcIp());
                mit.setServers(List.of(server));
                prepareCrossRealmTrustRequest.setMit(mit);
            }
            default -> throw new NotImplementedException(String.format("Unknown KDC type: %s.", data.getKdcType()));
        }
        prepareCrossRealmTrustRequest.setDnsServerIps(List.of(data.getDnsIp()));
        prepareCrossRealmTrustRequest.setTrustSecret(data.getTrustSecret());
        prepareCrossRealmTrustRequest.setEnvironmentCrn(data.getResourceCrn());
        prepareCrossRealmTrustRequest.setRemoteEnvironmentCrn(data.getRemoteEnvironmentCrn());
        return prepareCrossRealmTrustRequest;
    }
}
