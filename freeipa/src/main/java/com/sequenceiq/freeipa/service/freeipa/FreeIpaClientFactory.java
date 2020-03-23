package com.sequenceiq.freeipa.service.freeipa;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.client.ClusterProxyErrorRpcListener;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    private static final String ADMIN_USER = "admin";

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADERS = Map.of("Proxy-Ignore-Auth", "true");

    private static final RequestListener CLUSTER_PROXY_ERROR_LISTENER = new ClusterProxyErrorRpcListener();

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws FreeIpaClientException {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);

        Stack stack = stackService.getStackById(stackId);

        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientByAccountAndEnvironment(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return getFreeIpaClientForStack(stack);
    }

    private String toClusterProxyBasepath(String freeIpaClusterCrn) {
        return String.format("%s%s", clusterProxyService.getProxyPath(freeIpaClusterCrn), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
    }

    private FreeIpaClient getFreeIpaClient(Stack stack, boolean withPing) throws FreeIpaClientException {
        stack = stackService.getByIdWithListsInTransaction(stack.getId());
        Status stackStatus = stack.getStackStatus().getStatus();
        if (!stackStatus.isFreeIpaUnreachableStatus()) {
            try {
                if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
                    return getFreeIpaClientBuilderForClusterProxy(stack).build(withPing);
                } else {
                    return createClientForDirectConnection(stack, withPing);
                }
            } catch (Exception e) {
                throw createFreeIpaUnableToBuildClient(e);
            }
        } else {
            throw createFreeIpaStateIsInvalidException(stackStatus);
        }
    }

    private FreeIpaClient createClientForDirectConnection(Stack stack, boolean withPing) throws Exception {
        Optional<FreeIpaClient> client = Optional.empty();
        for (Iterator<InstanceMetaData> instanceIterator = getAllInstances(stack).iterator(); instanceIterator.hasNext() && client.isEmpty();) {
            InstanceMetaData instanceMetaData = instanceIterator.next();
            client = getFreeIpaClient(stack, instanceMetaData, withPing, !instanceIterator.hasNext());
        }
        return client.orElseThrow(() -> createFreeIpaUnableToBuildClient(new IllegalStateException("No FreeIPA client was available")));
    }

    private Optional<FreeIpaClient> getFreeIpaClient(Stack stack, InstanceMetaData instanceMetaData, boolean withPing, boolean lastInstance) throws Exception {
        Optional<FreeIpaClient> client = Optional.empty();
        try {
            client = Optional.of(getFreeIpaClientBuilder(stack, instanceMetaData).build(withPing));
        } catch (FreeIpaClientException e) {
            handleException(instanceMetaData, e, () -> canTryAnotherInstance(lastInstance, e));
        } catch (IOException e) {
            handleException(instanceMetaData, e, () -> canTryAnotherInstance(lastInstance, e));
        }
        return client;
    }

    private void handleException(InstanceMetaData instanceMetaData, Exception e, Supplier<Boolean> canTryAnotherInstanceSupplier) throws FreeIpaClientException {
        LOGGER.error("Unable to contact FreeIPA node {}.", instanceMetaData.getPublicIpWrapper(), e);
        if (!canTryAnotherInstanceSupplier.get()) {
            throw createFreeIpaUnableToBuildClient(e);
        }
    }

    private boolean canTryAnotherInstance(boolean lastInstance, FreeIpaClientException e) {
        return !lastInstance &&
                e.getStatusCode().isPresent() &&
                e.getStatusCode().getAsInt() == HttpStatus.UNAUTHORIZED.value();
    }

    private boolean canTryAnotherInstance(boolean lastInstance, IOException e) {
        return !lastInstance;
    }

    private List<InstanceMetaData> getAllInstances(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(instanceGroup ->
                instanceGroup.getInstanceMetaData().stream()).collect(Collectors.toList());
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());
        return getFreeIpaClient(stack, false);
    }

    public FreeIpaClient getFreeIpaClientForStackWithPing(Stack stack) throws Exception {
        LOGGER.debug("Ping the login endpoint and creating FreeIpaClient for stack {}", stack.getResourceCrn());
        return getFreeIpaClient(stack, true);
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForClusterProxy(Stack stack) throws Exception {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String clusterProxyPath = toClusterProxyBasepath(stack.getResourceCrn());

        return new FreeIpaClientBuilder(ADMIN_USER,
                freeIpa.getAdminPassword(),
                httpClientConfig,
                clusterProxyConfiguration.getClusterProxyHost(),
                clusterProxyConfiguration.getClusterProxyPort(),
                clusterProxyPath,
                ADDITIONAL_CLUSTER_PROXY_HEADERS,
                CLUSTER_PROXY_ERROR_LISTENER);
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilder(Stack stack, InstanceMetaData instanceMetaData) throws Exception {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(
                stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), httpClientConfig, stack.getGatewayport(),
                instanceMetaData.getDiscoveryFQDN());
    }

    private FreeIpaClientException createFreeIpaStateIsInvalidException(Status stackStatus) {
        String message = String.format("Couldn't build FreeIPA client. Because FreeIPA is in invalid state: '%s'", stackStatus);
        LOGGER.warn(message);
        return new FreeIpaClientException(message);
    }

    private FreeIpaClientException createFreeIpaUnableToBuildClient(Exception e) {
        String message = String.format("Couldn't build FreeIPA client. "
                + "Check if the FreeIPA security rules have not changed and the instance is in running state. " + e.getLocalizedMessage());
        LOGGER.error(message);
        return new FreeIpaClientException(message, e);
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}