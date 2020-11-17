package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;

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
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
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

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private ClouderaManagerConfigService clouderaManagerConfigService;

    public void configureKerberosViaApi(ApiClient client, HttpClientConfig clientConfig, Stack stack, KerberosConfig kerberosConfig)
            throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        if (kerberosDetailService.isAdJoinable(kerberosConfig) || kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            modificationService.stopCluster(false);
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
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
            ApiClient client = clouderaManagerApiClientProvider.getClient(stack.getGatewayPort(), user, password, clientConfig);
            clouderaManagerConfigService.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, client, stack.getName());

            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            modificationService.stopCluster(false);

            ClouderaManagerClusterDecomissionService decomissionService = applicationContext.getBean(ClouderaManagerClusterDecomissionService.class,
                    stack, clientConfig);
            decomissionService.removeManagementServices();

            ClouderaManagerResourceApi apiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiCommand command = apiInstance.deleteCredentialsCommand("all");
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, command.getId());
        } catch (ApiException | CloudbreakException | ClouderaManagerClientInitException e) {
            LOGGER.info("Failed to remove Kerberos credentials", e);
            throw new ClouderaManagerOperationFailedException("Failed to remove Kerberos credentials", e);
        }
    }
}
