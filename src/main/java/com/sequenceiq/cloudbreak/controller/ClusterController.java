package com.sequenceiq.cloudbreak.controller;

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

import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.UpdateClusterJson;
import com.sequenceiq.cloudbreak.converter.ClusterConverter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
public class ClusterController {

    @Autowired
    private ClusterConverter clusterConverter;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private StackService stackService;

    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> create(@ModelAttribute("user") CbUser user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest clusterRequest) {
        Cluster cluster = clusterConverter.convert(clusterRequest, stackId);
        clusterService.create(user, stackId, cluster);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveCluster(@PathVariable Long stackId) {
        Stack stack = stackService.get(stackId);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        ClusterResponse response = clusterConverter.convert(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "user/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePrivateCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Stack stack = stackService.getPrivateStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = clusterConverter.convert(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "account/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePublicCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Stack stack = stackService.getPublicStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = clusterConverter.convert(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateCluster(@PathVariable Long stackId, @RequestBody UpdateClusterJson updateJson) {
        Stack stack = stackService.get(stackId);
        Status stackStatus = stack.getStatus();

        if (updateJson.getStatus() != null) {
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isStateFailed()) {
            clusterService.recreate(stackId, updateJson.getBlueprintId(), updateJson.getHostgroups());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (updateJson.getHostGroupAdjustment() != null) {
            if (!stackStatus.equals(Status.AVAILABLE)) {
                throw new BadRequestException(String.format(
                        "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                        stackStatus));
            }
            clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
