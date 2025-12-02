package com.sequenceiq.freeipa.service.freeipa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.freeipa.client.FreeIpaClientBuildException;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaHostNotAvailableException;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;

@Service
public class FreeIpaClientFactory {

    public static final String ADMIN_USER = "admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADERS = Map.of(
            "Proxy-Ignore-Auth", "true",
            "Proxy-With-Timeout", "90000");

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

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws FreeIpaClientException {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);
        Stack stack = stackService.getStackById(stackId);
        MDCBuilder.buildMdcContext(stack);
        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientByAccountAndEnvironment(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return getFreeIpaClientForStack(stack);
    }

    private String toClusterProxyBasepath(Stack stack, String clusterProxyServiceName) {
        if (useLegacyClusterProxyRegistration(stack)) {
            LOGGER.debug("Using legacy cluster proxy base path");
            return toLegacyClusterProxyBasepath(stack);
        } else {
            return String.format("%s%s", clusterProxyService.getProxyPathPgwAsFallBack(stack, clusterProxyServiceName), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
        }
    }

    private String toLegacyClusterProxyBasepath(Stack stack) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack.getResourceCrn()), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
    }

    private boolean useLegacyClusterProxyRegistration(Stack stack) {
        return !clusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack);
    }

    private FreeIpaClient getFreeIpaClient(Long stackId, boolean withPing, boolean forceCheckUnreachable, Optional<String> freeIpaFqdn)
            throws FreeIpaClientException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Status stackStatus = stack.getStackStatus().getStatus();
        if (forceCheckUnreachable || !stackStatus.isFreeIpaUnreachableStatus()) {
            try {
                return createClientAgainstAnyAvailableOrSelectedInstancePreferringPgw(stack, withPing, forceCheckUnreachable, freeIpaFqdn);
            } catch (RetryableFreeIpaClientException e) {
                throw createFreeIpaUnableToBuildClient(e);
            } catch (Exception e) {
                throw createFreeIpaUnableToBuildClient(e);
            }
        } else {
            throw createFreeIpaStateIsInvalidException(stackStatus);
        }
    }

    private FreeIpaClient createClientAgainstAnyAvailableOrSelectedInstancePreferringPgw(Stack stack, boolean withPing, boolean forceCheckUnreachable,
            Optional<String> freeIpaFqdn) throws Exception {
        List<InstanceMetaData> instanceMetaDatas = getPriorityOrderedFreeIpaInstances(stack, forceCheckUnreachable).stream()
                .filter(instanceMetaData -> freeIpaFqdn.isEmpty() || freeIpaFqdn.get().equals(instanceMetaData.getDiscoveryFQDN()))
                .collect(Collectors.toList());
        Optional<FreeIpaClient> client = Optional.empty();
        for (Iterator<InstanceMetaData> instanceIterator = instanceMetaDatas.iterator(); instanceIterator.hasNext() && client.isEmpty();) {
            InstanceMetaData instanceMetaData = instanceIterator.next();
            client = tryToCreateFreeIpaClientForInstance(stack, instanceMetaData, withPing, !instanceIterator.hasNext());
        }
        return client.orElseThrow(() -> new FreeIpaHostNotAvailableException("No FreeIPA client was available"));
    }

    private Optional<FreeIpaClient> tryToCreateFreeIpaClientForInstance(Stack stack, InstanceMetaData instanceMetaData, boolean withPing, boolean lastInstance)
            throws Exception {
        try {
            FreeIpaClient client = clusterProxyService.isCreateConfigForClusterProxy(stack) ?
                    getFreeIpaClientBuilderForClusterProxy(stack, instanceMetaData.getDiscoveryFQDN()).build(withPing)
                    : getFreeIpaClientBuilderForDirectMode(stack, instanceMetaData).build(withPing);
            return Optional.of(client);
        } catch (FreeIpaClientException e) {
            handleException(instanceMetaData, e, () -> canTryAnotherInstance(lastInstance, e));
            return Optional.empty();
        }
    }

    private void handleException(InstanceMetaData instanceMetaData, Exception e, Supplier<Boolean> canTryAnotherInstanceSupplier) throws FreeIpaClientException {
        LOGGER.error("Unable to contact FreeIPA node {}.", instanceMetaData.getPublicIpWrapper(), e);
        if (!canTryAnotherInstanceSupplier.get()) {
            throw createFreeIpaUnableToBuildClient(e);
        }
    }

    private boolean canTryAnotherInstance(boolean lastInstance, FreeIpaClientException e) {
        LOGGER.debug("canTryAnotherInstance: lastInstance [{}] - status: [{}]", lastInstance, e.getStatusCode());
        return !lastInstance &&
                (e.getStatusCode().isEmpty() ||
                        e.getStatusCode().getAsInt() != HttpStatus.UNAUTHORIZED.value());
    }

    private List<InstanceMetaData> getPriorityOrderedFreeIpaInstances(Stack stack, boolean forceCheckUnreachable) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> forceCheckUnreachable || im.isAvailable())
                .sorted(new PrimaryGatewayFirstThenSortByFqdnComparator())
                .collect(Collectors.toList());
    }

    public List<FreeIpaClient> createClientForAllInstances(Stack stack) throws FreeIpaClientException {
        Set<String> freeIpaInstancesFqdn = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (freeIpaInstancesFqdn.size() > 1) {
            List<FreeIpaClient> freeIpaClients = new ArrayList<>(freeIpaInstancesFqdn.size());
            for (String fqdn : freeIpaInstancesFqdn) {
                freeIpaClients.add(getFreeIpaClientForInstance(stack, fqdn));
            }
            return freeIpaClients;
        } else {
            return List.of();
        }
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());
        return getFreeIpaClient(stack.getId(), false, false, Optional.empty());
    }

    public FreeIpaClient getFreeIpaClientForStackIgnoreUnreachable(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {} ignoring unreachable", stack.getResourceCrn());
        return getFreeIpaClient(stack.getId(), false, true, Optional.empty());
    }

    public FreeIpaClient getFreeIpaClientForInstance(Stack stack, String fqdn) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {} with fqdn {}", stack.getResourceCrn(), fqdn);
        return getFreeIpaClient(stack.getId(), false, false, Optional.of(fqdn));
    }

    public FreeIpaClient getFreeIpaClientForStackForLegacyHealthCheck(Stack stack, String freeIpaFqdn) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for legacy health checks for stack {} for {}", stack.getResourceCrn(), freeIpaFqdn);
        return getFreeIpaClient(stack.getId(), true, true, Optional.of(freeIpaFqdn));
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForClusterProxy(Stack stack, String freeIpaFqdn) throws Exception {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String clusterProxyPath = toClusterProxyBasepath(stack, freeIpaFqdn);

        return new FreeIpaClientBuilder(ADMIN_USER,
                freeIpa.getAdminPassword(),
                httpClientConfig,
                freeIpaFqdn,
                clusterProxyConfiguration.getClusterProxyPort(),
                clusterProxyPath,
                ADDITIONAL_CLUSTER_PROXY_HEADERS,
                CLUSTER_PROXY_ERROR_LISTENER
        );
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForDirectMode(Stack stack, InstanceMetaData instanceMetaData) throws Exception {
        LOGGER.info("Trying to create direct FreeIPA client against {}", instanceMetaData);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(
                stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), httpClientConfig, gatewayPort, instanceMetaData.getDiscoveryFQDN());
    }

    private InvalidFreeIpaStateException createFreeIpaStateIsInvalidException(Status stackStatus) {
        String message = String.format("Couldn't build FreeIPA client. Because FreeIPA is in invalid state: '%s'", stackStatus);
        LOGGER.warn(message);
        return new InvalidFreeIpaStateException(message, new FreeIpaHostNotAvailableException(message));
    }

    private FreeIpaClientException createFreeIpaUnableToBuildClient(Exception e) {
        String message = CANT_BUILD_CLIENT_MSG + e.getLocalizedMessage();
        LOGGER.error(message, e);
        return new FreeIpaClientBuildException(message, e);
    }

    private RetryableFreeIpaClientException createFreeIpaUnableToBuildClient(RetryableFreeIpaClientException e) {
        String message = CANT_BUILD_CLIENT_MSG + e.getLocalizedMessage();
        LOGGER.error(message, e);
        return new RetryableFreeIpaClientException(message, e);
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}
