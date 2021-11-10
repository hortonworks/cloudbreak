package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_VALIDATION_EVENT;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.domain.PemBasedEnvironmentDomainProvider;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EnvironmentInitHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentInitHandler.class);

    private final EnvironmentService environmentService;

    private final EventBus eventBus;

    private final VirtualGroupService virtualGroupService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final PemBasedEnvironmentDomainProvider domainProvider;

    protected EnvironmentInitHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentNetworkService environmentNetworkService,
            EventBus eventBus, VirtualGroupService virtualGroupService,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            PemBasedEnvironmentDomainProvider domainProvider) {
        super(eventSender);
        this.environmentService = environmentService;
        this.eventBus = eventBus;
        this.virtualGroupService = virtualGroupService;
        this.environmentNetworkService = environmentNetworkService;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.domainProvider = domainProvider;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresentOrElse(environment -> {
                            try {
                                LOGGER.debug("Environment initialization flow step started.");
                                initEnvironment(environment);
                                goToValidationState(environmentDtoEvent, environmentDto);
                            } catch (Exception e) {
                                LOGGER.warn("Couldn't initialize environment creation", e);
                                goToFailedState(environmentDtoEvent, e.getMessage());
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
    }

    private void initEnvironment(Environment environment) {
        generateDomainForEnvironment(environment);
        String environmentCrnForVirtualGroups = getEnvironmentCrnForVirtualGroups(environment);
        if (!createVirtualGroups(environment, environmentCrnForVirtualGroups)) {
            // To keep backward compatibility, if somebody passes the group name, then we shall just use it
            environmentService.setAdminGroupName(environment, environment.getAdminGroupName());
        }
        if (environment.getNetwork() != null && RegistrationType.EXISTING.equals(environment.getNetwork().getRegistrationType())) {
            EnvironmentNetworkConverter environmentNetworkConverter =
                    environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()));
            if (environmentNetworkConverter != null) {
                Network network = environmentNetworkConverter.convertToNetwork(environment.getNetwork());
                NetworkCidr networkCidr = environmentNetworkService.getNetworkCidr(
                        network,
                        environment.getCloudPlatform(),
                        environment.getCredential());
                environment.getNetwork().setNetworkCidr(networkCidr.getCidr());
                environment.getNetwork().setNetworkCidrs(StringUtils.join(networkCidr.getCidrs(), ","));
            }
        }
        environmentService.assignEnvironmentAdminRole(environment.getCreator(), environmentCrnForVirtualGroups);
        setLocationAndRegions(environment);
        environmentService.save(environment);
    }

    private void generateDomainForEnvironment(Environment environment) {
        String domain = domainProvider.generate(environment);
        environment.setDomain(domain);
    }

    private String getEnvironmentCrnForVirtualGroups(Environment environment) {
        String environmentCrnForVirtualGroups = environment.getResourceCrn();
        if (Objects.nonNull(environment.getParentEnvironment())) {
            environmentCrnForVirtualGroups = environment.getParentEnvironment().getResourceCrn();
        }
        return environmentCrnForVirtualGroups;
    }

    private boolean createVirtualGroups(Environment environment, String envCrn) {
        boolean result = false;
        if (StringUtils.isEmpty(environment.getAdminGroupName())) {
            Map<UmsRight, String> virtualGroups = virtualGroupService.createVirtualGroups(environment.getAccountId(), envCrn);
            LOGGER.info("The virtualgroups for [{}] environment are created: {}", environment.getName(), virtualGroups);
            result = true;
        }
        return result;
    }

    private void setLocationAndRegions(Environment environment) {
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        RegionWrapper regionWrapper = environment.getRegionWrapper();
        environmentService.setLocation(environment, regionWrapper, cloudRegions);
        if (cloudRegions.areRegionsSupported()) {
            environmentService.setRegions(environment, regionWrapper.getRegions(), cloudRegions);
        }
    }

    private void goToValidationState(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_ENVIRONMENT_VALIDATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    private void goToFailedState(Event<EnvironmentDto> environmentDtoEvent, String message) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                new BadRequestException(message),
                environmentDto.getResourceCrn());

        eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
    }

    @Override
    public String selector() {
        return INITIALIZE_ENVIRONMENT_EVENT.selector();
    }

}
