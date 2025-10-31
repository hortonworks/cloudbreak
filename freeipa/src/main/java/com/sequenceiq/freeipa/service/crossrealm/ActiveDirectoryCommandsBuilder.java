package com.sequenceiq.freeipa.service.crossrealm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class ActiveDirectoryCommandsBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveDirectoryCommandsBuilder.class);

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String buildCommands(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("trustCommandType", trustCommandType);
        model.put("adDomain", crossRealmTrust.getKdcRealm().toLowerCase());
        model.put("ipaDomain", freeIpa.getDomain());
        if (trustCommandType == TrustCommandType.SETUP) {
            model.put("ipaIpAdresses", getServerIps(stack));
            model.put("trustSecret", crossRealmTrust.getTrustSecret());
        }
        return build(trustCommandType, model);
    }

    private String build(TrustCommandType trustCommandType, Map<String, Object> model) {
        String template = String.format("crossrealmtrust/activedirectory_%s_commands.ftl", trustCommandType.name().toLowerCase());
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            String message = String.format("Failed to build %s freemarker template", template);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private List<String> getServerIps(Stack stack) {
        Optional<LoadBalancer> loadBalancer = freeIpaLoadBalancerService.findByStackId(stack.getId());
        return loadBalancer
                .map(balancer -> balancer.getIp().stream().toList())
                .orElseGet(() -> stack.getNotDeletedInstanceMetaDataSet().stream()
                        .map(InstanceMetaData::getPrivateIp)
                        .toList());
    }
}
