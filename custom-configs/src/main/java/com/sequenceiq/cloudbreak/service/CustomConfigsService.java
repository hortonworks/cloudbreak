package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.CustomConfigsExistsException;
import com.sequenceiq.cloudbreak.repository.CustomConfigsRepository;

@Service
public class CustomConfigsService implements ResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigsService.class);

    @Inject
    private CustomConfigsRepository customConfigsRepository;

    @Inject
    private CustomConfigsValidator validator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public CustomConfigsService(CustomConfigsRepository customConfigsRepository, CustomConfigsValidator validator) {
        this.customConfigsRepository = customConfigsRepository;
        this.validator = validator;
    }

    public String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CUSTOM_CONFIGS, accountId);
    }

    public void decorateWithCrn(CustomConfigs customConfigs, String accountId) {
        customConfigs.setResourceCrn(createCRN(accountId));
    }

    public List<CustomConfigs> getAll() {
        return customConfigsRepository.getAllCustomConfigs();
    }

    public CustomConfigs getByNameOrCrn(NameOrCrn nameOrCrn, String accountId) {
        return nameOrCrn.hasName() ? getByName(nameOrCrn.getName(), accountId) : getByCrn(nameOrCrn.getCrn());
    }

    public CustomConfigs getByCrn(String crn) {
        Optional<CustomConfigs> customConfigsByCrn = customConfigsRepository.findByResourceCrn(crn);
        if (customConfigsByCrn.isEmpty() && crn != null) {
            throw new NotFoundException("Custom Configs with crn " + crn + " does not exist.");
        }
        return crn == null ? null : customConfigsByCrn.get();
    }

    private List<CustomConfigProperty> getCustomServiceConfigs(Set<CustomConfigProperty> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() == null)
                .collect(Collectors.toList());
    }

    private List<CustomConfigProperty> getCustomRoleConfigs(Set<CustomConfigProperty> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() != null)
                .collect(Collectors.toList());
    }

    private void validate(CustomConfigs customConfigs) {
        try {
            validator.validateServiceNames(customConfigs);
        } catch (IOException e) {
            LOGGER.error("Could not validate Custom configs");
        }
    }

    public Map<String, List<ApiClusterTemplateConfig>> getCustomServiceConfigsMap(CustomConfigs customConfigs) {
        Map<String, List<ApiClusterTemplateConfig>> serviceMappedToConfigs = new HashMap<>();
        List<CustomConfigProperty> customServiceConfigsList = getCustomServiceConfigs(customConfigs.getConfigs());
        customServiceConfigsList.forEach(serviceConfig -> {
            serviceMappedToConfigs.computeIfAbsent(serviceConfig.getServiceType(), k -> new ArrayList<>());
            serviceMappedToConfigs.get(serviceConfig.getServiceType()).add(new ApiClusterTemplateConfig()
                    .name(serviceConfig.getConfigName()).value(serviceConfig.getConfigValue()));
        });
        return serviceMappedToConfigs;
    }

    public Map<String, List<ApiClusterTemplateRoleConfigGroup>> getCustomRoleConfigsMap(CustomConfigs customConfigs) {
        Map<String, List<ApiClusterTemplateRoleConfigGroup>> serviceMappedToRoleConfigs = new HashMap<>();
        List<CustomConfigProperty> customRoleConfigGroupsList = getCustomRoleConfigs(customConfigs.getConfigs());
        customRoleConfigGroupsList.forEach(customRoleConfigGroup -> {
            String configName = customRoleConfigGroup.getConfigName();
            String configValue = customRoleConfigGroup.getConfigValue();
            serviceMappedToRoleConfigs.computeIfAbsent(customRoleConfigGroup.getServiceType(), k -> new ArrayList<>());
            Optional<ApiClusterTemplateRoleConfigGroup> roleConfigGroupIfExists = serviceMappedToRoleConfigs.get(customRoleConfigGroup.getServiceType()).stream()
                    .filter(rcg -> rcg.getRoleType().equalsIgnoreCase(customRoleConfigGroup.getRoleType()))
                    .findFirst();
            roleConfigGroupIfExists.ifPresentOrElse(roleConfigGroup -> roleConfigGroup.getConfigs().add(new ApiClusterTemplateConfig()
                            .name(configName).value(configValue)),
                    () -> serviceMappedToRoleConfigs.get(customRoleConfigGroup.getServiceType()).add(new ApiClusterTemplateRoleConfigGroup()
                    .roleType(customRoleConfigGroup.getRoleType()).addConfigsItem(new ApiClusterTemplateConfig().name(configName)
                            .value(configValue))));
        });
        return serviceMappedToRoleConfigs;
    }

    public CustomConfigs getByName(String name, String accountId) {
        Optional<CustomConfigs> customConfigsByName = customConfigsRepository.findByNameAndAccountId(name, accountId);
        if (customConfigsByName.isEmpty() && name != null) {
            throw new NotFoundException("Custom configs with name " + name + " does not exist");
        }
        return name == null ? null : customConfigsByName.get();
    }

    public CustomConfigs create(CustomConfigs customConfigs, String accountId) {
        //validation
        validate(customConfigs);
        Optional<CustomConfigs> customConfigsByName =
                customConfigsRepository.findByNameAndAccountId(customConfigs.getName(), accountId);
        if (customConfigsByName.isPresent()) {
            throw new CustomConfigsExistsException("Custom Configs with name " +
                    customConfigsByName.get().getName() + "exists. Provide a different name");
        }
        decorateWithCrn(customConfigs, accountId);
        customConfigs.setAccount(accountId);
        customConfigs.getConfigs().forEach(config -> config.setCustomConfigs(customConfigs));
        customConfigsRepository.save(customConfigs);
        return customConfigs;
    }

    public CustomConfigs clone(NameOrCrn nameOrCrn, String newName, String accountId) {
        return nameOrCrn.hasName() ? cloneByName(nameOrCrn.getName(), newName, accountId) : cloneByCrn(nameOrCrn.getCrn(), newName, accountId);
    }

    public CustomConfigs cloneByName(String name, String newCustomConfigsName, String accountId) {
        Optional<CustomConfigs> customConfigsByName = Optional.of(customConfigsRepository.findByNameAndAccountId(name, accountId)
                .orElseThrow(() -> new NotFoundException("Custom configs with name " + name + " does not exist. Cannot be cloned.")));
        CustomConfigs newCustomConfigs = new CustomConfigs(customConfigsByName.get());
        Set<CustomConfigProperty> newConfigSet = Set.copyOf(newCustomConfigs.getConfigs());
        // removing associations of new configs set with original configs set
        newConfigSet.forEach(config -> {
            config.setId(null);
            config.setCustomConfigs(null);
        });
        newCustomConfigs.setConfigs(newConfigSet);
        newCustomConfigs.setName(newCustomConfigsName);
        return create(newCustomConfigs, accountId);
    }

    public CustomConfigs cloneByCrn(String crn, String newCustomConfigsName, String accountId) {
        Optional<CustomConfigs> customConfigsByCrn = Optional.of(customConfigsRepository.findByResourceCrn(crn))
                .orElseThrow(() -> new NotFoundException("Custom configs with crn " + crn + " not found. Cannot be cloned."));
        CustomConfigs newCustomConfigs = new CustomConfigs(customConfigsByCrn.get());
        Set<CustomConfigProperty> newConfigSet = Set.copyOf(customConfigsByCrn.get().getConfigs());
        newConfigSet.forEach(config -> {
            config.setId(null);
            config.setCustomConfigs(null);
        });
        newCustomConfigs.setConfigs(newConfigSet);
        newCustomConfigs.setName(newCustomConfigsName);
        return create(newCustomConfigs, accountId);
    }

    public CustomConfigs deleteByCrn(String crn) {
        Optional<CustomConfigs> customServiceConfigsByCrn = Optional.of(customConfigsRepository.findByResourceCrn(crn)
                .orElseThrow(NotFoundException.notFound("CustomConfigs", crn)));
        customConfigsRepository.deleteById(customServiceConfigsByCrn.get().getId());
        return customServiceConfigsByCrn.get();
    }

    public CustomConfigs deleteByName(String name, String accountId) {
        Optional<CustomConfigs> customServiceConfigsByName = Optional.of(customConfigsRepository.findByNameAndAccountId(name, accountId)
                .orElseThrow(NotFoundException.notFound("CustomConfigs", name)));
        customConfigsRepository.deleteById(customServiceConfigsByName.get().getId());
        return customServiceConfigsByName.get();
    }

    public List<CustomConfigs> deleteMultiple(Set<String> names, String accountId) {
        return customConfigsRepository.deleteMultipleByNames(accountId, names);
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return customConfigsRepository.findResourceCrnsByNamesAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), resourceNames);
    }

    @Override
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.CUSTOM_CONFIGS);
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CUSTOM_CONFIGS);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrns(Collection<String> crns) {
        Map<String, Optional<String>> crnMappedToName = new HashMap<>();
        customConfigsRepository.findResourceNamesByCrnsAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), crns)
                .forEach(nameAndCrn -> crnMappedToName.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return crnMappedToName;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return customConfigsRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(NotFoundException.notFound("CustomConfigs", resourceName));
    }
}
