package com.sequenceiq.freeipa.service.config;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.FreeIpa;
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
        createKerberosConfig(stackId, FREEIPA_DEFAULT_ADMIN, null, null, null);
    }

    public KerberosConfig createKerberosConfig(Long stackId, String dn, String password, String clusterName, String environmentCrn) {
        FreeIpa freeIpa = getFreeIpaService().findByStackId(stackId);
        Stack stack = getStackWithInstanceMetadata(stackId);
        if (StringUtils.isEmpty(environmentCrn)) {
            environmentCrn = stack.getEnvironmentCrn();
        }
        KerberosConfig kerberosConfig = new KerberosConfig();
        InstanceMetaData master = getMasterInstance(stack);
        kerberosConfig.setAdminUrl(master.getDiscoveryFQDN());
        kerberosConfig.setDomain(freeIpa.getDomain());
        kerberosConfig.setEnvironmentCrn(environmentCrn);
        kerberosConfig.setName(stack.getName());
        kerberosConfig.setPrincipal(dn);
        kerberosConfig.setRealm(freeIpa.getDomain().toUpperCase());
        kerberosConfig.setType(KerberosType.FREEIPA);
        Set<InstanceMetaData> allNotDeletedInstances = stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()).collect(Collectors.toSet());
        String allFreeIpaIpJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.joining(","));
        kerberosConfig.setNameServers(allFreeIpaIpJoined);
        String allNotDeletedIpaInstanceFQDNJoined = allNotDeletedInstances.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.joining(","));
        kerberosConfig.setUrl(allNotDeletedIpaInstanceFQDNJoined);
        kerberosConfig.setPassword(StringUtils.isBlank(password) ? freeIpa.getAdminPassword() : password);
        kerberosConfig.setClusterName(clusterName);
        return kerberosConfigService.createKerberosConfig(kerberosConfig, stack.getAccountId());
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
