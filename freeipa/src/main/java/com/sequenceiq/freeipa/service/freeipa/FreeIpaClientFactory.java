package com.sequenceiq.freeipa.service.freeipa;

import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.client.ClusterProxyErrorRpcListener;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaHostNotAvailableException;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;

import io.opentracing.Tracer;

@Service
public class FreeIpaClientFactory {

    public static final String ADMIN_USER = "admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADERS = Map.of(
            "Proxy-Ignore-Auth", "true",
            "Proxy-With-Timeout", "90000");

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION_FIRST_RPC = Map.of("Proxy-Flow-Type", "sticky");

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION = Map.of("Proxy-Flow-Type", "force");

    private static final String ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION_ID = "Proxy-Sticky-Id";

    private static final RequestListener CLUSTER_PROXY_ERROR_LISTENER = new ClusterProxyErrorRpcListener();

    private static final String CANT_BUILD_CLIENT_MSG = "Couldn't build FreeIPA client. "
            + "Check if the FreeIPA security rules have not changed and the instance is in running state. ";

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

    @Inject
    private ClusterProxyServiceAvailabilityChecker clusterProxyServiceAvailabilityChecker;

    @Inject
    private Tracer tracer;

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws FreeIpaClientException {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);

        Stack stack = stackService.getStackById(stackId);

        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientByAccountAndEnvironment(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return getFreeIpaClientForStack(stack);
    }

    private String toClusterProxyBasepath(Stack stack, Optional<String> clusterProxyServiceName) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack, clusterProxyServiceName), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
    }

    private String toLegacyClusterProxyBasepath(Stack stack) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack.getResourceCrn()), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
    }

    private boolean useLegacyClusterProxyRegistration(Stack stack) {
        return !clusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack);
    }

    private FreeIpaClient getFreeIpaClient(Stack stack, boolean withPing, boolean forceCheckUnreachable, Optional<String> freeIpaFqdn)
            throws FreeIpaClientException {
        stack = stackService.getByIdWithListsInTransaction(stack.getId());
        Status stackStatus = stack.getStackStatus().getStatus();
        if (forceCheckUnreachable || !stackStatus.isFreeIpaUnreachableStatus()) {
            try {
                Optional<FreeIpaClient> client = Optional.empty();
                if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
                    return getFreeIpaClientBuilderForClusterProxy(stack, freeIpaFqdn).build(withPing);
                } else {
                    List<InstanceMetaData> instanceMetaDatas = getPriorityOrderedFreeIpaInstances(stack, forceCheckUnreachable).stream()
                            .filter(instanceMetaData -> freeIpaFqdn.isEmpty() || freeIpaFqdn.get().equals(instanceMetaData.getDiscoveryFQDN()))
                            .collect(Collectors.toList());
                    for (Iterator<InstanceMetaData> instanceIterator = instanceMetaDatas.iterator();
                            instanceIterator.hasNext() && client.isEmpty();) {
                        InstanceMetaData instanceMetaData = instanceIterator.next();
                        client = getFreeIpaClientForDirectConnect(stack, instanceMetaData, withPing, !instanceIterator.hasNext());
                    }
                }
                return client.orElseThrow(() -> new FreeIpaHostNotAvailableException("No FreeIPA client was available"));
            } catch (RetryableFreeIpaClientException e) {
                throw createFreeIpaUnableToBuildClient(e);
            } catch (Exception e) {
                throw createFreeIpaUnableToBuildClient(e);
            }
        } else {
            throw createFreeIpaStateIsInvalidException(stackStatus);
        }
    }

    private Optional<FreeIpaClient> getFreeIpaClientForDirectConnect(Stack stack, InstanceMetaData instanceMetaData, boolean withPing, boolean lastInstance)
            throws Exception {
        Optional<FreeIpaClient> client = Optional.empty();
        try {
            client = Optional.of(getFreeIpaClientBuilderForDirectMode(stack, instanceMetaData).build(withPing));
        } catch (FreeIpaClientException e) {
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
                (!e.getStatusCode().isPresent() ||
                        e.getStatusCode().getAsInt() != HttpStatus.UNAUTHORIZED.value());
    }

    private List<InstanceMetaData> getPriorityOrderedFreeIpaInstances(Stack stack, boolean forceCheckUnreachable) {
        return stack.getNotDeletedInstanceMetaDataList().stream()
                .filter(im -> forceCheckUnreachable || im.isAvailable())
                .sorted(new PrimaryGatewayFirstThenSortByFqdnComparator())
                .collect(Collectors.toList());
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());
        return getFreeIpaClient(stack, false, false, Optional.empty());
    }

    public FreeIpaClient getFreeIpaClientForStackForLegacyHealthCheck(Stack stack, String freeIpaFqdn) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for legacy health checks for stack {} for {}", stack.getResourceCrn(), freeIpaFqdn);
        return getFreeIpaClient(stack, true, true, Optional.of(freeIpaFqdn));
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForClusterProxy(Stack stack, Optional<String> freeIpaFqdn) throws Exception {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String clusterProxyPath = toClusterProxyBasepath(stack, freeIpaFqdn);

        Map<String, String> additionalHeadersStickySessionFirstRpc = new HashMap<>();
        Map<String, String> additionalHeadersStickySession = new HashMap<>();
        Optional<String> stickyIdHeader = Optional.empty();
        if (freeIpaFqdn.isEmpty()) {
            additionalHeadersStickySessionFirstRpc.putAll(ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION_FIRST_RPC);
            additionalHeadersStickySession.putAll(ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION);
            stickyIdHeader = Optional.of(ADDITIONAL_CLUSTER_PROXY_HEADER_STICKY_SESSION_ID);
        }
        String freeIpaFqdnForClient = freeIpaFqdn.orElseGet(() -> FreeIpaDomainUtils.getFreeIpaFqdn(freeIpa.getDomain()));
        if (useLegacyClusterProxyRegistration(stack)) {
            clusterProxyPath = toLegacyClusterProxyBasepath(stack);
        }
        return new FreeIpaClientBuilder(ADMIN_USER,
                freeIpa.getAdminPassword(),
                httpClientConfig,
                freeIpaFqdnForClient,
                clusterProxyConfiguration.getClusterProxyPort(),
                clusterProxyPath,
                ADDITIONAL_CLUSTER_PROXY_HEADERS,
                additionalHeadersStickySessionFirstRpc,
                additionalHeadersStickySession,
                stickyIdHeader,
                CLUSTER_PROXY_ERROR_LISTENER,
                tracer);
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForDirectMode(Stack stack, InstanceMetaData instanceMetaData) throws Exception {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(
                stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), httpClientConfig, gatewayPort, instanceMetaData.getDiscoveryFQDN(), tracer);
    }

    private InvalidFreeIpaStateException createFreeIpaStateIsInvalidException(Status stackStatus) {
        String message = String.format("Couldn't build FreeIPA client. Because FreeIPA is in invalid state: '%s'", stackStatus);
        LOGGER.warn(message);
        return new InvalidFreeIpaStateException(message, new FreeIpaHostNotAvailableException(message));
    }

    private FreeIpaClientException createFreeIpaUnableToBuildClient(Exception e) {
        String message = CANT_BUILD_CLIENT_MSG + e.getLocalizedMessage();
        LOGGER.error(message);
        return new FreeIpaClientException(message, e);
    }

    private RetryableFreeIpaClientException createFreeIpaUnableToBuildClient(RetryableFreeIpaClientException e) {
        String message = CANT_BUILD_CLIENT_MSG + e.getLocalizedMessage();
        LOGGER.error(message);
        return new RetryableFreeIpaClientException(message, e);
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}