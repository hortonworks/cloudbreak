package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterSetupService {

    String BEAN_POST_TAG = "PRE_CLUSTER";

    void waitForServer(boolean defaultClusterManagerAuth) throws CloudbreakException, ClusterClientInitException;

    String prepareTemplate(
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup,
        TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxStackCrn,
        KerberosConfig kerberosConfig);

    void validateLicence();

    void configureManagementServices(TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxStackCrn,
        Telemetry telemetry,
        ProxyConfig proxyConfig);

    void updateSmonConfigs(Telemetry telemetry);

    void configureSupportTags(TemplatePreparationObject templatePreparationObject);

    void updateConfig();

    void refreshParcelRepos();

    void installCluster(String template);

    void publishPolicy(String template, boolean govCloud);

    void autoConfigureClusterManager();

    void startManagementServices();

    void suppressWarnings();

    void configureKerberos(KerberosConfig kerberosConfig) throws CloudbreakException;

    ExtendedPollingResult waitForHosts(Set<InstanceMetadataView> hostsInCluster) throws ClusterClientInitException;

    ExtendedPollingResult waitForHostsHealthy(Set<InstanceMetadataView> hostsInCluster) throws ClusterClientInitException;

    String getSdxContext();

    void setupProxy(ProxyConfig proxyConfig);

    String setupRemoteDataContext(String sdxContext);
}
