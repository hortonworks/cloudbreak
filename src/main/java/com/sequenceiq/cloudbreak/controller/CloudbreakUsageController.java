package com.sequenceiq.cloudbreak.controller;

import java.util.List;

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
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakUsagesFacade;

@Controller
public class CloudbreakUsageController {

    @Autowired
    private CloudbreakUsagesFacade cloudbreakUsagesFacade;

    @RequestMapping(method = RequestMethod.GET, value = "/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> deployerUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "user", required = false) String userId,
            @RequestParam(value = "account", required = false) String accountId,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) String vmtype,
            @RequestParam(value = "hours", required = false) Long hours,
            @RequestParam(value = "blueprintname", required = false) String bpName,
            @RequestParam(value = "blueprintid", required = false) Long bpId) {
        CbUsageFilterParameters params = new CbUsageFilterParameters(accountId, userId, since, cloud, zone, vmtype, hours, bpId, bpName);
        List<CloudbreakUsageJson> usages = cloudbreakUsagesFacade.getUsagesFor(params);
        return new ResponseEntity<>(usages, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/account/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> accountUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "user", required = false) String userId,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) String vmtype,
            @RequestParam(value = "hours", required = false) Long hours,
            @RequestParam(value = "blueprintname", required = false) String bpName,
            @RequestParam(value = "blueprintid", required = false) Long bpId) {
        CbUsageFilterParameters params = new CbUsageFilterParameters(user.getAccount(), userId, since, cloud, zone, vmtype, hours, bpId, bpName);
        List<CloudbreakUsageJson> usages = cloudbreakUsagesFacade.getUsagesFor(params);
        return new ResponseEntity<>(usages, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/user/usages")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> userUsages(@ModelAttribute("user") CbUser user,
            @RequestParam(value = "since", required = false) Long since,
            @RequestParam(value = "cloud", required = false) String cloud,
            @RequestParam(value = "zone", required = false) String zone,
            @RequestParam(value = "vmtype", required = false) String vmtype,
            @RequestParam(value = "hours", required = false) Long hours,
            @RequestParam(value = "blueprintname", required = false) String bpName,
            @RequestParam(value = "blueprintid", required = false) Long bpId) {
        CbUsageFilterParameters params = new CbUsageFilterParameters(user.getAccount(), user.getUserId(), since, cloud, zone, vmtype, hours, bpId, bpName);
        List<CloudbreakUsageJson> usages = cloudbreakUsagesFacade.getUsagesFor(params);
        return new ResponseEntity<>(usages, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/usages/generate")
    @ResponseBody
    public ResponseEntity<List<CloudbreakUsageJson>> generateUsages(@ModelAttribute("user") CbUser user) {
        cloudbreakUsagesFacade.generateUserUsages();
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
