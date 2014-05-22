package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.converter.AwsTemplateConverter;
import com.sequenceiq.provisioning.converter.AzureTemplateConverter;
import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.Template;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.StackRepository;
import com.sequenceiq.provisioning.repository.TemplateRepository;

@Service
public class SimpleTemplateService implements TemplateService {

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
            throw new NotFoundException(String.format("Template '%s' not found.", id));
        } else {
            switch (template.cloudPlatform()) {
                case AWS:
                    return awsTemplateConverter.convert((AwsTemplate) template);
                case AZURE:
                    return azureTemplateConverter.convert((AzureTemplate) template);
                default:
                    throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", template.cloudPlatform()));
            }
        }
    }

    @Override
    public void create(User user, TemplateJson templateRequest) {
        switch (templateRequest.getCloudPlatform()) {
            case AWS:
                Template awsTemplate = awsTemplateConverter.convert(templateRequest);
                awsTemplate.setUser(user);
                templateRepository.save(awsTemplate);
                break;
            case AZURE:
                Template azureTemplate = azureTemplateConverter.convert(templateRequest);
                azureTemplate.setUser(user);
                templateRepository.save(azureTemplate);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", templateRequest.getCloudPlatform()));
        }
    }

    @Override
    public void delete(Long id) {
        Template template = templateRepository.findOne(id);
        if (template == null) {
            throw new NotFoundException(String.format("Template '%s' not found.", id));
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
