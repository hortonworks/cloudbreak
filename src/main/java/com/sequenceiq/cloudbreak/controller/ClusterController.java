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
import com.sequenceiq.cloudbreak.controller.json.StatusRequestJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;

@Controller
@RequestMapping("/stacks/{stackId}/cluster")
public class ClusterController {

    @Autowired
    private AmbariClusterService ambariClusterService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> createCluster(@CurrentUser User user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest clusterRequest) {
        ambariClusterService.createCluster(user, stackId, clusterRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveClusters(@CurrentUser User user, @PathVariable Long stackId) {
        return new ResponseEntity<>(ambariClusterService.retrieveCluster(user, stackId), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> startOrStopAllServiceOnCluster(@CurrentUser User user, @PathVariable Long stackId,
            @RequestBody StatusRequestJson statusRequestJson) {
        switch (statusRequestJson.getStatusRequest()) {
        case STOP:
            ambariClusterService.stopAllService(user, stackId);
            return new ResponseEntity<>(HttpStatus.OK);
        case START:
            ambariClusterService.startAllService(user, stackId);
            return new ResponseEntity<>(HttpStatus.OK);
        default:
            throw new BadRequestException("The requested status not valid.");
        }
    }

}
