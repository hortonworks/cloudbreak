package com.sequenceiq.cloudbreak.controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Controller
@RequestMapping("user/blueprints")
public class BlueprintController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintController.class);

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> addBlueprint(@CurrentUser User user, @RequestBody @Valid BlueprintJson blueprintRequest) {
        Blueprint blueprint = blueprintService.addBlueprint(user, blueprintConverter.convert(blueprintRequest));
        return new ResponseEntity<>(new IdJson(blueprint.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<BlueprintJson>> retrieveBlueprints(Principal principal, HttpServletRequest request) {
        Set<Blueprint> blueprints = new HashSet<>();
        // User loadedUser = userRepository.findOneWithLists(user.getId());

        blueprints.add(blueprintService.get(50l));

        return new ResponseEntity<>(blueprintConverter.convertAllEntityToJson(blueprints), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{id}")
    @ResponseBody
    public ResponseEntity<BlueprintJson> retrieveBlueprint(@CurrentUser User user, @PathVariable Long id) {
        Blueprint blueprint = blueprintService.get(id);
        return new ResponseEntity<>(blueprintConverter.convert(blueprint), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{id}")
    @ResponseBody
    public ResponseEntity<BlueprintJson> deleteBlueprint(@CurrentUser User user, @PathVariable Long id) {
        blueprintService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
