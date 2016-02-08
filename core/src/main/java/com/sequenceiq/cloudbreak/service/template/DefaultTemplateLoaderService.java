package com.sequenceiq.cloudbreak.service.template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
@Transactional
public class DefaultTemplateLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTemplateLoaderService.class);

    @Value("#{'${cb.template.defaults:}'.split(',')}")
    private List<String> templateArray;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private TemplateService templateService;

    @Inject
    private JsonHelper jsonHelper;

    public Set<Template> loadTemplates(CbUser user) {
        Set<Template> templates = new HashSet<>();
        if (templateRepository.findAllDefaultInAccount(user.getAccount()).isEmpty()) {
            templates.addAll(createDefaultTemplates(user));
        }
        return templates;
    }

    private Set<Template> createDefaultTemplates(CbUser user) {
        Set<Template> templates = new HashSet<>();
        for (String templateName : templateArray) {
            if (!templateName.isEmpty() && templateRepository.findOneByName(templateName, user.getAccount()) == null) {
                try {
                    JsonNode jsonNode = jsonHelper.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/templates/%s.tmpl", templateName)));
                    TemplateRequest templateRequest = JsonUtil.treeToValue(jsonNode, TemplateRequest.class);
                    Template converted = conversionService.convert(templateRequest, Template.class);
                    converted.setAccount(user.getAccount());
                    converted.setOwner(user.getUserId());
                    converted.setPublicInAccount(true);
                    converted.setStatus(ResourceStatus.DEFAULT);
                    templateRepository.save(converted);
                    templates.add(converted);
                } catch (Exception e) {
                    LOGGER.error("Template is not available for '{}' user.", e, user);
                }
            }
        }
        return templates;
    }

}
