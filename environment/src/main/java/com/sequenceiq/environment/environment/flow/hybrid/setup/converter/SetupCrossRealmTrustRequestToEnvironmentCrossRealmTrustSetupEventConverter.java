package com.sequenceiq.environment.environment.flow.hybrid.setup.converter;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_VALIDATION_EVENT;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.AddCrossRealmTrustV2Request;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2ActiveDirectoryRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2MitRequest;
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
        EnvironmentCrossRealmTrustSetupEvent.Builder eventBuilder = createEventBuilder(envId, accountId, envName, envCrn, request.getTrustSecret());
        eventBuilder.withRemoteEnvironmentCrn(request.getRemoteEnvironmentCrn());
        convertKdcParameters(eventBuilder, request.getAd(), request.getMit(), request.getDnsServerIps());
        return eventBuilder.build();
    }

    private EnvironmentCrossRealmTrustSetupEvent.Builder createEventBuilder(long envId, String accountId, String envName, String envCrn, String trustSecret) {
        return EnvironmentCrossRealmTrustSetupEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(TRUST_SETUP_VALIDATION_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withAccountId(accountId)
                .withTrustSecret(trustSecret);
    }

    private void convertKdcParameters(
            EnvironmentCrossRealmTrustSetupEvent.Builder eventBuilder,
            SetupCrossRealmTrustV2ActiveDirectoryRequest ad,
            SetupCrossRealmTrustV2MitRequest mit,
            List<String> dnsServerIps) {
        if (ad != null) {
            LOGGER.info("Setting up cross realm trust with Active Directory");
            SetupCrossRealmTrustV2KdcServerRequest kdcServer = ad.getServers().getFirst();
            eventBuilder
                    .withKdcType(KdcType.ACTIVE_DIRECTORY)
                    .withKdcRealm(ad.getRealm())
                    .withKdcFqdn(kdcServer.getFqdn())
                    .withKdcIp(kdcServer.getIp())
                    .withDnsIp(dnsServerIps.isEmpty() ? kdcServer.getIp() : dnsServerIps.getFirst());
        } else if (mit != null) {
            LOGGER.info("Setting up cross realm trust with MIT KDC");
            SetupCrossRealmTrustV2KdcServerRequest kdcServer = mit.getServers().getFirst();
            eventBuilder
                    .withKdcType(KdcType.MIT)
                    .withKdcRealm(mit.getRealm())
                    .withKdcFqdn(kdcServer.getFqdn())
                    .withKdcIp(kdcServer.getIp())
                    .withDnsIp(dnsServerIps.isEmpty() ? kdcServer.getIp() : dnsServerIps.getFirst());
        } else {
            throw new BadRequestException("Missing required KDC parameters for cross realm trust.");
        }
    }

    public EnvironmentCrossRealmTrustSetupEvent convertAddCrossRealmTrustV2RequestToCrossRealmTrustSetupEvent(
            long envId,
            String accountId,
            String envName,
            String envCrn,
            AddCrossRealmTrustV2Request request) {
        EnvironmentCrossRealmTrustSetupEvent.Builder eventBuilder = createEventBuilder(envId, accountId, envName, envCrn, request.getTrustSecret());
        convertKdcParameters(eventBuilder, request.getAd(), null, request.getDnsServerIps());
        return eventBuilder.build();
    }
}
