package com.sequenceiq.freeipa.service.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class KerberosConfigUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigUpdateService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private StackService stackService;

    public void updateNameservers(Long stackId) {
        Stack stack = getStackWithInstanceMetadata(stackId);
        String environmentCrn = stack.getEnvironmentCrn();
        Set<InstanceMetaData> allNotDeletedInstances = stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()).collect(Collectors.toSet());
        String allFreeIpaIpJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.joining(","));
        List<KerberosConfig> kerberosConfigs = kerberosConfigService.findAllInEnvironment(environmentCrn);
        kerberosConfigs.forEach(kerberosConfig -> {
            kerberosConfig.setNameServers(allFreeIpaIpJoined);
        });
        kerberosConfigService.saveAll(kerberosConfigs);
    }

    protected Stack getStackWithInstanceMetadata(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }
}
