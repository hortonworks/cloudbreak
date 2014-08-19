package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
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
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.controller.json.StatusRequestJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.MetaDataConverter;
import com.sequenceiq.cloudbreak.converter.StackConverter;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataIncompleteException;

import groovyx.net.http.HttpResponseException;

@Controller
@RequestMapping("stacks")
public class StackController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StackService stackService;

    @Autowired
    private StackConverter stackConverter;

    @Autowired
    private MetaDataConverter metaDataConverter;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createStack(@CurrentUser User user, @RequestBody @Valid StackJson stackRequest) {
        User loadedUser = userRepository.findOneWithLists(user.getId());
        Stack stack = stackConverter.convert(stackRequest);
        if (stack.getUserRoles().isEmpty()) {
            stack.getUserRoles().addAll(loadedUser.getUserRoles());
        }
        stack = stackService.create(loadedUser, stack);
        return new ResponseEntity<>(new IdJson(stack.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackJson>> getAllStacks(@CurrentUser User user, HttpServletRequest request) {
        User currentUser = userRepository.findOneWithLists(user.getId());
        Set<Stack> stacks = stackService.getAll(currentUser);
        Set<StackJson> stackJsons = stackConverter.convertAllEntityToJson(stacks);
        return new ResponseEntity<>(stackJsons, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{stackId}")
    @ResponseBody
    public ResponseEntity<StackJson> getStack(@CurrentUser User user, @PathVariable Long stackId) {
        Stack stack = stackService.get(user, stackId);
        StackDescription stackDescription = stackService.getStackDescription(user, stack);
        StackJson stackJson = stackConverter.convert(stack, stackDescription);
        return new ResponseEntity<>(stackJson, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{stackId}")
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteStack(@CurrentUser User user, @PathVariable Long stackId) {
        stackService.delete(userRepository.findOne(user.getId()), stackId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "{stackId}")
    @ResponseBody
    public ResponseEntity<Boolean> startOrStopAllOnStack(@CurrentUser User user, @PathVariable Long stackId, @RequestBody StatusRequestJson statusRequestJson) {
        Stack stack = stackService.get(user, stackId);
        stack.setNodeCount(stack.getNodeCount() + 1);
        switch (statusRequestJson.getStatusRequest()) {
            case STOP:
                return new ResponseEntity<>(stackService.stopAll(user, stackId), HttpStatus.OK);
            case START:
                return new ResponseEntity<>(stackService.startAll(user, stackId), HttpStatus.OK);
            default:
                throw new BadRequestException("The requested status not valid.");
        }
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/r/{stackId}/{hostgroup}")
    @ResponseBody
    public void increaseNodeCount(@CurrentUser User user, @PathVariable Long stackId, @PathVariable String hostgroup) throws HttpResponseException {
        Stack stack = stackService.get(user, stackId);
        try {
            if (stackService.assignableHostgroup(stack, hostgroup)) {
                stack.setNodeCount(stack.getNodeCount() + 1);
                stackService.addNode(user, stack, hostgroup);
            } else {
                throw new BadRequestException(String.format("Invalid hostgroup: blueprint %s does not contain %s hostgroup.",
                        stack.getCluster().getBlueprint().getId(), hostgroup));
            }
        } catch (Exception e) {
            throw new BadRequestException(String.format("Stack %s put occurs a problem '%s': %s", stackId, e.getMessage(), e));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/metadata/{hash}")
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
