package com.sequenceiq.environment.environment.flow.modify.network.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class ModifyNetworkCidrsOnFreeIpaHandler extends ExceptionCatcherEventHandler<EnvNetworkCidrsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyNetworkCidrsOnFreeIpaHandler.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaPollerService freeIpaPollerService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private EnvironmentDtoConverter environmentDtoConverter;

    @Inject
    private DnsV1Endpoint dnsV1Endpoint;

    @Override
    public String selector() {
        return MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvNetworkCidrsModificationEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        List<String> networkCidrs = event.getData().getNetworkCidrs();
        try {
            freeIpaService.updateNetworkCidrs(resourceCrn, networkCidrs);
            freeIpaPollerService.waitForSaltUpdate(resourceId, resourceCrn);
            updateReverseDnsZonesOnFreeIpa(resourceId, resourceCrn);
        } catch (Exception e) {
            LOGGER.error("Modify network CIDRs on FreeIPA failed.", e);
            return new EnvNetworkCidrsModificationFailureEvent(resourceId, resourceName, resourceCrn, NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED, e);
        }
        return EnvNetworkCidrsModificationEvent.builder()
                .withSelector(START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT.name())
                .withResourceId(resourceId)
                .withResourceName(resourceName)
                .withResourceCrn(resourceCrn)
                .withNetworkCidrs(networkCidrs)
                .build();
    }

    private void updateReverseDnsZonesOnFreeIpa(Long resourceId, String resourceCrn) {
        Environment environment = environmentService.findEnvironmentByIdOrThrow(resourceId);
        NetworkDto networkDto = environmentDtoConverter.networkToNetworkDto(environment);
        AddDnsZoneForSubnetIdsRequest request = buildAddDnsZoneForSubnetIdsRequest(resourceCrn, networkDto);
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(resourceCrn);
        if (freeIpaResponse.isPresent() && shouldSendSubnetIdsToFreeIpa(request)) {
            LOGGER.info("Updating FreeIPA reverse DNS zones for environment {} after network CIDR modification", resourceCrn);
            dnsV1Endpoint.addDnsZoneForSubnetIds(request);
        } else {
            LOGGER.debug("Skipping FreeIPA reverse DNS zone update for environment {} (freeIpaPresent={}, sendable={})",
                    resourceCrn, freeIpaResponse.isPresent(), shouldSendSubnetIdsToFreeIpa(request));
        }
    }

    private AddDnsZoneForSubnetIdsRequest buildAddDnsZoneForSubnetIdsRequest(String environmentCrn, NetworkDto networkDto) {
        AddDnsZoneForSubnetIdsRequest request = new AddDnsZoneForSubnetIdsRequest();
        request.setEnvironmentCrn(environmentCrn);
        AddDnsZoneNetwork addDnsZoneNetwork = new AddDnsZoneNetwork();
        if (networkDto != null) {
            addDnsZoneNetwork.setNetworkId(networkDto.getNetworkId());
            addDnsZoneNetwork.setSubnetIds(networkDto.getSubnetIds());
        }
        request.setAddDnsZoneNetwork(addDnsZoneNetwork);
        return request;
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest request) {
        AddDnsZoneNetwork addDnsZoneNetwork = request.getAddDnsZoneNetwork();
        return StringUtils.isNotBlank(addDnsZoneNetwork.getNetworkId())
                && addDnsZoneNetwork.getSubnetIds() != null
                && !addDnsZoneNetwork.getSubnetIds().isEmpty();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvNetworkCidrsModificationEvent> event) {
        LOGGER.error("Modify network CIDRs on FreeIPA failed.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvNetworkCidrsModificationFailureEvent(resourceId, resourceName, resourceCrn, NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED, e);
    }
}
