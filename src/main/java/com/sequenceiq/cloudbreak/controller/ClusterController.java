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
        Cluster cluster = clusterConverter.convert(clusterRequest);
        clusterService.create(user, stackId, cluster);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveCluster(@ModelAttribute CbUser user, @PathVariable Long stackId) {
        Stack stack = stackService.get(stackId);
        Cluster cluster = clusterService.retrieveCluster(stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        ClusterResponse response = clusterConverter.convert(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateCluster(@ModelAttribute CbUser user, @PathVariable Long stackId, @RequestBody UpdateClusterJson updateClusterJson) {
        Stack stack = stackService.get(stackId);
        Status stackStatus = stack.getStatus();
        if (updateClusterJson.getStatus() != null) {
            clusterService.updateStatus(user, stackId, updateClusterJson.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!stackStatus.equals(Status.AVAILABLE)) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stackStatus));
        }
        clusterService.updateHosts(stackId, updateClusterJson.getHostGroupAdjustments());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
