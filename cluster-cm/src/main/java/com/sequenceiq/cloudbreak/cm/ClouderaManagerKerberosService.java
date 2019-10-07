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
import com.cloudera.api.swagger.model.ApiConfigureForKerberosArguments;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;

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

    @Inject
    private KerberosDetailService kerberosDetailService;

    public void configureKerberosViaApi(ApiClient client, HttpClientConfig clientConfig, Stack stack, KerberosConfig kerberosConfig)
            throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        if (kerberosDetailService.isAdJoinable(kerberosConfig) || kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerClientFactory.getClouderaManagerResourceApi(client);
            modificationService.stopCluster();
            ClustersResourceApi clustersResourceApi = clouderaManagerClientFactory.getClustersResourceApi(client);
            ApiCommand configureForKerberos = clustersResourceApi.configureForKerberos(cluster.getName(), new ApiConfigureForKerberosArguments());
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, configureForKerberos.getId());
            ApiCommand generateCredentials = clouderaManagerResourceApi.generateCredentialsCommand();
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, generateCredentials.getId());
            ApiCommand deployClusterConfig = clustersResourceApi.deployClientConfig(cluster.getName());
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, deployClusterConfig.getId());
            modificationService.startCluster(Collections.emptySet());
        }
    }

    public void deleteCredentials(HttpClientConfig clientConfig, Stack stack) {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient client = clouderaManagerClientFactory.getClient(stack.getGatewayPort(), user, password, clientConfig);
            ClouderaManagerResourceApi apiInstance = clouderaManagerClientFactory.getClouderaManagerResourceApi(client);
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            modificationService.stopCluster();

            ClouderaManagerClusterDecomissionService decomissionService = applicationContext.getBean(ClouderaManagerClusterDecomissionService.class,
                    stack, clientConfig);
            decomissionService.removeManagementServices();

            ApiCommand command = apiInstance.deleteCredentialsCommand("all");
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, command.getId());
        } catch (ApiException | CloudbreakException | ClouderaManagerClientInitException e) {
            LOGGER.info("Failed to remove Kerberos credentials", e);
            throw new ClouderaManagerOperationFailedException("Failed to remove Kerberos credentials", e);
        }
    }
}
