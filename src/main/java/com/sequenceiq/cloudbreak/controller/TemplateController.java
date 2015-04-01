package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.validation.Valid;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
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
import com.sequenceiq.cloudbreak.converter.GccTemplateConverter;
import com.sequenceiq.cloudbreak.converter.OpenStackTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Controller
@Api(value = "/templates", description = "Operations on templates", position = 2)
public class TemplateController {

    private static final String TEMPLATE_REQUEST_NOTES =
            "In the template request, id and public parameters are not considered.";

    @Autowired
    private TemplateService templateService;

    @Autowired
    private AwsTemplateConverter awsTemplateConverter;

    @Autowired
    private GccTemplateConverter gccTemplateConverter;

    @Autowired
    private AzureTemplateConverter azureTemplateConverter;

    @Autowired
    private OpenStackTemplateConverter openStackTemplateConverter;

    @ApiOperation(value = "create template as private resource", produces = "application/json", notes = TEMPLATE_REQUEST_NOTES)
    @RequestMapping(value = "user/templates", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateTemplate(@ModelAttribute("user") CbUser user, @RequestBody @Valid TemplateJson templateRequest) {
        return createTemplate(user, templateRequest, false);
    }

    @ApiOperation(value = "create template as public or private resource", produces = "application/json", notes = TEMPLATE_REQUEST_NOTES)
    @RequestMapping(value = "account/templates", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountTemplate(@ModelAttribute("user") CbUser user, @RequestBody @Valid TemplateJson templateRequest) {
        return createTemplate(user, templateRequest, true);
    }

    @ApiOperation(value = "retrieve private templates", produces = "application/json", notes = "")
    @RequestMapping(value = "user/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getPrivateTemplates(@ModelAttribute("user") CbUser user) {
        Set<Template> templates = templateService.retrievePrivateTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve public and private (owned) templates", produces = "application/json", notes = "")
    @RequestMapping(value = "account/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAccountTemplates(@ModelAttribute("user") CbUser user) {
        Set<Template> templates = templateService.retrieveAccountTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve template by id", produces = "application/json", notes = "")
    @RequestMapping(value = "templates/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Template template = templateService.get(id);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a private template by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/templates/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplateInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Template template = templateService.getPrivateTemplate(name, user);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a public or private (owned) template by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/templates/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplateInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Template template = templateService.getPublicTemplate(name, user);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @ApiOperation(value = "delete template by id", produces = "application/json", notes = "")
    @RequestMapping(value = "templates/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        templateService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "delete public (owned) or private template by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/templates/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePublicTemplate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        templateService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "delete private template by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/templates/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePrivateTemplate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        templateService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createTemplate(CbUser user, TemplateJson templateRequest, boolean publicInAccount) {
        Template template = convert(templateRequest, publicInAccount);
        template = templateService.create(user, template);
        return new ResponseEntity<>(new IdJson(template.getId()), HttpStatus.CREATED);
    }

    private Template convert(TemplateJson templateRequest, boolean publicInAccount) {
        switch (templateRequest.getCloudPlatform()) {
            case AWS:
                return awsTemplateConverter.convert(templateRequest, publicInAccount);
            case AZURE:
                return azureTemplateConverter.convert(templateRequest, publicInAccount);
            case GCC:
                return gccTemplateConverter.convert(templateRequest, publicInAccount);
            case OPENSTACK:
                return openStackTemplateConverter.convert(templateRequest, publicInAccount);
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", templateRequest.getCloudPlatform()));
        }
    }

    private TemplateJson convert(Template template) {
        switch (template.cloudPlatform()) {
            case AWS:
                return awsTemplateConverter.convert((AwsTemplate) template);
            case AZURE:
                return azureTemplateConverter.convert((AzureTemplate) template);
            case GCC:
                return gccTemplateConverter.convert((GccTemplate) template);
            case OPENSTACK:
                return openStackTemplateConverter.convert((OpenStackTemplate) template);
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", template.cloudPlatform()));
        }
    }

    private Set<TemplateJson> convert(Set<Template> templates) {
        Set<TemplateJson> jsons = new HashSet<>();
        for (Template template : templates) {
            jsons.add(convert(template));
        }
        return jsons;
    }
}
