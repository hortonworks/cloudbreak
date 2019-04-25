package com.sequenceiq.cloudbreak.cm;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiConfigureForKerberosArguments;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerKerberosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerKerberosService.class);

    private static final String ACTIVE_DIRECTORY = "Active Directory";

    private static final String FREEIPA = "Red Hat IPA";

    private static final String FREEIPA_FEATURE_FLAG = "FEATURE_FLAG_redhat_ipa";

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ApplicationContext applicationContext;

    public void setupKerberos(ApiClient client, Stack stack) throws ApiException {
        Cluster cluster = stack.getCluster();
        String clusterName = cluster.getName();
        if (cluster.isAdJoinable() || cluster.isIpaJoinable()) {
            LOGGER.debug("Configure cluster {} with Kerberos", clusterName);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerClientFactory.getClouderaManagerResourceApi(client);
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            ApiConfigList apiConfigList = new ApiConfigList()
                    .addItemsItem(new ApiConfig().name("security_realm").value(kerberosConfig.getRealm()))
                    .addItemsItem(new ApiConfig().name("kdc_host").value(kerberosConfig.getUrl()))
                    .addItemsItem(new ApiConfig().name("kdc_admin_host").value(kerberosConfig.getAdminUrl()));
            if (cluster.isAdJoinable()) {
                apiConfigList.addItemsItem(new ApiConfig().name("kdc_type").value(ACTIVE_DIRECTORY));
                apiConfigList.addItemsItem(new ApiConfig().name("ad_kdc_domain").value(kerberosConfig.getContainerDn()));
            } else if (cluster.isIpaJoinable()) {
                LOGGER.debug("Enable FreeIPA on cluster: {}", clusterName);
                apiConfigList.addItemsItem(new ApiConfig().name("kdc_type").value(FREEIPA));
                ApiConfigList enableIpa = new ApiConfigList()
                        .addItemsItem(new ApiConfig().name(FREEIPA_FEATURE_FLAG).value(Boolean.TRUE.toString()));
                clouderaManagerResourceApi.updateConfig("Enable FreeIPA", enableIpa);
            }
            clouderaManagerResourceApi.updateConfig("Add kerberos configuration", apiConfigList);
            if (cluster.isAdJoinable()) {
                LOGGER.debug("Import kerberos credentials for AD on cluster: {}", clusterName);
                ApiCommand importAdminCredentials =
                        clouderaManagerResourceApi.importAdminCredentials(kerberosConfig.getPassword(), kerberosConfig.getPrincipal());
                clouderaManagerPollingServiceProvider.kerberosConfigurePollingService(stack, client, importAdminCredentials.getId());
            }
        }
    }

    public void configureKerberosViaApi(ApiClient client, HttpClientConfig clientConfig, Stack stack, ClouderaManagerRepo clouderaManagerRepoDetails)
            throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        if (cluster.isAdJoinable() || cluster.isIpaJoinable()) {
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerClientFactory.getClouderaManagerResourceApi(client);
            modificationService.stopCluster();
            ClustersResourceApi clustersResourceApi = clouderaManagerClientFactory.getClustersResourceApi(client);
            ApiCommand configureForKerberos = clustersResourceApi.configureForKerberos(cluster.getName(), new ApiConfigureForKerberosArguments());
            clouderaManagerPollingServiceProvider.kerberosConfigurePollingService(stack, client, configureForKerberos.getId());
            ApiCommand generateCredentials = clouderaManagerResourceApi.generateCredentialsCommand();
            clouderaManagerPollingServiceProvider.kerberosConfigurePollingService(stack, client, generateCredentials.getId());
            ApiCommand deployClusterConfig = clustersResourceApi.deployClientConfig(cluster.getName());
            clouderaManagerPollingServiceProvider.kerberosConfigurePollingService(stack, client, deployClusterConfig.getId());
            modificationService.startCluster(Collections.emptySet());
        }
    }

    public void deleteCredentials(ApiClient client, HttpClientConfig clientConfig, Stack stack) {
        ClouderaManagerResourceApi apiInstance = clouderaManagerClientFactory.getClouderaManagerResourceApi(client);
        try {
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            modificationService.stopCluster();
            ApiCommand command = apiInstance.deleteCredentialsCommand("all");
            clouderaManagerPollingServiceProvider.kerberosConfigurePollingService(stack, client, command.getId());
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Failed to remove Kerberos credentials", e);
            throw new ClouderaManagerOperationFailedException("Failed to remove Kerberos credentials", e);
        }
    }
}
