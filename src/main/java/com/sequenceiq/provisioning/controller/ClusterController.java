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
    public ResponseEntity<String> createCluser(@CurrentUser User user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest clusterRequest) {
        ambariClusterService.createCluster(user, stackId, clusterRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveClusters(@CurrentUser User user, @PathVariable Long stackId) {
        return new ResponseEntity<>(ambariClusterService.retrieveCluster(user, stackId), HttpStatus.OK);
    }

}
