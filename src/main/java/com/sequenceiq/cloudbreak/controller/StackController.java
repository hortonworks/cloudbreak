package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;
import com.sequenceiq.cloudbreak.converter.MetaDataConverter;
import com.sequenceiq.cloudbreak.converter.StackConverter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

@Controller
public class StackController {

    @Autowired
    private StackService stackService;

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private MetaDataConverter metaDataConverter;

    @RequestMapping(value = "user/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackJson stackRequest) {
        Stack stack = stackConverter.convert(stackRequest);
        stack = stackService.create(user, stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "user/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackJson>> getPrivateStacks(@ModelAttribute("user") CbUser user) {
        Set<Stack> stacks = stackService.retrievePrivateStacks(user);
        return new ResponseEntity<>(stackConverter.convertAllEntityToJson(stacks), HttpStatus.OK);
    }

    @RequestMapping(value = "account/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackJson>> getAccountStacks(@ModelAttribute("user") CbUser user) {
        Set<Stack> stacks = stackService.retrieveAccountStacks(user);
        return new ResponseEntity<>(stackConverter.convertAllEntityToJson(stacks), HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackJson> getStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Stack stack = stackService.get(id);
        StackDescription stackDescription = stackService.getStackDescription(stack);
        StackJson stackJson = stackConverter.convert(stack, stackDescription);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStackStatus(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        return new ResponseEntity<>(stackConverter.convertStackStatus(stackService.get(id)), HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        stackService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateStack(@ModelAttribute("user") CbUser user, @PathVariable Long id, @Valid @RequestBody UpdateStackJson updateRequest) {
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            stackService.updateNodeCount(id, updateRequest.getScalingAdjustment());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @RequestMapping(value = "stacks/metadata/{hash}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<InstanceMetaDataJson>> getStackMetadata(@PathVariable String hash) {
        try {
            Set<InstanceMetaData> metaData = stackService.getMetaData(hash);
            return new ResponseEntity<>(metaDataConverter.convertAllEntityToJson(metaData), HttpStatus.OK);
        } catch (MetadataIncompleteException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

}
