package com.sequenceiq.provisioning.controller;

import java.util.List;

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

import com.sequenceiq.provisioning.controller.json.ClusterJson;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.AmbariClusterService;

@Controller
@RequestMapping("/cloud/{cloudId}/cluster")
public class ClusterController {

    @Autowired
    private AmbariClusterService ambariClusterService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> createCluser(@CurrentUser User user, @PathVariable Long cloudId, @RequestBody @Valid ClusterJson clusterRequest) {
        ambariClusterService.createCluster(user, cloudId, clusterRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<ClusterJson>> retrieveClusters(@CurrentUser User user, @PathVariable Long cloudId) {
        return new ResponseEntity<>(ambariClusterService.retrieveClusters(user, cloudId), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{id}")
    @ResponseBody
    public ResponseEntity<ClusterJson> retrieveCluster(@CurrentUser User user, @PathVariable Long cloudId, @PathVariable String id) {
        return new ResponseEntity<>(ambariClusterService.retrieveCluster(user, cloudId, id), HttpStatus.OK);
    }

}
