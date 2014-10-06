package com.sequenceiq.cloudbreak.controller;

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

import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.UpdateClusterJson;
import com.sequenceiq.cloudbreak.converter.ClusterConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
@RequestMapping("/stacks/{stackId}/cluster")
public class ClusterController {

    @Autowired
    private ClusterConverter clusterConverter;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private StackService stackService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> createCluster(@CurrentUser User user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest clusterRequest) {
        Cluster cluster = clusterConverter.convert(clusterRequest);
        clusterService.createCluster(user, stackId, cluster);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveClusters(@CurrentUser User user, @PathVariable Long stackId) {
        Stack stack = stackService.get(user, stackId);
        Cluster cluster = clusterService.retrieveCluster(user, stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        ClusterResponse response = clusterConverter.convert(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateCluster(@CurrentUser User user, @PathVariable Long stackId, @RequestBody UpdateClusterJson updateClusterJson) {
        Stack stack = stackService.get(user, stackId);
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
