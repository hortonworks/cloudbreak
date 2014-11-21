package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

@Controller
public class BlueprintController {

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @RequestMapping(value = "user/blueprints", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateBlueprint(@ModelAttribute("user") CbUser user, @RequestBody @Valid BlueprintJson blueprintRequest) {
        return createBlueprint(user, blueprintRequest, false);
    }

    @RequestMapping(value = "account/blueprints", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountBlueprint(@ModelAttribute("user") CbUser user, @RequestBody @Valid BlueprintJson blueprintRequest) {
        return createBlueprint(user, blueprintRequest, true);
    }

    @RequestMapping(value = "user/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> getPrivateBlueprints(@ModelAttribute("user") CbUser user) {
        Set<Blueprint> blueprints = blueprintService.retrievePrivateBlueprints(user);
        if (blueprints.isEmpty()) {
            Set<Blueprint> blueprintsList = defaultBlueprintLoaderService.loadBlueprints(user);
            blueprints = new HashSet<>((ArrayList<Blueprint>) blueprintRepository.save(blueprintsList));
        }
        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @RequestMapping(value = "account/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> getAccountBlueprints(@ModelAttribute("user") CbUser user) {
        Set<Blueprint> blueprints = blueprintService.retrieveAccountBlueprints(user);
        if (blueprints.isEmpty()) {
            Set<Blueprint> blueprintsList = defaultBlueprintLoaderService.loadBlueprints(user);
            blueprints = new HashSet<>((ArrayList<Blueprint>) blueprintRepository.save(blueprintsList));
        }
        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @RequestMapping(value = "blueprints/{parameter}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintJson> getBlueprint(@ModelAttribute("user") CbUser user, @PathVariable String parameter) {
        Blueprint blueprint = null;
        if (StringUtils.isNumeric(parameter)) {
            blueprint = blueprintService.get(Long.parseLong(parameter));
        } else {
            blueprint = blueprintService.get(parameter, user);
        }
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @RequestMapping(value = "blueprints/{parameter}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprint(@ModelAttribute("user") CbUser user, @PathVariable String parameter) {
        if (StringUtils.isNumeric(parameter)) {
            blueprintService.delete(Long.parseLong(parameter));
        } else {
            blueprintService.delete(parameter, user);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createBlueprint(CbUser user, BlueprintJson blueprintRequest, Boolean publicInAccount) {
        Blueprint blueprint = blueprintConverter.convert(blueprintRequest);
        blueprint.setPublicInAccount(publicInAccount);
        blueprint = blueprintService.create(user, blueprint);
        return new ResponseEntity<>(new IdJson(blueprint.getId()), HttpStatus.CREATED);
    }
}
