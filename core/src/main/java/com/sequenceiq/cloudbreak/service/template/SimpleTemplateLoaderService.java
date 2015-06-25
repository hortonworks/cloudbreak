package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TEMPLATE_DEFAULTS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class SimpleTemplateLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTemplateLoaderService.class);

    @Value("#{'${cb.template.defaults:" + CB_TEMPLATE_DEFAULTS + "}'.split(',')}")
    private List<String> templateArray;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private TemplateRepository templateRepository;

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
            Template oneByName = null;
            try {
                oneByName = templateRepository.findOneByName(templateName, user.getAccount());
            } catch (Exception e) {
                oneByName = null;
            }
            if (oneByName == null) {
                try {
                    JsonNode jsonNode = jsonHelper.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/templates/%s.tmpl", templateName)));
                    ObjectMapper mapper = new ObjectMapper();
                    TemplateRequest templateRequest = mapper.treeToValue(jsonNode, TemplateRequest.class);
                    Template converted = null;
                    switch (templateRequest.getCloudPlatform()) {
                        case AWS:
                            converted = conversionService.convert(templateRequest, AwsTemplate.class);
                            break;
                        case AZURE:
                            converted = conversionService.convert(templateRequest, AzureTemplate.class);
                            break;
                        case GCP:
                            converted = conversionService.convert(templateRequest, GcpTemplate.class);
                            break;
                        case OPENSTACK:
                            converted = conversionService.convert(templateRequest, OpenStackTemplate.class);
                            break;
                        default:
                            throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.",
                                    templateRequest.getCloudPlatform()));
                    }
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
