package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Controller
public class BlueprintController {

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @RequestMapping(value = "user/blueprints", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createBlueprint(@ModelAttribute("user") CbUser user, @RequestBody @Valid BlueprintJson blueprintRequest) {
        Blueprint blueprint = blueprintConverter.convert(blueprintRequest);
        blueprint = blueprintService.create(user, blueprint);
        return new ResponseEntity<>(new IdJson(blueprint.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "user/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> getPrivateBlueprints(@ModelAttribute("user") CbUser user) {
        Set<Blueprint> blueprints = blueprintService.retrievePrivateBlueprints(user);
        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @RequestMapping(value = "account/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> getAccountBlueprints(@ModelAttribute("user") CbUser user) {
        Set<Blueprint> blueprints = blueprintService.retrieveAccountBlueprints(user);
        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintJson> getBlueprint(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Blueprint blueprint = blueprintService.get(id);
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprint(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        blueprintService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
