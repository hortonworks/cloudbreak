package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.ClusterOpDescription;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.json.UpdateClusterJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, position = 4)
public class ClusterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private Decorator<Cluster> clusterDecorator;

    @Autowired
    private Decorator<HostGroup> hostGroupDecorator;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private StackService stackService;

    @Autowired
    private JsonHelper jsonHelper;

    @ApiOperation(value = ClusterOpDescription.POST_FOR_STACK, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> create(@ModelAttribute("user") CbUser user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest clusterRequest) {
        MDCBuilder.buildMdcContext(user);
        Cluster cluster = conversionService.convert(clusterRequest, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, stackId, clusterRequest.getBlueprintId(), clusterRequest.getHostGroups());
        clusterService.create(user, stackId, cluster);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiOperation(value = ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveCluster(@ModelAttribute("user") CbUser user, @PathVariable Long stackId) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.get(stackId);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "user/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePrivateCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "account/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePublicCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateCluster(@PathVariable Long stackId, @RequestBody UpdateClusterJson updateJson) {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        if (updateJson.getStatus() != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isStateFailed()) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId);
            Set<HostGroup> hostGroups = new HashSet<>();
            for (HostGroupJson json : updateJson.getHostgroups()) {
                HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
                hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, json.getInstanceGroupName(), json.getRecipeIds(), false);
                hostGroups.add(hostGroupService.save(hostGroup));
            }
            clusterService.recreate(stackId, updateJson.getBlueprintId(), hostGroups);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (updateJson.getHostGroupAdjustment() != null) {
            if (!stack.getStatus().equals(Status.AVAILABLE)) {
                throw new BadRequestException(String.format(
                        "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                        stack.getStatus()));
            }
            LOGGER.info("Cluster host adjustment request received. Stack id: {} ", stackId);
            clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId);
        throw new BadRequestException("Invalid update cluster request!");
    }

    private ClusterResponse getClusterResponse(Cluster cluster, String clusterJson) {
        ClusterResponse response = conversionService.convert(cluster, ClusterResponse.class);
        response.setCluster(jsonHelper.createJsonFromString(clusterJson));
        return response;
    }
}
