package com.sequenceiq.cloudbreak.service.decorator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
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
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Measure(ClusterDecorator.class)
    public Cluster decorate(@Nonnull Cluster cluster, @Nonnull ClusterV4Request request, Blueprint blueprint, User user, Workspace workspace,
            @Nonnull Stack stack) {
        prepareBlueprint(cluster, request, workspace, Optional.ofNullable(blueprint));
        prepareClusterManagerVariant(cluster);
        validateBlueprintIfRequired(cluster, request, stack);
        prepareRds(cluster, request, stack);
        setupEmbeddedDatabase(cluster, stack);
        cluster = sharedServiceConfigProvider.configureCluster(cluster, user, workspace);
        return cluster;
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterV4Request request, Stack stack) {
        BlueprintValidator blueprintValidator = blueprintValidatorFactory.createBlueprintValidator(subject.getBlueprint());
        blueprintValidator.validate(subject.getBlueprint(), subject.getHostGroups(), new ArrayList<>(stack.getInstanceGroups()),
                request.getValidateBlueprint());
    }

    private void prepareBlueprint(Cluster subject, ClusterV4Request request, Workspace workspace, Optional<Blueprint> blueprint) {
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
                        rdsConfigService.getByNameForWorkspaceId(confName, stack.getWorkspaceId()))));
    }

    private void setupEmbeddedDatabase(Cluster cluster, Stack stack) {
        cluster.setEmbeddedDatabaseOnAttachedDisk(embeddedDatabaseService.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, cluster));
    }
}
