package com.sequenceiq.cloudbreak.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.AwsTemplateConverter;
import com.sequenceiq.cloudbreak.converter.AzureTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Service
public class SimpleTemplateService implements TemplateService {

    private static final String CLOUD_PLATFORM_NOT_SUPPORTED_MSG = "The cloudPlatform '%s' is not supported.";

    private static final String TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found.";

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public Set<TemplateJson> getAll(User user) {
        Set<TemplateJson> result = new HashSet<>();
        result.addAll(awsTemplateConverter.convertAllEntityToJson(user.getAwsTemplates()));
        result.addAll(azureTemplateConverter.convertAllEntityToJson(user.getAzureTemplates()));
        return result;
    }

    @Override
    public TemplateJson get(Long id) {
        Template template = templateRepository.findOne(id);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        } else {
            switch (template.cloudPlatform()) {
                case AWS:
                    return awsTemplateConverter.convert((AwsTemplate) template);
                case AZURE:
                    return azureTemplateConverter.convert((AzureTemplate) template);
                default:
                    throw new UnknownFormatConversionException(String.format(CLOUD_PLATFORM_NOT_SUPPORTED_MSG, template.cloudPlatform()));
            }
        }
    }

    @Override
    public IdJson create(User user, TemplateJson templateRequest) {
        switch (templateRequest.getCloudPlatform()) {
            case AWS:
                Template awsTemplate = awsTemplateConverter.convert(templateRequest);
                awsTemplate.setUser(user);
                templateRepository.save(awsTemplate);
                return new IdJson(awsTemplate.getId());
            case AZURE:
                Template azureTemplate = azureTemplateConverter.convert(templateRequest);
                azureTemplate.setUser(user);
                templateRepository.save(azureTemplate);
                return new IdJson(azureTemplate.getId());
            default:
                throw new UnknownFormatConversionException(String.format(CLOUD_PLATFORM_NOT_SUPPORTED_MSG, templateRequest.getCloudPlatform()));
        }
    }

    @Override
    public void delete(Long id) {
        Template template = templateRepository.findOne(id);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        }
        List<Stack> allStackForTemplate = getAllStackForTemplate(id);
        if (allStackForTemplate.size() == 0) {
            templateRepository.delete(template);
        } else {
            throw new BadRequestException(String.format("Template '%s' has some cloud dependency please remove clouds before the deletion.", id));
        }
    }

    private List<Stack> getAllStackForTemplate(Long id) {
        return stackRepository.findAllStackForTemplate(id);
    }

}
