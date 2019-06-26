package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.flow.ReactorFlowManager;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentRepository environmentRepository;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final ReactorFlowManager reactorFlowManager;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            PlatformParameterService platformParameterService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentResourceDeletionService environmentResourceDeletionService,
            ReactorFlowManager reactorFlowManager) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.platformParameterService = platformParameterService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public Environment save(Environment environment) {
        return environmentRepository.save(environment);
    }

    public EnvironmentDto getEnvironmentDto(Environment environment) {
        return environmentDtoConverter.environmentToDto(environment);
    }

    public EnvironmentDto getByNameAndAccountId(String environmentName, String accountId) {
        Optional<Environment> environment = environmentRepository.findByNameAndAccountId(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(()
                -> new NotFoundException(String.format("No environment found with name '%s'", environmentName))));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public EnvironmentDto getByCrnAndAccountId(String crn, String accountId) {
        Optional<Environment> environment = environmentRepository.findByResourceCrnAndAccountId(crn, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(() -> new NotFoundException(String.format("No environment found with resource CRN '%s'", crn))));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public List<EnvironmentDto> listByAccountId(String accountId) {
        Set<Environment> environments = environmentRepository.findByAccountId(accountId);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }

    public EnvironmentDto deleteByNameAndAccountId(String environmentName, String accountId, String actualUserCrn) {
        Optional<Environment> environment = environmentRepository.findByNameAndAccountId(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(()
                -> new NotFoundException(String.format("No environment found with name '%s'", environmentName))));
        LOGGER.debug(String.format("Deleting environment [name: %s]", environment.get().getName()));
        delete(environment.get(), actualUserCrn);
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public EnvironmentDto deleteByCrnAndAccountId(String crn, String accountId, String actualUserCrn) {
        Optional<Environment> environment = environmentRepository.findByResourceCrnAndAccountId(crn, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(()
                -> new NotFoundException(String.format("No environment found with crn '%s'", crn))));
        LOGGER.debug(String.format("Deleting  environment [name: %s]", environment.get().getName()));
        delete(environment.get(), actualUserCrn);
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public Environment delete(Environment environment, String userCrn) {
        checkForDeletePermit(environment);
        MDCBuilder.buildMdcContext(environment);
        LOGGER.debug("Deleting environment with name: {}", environment.getName());
        reactorFlowManager.triggerDeleteFlow(environment, userCrn);
        return environment;
    }

    public List<EnvironmentDto> deleteMultipleByNames(Set<String> environmentNames, String accountId, String actualUserCrn) {
        List<EnvironmentDto> environmentDtos = new ArrayList<>();
        Set<Environment> environments = environmentRepository.findByNameInAndAccountId(environmentNames, accountId);
        for (Environment environment : environments) {
            LOGGER.debug(String.format("Starting to archive environment [name: %s]", environment.getName()));
            delete(environment, actualUserCrn);
            environmentDtos.add(environmentDtoConverter.environmentToDto(environment));
        }
        return environmentDtos;
    }

    public List<EnvironmentDto> deleteMultipleByCrns(Set<String> crns, String accountId, String actualUserCrn) {
        List<EnvironmentDto> environmentDtos = new ArrayList<>();
        Set<Environment> environments = environmentRepository.findByResourceCrnInAndAccountId(crns, accountId);
        for (Environment environment : environments) {
            LOGGER.debug(String.format("Starting to archive environment [CRN: %s]", environment.getName()));
            delete(environment, actualUserCrn);
            environmentDtos.add(environmentDtoConverter.environmentToDto(environment));
        }
        return environmentDtos;
    }

    public void setRegions(Environment environment, Set<String> requestedRegions, CloudRegions cloudRegions) {
        Set<Region> regionSet = new HashSet<>();
        Map<com.sequenceiq.cloudbreak.cloud.model.Region, String> displayNames = cloudRegions.getDisplayNames();
        for (com.sequenceiq.cloudbreak.cloud.model.Region r : cloudRegions.getCloudRegions().keySet()) {
            if (requestedRegions.contains(r.getRegionName())) {
                Region region = new Region();
                region.setName(r.getRegionName());
                String displayName = displayNames.get(r);
                region.setDisplayName(isEmpty(displayName) ? r.getRegionName() : displayName);
                regionSet.add(region);
            }
        }
        environment.setRegions(regionSet);
    }

    public Optional<Environment> findEnvironmentById(Long id) {
        return environmentRepository.findById(id);
    }

    public Optional<EnvironmentDto> findById(Long id) {
        return environmentRepository.findById(id).map(environmentDtoConverter::environmentToDto);
    }

    void setLocation(Environment environment, LocationDto requestedLocation, CloudRegions cloudRegions) {
        if (requestedLocation != null) {
            Coordinate coordinate = cloudRegions.getCoordinates().get(region(requestedLocation.getName()));
            if (coordinate != null) {
                environment.setLocation(requestedLocation.getName());
                environment.setLocationDisplayName(coordinate.getDisplayName());
                environment.setLatitude(coordinate.getLatitude());
                environment.setLongitude(coordinate.getLongitude());
            } else if (requestedLocation.getLatitude() != null && requestedLocation.getLongitude() != null) {
                environment.setLocation(requestedLocation.getName());
                environment.setLocationDisplayName(requestedLocation.getName());
                environment.setLatitude(requestedLocation.getLatitude());
                environment.setLongitude(requestedLocation.getLongitude());
            } else {
                throw new BadRequestException(String.format("No location found with name %s in the location list. The supported locations are: [%s]",
                        requestedLocation, cloudRegions.locationNames()));
            }
        }
    }

    EnvironmentValidatorService getValidatorService() {
        return validatorService;
    }

    boolean isNameOccupied(String name, String accountId) {
        return environmentRepository.existsWithNameInAccount(name, accountId);
    }

    CloudRegions getRegionsByEnvironment(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest);
    }

    private void checkForDeletePermit(Environment env) {
        LOGGER.debug("Checking if environment [name: {}] is deletable", env.getName());
        Set<String> sdxNames = environmentResourceDeletionService.getAttachedSdxClusterNames(env);
        if (!sdxNames.isEmpty()) {
            throw new BadRequestException(String.format("The following cluster(s) must be terminated before Environment deletion [%s]",
                    String.join(", ", sdxNames)));
        }
    }

    public List<EnvironmentDto> findAllByIdInAndStatusIn(Collection<Long> resourceIds, Collection<EnvironmentStatus> environmentStatuses) {
        List<Environment> environments = environmentRepository.findAllByIdInAndStatusIn(resourceIds, environmentStatuses);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }

    public List<EnvironmentDto> findAllByStatusIn(Collection<EnvironmentStatus> environmentStatuses) {
        List<Environment> environments = environmentRepository.findAllByStatusIn(environmentStatuses);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }
}
