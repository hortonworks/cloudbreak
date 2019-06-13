package com.sequenceiq.environment.environment.flow.creation.handler;

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
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.EnvironmentNetworkCreationService;
import com.sequenceiq.environment.network.NetworkService;
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

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentNetworkCreationService environmentNetworkCreationService;

    private final Set<String> enabledPlatforms;

    protected NetworkCreationHandler(EventSender eventSender,
            EnvironmentService environmentService, EnvironmentRepository environmentRepository,
            PlatformParameterService platformParameterService,
            NetworkService networkService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentNetworkCreationService environmentNetworkCreationService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(eventSender);
        this.environmentService = environmentService;
        this.platformParameterService = platformParameterService;
        this.networkService = networkService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentNetworkCreationService = environmentNetworkCreationService;
        this.enabledPlatforms = enabledPlatforms;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            Optional<Environment> environment = getEnvironment(environmentDtoEvent);
            environment.ifPresent(env -> {
                EnvironmentDto envDto = populateEnvironmentDto(env);
                env.setStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS);
                networkService.save(hasExistingNetwork(env)
                        ? networkService.decorateNetworkWithSubnetMeta(env.getNetwork().getId(), getSubnetMetadata(envDto))
                        : environmentNetworkCreationService.createNetwork(envDto, env.getNetwork()));
                environmentService.save(env);
            });

            EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                    .withResourceId(environmentDto.getResourceId())
                    .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                    .build();
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e);
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private EnvironmentDto populateEnvironmentDto(Environment environment) {
        return environmentDtoConverter.environmentToDto(environment);
    }

    private boolean hasExistingNetwork(Environment environment) {
        return networkService.hasExistingNetwork(environment.getNetwork(), CloudPlatform.valueOf(environment.getCloudPlatform()));
    }

    private Optional<Environment> getEnvironment(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        return environmentService.findEnvironmentById(environmentDto.getId())
                .filter(environment -> Objects.nonNull(environment.getNetwork())
                        && enabledPlatforms.contains(environment.getCloudPlatform()));
    }

    private Map<String, CloudSubnet> getSubnetMetadata(EnvironmentDto environmentDto) {
        String regionName = environmentDto.getRegionSet().iterator().next().getName();
        PlatformResourceRequest prr = new PlatformResourceRequest();
        prr.setCredential(environmentDto.getCredential());
        prr.setCloudPlatform(environmentDto.getCloudPlatform());
        prr.setRegion(regionName);
        CloudNetworks cnsr = platformParameterService.getCloudNetworks(prr);
        Set<CloudNetwork> cns = cnsr.getCloudNetworkResponses().get(regionName);
        return cns.stream()
                .filter(n -> n.getId().equals(getVpcId(environmentDto)))
                .findFirst()
                .map(CloudNetwork::getSubnetsMeta)
                .stream()
                .flatMap(Set::stream)
                .filter(sn -> environmentDto.getNetwork().getSubnetIds().contains(sn.getId()))
                .collect(Collectors.toMap(CloudSubnet::getId, Function.identity()));
    }

    private String getVpcId(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getNetwork())
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId)
                .orElse(null);
    }

    @Override
    public String selector() {
        return CREATE_NETWORK_EVENT.selector();
    }
}
