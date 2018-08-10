package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private HostGroupDecorator hostGroupDecorator;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private ClusterProxyDecorator clusterProxyDecorator;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private AmbariHaComponentFilter ambariHaComponentFilter;

    public Cluster decorate(@Nonnull Cluster subject, @Nonnull ClusterRequest request, Blueprint blueprint, @Nonnull IdentityUser user, @Nonnull Stack stack) {
        prepareBlueprint(subject, request, user, stack, Optional.ofNullable(blueprint));
        prepareHostGroups(stack, user, subject, request.getHostGroups());
        validateBlueprintIfRequired(subject, request, stack);
        prepareRds(subject, user, request, stack);
        subject = clusterProxyDecorator.prepareProxyConfig(subject, user, request.getProxyName(), stack);
        prepareLdap(subject, user, stack, Optional.ofNullable(request.getLdapConfigId()), Optional.ofNullable(request.getLdapConfig()),
                Optional.ofNullable(request.getLdapConfigName()));
        subject = sharedServiceConfigProvider.configureCluster(subject, user, request.getConnectedCluster());
        return subject;
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterRequest request, Stack stack) {
        if (request.getValidateBlueprint()) {
            blueprintValidator.validateBlueprintForStack(subject, subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups());
        }
    }

    private void prepareBlueprint(Cluster subject, ClusterRequest request, IdentityUser user, Stack stack, Optional<Blueprint> blueprint) {
        if (blueprint.isPresent()) {
            subject.setBlueprint(blueprint.get());
        } else {
            if (request.getBlueprintId() != null) {
                subject.setBlueprint(blueprintService.get(request.getBlueprintId()));
            } else if (request.getBlueprint() != null) {
                Blueprint newBlueprint = conversionService.convert(request.getBlueprint(), Blueprint.class);
                newBlueprint.setPublicInAccount(stack.isPublicInAccount());
                newBlueprint = blueprintService.create(user, newBlueprint, new ArrayList<>());
                subject.setBlueprint(newBlueprint);
            } else if (!Strings.isNullOrEmpty(request.getBlueprintName())) {
                subject.setBlueprint(blueprintService.get(request.getBlueprintName(), user.getAccount()));
            } else {
                throw new BadRequestException("Blueprint is not configured for the cluster!");
            }
        }
        removeHaComponentsFromGatewayTopologies(subject);
        subject.setTopologyValidation(request.getValidateBlueprint());
    }

    // because KNOX does not support them
    private void removeHaComponentsFromGatewayTopologies(Cluster subject) {
        Set<String> haComponents = ambariHaComponentFilter.getHaComponents(new BlueprintTextProcessor(subject.getBlueprint().getBlueprintText()));
        Set<String> haKnoxServices = ExposedService.filterSupportedKnoxServices().stream()
                .filter(es -> haComponents.contains(es.getServiceName())
                        && !ExposedService.RANGER.getServiceName().equalsIgnoreCase(es.getServiceName()))
                .map(ExposedService::getKnoxService)
                .collect(Collectors.toSet());
        if (subject.getGateway() != null) {
            subject.getGateway().getTopologies().forEach(topology -> {
                try {
                    ExposedServices exposedServicesOfTopology = topology.getExposedServices().get(ExposedServices.class);
                    exposedServicesOfTopology.getServices().removeAll(haKnoxServices);
                    topology.setExposedServices(new Json(exposedServicesOfTopology));
                } catch (IOException e) {
                    LOGGER.warn("This exception should never occur.", e);
                }
            });
        }
    }

    private void prepareLdap(Cluster subject, IdentityUser user, Stack stack, Optional<Long> ldapConfigId, Optional<LdapConfigRequest> ldapConfigRequest,
            Optional<String> ldapName) {
        if (ldapConfigId.isPresent()) {
            LdapConfig ldapConfig = ldapConfigService.get(ldapConfigId.get());
            subject.setLdapConfig(ldapConfig);
        } else if (ldapName.isPresent()) {
            LdapConfig ldapConfig = ldapConfigService.getByNameFromUsersDefaultOrganization(ldapName.get());
            subject.setLdapConfig(ldapConfig);
        } else if (ldapConfigRequest.isPresent()) {
            LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest.get(), LdapConfig.class);
            ldapConfig.setPublicInAccount(stack.isPublicInAccount());
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            ldapConfig = ldapConfigService.create(user, ldapConfig);
            subject.setLdapConfig(ldapConfig);
        }
    }

    private void prepareRds(Cluster subject, IdentityUser user, ClusterRequest request, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        if (request.getRdsConfigIds() != null) {
            for (Long rdsConfigId : request.getRdsConfigIds()) {
                RDSConfig rdsConfig = rdsConfigService.get(rdsConfigId);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        if (request.getRdsConfigJsons() != null) {
            for (RDSConfigRequest requestRdsConfig : request.getRdsConfigJsons()) {
                RDSConfig rdsConfig = conversionService.convert(requestRdsConfig, RDSConfig.class);
                rdsConfig.setPublicInAccount(stack.isPublicInAccount());
                rdsConfig = rdsConfigService.createIfNotExists(user, rdsConfig);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        Optional.of(request.getRdsConfigNames())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(rdsConfigService.getPublicRdsConfig(confName, user))));

        if (request.getAmbariDatabaseDetails() != null) {
            RDSConfig rdsConfig = ambariDatabaseMapper.mapAmbariDatabaseDetailsJsonToRdsConfig(request.getAmbariDatabaseDetails(), subject, stack,
                    stack.isPublicInAccount());
            subject.getRdsConfigs().add(rdsConfigService.createIfNotExists(user, rdsConfig));
        }
    }

    private void prepareHostGroups(Stack stack, IdentityUser user, Cluster cluster, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, user, stack.getId(), true, stack.isPublicInAccount());
            hostGroups.add(hostGroup);
        }
        cluster.setHostGroups(hostGroups);
    }

}
