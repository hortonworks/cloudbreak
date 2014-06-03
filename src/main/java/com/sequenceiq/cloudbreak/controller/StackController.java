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
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.controller.json.StatusRequestJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.StackService;

@Controller
@RequestMapping("stack")
public class StackController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StackService stackService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createStack(@CurrentUser User user, @RequestBody @Valid StackJson stackRequest) {
        return new ResponseEntity<>(stackService.create(userRepository.findOneWithLists(user.getId()), stackRequest), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<StackJson>> getAllStack(@CurrentUser User user) {
        User currentUser = userRepository.findOneWithLists(user.getId());
        return new ResponseEntity<>(stackService.getAll(currentUser), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{stackId}")
    @ResponseBody
    public ResponseEntity<StackJson> getStack(@CurrentUser User user, @PathVariable Long stackId) {
        StackJson stackJson = stackService.get(userRepository.findOne(user.getId()), stackId);
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
    public ResponseEntity<Boolean> startOrStopAllOnStack(@CurrentUser User user, @PathVariable Long stackId,
            @RequestBody StatusRequestJson statusRequestJson) {
        switch (statusRequestJson.getStatusRequest()) {
        case STOP:
            return new ResponseEntity<>(stackService.stopAll(user, stackId), HttpStatus.OK);
        case START:
            return new ResponseEntity<>(stackService.startAll(user, stackId), HttpStatus.OK);
        default:
            throw new BadRequestException("The requested status not valid.");
        }
    }

}
