package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.ThrowableUtil;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentAttachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentDetachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentEditV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.api.environment.model.request.LocationV1Request;
import com.sequenceiq.environment.api.environment.model.response.DetailedEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.env.service.EnvironmentDto;
import com.sequenceiq.environment.env.service.EnvironmentStatus;
import com.sequenceiq.environment.environment.converter.network.EnvironmentNetworkConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validator.EnvironmentAttachValidator;
import com.sequenceiq.environment.environment.validator.EnvironmentCreationValidator;
import com.sequenceiq.environment.environment.validator.EnvironmentDetachValidator;
import com.sequenceiq.environment.environment.validator.EnvironmentRegionValidator;
import com.sequenceiq.environment.network.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.proxy.ProxyConfig;
import com.sequenceiq.environment.proxy.ProxyConfigService;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private EnvironmentCredentialOperationService environmentCredentialOperationService;

    @Inject
    private EnvironmentCreationValidator environmentCreationValidator;

    @Inject
    private EnvironmentAttachValidator environmentAttachValidator;

    @Inject
    private EnvironmentRegionValidator environmentRegionValidator;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private EnvironmentDetachValidator environmentDetachValidator;

    @Inject
    private EnvironmentNetworkService environmentNetworkService;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    public EnvironmentDto getById(Long id) {
        return EnvironmentDto.EnvironmentDtoBuilder.anEnvironmentDto()
                .withId(id)
                .withName("helloka")
                .withCloudPlatform("AWS")
                .withStatus(EnvironmentStatus.CREATION_INITIATED)
                .withVpcDto(NetworkDto.NetworkDtoBuilder.aVpcDto()
                        .withId(1L)
                        .withEnvironmentId(id)
                        .withStatus("ok")
                        .build())
                .build();
    }

    public Set<SimpleEnvironmentV1Response> listByAccountId(String accountId) {
        Set<SimpleEnvironmentV1Response> environmentResponses = environmentViewService.findAllByAccountId(accountId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentV1Response.class))
                .collect(Collectors.toSet());
        return environmentResponses;
    }

    public DetailedEnvironmentV1Response get(String environmentName) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
                return conversionService.convert(environment, DetailedEnvironmentV1Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
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

    public Set<Environment> findByNamesInWorkspace(Set<String> names, @NotNull Long workspaceId) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentRepository.findAllByNameInAndAccountId(names, TEMP_ACCOUNT_ID);
    }

    public SimpleEnvironmentV1Response delete(String environmentName) {
        Environment environment = getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
        LOGGER.debug(String.format("Starting to archive environment [name: %s]", environment.getName()));
        delete(environment);
        return conversionService.convert(environment, SimpleEnvironmentV1Response.class);
    }

    public Environment delete(Environment resource) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Deleting environment with name: {}", resource.getName());
        environmentRepository.delete(resource);
        return resource;
    }

    public DetailedEnvironmentV1Response createForLoggedInUser(EnvironmentV1Request request) {
        Environment environment = initEnvironment(request, TEMP_ACCOUNT_ID);
        CloudRegions cloudRegions = getCloudRegions(environment);
        setLocation(environment, request.getLocation(), cloudRegions);
        ValidationResult validationResult;
        if (cloudRegions.areRegionsSupported()) {
            setRegions(environment, request.getRegions(), cloudRegions);
        }
        validationResult = environmentCreationValidator.validate(environment, request, cloudRegions);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        environment = create(environment, TEMP_ACCOUNT_ID);
        createAndSetNetwork(environment, request.getNetwork());
        return conversionService.convert(environment, DetailedEnvironmentV1Response.class);
    }

    public Environment create(Environment environment, String accountId) {
        try {
            MDCBuilder.buildMdcContext(environment);
            environment.setAccountId(accountId);
            return environmentRepository.save(environment);
        } catch (AccessDeniedException e) {
            ConstraintViolationException cve = ThrowableUtil.getSpecificCauseRecursively(e, ConstraintViolationException.class);
            if (cve != null) {
                String message = String.format("Environment already exists with name '%s' in account %s", environment.getName(), accountId);
                throw new BadRequestException(message, e);
            }
            throw e;
        }
    }

    private Environment initEnvironment(EnvironmentV1Request request, String accountId) {
        Environment environment = conversionService.convert(request, Environment.class);
        environment.setProxyConfigs(proxyConfigService.findByNamesInAccount(request.getProxies(), accountId));
        Credential credential = environmentCredentialOperationService.getCredentialFromRequest(request, accountId);
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.getCloudPlatform());
        return environment;
    }

    private CloudRegions getCloudRegions(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest);
    }

    public DetailedEnvironmentV1Response edit(String environmentName, EnvironmentEditV1Request request) {
        Environment environment = getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
        if (StringUtils.isNotEmpty(request.getDescription())) {
            environment.setDescription(request.getDescription());
        }
        CloudRegions cloudRegions = getCloudRegions(environment);
        if (locationAndRegionChanged(request)) {
            editRegionsAndLocation(request, environment, cloudRegions);
        } else if (locationChanged(request)) {
            editLocation(request, environment, cloudRegions);
        } else if (!CollectionUtils.isEmpty(request.getRegions())) {
            LocationV1Request locationRequest = conversionService.convert(environment, LocationV1Request.class);
            request.setLocation(locationRequest);
            editRegions(request, environment, cloudRegions);
        }

        try {
            return transactionService.required(() -> {
                Environment savedEnvironment = environmentRepository.save(environment);
                return conversionService.convert(savedEnvironment, DetailedEnvironmentV1Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private boolean locationAndRegionChanged(EnvironmentEditV1Request request) {
        return !CollectionUtils.isEmpty(request.getRegions())
                && locationChanged(request);
    }

    private boolean locationChanged(EnvironmentEditV1Request request) {
        return request.getLocation() != null && !request.getLocation().isEmpty();
    }

    private void editRegionsAndLocation(EnvironmentEditV1Request request, Environment environment, CloudRegions cloudRegions) {
        validateRegionAndLocation(request.getLocation(), request.getRegions(), cloudRegions, environment);
        setLocation(environment, request.getLocation(), cloudRegions);
        setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void validateRegionAndLocation(LocationV1Request location, Set<String> requestedRegions,
            CloudRegions cloudRegions, Environment environment) {
        ValidationResultBuilder validationResultBuilder = environmentRegionValidator
                .validateRegions(requestedRegions, cloudRegions, environment.getCloudPlatform(), ValidationResult.builder());
        environmentRegionValidator.validateLocation(location, requestedRegions, environment, validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void editLocation(EnvironmentEditV1Request request, Environment environment, CloudRegions cloudRegions) {
        Set<String> regions = environment.getRegionSet().stream()
                .map(Region::getName).collect(Collectors.toSet());
        validateRegionAndLocation(request.getLocation(), regions, cloudRegions, environment);
        setLocation(environment, request.getLocation(), cloudRegions);
    }

    private void editRegions(EnvironmentEditV1Request request, Environment environment, CloudRegions cloudRegions) {
        LocationV1Request locationRequest = conversionService.convert(environment, LocationV1Request.class);
        validateRegionAndLocation(locationRequest, request.getRegions(), cloudRegions, environment);
        setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void setRegions(Environment environment, Set<String> requestedRegions, CloudRegions cloudRegions) {
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

    private void setLocation(Environment environment, LocationV1Request requestedLocation, CloudRegions cloudRegions) {
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

    public DetailedEnvironmentV1Response attachResources(String environmentName, EnvironmentAttachV1Request request) {
        try {
            return transactionService.required(() -> {
                Set<ProxyConfig> proxiesToAttach = proxyConfigService.findByNamesInAccount(request.getProxies(), TEMP_ACCOUNT_ID);
                ValidationResult validationResult = environmentAttachValidator.validate(request, proxiesToAttach);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment environment = getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
                environment = doAttach(proxiesToAttach, environment);
                return conversionService.convert(environment, DetailedEnvironmentV1Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Environment doAttach(Set<ProxyConfig> proxiesToAttach, Environment environment) {
        proxiesToAttach.removeAll(environment.getProxyConfigs());
        environment.getProxyConfigs().addAll(proxiesToAttach);
        environment = environmentRepository.save(environment);
        return environment;
    }

    public DetailedEnvironmentV1Response detachResources(String environmentName, EnvironmentDetachV1Request request) {
        try {
            return transactionService.required(() -> {
                Environment environment = getByNameForAccountId(environmentName, TEMP_ACCOUNT_ID);
                ValidationResult validationResult = validateAndDetachProxies(request, environment);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
                Environment saved = environmentRepository.save(environment);
                return conversionService.convert(saved, DetailedEnvironmentV1Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private ValidationResult validateAndDetachProxies(EnvironmentDetachV1Request request, Environment environment) {
        Set<ProxyConfig> proxiesToDetach = environment.getProxyConfigs().stream()
                .filter(proxy -> request.getProxies().contains(proxy.getName())).collect(Collectors.toSet());
        //TODO Design and implement of the deletion flow between the services
//        Map<ProxyConfig, Set<Cluster>> proxiesToClusters = proxiesToDetach.stream()
//                .collect(Collectors.toMap(proxy -> proxy, proxy -> proxyConfigService.getClustersUsingResourceInEnvironment(proxy, environment.getId())));
//        ValidationResult validationResult = environmentDetachValidator.validate(environment, proxiesToClusters).merge(validationResult);
        environment.getProxyConfigs().removeAll(proxiesToDetach);
        return ValidationResult.builder().build();
    }

    public Optional<Environment> findById(Long id) {
        return environmentRepository.findById(id);
    }

    private void createAndSetNetwork(Environment environment, EnvironmentNetworkV1Request networkRequest) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        BaseNetwork network = createNetworkIfPossible(environment, networkRequest, cloudPlatform);
        if (network != null) {
            environment.setNetwork(network);
        }
    }

    private BaseNetwork createNetworkIfPossible(Environment environment, EnvironmentNetworkV1Request networkRequest, CloudPlatform cloudPlatform) {
        BaseNetwork network = null;
        if (networkRequest != null) {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
            if (environmentNetworkConverter != null) {
                BaseNetwork baseNetwork = environmentNetworkConverter.convert(networkRequest, environment);
                network = environmentNetworkService.save(baseNetwork);
            }
        }
        return network;
    }
}