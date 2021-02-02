package com.sequenceiq.mock.experience;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.experience.response.liftie.ClusterView;
import com.sequenceiq.mock.experience.response.liftie.DeleteClusterResponse;
import com.sequenceiq.mock.experience.response.liftie.ListClustersResponse;

@RestController
@RequestMapping("/liftie/api/v1/")
public class LiftieController {

    public static final int MAGIC_SIZE = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieController.class);

    @Inject
    private LiftieExperienceStoreService liftieExperienceStoreService;

    @GetMapping(value = "cluster", produces = MediaType.APPLICATION_JSON)
    public ListClustersResponse listCluster(@RequestParam("env") String env, @RequestParam("tenant") String tenant) throws Exception {
        //this should be deleted, when branch rebased to master
        liftieExperienceStoreService.createIfNotExist(env, tenant);
        return liftieExperienceStoreService.get(env);
    }

    @DeleteMapping(value = "cluster/{id}", produces = MediaType.APPLICATION_JSON)
    public DeleteClusterResponse deleteCluster(@PathVariable("id") String id) throws Exception {
        liftieExperienceStoreService.deleteById(id);
        DeleteClusterResponse deleteClusterResponse = new DeleteClusterResponse();
        deleteClusterResponse.setMessage("megyez");
        deleteClusterResponse.setStatus("deleted");
        return deleteClusterResponse;
    }

    @PostMapping(value = "mocksupport/{crn}", produces = MediaType.APPLICATION_JSON)
    public ListClustersResponse createNew(@PathVariable("crn") String env) throws Exception {
        liftieExperienceStoreService.create(env);
        return liftieExperienceStoreService.get(env);
    }

    @PutMapping(value = "mocksupport/experience/{id}", produces = MediaType.APPLICATION_JSON)
    public ClusterView putClusterId(@PathVariable("id") String id, @RequestBody ClusterView clusterView) throws Exception {
        return liftieExperienceStoreService.changeById(id, clusterView);
    }


    @PostMapping(value = "mocksupport/experience/{id}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.TEXT_PLAIN)
    public ClusterView putClusterId(@PathVariable("id") String id, @RequestBody String status) throws Exception {
        return liftieExperienceStoreService.setStatusById(id, status);
    }
}
