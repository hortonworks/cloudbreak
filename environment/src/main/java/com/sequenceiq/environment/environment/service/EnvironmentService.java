package com.sequenceiq.environment.environment.service;

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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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

    private final EnvironmentRequestToCreateAWSEnvironmentRequestConverter environmentRequestToCreateAWSEnvironmentRequestConverter;

    private final EnvironmentDtoToCreateAWSEnvironmentRequestConverter environmentDtoToCreateAWSEnvironmentRequestConverter;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            PlatformParameterService platformParameterService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentRequestToCreateAWSEnvironmentRequestConverter environmentRequestToCreateAWSEnvironmentRequestConverter,
            EnvironmentDtoToCreateAWSEnvironmentRequestConverter environmentDtoToCreateAWSEnvironmentRequestConverter) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.platformParameterService = platformParameterService;
        this.environmentDtoConverter = environmentDtoConverter;
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
        for (String requestedRegion : requestedRegions) {
            Optional<Map.Entry<com.sequenceiq.cloudbreak.cloud.model.Region, Coordinate>> coordinate =
                    cloudRegions.getCoordinates().entrySet()
                            .stream()
                            .filter(e -> e.getKey().getRegionName().equals(requestedRegion)
                                    || e.getValue().getDisplayName().equals(requestedRegion)
                                    || e.getValue().getKey().equals(requestedRegion))
                            .findFirst();
            if (coordinate.isPresent()) {
                Region region = new Region();
                region.setName(coordinate.get().getValue().getKey());
                String displayName = coordinate.get().getValue().getDisplayName();
                region.setDisplayName(isEmpty(displayName) ? coordinate.get().getKey().getRegionName() : displayName);
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
            Optional<Map.Entry<com.sequenceiq.cloudbreak.cloud.model.Region, Coordinate>> coordinate =
                    cloudRegions.getCoordinates().entrySet()
                            .stream()
                            .filter(e -> e.getKey().getRegionName().equals(requestedLocation.getName())
                                    || e.getValue().getDisplayName().equals(requestedLocation.getName())
                                    || e.getValue().getKey().equals(requestedLocation.getName()))
                            .findFirst();
            if (coordinate.isPresent()) {
                environment.setLocation(coordinate.get().getValue().getKey());
                environment.setLocationDisplayName(coordinate.get().getValue().getDisplayName());
                environment.setLatitude(coordinate.get().getValue().getLatitude());
                environment.setLongitude(coordinate.get().getValue().getLongitude());
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

    /**
     * The CIDR could not be edited so the first step we clear the the existed CIDR every time.
     * We are assuming the security access has the proper security group ids and we don't check it again.
     * If the knox or default security groups are blank we don't set
     *
     * @param environment    the security access will be update for this environment
     * @param securityAccess this should contains the knox and default security groups
     */
    void editSecurityAccess(Environment environment, SecurityAccessDto securityAccess) {
        environment.setCidr(null);
        if (StringUtils.isNotBlank(securityAccess.getDefaultSecurityGroupId())) {
            environment.setDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId());
        }
        if (StringUtils.isNotBlank(securityAccess.getSecurityGroupIdForKnox())) {
            environment.setSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox());
        }
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

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return getByCrnAndAccountId(resourceCrn, ThreadBasedUserCrnProvider.getAccountId()).getId();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return getByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId()).getId();
    }

    public List<String> findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(String accountId, Long parentEnvironmentId) {
        return environmentRepository.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(accountId, parentEnvironmentId);
    }

}
