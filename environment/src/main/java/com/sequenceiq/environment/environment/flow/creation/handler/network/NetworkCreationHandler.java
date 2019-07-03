package com.sequenceiq.environment.environment.flow.creation.handler.network;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class NetworkCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final PlatformParameterService platformParameterService;

    private final NetworkService networkService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final Set<String> enabledPlatforms;

    protected NetworkCreationHandler(EventSender eventSender,
            EnvironmentService environmentService,
            PlatformParameterService platformParameterService,
            NetworkService networkService,
            EnvironmentNetworkService environmentNetworkService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(eventSender);
        this.environmentService = environmentService;
        this.platformParameterService = platformParameterService;
        this.networkService = networkService;
        this.environmentNetworkService = environmentNetworkService;
        this.enabledPlatforms = enabledPlatforms;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            createNetwork(environmentDto);
            stepToFreeIpaCreation(environmentDtoEvent, environmentDto);
        } catch (Exception e) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                    environmentDto.getId(),
                    environmentDto.getName(),
                    e,
                    environmentDto.getResourceCrn());
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void createNetwork(EnvironmentDto environmentDto) {
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresent(environment -> {
                    environment.setStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS);
                    setNetworkIfNeeded(environmentDto, environment);
                    environmentService.save(environment);
                });
    }

    private void setNetworkIfNeeded(EnvironmentDto environmentDto, Environment environment) {
        if (hasNetwork(environment)) {
            BaseNetwork baseNetwork = hasExistingNetwork(environment)
                    ? decorateWithSubnetMeta(environment.getNetwork().getId(), environmentDto)
                    : environmentNetworkService.createNetwork(environmentDto, environment.getNetwork());
            baseNetwork = networkService.save(baseNetwork);
            environment.setNetwork(baseNetwork);
        }
    }

    private boolean hasNetwork(Environment environment) {
        return Objects.nonNull(environment.getNetwork()) && enabledPlatforms.contains(environment.getCloudPlatform());
    }

    private boolean hasExistingNetwork(Environment environment) {
        return networkService.hasExistingNetwork(environment.getNetwork(), CloudPlatform.valueOf(environment.getCloudPlatform()));
    }

    private BaseNetwork decorateWithSubnetMeta(Long networkId, EnvironmentDto envDto) {
        Map<String, CloudSubnet> subnetMetadata = getSubnetMetadata(envDto);
        if (subnetMetadata.isEmpty()) {
            throw new EnvironmentServiceException(String.format("Subnets of the environment (%s) are not found in vpc (%s). ",
                    String.join(",", envDto.getNetwork().getSubnetIds()), getVpcId(envDto).orElse("")));
        }
        return networkService.decorateNetworkWithSubnetMeta(networkId, subnetMetadata);
    }

    private Map<String, CloudSubnet> getSubnetMetadata(EnvironmentDto environmentDto) {
        String regionName = environmentDto.getRegionSet().iterator().next().getName();
        PlatformResourceRequest prr = new PlatformResourceRequest();
        prr.setCredential(environmentDto.getCredential());
        prr.setCloudPlatform(environmentDto.getCloudPlatform());
        prr.setRegion(regionName);
        getVpcId(environmentDto)
                .ifPresent(vpcId -> prr.setFilters(Map.of("vpcId", vpcId)));

        CloudNetworks cnsr = platformParameterService.getCloudNetworks(prr);
        Set<CloudNetwork> cns = cnsr.getCloudNetworkResponses().get(regionName);
        return cns.stream()
                .filter(n -> n.getId().equals(getVpcId(environmentDto).orElse(null)))
                .findFirst()
                .map(CloudNetwork::getSubnetsMeta)
                .stream()
                .flatMap(Set::stream)
                .filter(sn -> environmentDto.getNetwork().getSubnetIds().contains(sn.getId()))
                .collect(Collectors.toMap(CloudSubnet::getId, Function.identity()));
    }

    private Optional<String> getVpcId(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getNetwork())
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId);
    }

    private void stepToFreeIpaCreation(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_NETWORK_EVENT.selector();
    }
}
