package com.sequenceiq.provisioning.controller;

import java.util.List;

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

import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.AmbariBlueprintService;

@Controller
@RequestMapping("/cloud/{cloudId}/stack")
public class StackController {

    @Autowired
    private AmbariBlueprintService ambariBlueprintService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> addBlueprint(@CurrentUser User user, @PathVariable Long cloudId, @RequestBody @Valid BlueprintJson blueprintRequest) {
        ambariBlueprintService.addBlueprint(user, cloudId, blueprintRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<BlueprintJson>> retrieveBlueprints(@CurrentUser User user, @PathVariable Long cloudId) {
        return new ResponseEntity<>(ambariBlueprintService.retrieveBlueprints(user, cloudId), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{id}")
    @ResponseBody
    public ResponseEntity<BlueprintJson> retrieveBlueprint(@CurrentUser User user, @PathVariable Long cloudId, @PathVariable String id) {
        return new ResponseEntity<>(ambariBlueprintService.retrieveBlueprint(user, cloudId, id), HttpStatus.OK);
    }
}
