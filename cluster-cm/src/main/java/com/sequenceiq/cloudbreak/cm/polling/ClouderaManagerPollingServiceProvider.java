package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.polling.task.AbstractClouderaManagerCommandCheckerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerApplyHostTemplateListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDecommissionHostListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDeployClientConfigListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelActivationListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerGenerateCredentialsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerKerberosConfigureListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelRepoChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerRefreshServiceConfigsListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerRestartServicesListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerServiceStartListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartManagementServiceListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartupListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStopListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStopManagementServiceListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerTemplateInstallChecker;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;

@Service
public class ClouderaManagerPollingServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingServiceProvider.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int INFINITE_ATTEMPT = -1;

    private static final int TWELVE_HOUR = 8640;

    @Inject
    private PollingService<ClouderaManagerPollerObject> clouderaManagerPollerService;

    @Inject
    private PollingService<ClouderaManagerCommandPollerObject> clouderaManagerCommandPollerObjectPollingService;

    @Inject
    private ClouderaManagerStartupListenerTask clouderaManagerStartupListenerTask;

    @Inject
    private ClouderaManagerServiceStartListenerTask clouderaManagerServiceStartListenerTask;

    @Inject
    private ClouderaManagerStopListenerTask clouderaManagerStopListenerTask;

    @Inject
    private ClouderaManagerHostStatusChecker clouderaManagerHostStatusChecker;

    @Inject
    private ClouderaManagerTemplateInstallChecker clouderaManagerTemplateInstallChecker;

    @Inject
    private ClouderaManagerParcelRepoChecker clouderaManagerParcelRepoChecker;

    @Inject
    private ClouderaManagerKerberosConfigureListenerTask kerberosConfigureListenerTask;

    @Inject
    private ClouderaManagerParcelActivationListenerTask parcelActivationListenerTask;

    @Inject
    private ClouderaManagerDeployClientConfigListenerTask deployClientConfigListenerTask;

    @Inject
    private ClouderaManagerApplyHostTemplateListenerTask applyHostTemplateListenerTask;

    @Inject
    private ClouderaManagerDecommissionHostListenerTask decommissionHostListenerTask;

    @Inject
    private ClouderaManagerStartManagementServiceListenerTask startManagementServiceListenerTask;

    @Inject
    private ClouderaManagerStopManagementServiceListenerTask stopManagementServiceListenerTask;

    @Inject
    private ClouderaManagerRestartServicesListenerTask restartServicesListenerTask;

    @Inject
    private ClouderaManagerGenerateCredentialsListenerTask generateCredentialsListenerTask;

    @Inject
    private ClouderaManagerRefreshServiceConfigsListenerTask refreshServiceConfigsListenerTask;

    public PollingResult clouderaManagerStartupPollerObjectPollingService(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager startup. [Server address: {}]", stack.getClusterManagerIp());
        return pollCMWithListener(stack, apiClient, clouderaManagerStartupListenerTask);
    }

    public PollingResult hostsPollingService(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}]", stack.getClusterManagerIp());
        return pollCMWithListener(stack, apiClient, clouderaManagerHostStatusChecker);
    }

    public PollingResult templateInstallCheckerService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to install template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, clouderaManagerTemplateInstallChecker);
    }

    public PollingResult parcelRepoRefreshCheckerService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to refresh parcel repo. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, clouderaManagerParcelRepoChecker);
    }

    public PollingResult stopPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to stop. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, clouderaManagerStopListenerTask);
    }

    public PollingResult startPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to start. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, clouderaManagerServiceStartListenerTask);
    }

    public PollingResult kerberosConfigurePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to configure kerberos. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, kerberosConfigureListenerTask);
    }

    public PollingResult parcelActivationPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configuratuions. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, parcelActivationListenerTask);
    }

    public PollingResult deployClientConfigPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configuratuions. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, deployClientConfigListenerTask);
    }

    public PollingResult refreshClusterPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to refresh cluster. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, refreshServiceConfigsListenerTask);
    }

    public PollingResult applyHostTemplatePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to apply host template. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, applyHostTemplateListenerTask);
    }

    public PollingResult decommissionHostPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, INFINITE_ATTEMPT, decommissionHostListenerTask);
    }

    public PollingResult startManagementServicePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to start management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, startManagementServiceListenerTask);
    }

    public PollingResult stopManagementServicePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to stop management service. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, stopManagementServiceListenerTask);
    }

    public PollingResult restartServicesPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to restart services. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, restartServicesListenerTask);
    }

    public PollingResult generateCredentialsPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to finish generate credentials. [Server address: {}]", stack.getClusterManagerIp());
        return pollCommandWithListener(stack, apiClient, commandId, TWELVE_HOUR, generateCredentialsListenerTask);
    }

    private PollingResult pollCMWithListener(Stack stack, ApiClient apiClient, StatusCheckerTask<ClouderaManagerPollerObject> listenerTask) {
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerService.pollWithTimeoutSingleFailure(
                listenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                TWELVE_HOUR);
    }

    private PollingResult pollCommandWithListener(Stack stack, ApiClient apiClient, BigDecimal commandId, int numAttempts,
            AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> listenerTask) {
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                listenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                numAttempts);
    }
}
