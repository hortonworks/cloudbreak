package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.account.AbstractAccountAwareResourceService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@Service
public class EnvironmentService extends AbstractAccountAwareResourceService<Environment> implements ResourceIdProvider, EnvironmentPropertyProvider,
        PayloadContextProvider, CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Value("${environment.admin.group.default.prefix:}")
    private String adminGroupNamePrefix;

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentRepository environmentRepository;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final OwnerAssignmentService ownerAssignmentService;

    private final GrpcUmsClient grpcUmsClient;

    private final TransactionService transactionService;

    private final RoleCrnGenerator roleCrnGenerator;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public EnvironmentService(
            EnvironmentValidatorService validatorService,
            EnvironmentRepository environmentRepository,
            PlatformParameterService platformParameterService,
            EnvironmentDtoConverter environmentDtoConverter,
            OwnerAssignmentService ownerAssignmentService,
            GrpcUmsClient grpcUmsClient,
            TransactionService transactionService,
            RoleCrnGenerator roleCrnGenerator,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.validatorService = validatorService;
        this.environmentRepository = environmentRepository;
        this.platformParameterService = platformParameterService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.ownerAssignmentService = ownerAssignmentService;
        this.grpcUmsClient = grpcUmsClient;
        this.transactionService = transactionService;
        this.roleCrnGenerator = roleCrnGenerator;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public Environment save(Environment environment) {
        LOGGER.debug("Saving environment '{}'.", environment);
        return environmentRepository.save(environment);
    }

    public Environment saveWithOwnerRoleAssignment(Environment environment) {
        try {
            return transactionService.required(() -> {
                Environment saved = save(environment);
                ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(),
                        saved.getResourceCrn(), ThreadBasedUserCrnProvider.getAccountId());
                return saved;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new InternalServerErrorException("Error happened during saving environment state and assigned owner role: ", e);
        }
    }

    public EnvironmentDto getEnvironmentDto(Environment environment) {
        return environmentDtoConverter.environmentToDto(environment);
    }

    public EnvironmentDto getByNameAndAccountId(String environmentName, String accountId) {
        LOGGER.debug("Getting environment by name: '{}'.", environmentName);
        Optional<Environment> environment = environmentRepository.findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(notFound("Environment with name:", environmentName)));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public EnvironmentDto getByCrnAndAccountId(String crn, String accountId) {
        LOGGER.debug("Getting environment by crn '{}' and account id '{}'.", crn, accountId);
        Optional<Environment> environment = environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(() -> new NotFoundException(String.format("No environment found with resource CRN '%s'.", crn))));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public List<EnvironmentDto> listByAccountId(String accountId) {
        LOGGER.debug("Listing environments by account id '{}'.", accountId);
        Set<Environment> environments = environmentRepository.findByAccountId(accountId);
        return environments.stream().map(environmentDtoConverter::environmentToDto).collect(Collectors.toList());
    }

    public EnvironmentDto internalGetByCrn(String crn) {
        LOGGER.debug("Listing environments by crn '{}'.", crn);
        Optional<Environment> environment = environmentRepository.findOneByResourceCrnEvenIfDeleted(crn);
        MDCBuilder.buildMdcContext(environment.orElseThrow(() -> new NotFoundException(String.format("No environment found with resource CRN '%s'.", crn))));
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public void setRegions(Environment environment, Set<String> requestedRegions, CloudRegions cloudRegions) {
        Set<Region> regionSet = new HashSet<>();
        LOGGER.debug("Setting regions for environment. Regions: [{}].", String.join(", ", requestedRegions));
        setRegionsFromCloudCoordinates(requestedRegions, cloudRegions, regionSet);
        if (regionSet.isEmpty()) {
            setRegionsFromCloudRegions(requestedRegions, cloudRegions, regionSet);
        }
        environment.setRegions(regionSet);
    }

    private void setRegionsFromCloudCoordinates(Set<String> requestedRegions, CloudRegions cloudRegions, Set<Region> regionSet) {
        for (String requestedRegion : requestedRegions) {
            Optional<Entry<com.sequenceiq.cloudbreak.cloud.model.Region, Coordinate>> coordinate =
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
    }

    private void setRegionsFromCloudRegions(Set<String> requestedRegions, CloudRegions cloudRegions, Set<Region> regionSet) {
        for (String requestedRegion : requestedRegions) {
            Set<String> regions = cloudRegions.getCloudRegions().keySet()
                    .stream().map(region -> region.getRegionName()).collect(Collectors.toSet());
            if (regions.contains(requestedRegion)) {
                Region region = new Region();
                region.setName(requestedRegion);
                region.setDisplayName(requestedRegion);
                regionSet.add(region);
            }
        }
    }

    public CloudRegions getRegionsByEnvironment(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest, false);
    }

    public Environment findEnvironmentByIdOrThrow(Long id) {
        Optional<Environment> environmentOptional = findEnvironmentById(id);
        return environmentOptional.orElseThrow(notFound("Environment", id));
    }

    public Optional<Environment> findEnvironmentById(Long id) {
        return environmentRepository.findById(id);
    }

    public Optional<EnvironmentDto> findById(Long id) {
        return environmentRepository.findById(id).map(environmentDtoConverter::environmentToDto);
    }

    public List<EnvironmentDto> findAllByIds(List<Long> ids) {
        return Lists.newArrayList(environmentRepository.findAllByIdNotArchived(ids))
                .stream()
                .map(environmentDtoConverter::environmentToDto)
                .collect(Collectors.toList());
    }

    public List<ResourceWithId> findAsAuthorizationResourcesInAccount(String accountId) {
        return environmentRepository.findAsAuthorizationResourcesInAccount(accountId);
    }

    public void setLocation(Environment environment, RegionWrapper regionWrapper, CloudRegions cloudRegions) {
        if (regionWrapper != null) {
            LOGGER.debug("Setting location for environment. Location: '{}'.", regionWrapper);
            Optional<Entry<com.sequenceiq.cloudbreak.cloud.model.Region, Coordinate>> coordinate =
                    cloudRegions.getCoordinates().entrySet()
                            .stream()
                            .filter(e -> e.getKey().getRegionName().equals(regionWrapper.getName())
                                    || e.getValue().getDisplayName().equals(regionWrapper.getName())
                                    || e.getValue().getKey().equals(regionWrapper.getName()))
                            .findFirst();
            if (coordinate.isPresent()) {
                environment.setLocation(coordinate.get().getValue().getKey());
                environment.setLocationDisplayName(coordinate.get().getValue().getDisplayName());
                environment.setLatitude(coordinate.get().getValue().getLatitude());
                environment.setLongitude(coordinate.get().getValue().getLongitude());
            } else if (regionWrapper.getLatitude() != null && regionWrapper.getLongitude() != null) {
                environment.setLocation(regionWrapper.getName());
                environment.setLocationDisplayName(regionWrapper.getDisplayName());
                environment.setLatitude(regionWrapper.getLatitude());
                environment.setLongitude(regionWrapper.getLongitude());
            } else {
                throw new BadRequestException(String.format("No location found with name %s in the location list. The supported locations are: [%s]",
                        regionWrapper.getName(), cloudRegions.locationNames()));
            }
        }
    }

    EnvironmentValidatorService getValidatorService() {
        return validatorService;
    }

    boolean isNameOccupied(String name, String accountId) {
        return environmentRepository.existsWithNameAndAccountAndArchivedIsFalse(name, accountId);
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
        LOGGER.debug("Editing security access for environment.");
        environment.setCidr(null);
        if (StringUtils.isNotBlank(securityAccess.getDefaultSecurityGroupId())) {
            environment.setDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId());
        }
        if (StringUtils.isNotBlank(securityAccess.getSecurityGroupIdForKnox())) {
            environment.setSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox());
        }
    }

    void setSecurityAccess(Environment environment, SecurityAccessDto securityAccess) {
        LOGGER.debug("Setting security access for environment.");
        if (securityAccess != null) {
            environment.setCidr(securityAccess.getCidr());
            environment.setSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox());
            environment.setDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId());
        }
    }

    public void setAdminGroupName(Environment environment, String adminGroupName) {
        if (isEmpty(adminGroupName)) {
            environment.setAdminGroupName(adminGroupNamePrefix + environment.getName());
        } else {
            environment.setAdminGroupName(adminGroupName);
        }
    }

    public String getCrnByNameAndAccountId(String environmentName, String accountId) {
        return environmentRepository.findResourceCrnByNameAndAccountId(environmentName, accountId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Environment with name '%s' was not found for account '%s'.", environmentName, accountId)));
    }

    public String getNameByCrnAndAccountId(String environmentCrn, String accountId) {
        return environmentRepository.findNameByResourceCrnAndAccountId(environmentCrn, accountId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Environment with crn '%s' was not found for account '%s'.", environmentCrn, accountId)));
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return environmentRepository.findIdByResourceCrnAndAccountIdAndArchivedIsFalse(resourceCrn, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Environment with crn:", resourceCrn));
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return environmentRepository.findIdByNameAndAccountIdAndArchivedIsFalse(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Environment with name:", resourceName));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return environmentRepository.findStackAsPayloadContext(resourceId).orElse(null);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return environmentRepository.findAllCrnByNameInAndAccountIdAndArchivedIsFalse(resourceNames, ThreadBasedUserCrnProvider.getAccountId());
    }

    public List<Environment> findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(String accountId, Long parentEnvironmentId) {
        return environmentRepository.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(accountId, parentEnvironmentId);
    }

    public void assignEnvironmentAdminRole(String userCrn, String environmentCrn) {
        try {
            grpcUmsClient.assignResourceRole(userCrn,
                    environmentCrn,
                    roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(Crn.safeFromString(environmentCrn).getAccountId()),
                    regionAwareInternalCrnGeneratorFactory);
            LOGGER.debug("EnvironmentAdmin role of {} environemnt is successfully assigned to the {} user", environmentCrn, userCrn);
        } catch (StatusRuntimeException ex) {
            if (Code.ALREADY_EXISTS.equals(ex.getStatus().getCode())) {
                LOGGER.debug("EnvironmentAdmin role of {} environemnt is already assigned to the {} user", environmentCrn, userCrn);
            } else {
                LOGGER.warn(String.format("EnvironmentAdmin role of %s environment cannot be assigned to the %s user", environmentCrn, userCrn), ex);
            }
        } catch (RuntimeException rex) {
            LOGGER.warn(String.format("EnvironmentAdmin role of %s environment cannot be assigned to the %s user", environmentCrn, userCrn), rex);
        }
    }

    public List<JobResource> findAllForAutoSync() {
        return environmentRepository.findAllRunningAndStatusIn(List.of(
                EnvironmentStatus.AVAILABLE,
                EnvironmentStatus.UPDATE_FAILED,
                EnvironmentStatus.START_DATAHUB_FAILED,
                EnvironmentStatus.START_DATALAKE_FAILED,
                EnvironmentStatus.START_FREEIPA_FAILED,
                EnvironmentStatus.START_DATAHUB_STARTED,
                EnvironmentStatus.START_DATALAKE_STARTED,
                EnvironmentStatus.START_FREEIPA_STARTED,
                EnvironmentStatus.START_SYNCHRONIZE_USERS_STARTED,
                EnvironmentStatus.START_SYNCHRONIZE_USERS_FAILED,
                EnvironmentStatus.STOP_DATAHUB_FAILED,
                EnvironmentStatus.STOP_DATALAKE_FAILED,
                EnvironmentStatus.STOP_FREEIPA_FAILED,
                EnvironmentStatus.STOP_DATAHUB_STARTED,
                EnvironmentStatus.STOP_DATALAKE_STARTED,
                EnvironmentStatus.STOP_FREEIPA_STARTED,
                EnvironmentStatus.ENV_STOPPED,
                EnvironmentStatus.UPGRADE_CCM_FAILED,
                EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS,
                EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED,
                EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS,
                EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED,
                EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS,
                EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED,
                EnvironmentStatus.UPGRADE_CCM_TUNNEL_UPDATE_IN_PROGRESS,
                EnvironmentStatus.UPGRADE_CCM_TUNNEL_UPDATE_FAILED,
                EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS,
                EnvironmentStatus.UPGRADE_CCM_VALIDATION_FAILED,
                EnvironmentStatus.UPGRADE_CCM_ROLLING_BACK
        ));
    }

    @Override
    protected AccountAwareResourceRepository<Environment, Long> repository() {
        return environmentRepository;
    }

    @Override
    protected void prepareDeletion(Environment resource) {

    }

    @Override
    protected void prepareCreation(Environment resource) {

    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        environmentRepository.findResourceNamesByCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId()).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }

    public Optional<Environment> getById(Long environmentId) {
        return environmentRepository.findById(environmentId);
    }

    public Environment updateTunnelByEnvironmentId(Long environmentId, Tunnel tunnel) {
        Optional<Environment> optEnv = getById(environmentId);
        if (optEnv.isPresent()) {
            Environment env = optEnv.get();
            ExperimentalFeatures experimentalFeatures = env.getExperimentalFeaturesJson();
            LOGGER.debug("Updating tunnel type from {} to {} in environment {}", experimentalFeatures.getTunnel(), tunnel, env.getName());
            experimentalFeatures.setTunnel(tunnel);
            env.setExperimentalFeaturesJson(experimentalFeatures);
            return save(env);
        } else {
            throw notFoundException("Environment with ID: ", environmentId.toString());
        }
    }

}
