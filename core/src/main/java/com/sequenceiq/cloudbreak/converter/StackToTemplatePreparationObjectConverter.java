package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.dto.LdapView;

@Component
public class StackToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<Stack, TemplatePreparationObject> {

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private InstanceGroupMetadataCollector instanceGroupMetadataCollector;

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
    private BlueprintViewProvider blueprintViewProvider;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Override
    public TemplatePreparationObject convert(Stack source) {
        try {
            Cluster cluster = clusterService.getById(source.getCluster().getId());
            FileSystem fileSystem = cluster.getFileSystem();
            Optional<LdapView> ldapView = ldapConfigService.get(source.getEnvironmentCrn());
            StackRepoDetails hdpRepo = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
            String stackRepoDetailsHdpVersion = hdpRepo != null ? hdpRepo.getHdpVersion() : null;
            Map<String, List<InstanceMetaData>> groupInstances = instanceGroupMetadataCollector.collectMetadata(source);
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            HdfConfigs hdfConfigs = hdfConfigProvider.createHdfConfig(cluster.getHostGroups(), groupInstances, blueprintText);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, fileSystem);
            Optional<DatalakeResources> dataLakeResource = getDataLakeResource(source);
            StackInputs stackInputs = getStackInputs(source);
            Map<String, Object> fixInputs = stackInputs.getFixInputs() == null ? new HashMap<>() : stackInputs.getFixInputs();
            fixInputs.putAll(stackInputs.getDatalakeInputs() == null ? new HashMap<>() : stackInputs.getDatalakeInputs());
            Gateway gateway = cluster.getGateway();
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            return Builder.builder()
                    .withRdsConfigs(postgresConfigService.createRdsConfigIfNeeded(source, cluster))
                    .withHostgroups(hostGroupService.getByCluster(cluster.getId()))
                    .withGateway(gateway, gatewaySignKey)
                    .withCustomInputs(stackInputs.getCustomInputs() == null ? new HashMap<>() : stackInputs.getCustomInputs())
                    .withFixInputs(fixInputs)
                    .withBlueprintView(blueprintViewProvider.getBlueprintView(cluster.getBlueprint()))
                    .withStackRepoDetailsHdpVersion(stackRepoDetailsHdpVersion)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigsProvider.generalClusterConfigs(source, cluster))
                    .withLdapConfig(ldapView.orElse(null))
                    .withHdfConfigs(hdfConfigs)
                    .withKerberosConfig(kerberosConfigService.get(source.getEnvironmentCrn()).orElse(null))
                    .withSharedServiceConfigs(sharedServiceConfigProvider.createSharedServiceConfigs(source, dataLakeResource))
                    .build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Optional<DatalakeResources> getDataLakeResource(Stack source) {
        if (source.getDatalakeResourceId() != null) {
            return datalakeResourcesService.findById(source.getDatalakeResourceId());
        }
        return Optional.empty();
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(Stack source, FileSystem fileSystem) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        Credential credentialResponse = credentialClientService.getByCrn(source.getCredentialCrn());
        if (source.getCluster().getFileSystem() != null) {
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credentialResponse.getAttributes());
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
