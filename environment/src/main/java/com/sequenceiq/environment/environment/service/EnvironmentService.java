package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.v1.EnvironmentDtoToCreateAWSEnvironmentRequestConverter;
import com.sequenceiq.environment.environment.v1.EnvironmentRequestToCreateAWSEnvironmentRequestConverter;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class EnvironmentService implements ResourceIdProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Value("${environment.admin.group.default.prefix:}")
    private String adminGroupNamePrefix;

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentRepository environmentRepository;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentRequestToCreateAWSEnvironmentRequestConverter environmentRequestToCreateAWSEnvironmentRequestConverter;

    private final EnvironmentDtoToCreateAWSEnvironmentRequestConverter environmentDtoToCreateAWSEnvironmentRequestConverter;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            PlatformParameterService platformParameterService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentResourceDeletionService environmentResourceDeletionService,
            EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentRequestToCreateAWSEnvironmentRequestConverter environmentRequestToCreateAWSEnvironmentRequestConverter,
            EnvironmentDtoToCreateAWSEnvironmentRequestConverter environmentDtoToCreateAWSEnvironmentRequestConverter) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.platformParameterService = platformParameterService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.reactorFlowManager = reactorFlowManager;
        this.environmentRequestToCreateAWSEnvironmentRequestConverter = environmentRequestToCreateAWSEnvironmentRequestConverter;
        this.environmentDtoToCreateAWSEnvironmentRequestConverter = environmentDtoToCreateAWSEnvironmentRequestConverter;
    }

    public Environment save(Environment environment) {
        return environmentRepository.save(environment);
    }

    public EnvironmentDto getEnvironmentDto(Environment environment) {
        return environmentDtoConverter.environmentToDto(environment);
    }

    public EnvironmentDto getByNameAndAccountId(String environmentName, String accountId) {
        Optional<Environment> environment = environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment
                .orElseThrow(notFound("Environment with name:", environmentName)));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public EnvironmentDto getByCrnAndAccountId(String crn, String accountId) {
        Optional<Environment> environment = environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(() -> new NotFoundException(String.format("No environment found with resource CRN '%s'", crn))));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public List<EnvironmentDto> listByAccountId(String accountId) {
        Set<Environment> environments = environmentRepository.findByAccountId(accountId);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
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
                environment.setLocationDisplayName(requestedLocation.getDisplayName());
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
        return environmentRepository.existsWithNameAndAccountAndArchivedIsFalse(name, accountId);
    }

    CloudRegions getRegionsByEnvironment(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest);
    }

    public List<EnvironmentDto> findAllByIdInAndStatusIn(Collection<Long> resourceIds, Collection<EnvironmentStatus> environmentStatuses) {
        List<Environment> environments = environmentRepository
                .findAllByIdInAndStatusInAndArchivedIsFalse(resourceIds, environmentStatuses);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }

    public List<EnvironmentDto> findAllByStatusIn(Collection<EnvironmentStatus> environmentStatuses) {
        List<Environment> environments = environmentRepository
                .findAllByStatusInAndArchivedIsFalse(environmentStatuses);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }

    public CreateAWSEnvironmentRequest getCreateAWSEnvironmentForCli(EnvironmentRequest environmentRequest, String cloudPlatform) {
        ValidationResult validationResult = validatorService.validateAwsEnvironmentRequest(environmentRequest, cloudPlatform);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return environmentRequestToCreateAWSEnvironmentRequestConverter.convert(environmentRequest);
    }

    public CreateAWSEnvironmentRequest getCreateAWSEnvironmentForCli(EnvironmentDto environmentDto) {
        ValidationResult validationResult = validatorService.validateAwsEnvironmentRequest(environmentDto);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return environmentDtoToCreateAWSEnvironmentRequestConverter.convert(environmentDto);
    }

    Optional<Environment> findByNameAndAccountIdAndArchivedIsFalse(String name, String accountId) {
        return environmentRepository.findByNameAndAccountIdAndArchivedIsFalse(name, accountId);
    }

    Optional<Environment> findByResourceCrnAndAccountIdAndArchivedIsFalse(String resourceCrn, String accountId) {
        return environmentRepository.findByResourceCrnAndAccountIdAndArchivedIsFalse(resourceCrn, accountId);
    }

    void setSecurityAccess(Environment environment, SecurityAccessDto securityAccess) {
        if (securityAccess != null) {
            environment.setCidr(securityAccess.getCidr());
            environment.setSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox());
            environment.setDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId());
        }
    }

    void setAdminGroupName(Environment environment, String adminGroupName) {
        if (isEmpty(adminGroupName)) {
            environment.setAdminGroupName(adminGroupNamePrefix + environment.getName());
        } else {
            environment.setAdminGroupName(adminGroupName);
        }
    }

    public Collection<Environment> findByResourceCrnInAndAccountIdAndArchivedIsFalse(Collection<String> crns, String accountId) {
        return environmentRepository.findByResourceCrnInAndAccountIdAndArchivedIsFalse(crns, accountId);
    }

    public Collection<Environment> findByNameInAndAccountIdAndArchivedIsFalse(Collection<String> environmentNames, String accountId) {
        return environmentRepository.findByNameInAndAccountIdAndArchivedIsFalse(environmentNames, accountId);
    }

    public String getCrnByNameAndAccountId(String environmentName, String accountId) {
        return environmentRepository.findResourceCrnByNameAndAccountId(environmentName, accountId)
                .orElseThrow(() -> new BadRequestException(
                        String.format("Environment with name '%s' was not found for account '%s'.", environmentName, accountId)));
    }
}
