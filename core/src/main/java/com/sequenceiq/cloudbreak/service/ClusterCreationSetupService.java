package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.mpack.ManagementPackValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConfigValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.util.Benchmark.MultiCheckedSupplier;

@Service
public class ClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSetupService.class);

    @Inject
    private AmbariClusterCreationSetupService ambariClusterCreationSetupService;

    @Inject
    private ClouderaManagerClusterCreationSetupService clouderaManagerClusterCreationSetupService;

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ClusterDecorator clusterDecorator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ManagementPackValidator mpackValidator;

    @Inject
    private RdsConfigValidator rdsConfigValidator;

    @Inject
    private ClusterCreationEnvironmentValidator environmentValidator;

    public void validate(ClusterV4Request request, Stack stack, User user, Workspace workspace) {
        validate(request, null, stack, user, workspace);
    }

    @Measure(ClusterCreationSetupService.class)
    public void validate(ClusterV4Request request, CloudCredential cloudCredential, Stack stack, User user, Workspace workspace) {
        if (stack.getDatalakeResourceId() != null && StringUtils.isNotBlank(request.getKerberosName())) {
            throw new BadRequestException("Invalid kerberos settings, attached cluster should inherit kerberos parameters");
        }
        MDCBuilder.buildUserMdcContext(user.getUserId(), user.getUserName());
        CloudCredential credential = cloudCredential;
        if (credential == null) {
            credential = credentialToCloudCredentialConverter.convert(stack.getCredential());
        }
        fileSystemValidator.validateCloudStorage(stack.cloudPlatform(), credential, request.getCloudStorage(),
                stack.getCreator().getUserId(), stack.getWorkspace().getId());
        mpackValidator.validateMpacks(request.getAmbari(), workspace);
        rdsConfigValidator.validateRdsConfigs(request, user, workspace);
        ValidationResult environmentValidationResult = environmentValidator.validate(request, stack);
        if (environmentValidationResult.hasError()) {
            throw new BadRequestException(environmentValidationResult.getFormattedErrors());
        }
    }

    public Cluster prepare(ClusterV4Request request, Stack stack, User user) throws CloudbreakImageNotFoundException, IOException,
            TransactionExecutionException {
        return prepare(request, stack, null, user);
    }

    public Cluster prepare(ClusterV4Request request, Stack stack, Blueprint blueprint, User user) throws IOException,
            CloudbreakImageNotFoundException, TransactionExecutionException {
        String stackName = stack.getName();
        Cluster clusterStub = stack.getCluster();
        stack.setCluster(null);

        decorateHostGroupWithConstraint(stack, clusterStub);

        if (request.getCloudStorage() != null) {
            FileSystem fs = measure(() -> fileSystemConfigService.create(converterUtil.convert(request.getCloudStorage(), FileSystem.class),
                        stack.getWorkspace(), stack.getCreator()),
                    LOGGER, "File system saving took {} ms for stack {}", stackName);
        }

        clusterStub.setStack(stack);
        clusterStub.setWorkspace(stack.getWorkspace());

        Cluster cluster = clusterDecorator.decorate(clusterStub, request, blueprint, user, stack.getWorkspace(), stack);

        decorateStackWithCustomDomainIfAdOrIpaJoinable(stack, cluster);

        List<ClusterComponent> components = checkedMeasure((MultiCheckedSupplier<List<ClusterComponent>, IOException, CloudbreakImageNotFoundException>) () -> {
            if (blueprint != null) {
                Set<Component> allComponent = componentConfigProviderService.getAllComponentsByStackIdAndType(stack.getId(),
                        Sets.newHashSet(ComponentType.AMBARI_REPO_DETAILS,
                                ComponentType.HDP_REPO_DETAILS,
                                ComponentType.CM_REPO_DETAILS,
                                ComponentType.CDH_PRODUCT_DETAILS,
                                ComponentType.IMAGE));
                Optional<Component> stackAmbariRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.AMBARI_REPO_DETAILS)
                        && c.getName().equalsIgnoreCase(ComponentType.AMBARI_REPO_DETAILS.name())).findAny();
                Optional<Component> stackHdpRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.HDP_REPO_DETAILS)
                        && c.getName().equalsIgnoreCase(ComponentType.HDP_REPO_DETAILS.name())).findAny();
                Optional<Component> stackCmRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.CM_REPO_DETAILS)).findAny();
                List<Component> stackCdhRepoConfig =
                        allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.CDH_PRODUCT_DETAILS)).collect(Collectors.toList());
                Optional<Component> stackImageComponent = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.IMAGE)
                        && c.getName().equalsIgnoreCase(ComponentType.IMAGE.name())).findAny();

                if (blueprintService.isAmbariBlueprint(blueprint)) {
                    return ambariClusterCreationSetupService.prepareAmbariCluster(
                            request, stack, blueprint, cluster, stackAmbariRepoConfig, stackHdpRepoConfig, stackImageComponent);
                } else if (blueprintService.isClouderaManagerTemplate(blueprint)) {
                    return clouderaManagerClusterCreationSetupService.prepareClouderaManagerCluster(
                            request, cluster, stackCmRepoConfig, stackCdhRepoConfig, stackImageComponent);
                }
            }
            return Collections.emptyList();
        }, LOGGER, "Cluster components saved in {} ms for stack {}", stackName);

        return clusterService.create(stack, cluster, components, user);
    }

    private void decorateHostGroupWithConstraint(Stack stack, Cluster cluster) {
        stack.getInstanceGroups().forEach(ig -> cluster.getHostGroups().stream()
                .filter(hostGroup -> hostGroup.getName().equals(ig.getGroupName()))
                .findFirst()
                .ifPresent(hostGroup -> {
                    Constraint constraint = new Constraint();
                    constraint.setInstanceGroup(ig);
                    hostGroup.setConstraint(constraint);
                }));
    }

    private void decorateStackWithCustomDomainIfAdOrIpaJoinable(Stack stack, Cluster cluster) {
        if (cluster.getKerberosConfig() != null && StringUtils.isNotBlank(cluster.getKerberosConfig().getDomain())) {
            stack.setCustomDomain(cluster.getKerberosConfig().getDomain());
        }
    }
}
