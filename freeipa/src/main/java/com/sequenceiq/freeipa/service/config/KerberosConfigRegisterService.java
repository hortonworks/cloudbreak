package com.sequenceiq.freeipa.service.config;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.util.BalancedDnsAvailabilityChecker;

@Service
public class KerberosConfigRegisterService extends AbstractConfigRegister {

    public static final String FREEIPA_DEFAULT_ADMIN = "admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigRegisterService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private BalancedDnsAvailabilityChecker balancedDnsAvailabilityChecker;

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Override
    public void register(Long stackId) {
        Crn environmentCrn = Crn.safeFromString(getEnvironmentCrnByStackId(stackId));
        if (kerberosConfigService.doesEnvironmentLevelKerberosConfigExists(environmentCrn)) {
            LOGGER.info("KerberosConfig already exists for environment [{}]", environmentCrn);
        } else {
            createKerberosConfig(stackId, FREEIPA_DEFAULT_ADMIN, null, null, null);
        }
    }

    public KerberosConfig createKerberosConfig(Long stackId, String dn, String password, String clusterName, String environmentCrn) {
        FreeIpa freeIpa = getFreeIpaService().findByStackId(stackId);
        Stack stack = getStackWithInstanceMetadata(stackId);
        String envCrn = StringUtils.isEmpty(environmentCrn) ? stack.getEnvironmentCrn() : environmentCrn;
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDomain(freeIpa.getDomain());
        kerberosConfig.setEnvironmentCrn(envCrn);
        kerberosConfig.setName(stack.getName());
        kerberosConfig.setPrincipal(dn);
        kerberosConfig.setRealm(freeIpa.getDomain().toUpperCase(Locale.ROOT));
        kerberosConfig.setType(KerberosType.FREEIPA);
        Set<InstanceMetaData> allNotDeletedInstances = stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()).collect(Collectors.toSet());
        String allFreeIpaIpJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.joining(","));
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        kerberosConfig.setNameServers(loadBalancer.map(LoadBalancer::getIp).orElse(allFreeIpaIpJoined));
        addServerAddress(freeIpa, stack, kerberosConfig, allNotDeletedInstances);
        kerberosConfig.setPassword(StringUtils.isBlank(password) ? freeIpa.getAdminPassword() : password);
        kerberosConfig.setClusterName(clusterName);
        return kerberosConfigService.createKerberosConfig(kerberosConfig, stack.getAccountId());
    }

    private void addServerAddress(FreeIpa freeIpa, Stack stack, KerberosConfig kerberosConfig, Set<InstanceMetaData> allNotDeletedInstances) {
        if (balancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack)) {
            kerberosConfig.setUrl(FreeIpaDomainUtils.getKdcFqdn(freeIpa.getDomain()));
            kerberosConfig.setAdminUrl(FreeIpaDomainUtils.getKerberosFqdn(freeIpa.getDomain()));
        } else {
            addAddressForLegacy(stack, kerberosConfig, allNotDeletedInstances);
        }
    }

    /**
     * old FreeIPA instance doesn't have kerberos CNAME so we have to create the config differently
     */
    private void addAddressForLegacy(Stack stack, KerberosConfig kerberosConfig, Set<InstanceMetaData> allNotDeletedInstances) {
        InstanceMetaData master = getMasterInstance(stack);
        kerberosConfig.setAdminUrl(master.getDiscoveryFQDN());
        String allNotDeletedIpaInstanceFQDNJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.joining(","));
        kerberosConfig.setUrl(allNotDeletedIpaInstanceFQDNJoined);
    }

    @Override
    public void delete(Stack stack) {
        try {
            kerberosConfigService.deleteAllInEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
        } catch (NotFoundException e) {
            LOGGER.info("Kerberos config not exists for environment {}", stack.getEnvironmentCrn());
        }
    }
}
