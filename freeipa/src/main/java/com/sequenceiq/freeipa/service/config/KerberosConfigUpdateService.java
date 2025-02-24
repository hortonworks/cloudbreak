package com.sequenceiq.freeipa.service.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KerberosConfigUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigUpdateService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    public void updateNameservers(Long stackId) {
        Stack stack = getStackWithInstanceMetadata(stackId);
        String environmentCrn = stack.getEnvironmentCrn();
        Set<InstanceMetaData> allNotDeletedInstances = stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()).collect(Collectors.toSet());
        String allFreeIpaIpJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.joining(","));
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        List<KerberosConfig> kerberosConfigs = kerberosConfigService.findAllInEnvironment(environmentCrn);
        kerberosConfigs.forEach(kerberosConfig -> {
            kerberosConfig.setNameServers(loadBalancer.map(LoadBalancer::getIp).map(ip -> String.join(",", ip)).orElse(allFreeIpaIpJoined));
        });
        kerberosConfigService.saveAll(kerberosConfigs);
    }

    protected Stack getStackWithInstanceMetadata(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }
}
