package com.sequenceiq.cloudbreak.controller;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CloudbreakEventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakEventController.class);

    @Autowired
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @RequestMapping(method = RequestMethod.GET, value = "/events")
    @ResponseBody
    public ResponseEntity<List<CloudbreakEventsJson>> events(@ModelAttribute("user") CbUser user, @RequestParam(value = "since", required = false) Long since) {
        LOGGER.info("Retrieving events for user {}, since {}", user.getUsername(), since == null ? null : new Date(since));
        List<CloudbreakEventsJson> cloudbreakEvents = cloudbreakEventsFacade.retrieveEvents(user.getUsername(), since);
        return new ResponseEntity<>(cloudbreakEvents, HttpStatus.OK);
    }
}
