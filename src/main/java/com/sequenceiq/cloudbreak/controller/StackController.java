package com.sequenceiq.cloudbreak.controller;

import java.util.HashMap;
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

import com.sequenceiq.cloudbreak.controller.json.AmbariAddressJson;
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
    public ResponseEntity<IdJson> createPrivateStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackJson stackRequest) {
        return createStack(user, stackRequest, false);
    }

    @RequestMapping(value = "account/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackJson stackRequest) {
        return createStack(user, stackRequest, true);
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

    @RequestMapping(value = "stacks/{parameter}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackJson> getStack(@PathVariable String parameter) {
        StackJson stackJson = null;
        try {
            Stack stack = stackService.get(Long.parseLong(parameter));
            StackDescription stackDescription = stackService.getStackDescription(stack);
            stackJson = stackConverter.convert(stack, stackDescription);
        } catch (NumberFormatException e) {
            Stack stack = stackService.get(parameter);
            StackDescription stackDescription = stackService.getStackDescription(stack);
            stackJson = stackConverter.convert(stack, stackDescription);
        }
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{parameter}/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStackStatus(@PathVariable String parameter) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        try {
            stringObjectMap = stackConverter.convertStackStatus(stackService.get(Long.parseLong(parameter)));
        } catch (NumberFormatException e) {
            stringObjectMap = stackConverter.convertStackStatus(stackService.get(parameter));
        }
        return new ResponseEntity<>(stringObjectMap, HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{parameter}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteStack(@PathVariable String parameter) {
        try {
            stackService.delete(Long.parseLong(parameter));
        } catch (NumberFormatException e) {
            stackService.delete(parameter);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "stacks/{parameter}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateStack(@PathVariable String parameter, @Valid @RequestBody UpdateStackJson updateRequest) {
        if (updateRequest.getStatus() != null) {
            try {
                stackService.updateStatus(Long.parseLong(parameter), updateRequest.getStatus());
            } catch (NumberFormatException e) {
                stackService.updateStatus(parameter, updateRequest.getStatus());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            try {
                stackService.updateNodeCount(Long.parseLong(parameter), updateRequest.getScalingAdjustment());
            } catch (NumberFormatException e) {
                stackService.updateNodeCount(parameter, updateRequest.getScalingAdjustment());
            }
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

    @RequestMapping(value = "stacks/ambari", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<StackJson> getStackForAmbari(@RequestBody AmbariAddressJson json) {
        Stack stack = stackService.getByAmbariAdress(json.getAmbariAddress());
        return new ResponseEntity<>(stackConverter.convert(stack), HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createStack(CbUser user, StackJson stackRequest, Boolean publicInAccount) {
        Stack stack = stackConverter.convert(stackRequest);
        stack.setPublicInAccount(publicInAccount);
        stack = stackService.create(user, stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

}
