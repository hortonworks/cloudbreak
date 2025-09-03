package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.model.ClouderaManagerClientConfigDeployRequest;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class ClouderaManagerKerberosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerKerberosService.class);

    private static final String SUMMARY = "SUMMARY";

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

    @Inject
    private ClouderaManagerClientConfigDeployService clouderaManagerClientConfigDeployService;

    public void configureKerberosViaApi(ApiClient client, HttpClientConfig clientConfig, StackDtoDelegate stack, KerberosConfig kerberosConfig)
            throws ApiException, CloudbreakException {
        ClusterView cluster = stack.getCluster();
        if (kerberosDetailService.isAdJoinable(kerberosConfig) || kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            modificationService.stopCluster(false);
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
            ApiCommand configureForKerberos = clustersResourceApi.configureForKerberos(cluster.getName(), new ApiConfigureForKerberosArguments());
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, configureForKerberos.getId());
            ApiCommand generateCredentials = clouderaManagerResourceApi.generateCredentialsCommand();
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, generateCredentials.getId());
            clouderaManagerClientConfigDeployService.deployAndPollClientConfig(
                    ClouderaManagerClientConfigDeployRequest.builder()
                            .pollerMessage("Configure for kerberos")
                            .clustersResourceApi(clustersResourceApi)
                            .client(client)
                            .stack(stack)
                            .build()
            );
            modificationService.startCluster();
        }
    }

    public void deleteCredentials(HttpClientConfig clientConfig, StackDtoDelegate stack) {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
            clouderaManagerConfigService.modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, client, stack.getName(), false);

            ClouderaManagerModificationService modificationService = applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig);
            modificationService.stopCluster(false);

            ClouderaManagerClusterDecommissionService decomissionService = applicationContext.getBean(ClouderaManagerClusterDecommissionService.class,
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
