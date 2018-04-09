package com.sequenceiq.cloudbreak.service.decorator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariConfigurationService;
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
    private AmbariConfigurationService ambariConfigurationService;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    public Cluster decorate(Cluster subject, ClusterRequest request, Blueprint blueprint, IdentityUser user, Stack stack) {
        subject = prepareBlueprint(subject, request, blueprint, user, stack);
        subject = prepareHostGroups(stack, user, subject, request.getHostGroups());
        validateBlueprintIfRequired(subject, request, stack);
        subject = prepareRds(subject, user, request, stack);
        subject = clusterProxyDecorator.prepareProxyConfig(subject, user, request.getProxyName(), stack);
        subject = prepareLdap(subject, user, request.getLdapConfigId(), request.getLdapConfig(), request.getLdapConfigName(), stack);
        subject = sharedServiceConfigProvider.configureCluster(subject, user, request.getConnectedCluster());
        return subject;
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterRequest request, Stack stack) {
        if (request.getValidateBlueprint()) {
            blueprintValidator.validateBlueprintForStack(subject, subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups());
        }
    }

    private Cluster prepareBlueprint(Cluster subject, ClusterRequest request, Blueprint blueprint, IdentityUser user, Stack stack) {
        if (blueprint != null) {
            subject.setBlueprint(blueprint);
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
                throw new BadRequestException("Blueprint does not configured for the cluster!");
            }
        }
        subject.setTopologyValidation(request.getValidateBlueprint());
        return subject;
    }

    private Cluster prepareLdap(Cluster subject, IdentityUser user, Long ldapConfigId, LdapConfigRequest ldapConfigRequest, String ldapName, Stack stack) {
        if (ldapConfigId != null) {
            LdapConfig ldapConfig = ldapConfigService.get(ldapConfigId);
            subject.setLdapConfig(ldapConfig);
        } else if (ldapName != null) {
            LdapConfig ldapConfig = ldapConfigService.getPublicConfig(ldapName, user);
            subject.setLdapConfig(ldapConfig);
        } else if (ldapConfigRequest != null) {
            LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
            ldapConfig.setPublicInAccount(stack.isPublicInAccount());
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            ldapConfig = ldapConfigService.create(user, ldapConfig);
            subject.setLdapConfig(ldapConfig);
        }
        return subject;
    }

    private Cluster prepareRds(Cluster subject, IdentityUser user, ClusterRequest request, Stack stack) {
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

        ambariConfigurationService.createDefaultRdsConfigIfNeeded(stack, subject).ifPresent(rdsConfig -> subject.getRdsConfigs().add(rdsConfig));
        return subject;
    }

    private Cluster prepareHostGroups(Stack stack, IdentityUser user, Cluster cluster, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, user, stack.getId(), true, stack.isPublicInAccount());
            hostGroups.add(hostGroup);
        }
        cluster.setHostGroups(hostGroups);
        return cluster;
    }

}
