package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.controller.json.BlueprintRequest;
import com.sequenceiq.cloudbreak.controller.json.BlueprintResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

@Controller
@Api(value = "/blueprints", description = ControllerDescription.BLUEPRINT_DESCRIPTION, position = 0)
public class BlueprintController {

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @RequestMapping(value = "user/blueprints", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = BlueprintOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    public ResponseEntity<IdJson> createPrivateBlueprint(
            @ModelAttribute("user") CbUser user,
            @RequestBody @Valid BlueprintRequest blueprintRequest) {
        MDCBuilder.buildMdcContext(user);
        return createBlueprint(user, blueprintRequest, false);
    }

    @RequestMapping(value = "account/blueprints", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = BlueprintOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    public ResponseEntity<IdJson> createAccountBlueprint(
            @ModelAttribute("user") CbUser user,
            @RequestBody @Valid BlueprintRequest blueprintRequest) {
        MDCBuilder.buildMdcContext(user);
        return createBlueprint(user, blueprintRequest, true);
    }


    @ApiOperation(value = BlueprintOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "user/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintResponse>> getPrivateBlueprints(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Blueprint> blueprints = blueprintService.retrievePrivateBlueprints(user);
        if (blueprints.isEmpty()) {
            Set<Blueprint> blueprintsList = defaultBlueprintLoaderService.loadBlueprints(user);
            blueprints = new HashSet<>((ArrayList<Blueprint>) blueprintRepository.save(blueprintsList));
        }
        Set<BlueprintResponse> jsons = toJsonList(blueprints);
        return new ResponseEntity<>(jsons, HttpStatus.OK);
    }

    @ApiOperation(value = BlueprintOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "user/blueprints/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> getPrivateBlueprint(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Blueprint blueprint = blueprintService.getPrivateBlueprint(name, user);
        return new ResponseEntity<>(conversionService.convert(blueprint, BlueprintResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = BlueprintOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "account/blueprints/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> createAccountBlueprint(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        Blueprint blueprint = blueprintService.getPublicBlueprint(name, user);
        return new ResponseEntity<>(conversionService.convert(blueprint, BlueprintResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = BlueprintOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "account/blueprints", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintResponse>> getAccountBlueprints(@ApiIgnore @ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Blueprint> blueprints = blueprintService.retrieveAccountBlueprints(user);
        if (blueprints.isEmpty()) {
            Set<Blueprint> blueprintsList = defaultBlueprintLoaderService.loadBlueprints(user);
            blueprints = new HashSet<>((ArrayList<Blueprint>) blueprintRepository.save(blueprintsList));
        }
        return new ResponseEntity<>(toJsonList(blueprints), HttpStatus.OK);
    }

    @ApiOperation(value = BlueprintOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> getBlueprint(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        Blueprint blueprint = blueprintService.get(id);
        return new ResponseEntity<>(conversionService.convert(blueprint, BlueprintResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "blueprints/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> deleteBlueprint(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        blueprintService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = BlueprintOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "account/blueprints/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> deleteBlueprintInAccount(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        blueprintService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = BlueprintOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.BLUEPRINT_NOTES)
    @RequestMapping(value = "user/blueprints/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<BlueprintResponse> deleteBlueprintInPrivate(@ApiIgnore @ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        blueprintService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createBlueprint(CbUser user, BlueprintRequest blueprintRequest, boolean publicInAccount) {
        Blueprint blueprint = conversionService.convert(blueprintRequest, Blueprint.class);
        blueprint.setPublicInAccount(publicInAccount);
        blueprint = blueprintService.create(user, blueprint);
        return new ResponseEntity<>(new IdJson(blueprint.getId()), HttpStatus.CREATED);
    }

    private Set<BlueprintResponse> toJsonList(Set<Blueprint> blueprints) {
        return (Set<BlueprintResponse>) conversionService.convert(blueprints,
                TypeDescriptor.forObject(blueprints),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(BlueprintResponse.class)));
    }

}
