package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDetachRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.BaseNetwork;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentRepository environmentRepository;

    private final ConversionService conversionService;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final NetworkService networkService;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            ConversionService conversionService,
            PlatformParameterService platformParameterService,
            EnvironmentDtoConverter environmentDtoConverter,
            NetworkService networkService) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.conversionService = conversionService;
        this.platformParameterService = platformParameterService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.networkService = networkService;
    }

    public Environment save(Environment environment) {
        return environmentRepository.save(environment);
    }

    public DetailedEnvironmentResponse get(String environmentName, String accountId) {
        Environment environment = getByNameForAccountId(environmentName, accountId);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    public Set<Environment> getByNamesForAccountId(Set<String> names, String accountId) {
        Set<Environment> results = environmentRepository.findByNameInAndAccountId(names, accountId);
        Set<String> notFound = Sets.difference(names,
                results.stream().map(Environment::getName).collect(Collectors.toSet()));
        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No environment(s) found with name(s) '%s'",
                    notFound.stream().map(name -> '\'' + name + '\'').collect(Collectors.joining(", "))));
        }
        return results;
    }

    public Environment getByNameForAccountId(String name, String accountId) {
        Optional<Environment> object = environmentRepository.findByNameAndAccountId(name, accountId);
        if (object.isEmpty()) {
            throw new NotFoundException(String.format("No environment found with name '%s'", name));
        }
        MDCBuilder.buildMdcContext(object.get());
        return object.get();
    }

    public SimpleEnvironmentResponse delete(String environmentName, String accountId) {
        Environment environment = getByNameForAccountId(environmentName, accountId);
        LOGGER.debug(String.format("Starting to archive environment [name: %s]", environment.getName()));
        delete(environment);
        return conversionService.convert(environment, SimpleEnvironmentResponse.class);
    }

    public Environment delete(Environment resource) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Deleting environment with name: {}", resource.getName());
        environmentRepository.delete(resource);
        return resource;
    }

    public DetailedEnvironmentResponse edit(String environmentName, EnvironmentEditDto editDto) {
        Environment environment = getByNameForAccountId(environmentName, editDto.getAccountId());
        if (StringUtils.isNotEmpty(editDto.getDescription())) {
            environment.setDescription(editDto.getDescription());
        }
        CloudRegions cloudRegions = getRegionsByEnvironment(environment);
        if (locationAndRegionChanged(editDto)) {
            editRegionsAndLocation(editDto, environment, cloudRegions);
        } else if (locationChanged(editDto)) {
            editLocation(editDto, environment, cloudRegions);
        } else if (!CollectionUtils.isEmpty(editDto.getRegions())) {
            LocationDto locationDto = environmentDtoConverter.environmentToLocationDto(environment);
            editDto.setLocation(locationDto);
            editRegions(editDto, environment, cloudRegions);
        }
        if (networkChanged(editDto)) {
            BaseNetwork network = createAndSetNetwork(environment, editDto.getNetworkDto());
            if (network != null) {
                environment.setNetwork(network);
            }
        }
        Environment savedEnvironment = environmentRepository.save(environment);
        return conversionService.convert(savedEnvironment, DetailedEnvironmentResponse.class);
    }

    public CloudRegions getRegionsByEnvironment(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest);
    }

    private boolean locationAndRegionChanged(EnvironmentEditDto editDto) {
        return !CollectionUtils.isEmpty(editDto.getRegions())
                && locationChanged(editDto);
    }

    private boolean networkChanged(EnvironmentEditDto editDto) {
        return editDto.getNetworkDto() != null;
    }

    private BaseNetwork createAndSetNetwork(Environment environment, NetworkDto networkDto) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        return networkService.createNetworkIfPossible(environment, networkDto, cloudPlatform);
    }

    private boolean locationChanged(EnvironmentEditDto editDto) {
        return editDto.getLocation() != null && !editDto.getLocation().isEmpty();
    }

    private void editRegionsAndLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        validateRegionAndLocation(editDto.getLocation(), editDto.getRegions(), cloudRegions, environment);
        setLocation(environment, editDto.getLocation(), cloudRegions);
        setRegions(environment, editDto.getRegions(), cloudRegions);
    }

    private void validateRegionAndLocation(LocationDto location, Set<String> requestedRegions,
            CloudRegions cloudRegions, Environment environment) {
        ValidationResultBuilder validationResultBuilder = validatorService
                .validateRegions(requestedRegions, cloudRegions, environment.getCloudPlatform(), ValidationResult.builder());
        validatorService.validateLocation(location, requestedRegions, environment, validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void editLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        Set<String> regions = environment.getRegionSet().stream()
                .map(Region::getName).collect(Collectors.toSet());
        validateRegionAndLocation(editDto.getLocation(), regions, cloudRegions, environment);
        setLocation(environment, editDto.getLocation(), cloudRegions);
    }

    private void editRegions(EnvironmentEditDto request, Environment environment, CloudRegions cloudRegions) {
        LocationDto locationDto = environmentDtoConverter.environmentToLocationDto(environment);
        validateRegionAndLocation(locationDto, request.getRegions(), cloudRegions, environment);
        setRegions(environment, request.getRegions(), cloudRegions);
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

    public void setLocation(Environment environment, LocationDto requestedLocation, CloudRegions cloudRegions) {
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

    public DetailedEnvironmentResponse detachResources(String environmentName, String accountId, EnvironmentDetachRequest request) {
        Environment environment = getByNameForAccountId(environmentName, accountId);
        Environment saved = environmentRepository.save(environment);
        return conversionService.convert(saved, DetailedEnvironmentResponse.class);
    }

    public Optional<Environment> findById(Long id) {
        return environmentRepository.findById(id);
    }
}