package com.sequenceiq.freeipa.service.crossrealm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class BaseClusterKrb5ConfBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseClusterKrb5ConfBuilder.class);

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String buildCommands(FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("adDomain", crossRealmTrust.getRealm());
        model.put("ipaDomain", freeIpa.getDomain());
        return build(model);
    }

    private String build(Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfiguration.getTemplate("crossrealmtrust/basecluster_krb5conf.ftl", "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudbreakServiceException("Failed to build krb5conf freemarker template", e);
        }
    }
}
