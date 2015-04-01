package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;

@Controller
@Api(value = "/events", description = "Operations on events", position = 6)
public class CloudbreakEventController {

    @Autowired
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @ApiOperation(value = "retrieve events by timestamp (long)", produces = "application/json", notes = "")
    @RequestMapping(method = RequestMethod.GET, value = "/events")
    @ResponseBody
    public ResponseEntity<List<CloudbreakEventsJson>> events(@ModelAttribute("user") CbUser user, @RequestParam(value = "since", required = false) Long since) {
        List<CloudbreakEventsJson> cloudbreakEvents = cloudbreakEventsFacade.retrieveEvents(user.getUserId(), since);
        return new ResponseEntity<>(cloudbreakEvents, HttpStatus.OK);
    }
}
