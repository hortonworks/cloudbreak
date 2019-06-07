package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentRepository environmentRepository;

    private final ConversionService conversionService;

    private final PlatformParameterService platformParameterService;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            ConversionService conversionService,
            PlatformParameterService platformParameterService) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.conversionService = conversionService;
        this.platformParameterService = platformParameterService;
    }

    public Environment save(Environment environment) {
        return environmentRepository.save(environment);
    }

    public DetailedEnvironmentResponse getByName(String environmentName, String accountId) {
        Environment environment = getByNameForAccountId(environmentName, accountId);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    public DetailedEnvironmentResponse getByCrn(String crn, String accountId) {
        Environment environment = getByCrnForAccountId(crn, accountId);
        return conversionService.convert(environment, DetailedEnvironmentResponse.class);
    }

    public Environment getByNameForAccountId(String name, String accountId) {
        Optional<Environment> object = environmentRepository.findByNameAndAccountId(name, accountId);
        MDCBuilder.buildMdcContext(object.orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", name))));
        return object.get();
    }

    public Environment getByCrnForAccountId(String crn, String accountId) {
        Optional<Environment> object = environmentRepository.findByResourceCrnAndAccountId(crn, accountId);
        MDCBuilder.buildMdcContext(object.orElseThrow(() -> new NotFoundException(String.format("No environment found with resource CRN '%s'", crn))));
        return object.get();
    }

    public SimpleEnvironmentResponse deleteByName(String environmentName, String accountId) {
        Environment environment = getByNameForAccountId(environmentName, accountId);
        LOGGER.debug(String.format("Starting to archive environment [name: %s]", environment.getName()));
        delete(environment);
        return conversionService.convert(environment, SimpleEnvironmentResponse.class);
    }

    public SimpleEnvironmentResponse deleteByCrn(String crn, String accountId) {
        Environment environment = getByCrnForAccountId(crn, accountId);
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

    public SimpleEnvironmentResponses deleteMultipleByNames(Set<String> environmentNames, String accountId) {
        Set<SimpleEnvironmentResponse> responses = new HashSet<>();
        for (String environmentName : environmentNames) {
            Environment environment = getByNameForAccountId(environmentName, accountId);
            LOGGER.debug(String.format("Starting to archive environment [name: %s]", environment.getName()));
            delete(environment);
            responses.add(conversionService.convert(environment, SimpleEnvironmentResponse.class));
        }
        return new SimpleEnvironmentResponses(responses);
    }

    public SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns, String accountId) {
        Set<SimpleEnvironmentResponse> responses = new HashSet<>();
        for (String crn : crns) {
            Environment environment = getByCrnForAccountId(crn, accountId);
            LOGGER.debug(String.format("Starting to archive environment [CRN: %s]", environment.getName()));
            delete(environment);
            responses.add(conversionService.convert(environment, SimpleEnvironmentResponse.class));
        }
        return new SimpleEnvironmentResponses(responses);
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

    public Optional<Environment> findById(Long id) {
        return environmentRepository.findById(id);
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

}
