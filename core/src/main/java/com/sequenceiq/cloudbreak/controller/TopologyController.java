package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.TopologyOpDesctiption;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.json.TopologyRequest;
import com.sequenceiq.cloudbreak.controller.json.TopologyResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/topologies", description = ControllerDescription.TOPOLOGY_DESCRIPTION, position = 9)
public class TopologyController {
    @Inject
    private TopologyService topologyService;
    @Inject
    private TopologyRepository topologyRepository;
    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;


    @ApiOperation(value = TopologyOpDesctiption.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES)
    @RequestMapping(value = "account/topologies", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TopologyResponse>> getAccountTopoligies(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildUserMdcContext(user);
        Set<Topology> stacks = topologyRepository.findAllInAccount(user.getAccount());

        return new ResponseEntity<>(convert(stacks), HttpStatus.OK);
    }

    private TopologyResponse convert(Topology topology) {
        return conversionService.convert(topology, TopologyResponse.class);
    }

    private Set<TopologyResponse> convert(Set<Topology> topologies) {
        Set<TopologyResponse> jsons = new HashSet<>();
        for (Topology topology : topologies) {
            jsons.add(convert(topology));
        }
        return jsons;
    }

    @RequestMapping(value = "account/topologies", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = TopologyOpDesctiption.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES)
    public ResponseEntity<IdJson> createAccountTopology(@ModelAttribute("user") CbUser user, @RequestBody @Valid TopologyRequest topologyRequest) {
        MDCBuilder.buildUserMdcContext(user);
        Topology topology = conversionService.convert(topologyRequest, Topology.class);
        topology = topologyService.create(user, topology);
        return new ResponseEntity<>(new IdJson(topology.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "account/topologies/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = TopologyOpDesctiption.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES)
    public ResponseEntity<TemplateResponse> deleteTopology(@ModelAttribute("user") CbUser user, @PathVariable Long id,
            @RequestParam(value = "forced", required = false, defaultValue = "false") Boolean forced) {
        MDCBuilder.buildUserMdcContext(user);
        topologyService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
