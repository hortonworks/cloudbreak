package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.Benchmark.multiCheckedMeasure;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConfigValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.util.Benchmark.MultiCheckedSupplier;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class ClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSetupService.class);

    @Inject
    private ClouderaManagerClusterCreationSetupService clouderaManagerClusterCreationSetupService;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private ClusterDecorator clusterDecorator;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private RdsConfigValidator rdsConfigValidator;

    @Inject
    private ClusterCreationEnvironmentValidator environmentValidator;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Measure(ClusterCreationSetupService.class)
    public void validate(ClusterV4Request request, Stack stack, User user, Workspace workspace, DetailedEnvironmentResponse environment) {
        MdcContext.builder().userCrn(user.getUserCrn()).tenant(user.getTenant().getName()).buildMdc();
        rdsConfigValidator.validateRdsConfigs(request, user, workspace);
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();

        environmentValidator.validateRdsConfigNames(request.getDatabases(), resultBuilder, stack.getWorkspaceId());
        environmentValidator.validateProxyConfig(request.getProxyConfigCrn(), resultBuilder);
        String parentEnvironmentCloudPlatform = environment.getParentEnvironmentCloudPlatform();
        environmentValidator.validateAutoTls(request, stack, resultBuilder, parentEnvironmentCloudPlatform);
        ValidationResult build = resultBuilder.build();
        if (build.hasError()) {
            throw new BadRequestException(build.getFormattedErrors());
        }
    }

    public Cluster prepare(ClusterV4Request request, Stack stack, Blueprint blueprint, User user)
            throws IOException, CloudbreakImageCatalogException, TransactionExecutionException {
        String stackName = stack.getName();
        Cluster clusterStub = stack.getCluster();
        stack.setCluster(null);

        if (request.getCloudStorage() != null) {
            FileSystem fileSystem = cloudStorageConverter.requestToFileSystem(request.getCloudStorage());
            measure(() -> fileSystemConfigService.createWithMdcContextRestore(fileSystem, stack.getWorkspace(), user),
                    LOGGER, "File system saving took {} ms for stack {}", stackName);
        }

        clusterStub.setStack(stack);
        clusterStub.setWorkspace(stack.getWorkspace());

        Cluster cluster = measure(() ->
                clusterDecorator.decorate(clusterStub, request, blueprint, user, stack.getWorkspace(), stack),
                LOGGER, "Cluster decorator {} ms for stack {}", stackName);

        List<ClusterComponent> components = multiCheckedMeasure(
                (MultiCheckedSupplier<List<ClusterComponent>, IOException, CloudbreakImageCatalogException>) () -> {
                    if (blueprint != null) {
                        Set<Component> allComponent = componentConfigProviderService.getAllComponentsByStackIdAndType(stack.getId(),
                                Sets.newHashSet(ComponentType.CM_REPO_DETAILS, cdhProductDetails(), ComponentType.IMAGE));

                        Component stackCmRepoConfig = allComponent.stream()
                                .filter(c -> c.getComponentType().equals(ComponentType.CM_REPO_DETAILS))
                                .findAny()
                                .orElse(null);

                        List<Component> stackCdhRepoConfig = allComponent.stream()
                                .filter(c -> centralCDHVersionCoordinator.isCdhProductDetails(c))
                                .collect(Collectors.toList());

                        Component stackImageComponent = allComponent.stream()
                                .filter(c -> c.getComponentType().equals(ComponentType.IMAGE) && c.getName().equalsIgnoreCase(ComponentType.IMAGE.name()))
                                .findAny()
                                .orElseThrow(() -> new NotFoundException("Image component not found for stack " + stackName));
                        return clouderaManagerClusterCreationSetupService.prepareClouderaManagerCluster(
                                request, cluster, stackCmRepoConfig, stackCdhRepoConfig, stackImageComponent);
                    }
                    return Collections.emptyList();
                }, LOGGER, "Cluster components saved in {} ms for stack {}", stackName);

        return clusterOperationService.create(stack, cluster, components, user);
    }
}