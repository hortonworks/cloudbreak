package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.validation.Valid;

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
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.TemplateService;

@Controller
@RequestMapping("templates")
public class TemplateController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemplateService templateService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createTemplate(@CurrentUser User user, @RequestBody @Valid TemplateJson templateRequest) {
        IdJson idJson = templateService.create(userRepository.findOneWithLists(user.getId()), templateRequest);
        return new ResponseEntity<>(idJson, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAllTemplates(@CurrentUser User user) {
        return new ResponseEntity<>(templateService.getAll(userRepository.findOneWithLists(user.getId())), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        TemplateJson templateRequest = templateService.get(templateId);
        return new ResponseEntity<>(templateRequest, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{templateId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@CurrentUser User user, @PathVariable Long templateId) {
        templateService.delete(templateId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
