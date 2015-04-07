package com.sequenceiq.cloudbreak.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
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
import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

@Controller
public class StackController {

    @Autowired
    private StackService stackService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

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
        return new ResponseEntity<>(convertStacks(stacks), HttpStatus.OK);
    }

    protected Set<StackJson> convertStacks(Set<Stack> stacks) {
        return (Set<StackJson>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackJson.class)));
    }

    @RequestMapping(value = "account/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackJson>> getAccountStacks(@ModelAttribute("user") CbUser user) {
        Set<Stack> stacks = stackService.retrieveAccountStacks(user);

        return new ResponseEntity<>(convertStacks(stacks), HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackJson> getStack(@PathVariable Long id) {
        Stack stack = stackService.get(id);
        StackJson stackJson = conversionService.convert(stack, StackJson.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackJson> getStackInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Stack stack = stackService.getPrivateStack(name, user);
        StackJson stackJson = conversionService.convert(stack, StackJson.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackJson> getStackInPublic(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Stack stack = stackService.getPublicStack(name, user);
        StackJson stackJson = conversionService.convert(stack, StackJson.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStackStatus(@PathVariable Long id) {
        Map<String, Object> statusMap = conversionService.convert(stackService.get(id), Map.class);
        return new ResponseEntity<>(statusMap, HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        stackService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePrivateStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePublicStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "stacks/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateStack(@PathVariable Long id, @Valid @RequestBody UpdateStackJson updateRequest) {
        MDCBuilder.buildMdcContext();
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (updateRequest.getInstanceGroupAdjustment() != null) {
            stackService.updateNodeCount(id, updateRequest.getInstanceGroupAdjustment());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            List<Subnet> subnetList = (List<Subnet>) conversionService.convert(updateRequest.getAllowedSubnets(),
                    TypeDescriptor.forObject(updateRequest.getAllowedSubnets()),
                    TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Subnet.class)));
            decorateSubnetEntities(subnetList, stackService.get(id));
            stackService.updateAllowedSubnets(id, subnetList);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    private void decorateSubnetEntities(List<Subnet> subnetList, Stack stack) {
        for (Subnet subnet : subnetList) {
            subnet.setStack(stack);
        }
    }

    @RequestMapping(value = "stacks/metadata/{hash}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<InstanceMetaDataJson>> getStackMetadata(@PathVariable String hash) {
        try {
            Set<InstanceMetaData> metaData = stackService.getMetaData(hash);
            Set<InstanceMetaDataJson> metaDataJsons = (Set<InstanceMetaDataJson>) conversionService.convert(metaData, TypeDescriptor.forObject(metaData),
                    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceMetaDataJson.class)));
            return new ResponseEntity<>(metaDataJsons, HttpStatus.OK);
        } catch (MetadataIncompleteException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @RequestMapping(value = "stacks/ambari", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<StackJson> getStackForAmbari(@RequestBody AmbariAddressJson json) {
        Stack stack = stackService.get(json.getAmbariAddress());
        return new ResponseEntity<>(conversionService.convert(stack, StackJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "stacks/validate", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> validateStack(@RequestBody @Valid StackValidationRequest stackValidationRequest) {
        StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
        stackService.validateStack(stackValidation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createStack(CbUser user, StackJson stackRequest, boolean publicInAccount) {
        Stack stack = conversionService.convert(stackRequest, Stack.class);
        stack.setPublicInAccount(publicInAccount);
        stack = stackService.create(user, stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

}
