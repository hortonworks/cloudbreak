package com.sequenceiq.mock.experience;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.mock.experience.response.liftie.DeleteClusterResponse;
import com.sequenceiq.mock.experience.response.liftie.LiftieClusterView;
import com.sequenceiq.mock.experience.response.liftie.ListClustersResponse;

@RestController
@RequestMapping("/liftie/api/v1/")
public class LiftieController {

    public static final int MAGIC_SIZE = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieController.class);

    @Value("${mock.experiences.liftie.createDummyCluster}")
    private boolean createDummyCluster;

    @Inject
    private LiftieExperienceStoreService liftieExperienceStoreService;

    @GetMapping(value = "cluster")
    public ListClustersResponse listCluster(@RequestParam("env") String env, @RequestParam("tenant") String tenant) throws Exception {
        if (createDummyCluster) {
            LOGGER.debug("Creting dummy Liftie cluster if it does not exist in the local store");
            liftieExperienceStoreService.createIfNotExist(env, tenant, "cluster" + UUID.randomUUID(), false);
        }
        return liftieExperienceStoreService.get(env);
    }

    @DeleteMapping(value = "cluster/{id}")
    public DeleteClusterResponse deleteCluster(@PathVariable("id") String id) throws Exception {
        liftieExperienceStoreService.deleteById(id);
        DeleteClusterResponse deleteClusterResponse = new DeleteClusterResponse();
        deleteClusterResponse.setMessage("Delete success");
        deleteClusterResponse.setStatus("DELETED");
        return deleteClusterResponse;
    }

    @PostMapping(value = "mocksupport/{crn}")
    public ListClustersResponse createNew(@PathVariable("crn") String env) throws Exception {
        liftieExperienceStoreService.create(env, ThreadBasedUserCrnProvider.getAccountId(), "cluster" + UUID.randomUUID(), false);
        return liftieExperienceStoreService.get(env);
    }

    @PutMapping(value = "mocksupport/experience/{id}")
    public LiftieClusterView putClusterId(@PathVariable("id") String id, @RequestBody LiftieClusterView clusterView) throws Exception {
        return liftieExperienceStoreService.changeById(id, clusterView);
    }

    @PostMapping(value = "mocksupport/experience/{id}", consumes = MediaType.TEXT_PLAIN)
    public LiftieClusterView putClusterId(@PathVariable("id") String id, @RequestBody String status) throws Exception {
        return liftieExperienceStoreService.setStatusById(id, status);
    }
}
