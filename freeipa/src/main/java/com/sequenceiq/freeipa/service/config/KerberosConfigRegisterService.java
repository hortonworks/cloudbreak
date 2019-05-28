package com.sequenceiq.freeipa.service.config;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;

@Service
public class KerberosConfigRegisterService extends AbstractConfigRegister {

    public static final String FREEIPA_DEFAULT_ADMIN = "admin";

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigRegisterService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Override
    public void register(Long stackId) {
        FreeIpa freeIpa = getFreeIpaService().findByStackId(stackId);
        Stack stack = getStackWithInstanceMetadata(stackId);
        KerberosConfig kerberosConfig = new KerberosConfig();
        InstanceGroup masterGroup =
                stack.getInstanceGroups().stream().filter(instanceGroup -> InstanceGroupType.MASTER == instanceGroup.getInstanceGroupType()).findFirst().get();
        InstanceMetaData master = masterGroup.getNotDeletedInstanceMetaDataSet().stream().findFirst().get();
        kerberosConfig.setAdminUrl(master.getDiscoveryFQDN());
        kerberosConfig.setDomain(freeIpa.getDomain());
        kerberosConfig.setEnvironmentId(stack.getEnvironment());
        kerberosConfig.setName(stack.getName());
        kerberosConfig.setPrincipal(FREEIPA_DEFAULT_ADMIN);
        kerberosConfig.setRealm(freeIpa.getDomain().toUpperCase());
        kerberosConfig.setType(KerberosType.FREEIPA);
        Set<InstanceMetaData> allNotDeletedInstances = stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()).collect(Collectors.toSet());
        String allFreeIpaIpJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.joining(","));
        kerberosConfig.setNameServers(allFreeIpaIpJoined);
        String allNotDeletedIpaInstanceFQDNJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.joining(","));
        kerberosConfig.setUrl(allNotDeletedIpaInstanceFQDNJoined);
        kerberosConfig.setPassword(freeIpa.getAdminPassword());
        kerberosConfigService.createKerberosConfig(kerberosConfig, stack.getAccountId());
    }

    @Override
    public void delete(Stack stack) {
        try {
            kerberosConfigService.delete(stack.getEnvironment(), stack.getAccountId());
        } catch (NotFoundException e) {
            LOGGER.info("Kerberos config not exists for environment {}", stack.getEnvironment());
        }
    }
}
