package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Controller
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @RequestMapping(value = "user/templates", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createTemplate(@ModelAttribute("user") CbUser user, @RequestBody @Valid TemplateJson templateRequest) {
        Template template = convert(templateRequest);
        template = templateService.create(user, template);
        return new ResponseEntity<>(new IdJson(template.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "user/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getPrivateTemplates(@ModelAttribute("user") CbUser user) {
        Set<Template> templates = templateService.retrievePrivateTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @RequestMapping(value = "account/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAccountTemplates(@ModelAttribute("user") CbUser user) {
        Set<Template> templates = templateService.retrieveAccountTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @RequestMapping(value = "templates/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Template template = templateService.get(id);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @RequestMapping(value = "templates/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        templateService.delete(id);
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
