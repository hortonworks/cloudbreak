package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.AwsTemplateConverter;
import com.sequenceiq.cloudbreak.converter.AzureTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Controller
@RequestMapping("templates")
public class TemplateController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createTemplate(@CurrentUser User user, @RequestBody @Valid TemplateJson templateRequest) {
        User loadedUser = userRepository.findOneWithLists(user.getId());
        Template template = convert(templateRequest);
        template = templateService.create(loadedUser, template);
        return new ResponseEntity<>(new IdJson(template.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAllTemplates(@CurrentUser User user, HttpServletRequest request) {
        User loadedUser = userRepository.findOneWithLists(user.getId());
        Set<Template> templates = templateService.getAll(loadedUser);
        Set<TemplateJson> templateJsons = convert(templates);
        return new ResponseEntity<>(templateJsons, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        Template template = templateService.get(templateId);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        templateService.delete(templateId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Template convert(TemplateJson templateRequest) {
        Template template = null;
        switch (templateRequest.getCloudPlatform()) {
            case AWS:
                template = awsTemplateConverter.convert(templateRequest);
                break;
            case AZURE:
                template = azureTemplateConverter.convert(templateRequest);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", templateRequest.getCloudPlatform()));
        }
        return template;
    }

    private TemplateJson convert(Template template) {
        TemplateJson templateJson = null;
        switch (template.cloudPlatform()) {
            case AWS:
                templateJson = awsTemplateConverter.convert((AwsTemplate) template);
                break;
            case AZURE:
                templateJson = azureTemplateConverter.convert((AzureTemplate) template);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", template.cloudPlatform()));
        }
        return templateJson;
    }

    private Set<TemplateJson> convert(Set<Template> templates) {
        Set<TemplateJson> jsons = new HashSet<>();
        for (Template template : templates) {
            jsons.add(convert(template));
        }
        return jsons;
    }
}
