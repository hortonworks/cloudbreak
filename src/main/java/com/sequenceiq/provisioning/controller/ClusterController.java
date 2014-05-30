package com.sequenceiq.provisioning.controller;

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

import com.sequenceiq.provisioning.controller.json.ClusterRequest;
import com.sequenceiq.provisioning.controller.json.ClusterResponse;
import com.sequenceiq.provisioning.controller.json.StatusRequestJson;
import com.sequenceiq.provisioning.domain.StatusRequest;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.AmbariClusterService;

@Controller
@RequestMapping("/stackId/{stackId}/cluster")
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
    public ResponseEntity<String> startOrStopAllServiceOnCLuster(@CurrentUser User user, @PathVariable Long stackId,
            @RequestBody StatusRequestJson statusRequestJson) {
        if (StatusRequest.START.equals(statusRequestJson.getStatusRequest())) {
            return new ResponseEntity<>(ambariClusterService.startAllService(user, stackId), HttpStatus.OK);
        } else if (StatusRequest.STOP.equals(statusRequestJson.getStatusRequest())) {
            return new ResponseEntity<>(ambariClusterService.stopAllService(user, stackId), HttpStatus.OK);
        }
        throw new BadRequestException("The requested status not valid.");
    }

}
