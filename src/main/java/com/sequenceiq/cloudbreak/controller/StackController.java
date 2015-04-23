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

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.controller.json.AmbariAddressJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import com.sequenceiq.cloudbreak.controller.json.StackResponse;
import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/stack", description = ControllerDescription.STACK_DESCRIPTION, position = 3)
public class StackController {

    @Autowired
    private StackService stackService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private Decorator<Stack> stackDecorator;

    @ApiOperation(value = StackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackRequest stackRequest) {
        MDCBuilder.buildMdcContext(user);
        return createStack(user, stackRequest, false);
    }

    @ApiOperation(value = StackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackRequest stackRequest) {
        MDCBuilder.buildMdcContext(user);
        return createStack(user, stackRequest, true);
    }

    @ApiOperation(value = StackOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackResponse>> getPrivateStacks(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Stack> stacks = stackService.retrievePrivateStacks(user);
        return new ResponseEntity<>(convertStacks(stacks), HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = "")
    protected Set<StackResponse> convertStacks(Set<Stack> stacks) {
        return (Set<StackResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackResponse.class)));
    }

    @ApiOperation(value = StackOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackResponse>> getAccountStacks(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Stack> stacks = stackService.retrieveAccountStacks(user);

        return new ResponseEntity<>(convertStacks(stacks), HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.get(id);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStackInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStackInPublic(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_STATUS_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStackStatus(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        Map<String, Object> statusMap = conversionService.convert(stackService.get(id), Map.class);
        return new ResponseEntity<>(statusMap, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deleteStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        stackService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deletePrivateStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deletePublicStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateStack(@ModelAttribute("user") CbUser user, @PathVariable Long id, @Valid @RequestBody UpdateStackJson updateRequest) {
        MDCBuilder.buildMdcContext(user);
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

    @ApiOperation(value = StackOpDescription.GET_METADATA, produces = ContentType.JSON, notes = "")
    private void decorateSubnetEntities(List<Subnet> subnetList, Stack stack) {
        for (Subnet subnet : subnetList) {
            subnet.setStack(stack);
        }
    }

    @ApiOperation(value = StackOpDescription.GET_METADATA, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/metadata/{hash}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<InstanceMetaDataJson>> getStackMetadata(@ModelAttribute("user") CbUser user, @PathVariable String hash) {
        MDCBuilder.buildMdcContext(user);
        try {
            Set<InstanceMetaData> metaData = stackService.getMetaData(hash);
            Set<InstanceMetaDataJson> metaDataJsons = (Set<InstanceMetaDataJson>) conversionService.convert(metaData, TypeDescriptor.forObject(metaData),
                    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(InstanceMetaDataJson.class)));
            return new ResponseEntity<>(metaDataJsons, HttpStatus.OK);
        } catch (MetadataIncompleteException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/ambari", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<StackResponse> getStackForAmbari(@RequestBody AmbariAddressJson json) {
        Stack stack = stackService.get(json.getAmbariAddress());
        return new ResponseEntity<>(conversionService.convert(stack, StackResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/validate", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> validateStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackValidationRequest stackValidationRequest) {
        MDCBuilder.buildMdcContext(user);
        StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
        stackService.validateStack(stackValidation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createStack(CbUser user, StackRequest stackRequest, boolean publicInAccount) {
        Stack stack = conversionService.convert(stackRequest, Stack.class);
        stack = stackDecorator.decorate(stack, stackRequest.getCredentialId(), stackRequest.getConsulServerCount());
        stack.setPublicInAccount(publicInAccount);
        stack = stackService.create(user, stack);
        MDCBuilder.buildMdcContext(stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

}
