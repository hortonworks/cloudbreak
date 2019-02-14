package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintViewProvider;
import com.sequenceiq.cloudbreak.clusterdefinition.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.clusterdefinition.nifi.HdfConfigProvider;
import com.sequenceiq.cloudbreak.clusterdefinition.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;

@Component
public class StackToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<Stack, TemplatePreparationObject> {

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private InstanceGroupMetadataCollector instanceGroupMetadataCollector;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private HdfConfigProvider hdfConfigProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private SharedServiceConfigsViewProvider sharedServiceConfigProvider;

    @Inject
    private AmbariBlueprintViewProvider ambariBlueprintViewProvider;

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    @Override
    public TemplatePreparationObject convert(Stack source) {
        try {
            Optional<SmartSenseSubscription> aDefault = smartSenseSubscriptionService.getDefault();
            Cluster cluster = clusterService.getById(source.getCluster().getId());
            FileSystem fileSystem = cluster.getFileSystem();
            LdapConfig ldapConfig = cluster.getLdapConfig();
            StackRepoDetails hdpRepo = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
            String stackRepoDetailsHdpVersion = hdpRepo != null ? hdpRepo.getHdpVersion() : null;
            Map<String, List<InstanceMetaData>> groupInstances = instanceGroupMetadataCollector.collectMetadata(source);
            String clusterDefinitionText = cluster.getClusterDefinition().getClusterDefinitionText();
            HdfConfigs hdfConfigs = hdfConfigProvider.createHdfConfig(cluster.getHostGroups(), groupInstances, clusterDefinitionText);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, fileSystem);
            Optional<DatalakeResources> dataLakeResource = getDataLakeResource(source);
            StackInputs stackInputs = getStackInputs(source);
            Map<String, Object> fixInputs = stackInputs.getFixInputs() == null ? new HashMap<>() : stackInputs.getFixInputs();
            fixInputs.putAll(stackInputs.getDatalakeInputs() == null ? new HashMap<>() : stackInputs.getDatalakeInputs());
            String bindDn = null;
            String bindPassword = null;
            if (ldapConfig != null) {
                bindDn = ldapConfig.getBindDn();
                bindPassword = ldapConfig.getBindPassword();
            }
            Gateway gateway = cluster.getGateway();
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            return Builder.builder()
                    .withFlexSubscription(source.getFlexSubscription())
                    .withRdsConfigs(postgresConfigService.createRdsConfigIfNeeded(source, cluster))
                    .withHostgroups(hostGroupService.getByCluster(cluster.getId()))
                    .withGateway(gateway, gatewaySignKey)
                    .withCustomInputs(stackInputs.getCustomInputs() == null ? new HashMap<>() : stackInputs.getCustomInputs())
                    .withFixInputs(fixInputs)
                    .withClusterDefinitionView(ambariBlueprintViewProvider.getBlueprintView(cluster.getClusterDefinition()))
                    .withStackRepoDetailsHdpVersion(stackRepoDetailsHdpVersion)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigsProvider.generalClusterConfigs(source, cluster))
                    .withSmartSenseSubscription(aDefault.isPresent() ? aDefault.get() : null)
                    .withLdapConfig(ldapConfig, bindDn, bindPassword)
                    .withHdfConfigs(hdfConfigs)
                    .withKerberosConfig(cluster.getKerberosConfig())
                    .withSharedServiceConfigs(sharedServiceConfigProvider.createSharedServiceConfigs(source, dataLakeResource))
                    .build();
        } catch (ClusterDefinitionProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Optional<DatalakeResources> getDataLakeResource(Stack source) {
        if (source.getDatalakeResourceId() != null) {
            return datalakeResourcesRepository.findById(source.getDatalakeResourceId());
        }
        return Optional.empty();
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(Stack source, FileSystem fileSystem) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (source.getCluster().getFileSystem() != null) {
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source);
        }
        return fileSystemConfigurationView;
    }

    private StackInputs getStackInputs(Stack source) throws IOException {
        StackInputs stackInputs = source.getInputs().get(StackInputs.class);
        if (stackInputs == null) {
            stackInputs = new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        return stackInputs;
    }

}
