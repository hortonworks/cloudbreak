package com.sequenceiq.cloudbreak.service.publicendpoint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_REGISTER_PUBLIC_DNS_FAILED;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class GatewayPublicEndpointManagementService extends BasePublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayPublicEndpointManagementService.class);

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public boolean isCertRenewalTriggerable(StackView stack) {
        return manageCertificateAndDnsInPem(stack)
                && stack != null
                && stack.getClusterId() != null;
    }

    public void generateCertAndSaveForStackAndUpdateDnsEntry(StackDtoDelegate stack) {
        if (stack != null) {
            if (isCertRenewalTriggerable(stack.getStack())) {
                if (StringUtils.isEmpty(stack.getSecurityConfig().getUserFacingCert())) {
                    generateCertAndSaveForStack(stack);
                    generateAlternativeCertAndSaveForStack(stack);
                }
                updateDnsEntryForCluster(stack);
                updateDnsEntryForLoadBalancers(stack);
            } else {
                LOGGER.info("External FQDN in PEM service and valid certificate creation is disabled.");
                setLoadBalancerFqdn(stack);
            }
        }
    }

    private void setLoadBalancerFqdn(StackDtoDelegate stack) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerWithEndpoint(stack);
        if (loadBalancerOptional.isPresent()) {
            Set<String> hueHostGroups = getHueHostGroups(stack);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            setLoadBalancerFqdn(hueHostGroups, loadBalancerOptional.get(), environment, ThreadBasedUserCrnProvider.getAccountId());
        }
    }

    public String updateDnsEntry(StackDtoDelegate stack, String gatewayIp) {
        LOGGER.info("Update dns entry");
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Set<String> hueHostGroups = getHueHostGroups(stack);

        if (StringUtils.isEmpty(gatewayIp)) {
            Optional<InstanceMetadataView> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
            if (gateway.isEmpty()) {
                LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
                return null;
            } else {
                gatewayIp = gateway.get().getPublicIpWrapper();
            }
        }
        String endpointName = getPrimaryGatewayEndpointName(stack);
        LOGGER.info("Creating DNS entry with endpoint name: '{}', environment name: '{}' and gateway IP: '{}'", endpointName, environment.getName(), gatewayIp);
        List<String> ips = List.of(gatewayIp);
        try {
            getDnsManagementService().createOrUpdateDnsEntryWithIp(accountId, endpointName, environment.getName(), false, ips);
            String fullQualifiedDomainName = getDomainNameProvider()
                    .getFullyQualifiedEndpointName(hueHostGroups, endpointName, environment);
            LOGGER.info("Dns entry updated: ip: {}, FQDN: {}", gatewayIp, fullQualifiedDomainName);
            return fullQualifiedDomainName;
        } catch (PemDnsEntryCreateOrUpdateException exception) {
            String message = String.format("Failed to create or update DNS entry for endpoint '%s' and environment name '%s' and IP: '%s'",
                    endpointName, environment.getName(), gatewayIp);
            LOGGER.warn(message, exception);
            throw new CloudbreakServiceException(message, exception);
        }
    }

    public Set<String> getHueHostGroups(StackDtoDelegate stack) {
        return new CmTemplateProcessor(stack.getBlueprintJsonText())
                .getHostGroupsWithComponent(HueRoles.HUE_SERVER);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public void updateDnsEntryForLoadBalancers(StackDtoDelegate stack) {
        if (stack != null && manageCertificateAndDnsInPem(stack.getStack())) {
            Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerWithEndpoint(stack);
            if (loadBalancerOptional.isEmpty()) {
                LOGGER.warn("Unable find appropriate load balancer in stack. Load balancer public domain name will not be registered.");
            } else {
                registerLoadBalancersDnsEntries(loadBalancerOptional.get(), stack.getEnvironmentCrn(), getHueHostGroups(stack), stack.getId());
            }
        } else {
            LOGGER.debug("DNS registration in PEM service not enabled for load balancer.");
        }
    }

    private void registerLoadBalancersDnsEntries(LoadBalancer loadBalancer, String environmentCrn, Set<String> hueHostGroups, Long stackId) {
        LOGGER.info("Updating load balancer DNS entries");
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);
        String endpoint = loadBalancer.getEndpoint();

        try {
            if (loadBalancer.getDns() != null && loadBalancer.getHostedZoneId() != null) {
                LOGGER.info("Creating load balancer DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'",
                        endpoint, environment.getName(), loadBalancer.getDns());
                getDnsManagementService().createOrUpdateDnsEntryWithCloudDns(accountId, endpoint,
                        environment.getName(), loadBalancer.getDns(), loadBalancer.getHostedZoneId());
            } else if (loadBalancer.getIp() != null) {
                LOGGER.info("Creating load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                        endpoint, environment.getName(), loadBalancer.getIp());
                getDnsManagementService().createOrUpdateDnsEntryWithIp(accountId, endpoint,
                        environment.getName(), false, List.of(loadBalancer.getIp()));
            } else {
                String message = String.format("Could not find IP or cloud DNS info for load balancer with endpoint '%s'" +
                        " and environment name: '%s'. DNS registration could not be executed.", loadBalancer.getEndpoint(), environment.getName());
                LOGGER.warn(message);
                throw new CloudbreakServiceException(message);
            }

            setLoadBalancerFqdn(hueHostGroups, loadBalancer, environment, accountId);
        } catch (PemDnsEntryCreateOrUpdateException exception) {
            String message = String.format("Failed to create or update DNS entry for load balancer with endpoint '%s' and environment name '%s'",
                    loadBalancer.getEndpoint(), environment.getName());
            LOGGER.warn(message, exception);
            flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), STACK_LB_REGISTER_PUBLIC_DNS_FAILED);
            throw new CloudbreakServiceException(message, exception);
        }
    }

    private void setLoadBalancerFqdn(Set<String> hueHostGroups, LoadBalancer loadBalancer, DetailedEnvironmentResponse env, String accountId) {
        loadBalancer.setFqdn(getDomainNameProvider().getFullyQualifiedEndpointName(hueHostGroups, loadBalancer.getEndpoint(), env));
        loadBalancerPersistenceService.save(loadBalancer);
        LOGGER.info("Set load balancer's FQDN to {}.", loadBalancer.getFqdn());
    }

    public String deleteDnsEntry(StackDtoDelegate stack, String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (StringUtils.isEmpty(environmentName)) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            environmentName = environment.getName();
        }
        Optional<InstanceMetadataView> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
        if (gateway.isEmpty()) {
            LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
            return null;
        }
        String ip = gateway.get().getPublicIpWrapper();
        if (ip == null) {
            return null;
        }
        String endpointName = getPrimaryGatewayEndpointName(stack);
        if (endpointName == null || endpointName.isEmpty()) {
            LOGGER.info("No endpoint name for stack, we skip the dns entry deletion.");
            return null;
        }
        LOGGER.info("Deleting DNS entry with endpoint name: '{}', environment name: '{}' and gateway IP: '{}'", endpointName, environmentName, ip);
        getDnsManagementService().deleteDnsEntryWithIp(accountId, endpointName, environmentName, false, List.of(ip));
        return ip;
    }

    public void deleteLoadBalancerDnsEntry(StackDtoDelegate stack, String environmentName) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerWithEndpoint(stack);

        if (loadBalancerOptional.isEmpty()) {
            LOGGER.warn("Unable to find appropriate load balancer in stack. Load balancer public domain name will not be deleted.");
        } else {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            if (StringUtils.isEmpty(environmentName)) {
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                environmentName = environment.getName();
            }

            LoadBalancer loadBalancer = loadBalancerOptional.get();
            String endpoint = loadBalancer.getEndpoint();
            if (loadBalancer.getDns() != null && loadBalancer.getHostedZoneId() != null) {
                LOGGER.info("Deleting load balancer DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'",
                        endpoint, environmentName, loadBalancer.getDns());
                getDnsManagementService().deleteDnsEntryWithCloudDns(accountId, endpoint,
                        environmentName, loadBalancer.getDns(), loadBalancer.getHostedZoneId());
            } else if (loadBalancer.getIp() != null) {
                LOGGER.info("Deleting load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                        endpoint, environmentName, loadBalancer.getIp());
                getDnsManagementService().deleteDnsEntryWithIp(accountId, endpoint,
                        environmentName, false, List.of(loadBalancer.getIp()));
            }
        }
    }

    public void renewCertificate(StackDtoDelegate stack) {
        if (stack != null && isCertRenewalTriggerable(stack.getStack())) {
            LOGGER.info("Renew certificate for stack: '{}'", stack.getName());
            generateCertAndSaveForStack(stack);
            generateAlternativeCertAndSaveForStack(stack);
            LOGGER.info("Generate cert was successful update dns entry for cluster");
            updateDnsEntryForCluster(stack);
        }
    }

    private void generateCertAndSaveForStack(StackDtoDelegate stack) {
        LOGGER.info("Acquire certificate from PEM service and save for stack");
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<String> hueHostGroups = getHueHostGroups(stack);
        SecurityConfig securityConfig = stack.getSecurityConfig();
        try {
            KeyPair keyPair = getKeyPairForStack(securityConfig);
            String primaryGatewayEndpointName = getPrimaryGatewayEndpointName(stack);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            String environmentName = environment.getName();

            String commonName = getDomainNameProvider().getCommonName(primaryGatewayEndpointName, environment);
            List<String> subjectAlternativeNames = getSubjectAlternativeNames(stack, commonName, environment);
            LOGGER.info("Acquiring certificate with common name:{} and SANs: {}", commonName, String.join(",", subjectAlternativeNames));

            PKCS10CertificationRequest csr = PkiUtil.csr(keyPair, commonName, subjectAlternativeNames);
            List<String> certs = getCertificateCreationService().create(accountId, primaryGatewayEndpointName, environmentName, csr, stack.getResourceCrn());
            saveCertificates(securityConfig, certs);
        } catch (Exception e) {
            String msg = "The public certificate could not be generated by Public Endpoint Management service" + getMessage(e);
            LOGGER.error(msg, e);
            throw new CloudbreakServiceException(msg, e);
        }
    }

    void generateAlternativeCertAndSaveForStack(StackDtoDelegate stack) {
        try {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());

            if (entitlementService.isConfigureEncryptionProfileEnabled(accountId) &&
                    isVersionNewerOrEqualThanLimited(clouderaManagerRepo.getVersion(), CLOUDERAMANAGER_VERSION_7_13_2_0)) {
                LOGGER.info("Generating alternative certificate (ECDSA)");

                SecurityConfig securityConfig = stack.getSecurityConfig();
                KeyPair ecdsaKeyPair = getEcdsaKeyPairForStack(securityConfig);
                String primaryGatewayEndpointName = getPrimaryGatewayEndpointName(stack);
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                String commonName = getDomainNameProvider().getCommonName(primaryGatewayEndpointName, environment);
                List<String> subjectAlternativeNames = getSubjectAlternativeNames(stack, commonName, environment);
                PKCS10CertificationRequest ecdsaCsr = PkiUtil.csr(ecdsaKeyPair, commonName, subjectAlternativeNames);
                List<String> ecdsaCerts = getCertificateCreationService().create(accountId, primaryGatewayEndpointName, environment.getName(), ecdsaCsr,
                        stack.getResourceCrn());

                saveAlternativeCertificates(securityConfig, ecdsaCerts);
            }
        } catch (Exception e) {
            String msg = "The alternative public certificate could not be generated by Public Endpoint Management service" + getMessage(e);
            LOGGER.error(msg, e);
            throw new CloudbreakServiceException(msg, e);
        }
    }

    private List<String> getSubjectAlternativeNames(StackDtoDelegate stack, String commonName, DetailedEnvironmentResponse environment) {
        Set<String> hueHostGroups = getHueHostGroups(stack);
        List<String> subjectAlternativeNames = new ArrayList<>();
        subjectAlternativeNames.add(commonName);
        subjectAlternativeNames.addAll(getGatewaysFullyQualifiedEndpointNames(stack, hueHostGroups, environment));
        subjectAlternativeNames.addAll(getLoadBalancersFullyQualifiedEndpointNames(stack, hueHostGroups, environment));

        return subjectAlternativeNames;
    }

    private Set<String> getGatewaysFullyQualifiedEndpointNames(StackDtoDelegate stack, Set<String> hueHostGroups, DetailedEnvironmentResponse environment) {
        return stack.getAllAvailableGatewayInstances().stream()
                .map(im -> getDomainNameProvider().getFullyQualifiedEndpointName(hueHostGroups, im.getShortHostname(), environment))
                .collect(Collectors.toSet());
    }

    private Set<String> getLoadBalancersFullyQualifiedEndpointNames(StackDtoDelegate stack, Set<String> hueHostGroups,
            DetailedEnvironmentResponse environment) {
        Set<String> loadBalancerEndpoints = getLoadBalancerNamesForStack(stack);
        return loadBalancerEndpoints.stream()
                .map(loadBalancerEndpoint -> getDomainNameProvider().getFullyQualifiedEndpointName(hueHostGroups, loadBalancerEndpoint, environment))
                .collect(Collectors.toSet());
    }

    private String getMessage(Exception e) {
        return StringUtils.isNotBlank(e.getMessage()) ? ": " + e.getMessage() : "";
    }

    private void saveCertificates(SecurityConfig securityConfig, List<String> certs) throws TransactionService.TransactionExecutionException {
        if (CollectionUtils.isNotEmpty(certs)) {
            LOGGER.debug("Saving the new certificates into the database");
            transactionService.required(() -> {
                securityConfig.setUserFacingCert(String.join("", certs));
                return securityConfigService.save(securityConfig);
            });
        } else {
            LOGGER.debug("No certificate has been generated, no save is necessary");
        }
    }

    private void saveAlternativeCertificates(SecurityConfig securityConfig, List<String> certs) throws TransactionService.TransactionExecutionException {
        if (CollectionUtils.isNotEmpty(certs)) {
            LOGGER.debug("Saving alternative certificate into the database");
            transactionService.required(() -> {
                securityConfig.setAlternativeUserFacingCert(String.join("", certs));
                return securityConfigService.save(securityConfig);
            });
        } else {
            LOGGER.debug("No alternative certificate has been generated, no save is necessary");
        }
    }

    private KeyPair getKeyPairForStack(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        securityConfig.setUserFacingKey(PkiUtil.convert(keyPair.getPrivate()));
        return keyPair;
    }

    private KeyPair getEcdsaKeyPairForStack(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateEcdsaKeypair();
        securityConfig.setAlternativeUserFacingKey(PkiUtil.convertEcPrivateKey(keyPair.getPrivate()));
        return keyPair;
    }

    public String updateDnsEntryForCluster(StackDtoDelegate stack) {
        String fqdn = updateDnsEntry(stack, null);
        saveClusterFqdn(stack, fqdn);
        return fqdn;
    }

    private void saveClusterFqdn(StackDtoDelegate stack, String fqdn) {
        try {
            transactionService.required(() -> {
                if (fqdn != null) {
                    ClusterView cluster = stack.getCluster();
                    clusterService.updateFqdnOnCluster(cluster.getId(), fqdn);
                }
            });
            LOGGER.info("The '{}' domain name has been generated, registered through PEM service and saved for the cluster.", fqdn);
        } catch (TransactionService.TransactionExecutionException e) {
            String msg = "The FQDN after DNS update could not be saved to the cluster due to transaction issues: " + e.getMessage();
            LOGGER.warn(msg, e);
            throw new CloudbreakServiceException(msg, e);
        }
    }

    private String getPrimaryGatewayEndpointName(StackDtoDelegate stack) {
        return stack.getPrimaryGatewayInstance().getShortHostname();
    }

    @VisibleForTesting
    Set<String> getLoadBalancerNamesForStack(StackDtoDelegate stack) {
        return loadBalancerPersistenceService.findByStackId(stack.getId()).stream()
                .filter(lb -> StringUtils.isNotEmpty(lb.getEndpoint()))
                .map(LoadBalancer::getEndpoint)
                .collect(Collectors.toSet());
    }

    private Optional<LoadBalancer> getLoadBalancerWithEndpoint(StackDtoDelegate stack) {
        Optional<LoadBalancer> loadBalancerOptional;
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stack.getId());
        if (loadBalancers.isEmpty()) {
            LOGGER.info("No load balancers in stack {}", stack.getId());
            loadBalancerOptional = Optional.empty();
        } else {
            loadBalancerOptional = loadBalancerConfigService.selectLoadBalancerForFrontend(loadBalancers, LoadBalancerType.PUBLIC);

            if (loadBalancerOptional.isEmpty()) {
                LOGGER.error("Unable to determine load balancer type. Load balancer public domain name will not be registered.");
            } else if (Strings.isNullOrEmpty(loadBalancerOptional.get().getEndpoint())) {
                LOGGER.error("No endpoint set for load balancer. Can't register domain.");
                loadBalancerOptional = Optional.empty();
            } else {
                LOGGER.debug("Found load balancer {}", loadBalancerOptional.get().getEndpoint());
            }
        }

        return loadBalancerOptional;
    }
}
