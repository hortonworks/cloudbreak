package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

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
import com.sequenceiq.cloudbreak.controller.json.CertificateResponse;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import com.sequenceiq.cloudbreak.controller.json.StackResponse;
import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/stack", description = ControllerDescription.STACK_DESCRIPTION, position = 3)
public class StackController {

    @Inject
    private StackService stackService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private Decorator<Stack> stackDecorator;

    @ApiOperation(value = StackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackRequest stackRequest) {
        MDCBuilder.buildUserMdcContext(user);
        return createStack(user, stackRequest, false);
    }

    @ApiOperation(value = StackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackRequest stackRequest) {
        MDCBuilder.buildUserMdcContext(user);
        return createStack(user, stackRequest, true);
    }

    @ApiOperation(value = StackOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackResponse>> getPrivateStacks(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildUserMdcContext(user);
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
        MDCBuilder.buildUserMdcContext(user);
        Set<Stack> stacks = stackService.retrieveAccountStacks(user);

        return new ResponseEntity<>(convertStacks(stacks), HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.get(id);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStackInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<StackResponse> getStackInPublic(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        StackResponse stackJson = conversionService.convert(stack, StackResponse.class);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.GET_STATUS_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStackStatus(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildUserMdcContext(user);
        Map<String, Object> statusMap = conversionService.convert(stackService.get(id), Map.class);
        return new ResponseEntity<>(statusMap, HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deleteStack(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildUserMdcContext(user);
        stackService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "user/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deletePrivateStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "account/stacks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateResponse> deletePublicStack(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        stackService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateStack(@ModelAttribute("user") CbUser user, @PathVariable Long id, @Valid @RequestBody UpdateStackJson updateRequest) {
        MDCBuilder.buildUserMdcContext(user);
        if (updateRequest.getStatus() != null) {
            stackService.updateStatus(id, updateRequest.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            stackService.updateNodeCount(id, updateRequest.getInstanceGroupAdjustment());
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


    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/{id}/certificate", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable Long id) {
        return new ResponseEntity<>(new CertificateResponse(stackService.getCertificate(id)), HttpStatus.OK);
    }

    @ApiOperation(value = StackOpDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @RequestMapping(value = "stacks/validate", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> validateStack(@ModelAttribute("user") CbUser user, @RequestBody @Valid StackValidationRequest stackValidationRequest) {
        MDCBuilder.buildUserMdcContext(user);
        StackValidation stackValidation = conversionService.convert(stackValidationRequest, StackValidation.class);
        stackService.validateStack(stackValidation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createStack(CbUser user, StackRequest stackRequest, boolean publicInAccount) {
        Stack stack = conversionService.convert(stackRequest, Stack.class);
        MDCBuilder.buildMdcContext(stack);
        stack = stackDecorator.decorate(stack, stackRequest.getCredentialId(), stackRequest.getConsulServerCount(), stackRequest.getNetworkId(),
                stackRequest.getSecurityGroupId());
        stack.setPublicInAccount(publicInAccount);
        stack = stackService.create(user, stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

}
