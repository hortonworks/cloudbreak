package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerApplyHostTemplateListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDecommissionHostListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDeployClientConfigListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerGenerateCredentialsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerKerberosConfigureListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelRepositoryRefreshChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerRefreshServiceConfigsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerRestartServicesListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerServiceStartListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartManagementServiceListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartupListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStopListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStopManagementServiceListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerTemplateInstallationChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDistributeListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeParcelDownloadListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerUpgradeRuntimeListenerTask;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class ClouderaManagerPollingServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingServiceProvider.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int INFINITE_ATTEMPT = -1;

    private static final int POLL_ATTEMPTS_FOUR_HOURS = 2880;

    @Inject
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Inject
    private PollingService<ClouderaManagerPollerObject> clouderaManagerCommandPollerObjectPollingService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public PollingResult startPollingCmStartup(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager startup. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, null, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerStartupListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmHostStatus(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, null, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmTemplateInstallation(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to install template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerTemplateInstallationChecker(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmParcelRepositoryRefresh(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to refresh parcel repo. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerParcelRepositoryRefreshChecker(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmShutdown(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to stop. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerStopListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmStartup(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to start. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerServiceStartListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmKerberosJob(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to configure kerberos. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerKerberosConfigureListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmParcelActivation(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configuratuions. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerParcelActivationListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmClientConfigDeployment(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configuratuions. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerDeployClientConfigListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmConfigurationRefresh(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to refresh cluster. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerRefreshServiceConfigsListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmApplyHostTemplate(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to apply host template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerApplyHostTemplateListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmHostDecommissioning(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithAttemptListener(stack, apiClient, commandId, INFINITE_ATTEMPT,
                new ClouderaManagerDecommissionHostListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmManagementServiceStartup(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to start management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerStartManagementServiceListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmManagementServiceShutdown(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to stop management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerStopManagementServiceListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCmServicesRestart(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to restart services. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerRestartServicesListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCdpRuntimeUpgrade(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to upgrade CDP Runtime services. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerUpgradeRuntimeListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    public PollingResult startPollingCdpRuntimeParcelDownload(Stack stack, ApiClient apiClient, BigDecimal commandId, ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to download CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerUpgradeParcelDownloadListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService, parcelResource));
    }

    public PollingResult startPollingCdpRuntimeParcelDistribute(Stack stack, ApiClient apiClient, BigDecimal commandId, ParcelResource parcelResource) {
        LOGGER.debug("Waiting for Cloudera Manager to distribute CDP Runtime Parcel. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerUpgradeParcelDistributeListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService, parcelResource));
    }

    public PollingResult startPollingCmGenerateCredentials(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to finish generate credentials. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithTimeListener(stack, apiClient, commandId, POLL_ATTEMPTS_FOUR_HOURS,
                new ClouderaManagerGenerateCredentialsListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService));
    }

    private PollingResult pollCommandWithTimeListener(Stack stack, ApiClient apiClient, BigDecimal commandId, int numAttempts,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                listenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                numAttempts);
    }

    private PollingResult pollCommandWithAttemptListener(Stack stack, ApiClient apiClient, BigDecimal commandId, int numAttempts,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithAttemptSingleFailure(
                listenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                numAttempts);
    }
}
