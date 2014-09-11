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

import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakUsagesFacade;

@Controller
public class CloudbreakUsageController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUsageController.class);

    @Autowired
    private CloudbreakUsagesFacade cloudbreakUsagesFacade;

    @RequestMapping(method = RequestMethod.GET, value = "/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> deployerUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "account", required = false) Long accountId,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) Long vmtype,
            @RequestParam(value = "hours", required = false) String hours) {
        LOGGER.info("Retrieving events for user {}, since {}", user.getUsername(), since == null ? null : new Date(since));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/accounts/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> accountUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "account", required = false) Long accountId,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) Long vmtype,
            @RequestParam(value = "hours", required = false) String hours) {
        LOGGER.info("Retrieving events for user {}, since {}", user.getUsername(), since == null ? null : new Date(since));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/user/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> userUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "account", required = false) Long accountId,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) Long vmtype,
            @RequestParam(value = "hours", required = false) String hours) {
        LOGGER.info("Retrieving events for user {}, since {}", user.getUsername(), since == null ? null : new Date(since));
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
