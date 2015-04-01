package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
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
@Api(value = "/blueprints", description = "Operations on blueprints", position = 0)
public class BlueprintController {

    private static final String BLUEPRINT_REQUEST_NOTES =
            "In the blueprint request, id, blueprintName and public parameters are not considered.";

    private static final String BLUEPRINT_RESPONSE_NOTES =
            "In the blueprint response, name and url parameters are not considered.";

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @ApiOperation(value = "create blueprint as private resource", produces = "application/json",
            notes = BLUEPRINT_REQUEST_NOTES)
    @RequestMapping(value = "user/blueprints", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateBlueprint(
            @ModelAttribute("user") CbUser user,
            @ApiParam(name = "blueprint", required = true) @RequestBody @Valid BlueprintJson blueprintRequest) {
        return createBlueprint(user, blueprintRequest, false);
    }

    @ApiOperation(value = "create blueprint as public or private resource", produces = "application/json",
            notes = BLUEPRINT_REQUEST_NOTES)
    @RequestMapping(value = "account/blueprints", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountBlueprint(
            @ModelAttribute("user") CbUser user,
            @ApiParam(name = "blueprint", required = true) @RequestBody @Valid BlueprintJson blueprintRequest) {
        return createBlueprint(user, blueprintRequest, true);
    }


    @ApiOperation(value = "retrieve private blueprints", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
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

    @ApiOperation(value = "retrieve a private blueprint by name", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "user/blueprints/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintJson> getPrivateBlueprint(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Blueprint blueprint = blueprintService.getPrivateBlueprint(name, user);
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a public or private (owned) blueprint by name", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "account/blueprints/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintJson> createAccountBlueprint(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        Blueprint blueprint = blueprintService.getPublicBlueprint(name, user);
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve public and private (owned) blueprints", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "account/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> getAccountBlueprints(@ApiIgnore @ModelAttribute("user") CbUser user) {
        Set<Blueprint> blueprints = blueprintService.retrieveAccountBlueprints(user);
        if (blueprints.isEmpty()) {
            Set<Blueprint> blueprintsList = defaultBlueprintLoaderService.loadBlueprints(user);
            blueprints = new HashSet<>((ArrayList<Blueprint>) blueprintRepository.save(blueprintsList));
        }
        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve blueprint by id", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintJson> getBlueprint(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Blueprint blueprint = blueprintService.get(id);
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @ApiOperation(value = "delete blueprint by id", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprint(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable Long id) {
        blueprintService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "delete public (owned) or private blueprint by name", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "account/blueprints/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprintInAccount(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        blueprintService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "delete private blueprint by name", produces = "application/json",
            notes = BLUEPRINT_RESPONSE_NOTES)
    @RequestMapping(value = "user/blueprints/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprintInPrivate(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        blueprintService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createBlueprint(CbUser user, BlueprintJson blueprintRequest, boolean publicInAccount) {
        Blueprint blueprint = blueprintConverter.convert(blueprintRequest, publicInAccount);
        blueprint = blueprintService.create(user, blueprint);
        return new ResponseEntity<>(new IdJson(blueprint.getId()), HttpStatus.CREATED);
    }
}
