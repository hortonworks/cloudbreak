package com.sequenceiq.environment.environment.flow.hybrid.setup.converter;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_VALIDATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2Request;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;

@Component
public class SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter.class);

    public EnvironmentCrossRealmTrustSetupEvent convertV1(
            long envId,
            String accountId,
            String envName,
            String envCrn,
            SetupCrossRealmTrustRequest request) {
        return EnvironmentCrossRealmTrustSetupEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(TRUST_SETUP_VALIDATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withKdcType(KdcType.ACTIVE_DIRECTORY)
                .withKdcRealm(request.getRealm())
                .withKdcFqdn(request.getFqdn())
                .withAccountId(accountId)
                .withKdcIp(request.getIp())
                .withDnsIp(request.getIp())
                .withRemoteEnvironmentCrn(request.getRemoteEnvironmentCrn())
                .withTrustSecret(request.getTrustSecret())
                .build();
    }

    public EnvironmentCrossRealmTrustSetupEvent convertV2(
            long envId,
            String accountId,
            String envName,
            String envCrn,
            SetupCrossRealmTrustV2Request request) {
        EnvironmentCrossRealmTrustSetupEvent.Builder eventBuilder = EnvironmentCrossRealmTrustSetupEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(TRUST_SETUP_VALIDATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withAccountId(accountId)
                .withRemoteEnvironmentCrn(request.getRemoteEnvironmentCrn())
                .withTrustSecret(request.getTrustSecret());
        if (request.getAd() != null) {
            LOGGER.info("Setting up cross realm trust with Active Directory");
            SetupCrossRealmTrustV2KdcServerRequest kdcServer = request.getAd().getServers().getFirst();
            eventBuilder
                    .withKdcType(KdcType.ACTIVE_DIRECTORY)
                    .withKdcRealm(request.getAd().getRealm())
                    .withKdcFqdn(kdcServer.getFqdn())
                    .withKdcIp(kdcServer.getIp())
                    .withDnsIp(request.getDnsServerIps().isEmpty() ? kdcServer.getIp() : request.getDnsServerIps().getFirst());
        } else if (request.getMit() != null) {
            LOGGER.info("Setting up cross realm trust with MIT KDC");
            SetupCrossRealmTrustV2KdcServerRequest kdcServer = request.getMit().getServers().getFirst();
            eventBuilder
                    .withKdcType(KdcType.MIT)
                    .withKdcRealm(request.getMit().getRealm())
                    .withKdcFqdn(kdcServer.getFqdn())
                    .withKdcIp(kdcServer.getIp())
                    .withDnsIp(request.getDnsServerIps().isEmpty() ? kdcServer.getIp() : request.getDnsServerIps().getFirst());
        } else {
            throw new BadRequestException("Missing required KDC parameters for cross realm trust.");
        }
        return eventBuilder.build();
    }
}
