package com.sequenceiq.cloudbreak.cm.polling;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerApplyHostTemplateListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDecommissionHostListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerDeployClientConfigListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerHostStatusChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerKerberosConfigureListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerParcelRepoChecker;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerServiceStartListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStartupListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerStopListenerTask;
import com.sequenceiq.cloudbreak.cm.polling.task.ClouderaManagerTemplateInstallChecker;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;

@Service
public class ClouderaManagerPollingServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerPollingServiceProvider.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int MAX_ATTEMPT = 120;

    private static final int INFINITE_ATTEMPT = -1;

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
    private ClouderaManagerDeployClientConfigListenerTask deployClientConfigListenerTask;

    @Inject
    private ClouderaManagerApplyHostTemplateListenerTask applyHostTemplateListenerTask;

    @Inject
    private ClouderaManagerDecommissionHostListenerTask decommissionHostListenerTask;

    public PollingResult clouderaManagerStartupPollerObjectPollingService(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager startup. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerService.pollWithTimeoutSingleFailure(
                clouderaManagerStartupListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult hostsPollingService(Stack stack, ApiClient apiClient) {
        LOGGER.debug("Waiting for Cloudera Manager hosts to connect. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerPollerObject clouderaManagerPollerObject = new ClouderaManagerPollerObject(stack, apiClient);
        return clouderaManagerPollerService.pollWithTimeoutSingleFailure(
                clouderaManagerHostStatusChecker,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult templateInstallCheckerService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to install template. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject =
                new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                clouderaManagerTemplateInstallChecker,
                clouderaManagerCommandPollerObject,
                POLL_INTERVAL,
                INFINITE_ATTEMPT);
    }

    public PollingResult parcelRepoRefreshCheckerService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to refresh parcel repo. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject =
                new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                clouderaManagerParcelRepoChecker,
                clouderaManagerCommandPollerObject,
                POLL_INTERVAL,
                INFINITE_ATTEMPT);
    }

    public PollingResult stopPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to stop. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                clouderaManagerStopListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult startPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager services to start. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                clouderaManagerServiceStartListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult kerberosConfigurePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to configure kerberos. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                kerberosConfigureListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult deployClientConfigPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to deploy client configuratuions. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                deployClientConfigListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult applyHostTemplatePollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to apply host template. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                applyHostTemplateListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

    public PollingResult decommissionHostPollingService(Stack stack, ApiClient apiClient, BigDecimal commandId) {
        LOGGER.debug("Waiting for Cloudera Manager to decommission host. [Server address: {}]", stack.getAmbariIp());
        ClouderaManagerCommandPollerObject clouderaManagerPollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, commandId);
        return clouderaManagerCommandPollerObjectPollingService.pollWithTimeoutSingleFailure(
                decommissionHostListenerTask,
                clouderaManagerPollerObject,
                POLL_INTERVAL,
                MAX_ATTEMPT);
    }

}
