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

    public String buildCommands(Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("adDomain", crossRealmTrust.getRealm().toLowerCase());
        model.put("ipaIpAdresses", getServerIps(stack));
        model.put("ipaDomain", freeIpa.getDomain());
        model.put("trustSecret", crossRealmTrust.getTrustSecret());
        return build(model);
    }

    private String build(Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfiguration.getTemplate("crossrealmtrust/activedirectory_commands.ftl", "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudbreakServiceException("Failed to build activedirectory_commands freemarker template", e);
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
