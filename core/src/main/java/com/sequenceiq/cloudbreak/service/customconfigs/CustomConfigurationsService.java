package com.sequenceiq.cloudbreak.service.customconfigs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.CustomConfigurationPropertyRepository;
import com.sequenceiq.cloudbreak.repository.CustomConfigurationsRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.CustomConfigurationsValidator;

@Service
public class CustomConfigurationsService implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigurationsService.class);

    @Inject
    private CustomConfigurationsRepository customConfigurationsRepository;

    @Inject
    private CustomConfigurationPropertyRepository customConfigurationPropertyRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private SecretService secretService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private CustomConfigurationsValidator validator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public void initializeCrnForCustomConfigs(CustomConfigurations customConfigurations, String accountId) {
        customConfigurations.setCrn(regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CUSTOM_CONFIGURATIONS, accountId));
    }

    public List<CustomConfigurations> getAll(String accountId) {
        List<CustomConfigurations> customConfigsByAccountId = customConfigurationsRepository.findCustomConfigsByAccountId(accountId);
        LOGGER.info("Retrieved {} configurations for {}", customConfigsByAccountId.size(), accountId);
        return customConfigsByAccountId;
    }

    private CustomConfigurationProperty storePropertyAsSecret(CustomConfigurationProperty property) {
        property.setSecretValue(property.getValue());
        property.setValue(null);
        return property;
    }

    private boolean propertiesNotInVault(Set<CustomConfigurationProperty> properties) {
        return properties.stream()
                .filter(p -> secretService.isSecret(p.getSecret()))
                .collect(Collectors.toSet()).isEmpty();
    }

    public void migrateToSecretStore(CustomConfigurations customConfigurations) {
        Set<CustomConfigurationProperty> properties = customConfigurations.getConfigurations()
                .stream()
                .map(this::storePropertyAsSecret)
                .collect(Collectors.toSet());
        customConfigurationPropertyRepository.saveAll(properties);
    }

    public CustomConfigurations getByNameOrCrn(NameOrCrn nameOrCrn) {
        CustomConfigurations customConfigurations = nameOrCrn.hasName()
                ? getByName(nameOrCrn.getName(), ThreadBasedUserCrnProvider.getAccountId())
                : getByCrn(nameOrCrn.getCrn());
        if (propertiesNotInVault(customConfigurations.getConfigurations())) {
            migrateToSecretStore(customConfigurations);
        }
        return customConfigurations;
    }

    public CustomConfigurations getByCrn(String crn) {
        LOGGER.info("Retrieving custom configurations by crn: {}", crn);

        return customConfigurationsRepository.findByCrn(crn).orElseThrow(NotFoundException.notFound("Custom Configurations", crn));
    }

    private void validate(CustomConfigurations customConfigurations) {
        validator.validateServiceNames(customConfigurations);
    }

    private CustomConfigurations getByName(String name, String accountId) {
        LOGGER.info("Retrieving custom configurations by name: {} for account: {}", name, accountId);
        return customConfigurationsRepository.findByNameAndAccountId(name, accountId)
                .orElseThrow(NotFoundException.notFound("Custom Configurations", name));
    }

    public CustomConfigurations create(CustomConfigurations customConfigurations, String accountId) {
        validate(customConfigurations);
        customConfigurationsRepository
                .findByNameAndAccountId(customConfigurations.getName(), accountId)
                .ifPresent(retrievedCustomConfigs -> {
                    throw new BadRequestException("Custom Configurations with name " + retrievedCustomConfigs.getName()
                        + " exists. Provide a different name"); });
        initializeCrnForCustomConfigs(customConfigurations, accountId);
        customConfigurations.setAccount(accountId);
        Set<CustomConfigurationProperty> configurationProperties = new HashSet<>(customConfigurations.getConfigurations());
        configurationProperties.forEach(config -> config.setCustomConfigurations(customConfigurations));
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(), customConfigurations.getCrn());
        customConfigurationPropertyRepository.saveAll(configurationProperties);
        return customConfigurations;
    }

    public CustomConfigurations clone(NameOrCrn nameOrCrn, String newName, String newVersion, String accountId) {
        return nameOrCrn.hasName()
                ? cloneByName(nameOrCrn.getName(), newName, newVersion, accountId)
                : cloneByCrn(nameOrCrn.getCrn(), newName, newVersion, accountId);
    }

    public CustomConfigurations cloneByName(String name, String newName, String newRuntimeVersion, String accountId) {
        CustomConfigurations customConfigurationsByName = getByName(name, accountId);
        CustomConfigurations newCustomConfigurations = cloneCustomConfigs(customConfigurationsByName, newName, newRuntimeVersion);
        return create(newCustomConfigurations, accountId);
    }

    private CustomConfigurations cloneCustomConfigs(CustomConfigurations existingCustomConfigurations, String newName, String newRuntimeVersion) {
        CustomConfigurations clone = new CustomConfigurations(existingCustomConfigurations);
        Set<CustomConfigurationProperty> newConfigSet = existingCustomConfigurations.getConfigurations()
                .stream()
                .map(config -> new CustomConfigurationProperty(
                        config.getName(),
                        config.getValue(),
                        config.getRoleType(),
                        config.getServiceType()))
                .collect(Collectors.toSet());
        clone.setConfigurations(newConfigSet);
        clone.setName(newName);
        clone.setRuntimeVersion(newRuntimeVersion);
        return clone;
    }

    public CustomConfigurations cloneByCrn(String crn, String newName, String newRuntimeVersion, String accountId) {
        CustomConfigurations customConfigurationsByCrn = getByCrn(crn);
        CustomConfigurations newCustomConfigurations = cloneCustomConfigs(customConfigurationsByCrn, newName, newRuntimeVersion);
        return create(newCustomConfigurations, accountId);
    }

    private void prepareDeletion(CustomConfigurations customConfigurations) {
        List<Cluster> clustersWithThisCustomConfigs = new ArrayList<>(clusterService.findByCustomConfigurations(customConfigurations));
        LOGGER.info("Checking deletion of custom configurations");
        if (!clustersWithThisCustomConfigs.isEmpty()) {
            if (clustersWithThisCustomConfigs.size() > 1) {
                String clusters = clustersWithThisCustomConfigs
                        .stream()
                        .map(cluster -> cluster.getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format("There are clusters associated with the Custom Configurations '%s'. " +
                        "Please delete these clusters before deleting the Custom Configurations. " +
                        "The following clusters are associated with these Custom Configurations: [%s]", customConfigurations.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster associated with the Custom Configurations '%s'. " +
                    "Please delete the cluster before deleting the Custom Configurations. " +
                    "The following cluster is associated with these Custom Configurations: %s", customConfigurations.getName(),
                    clustersWithThisCustomConfigs.get(0).getName()));
        }
    }

    public CustomConfigurations deleteByCrn(String crn) {
        CustomConfigurations customConfigurationsByCrn = getByCrn(crn);
        LOGGER.info("Deleting custom configurations: {}", customConfigurationsByCrn.getId());
        prepareDeletion(customConfigurationsByCrn);
        customConfigurationPropertyRepository.deleteAll(customConfigurationsByCrn.getConfigurations());
        ownerAssignmentService.notifyResourceDeleted(crn);
        return customConfigurationsByCrn;
    }

    public CustomConfigurations deleteByName(String name, String accountId) {
        CustomConfigurations customConfigurationsByName = getByName(name, accountId);
        LOGGER.info("Deleting custom configurations: {}", customConfigurationsByName.getId());
        prepareDeletion(customConfigurationsByName);
        customConfigurationPropertyRepository.deleteAll(customConfigurationsByName.getConfigurations());
        ownerAssignmentService.notifyResourceDeleted(customConfigurationsByName.getCrn());
        return customConfigurationsByName;
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return customConfigurationsRepository.findResourceCrnsByNamesAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), resourceNames);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CUSTOM_CONFIGURATIONS;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CUSTOM_CONFIGURATIONS);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return customConfigurationsRepository.findResourceNamesByCrnsAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), crns)
                .stream()
                .collect(Collectors.toMap(k -> k.getCrn(), v -> Optional.ofNullable(v.getName())));
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return customConfigurationsRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(NotFoundException.notFound("Custom Configurations", resourceName));
    }
}
