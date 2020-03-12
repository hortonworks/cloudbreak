package com.sequenceiq.cloudbreak.service.decorator;

import java.util.HashSet;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class ClusterDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidatorFactory blueprintValidatorFactory;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Measure(ClusterDecorator.class)
    public Cluster decorate(@Nonnull Cluster cluster, @Nonnull ClusterV4Request request, Blueprint blueprint, User user, Workspace workspace,
            @Nonnull Stack stack) {
        prepareBlueprint(cluster, request, workspace, stack, Optional.ofNullable(blueprint), user);
        prepareClusterManagerVariant(cluster);
        validateBlueprintIfRequired(cluster, request, stack);
        prepareRds(cluster, request, stack);
        prepareAutoTlsFlag(cluster, request, stack);
        cluster = sharedServiceConfigProvider.configureCluster(cluster, user, workspace);
        return cluster;
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterV4Request request, Stack stack) {
        BlueprintValidator blueprintValidator = blueprintValidatorFactory.createBlueprintValidator(subject.getBlueprint());
        blueprintValidator.validate(subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups(),
                request.getValidateBlueprint());
    }

    private void prepareBlueprint(Cluster subject, ClusterV4Request request, Workspace workspace, Stack stack,
            Optional<Blueprint> blueprint, User user) {
        if (blueprint.isPresent()) {
            subject.setBlueprint(blueprint.get());
        } else if (!Strings.isNullOrEmpty(request.getBlueprintName())) {
            subject.setBlueprint(blueprintService.getByNameForWorkspace(request.getBlueprintName(), workspace));
        } else {
            throw new BadRequestException("Cluster definition is not configured for the cluster!");
        }
        subject.setTopologyValidation(request.getValidateBlueprint());
    }

    private void prepareClusterManagerVariant(Cluster cluster) {
        cluster.setVariant(blueprintService.getBlueprintVariant(cluster.getBlueprint()));
    }

    private void prepareRds(Cluster subject, ClusterV4Request request, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        Optional.ofNullable(request.getDatabases())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(
                        rdsConfigService.getByNameForWorkspace(confName, stack.getWorkspace()))));
    }

    private void prepareAutoTlsFlag(Cluster cluster, ClusterV4Request request, Stack stack) {
        cluster.setAutoTlsEnabled(Optional.ofNullable(request.getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls)
                .orElseGet(() -> {
                    CloudConnector<Object> connector = cloudPlatformConnectors.get(
                            Platform.platform(stack.cloudPlatform()), Variant.variant(stack.getPlatformVariant()));
                    PlatformParameters platformParameters = connector.parameters();
                    return platformParameters.isAutoTlsSupported();
                }));
    }
}
