package com.sequenceiq.mock.experience;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

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
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.experience.response.common.CpInternalCluster;
import com.sequenceiq.mock.experience.response.common.CpInternalEnvironmentResponse;
import com.sequenceiq.mock.experience.response.common.DeleteCommonExperienceWorkspaceResponse;

@RestController
@RequestMapping("/dwx")
public class DwxController {

    public static final int MAGIC_SIZE = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(DwxController.class);

    @Value("${mock.experiences.dwx.createDummyCluster}")
    private boolean createDummyCluster;

    @Inject
    private ExperienceStoreService experienceStoreService;

    @GetMapping(value = "/{crn}", produces = MediaType.APPLICATION_JSON)
    public CpInternalEnvironmentResponse listCluster(@PathVariable("crn") String env) throws Exception {
        experienceStoreService.createIfNotExist(env);
        if (createDummyCluster) {
            LOGGER.debug("Creting dummy DWX cluster if it does not exist in the local store");
            if (experienceStoreService.get(env) == null || experienceStoreService.get(env).getResults().isEmpty()) {
                experienceStoreService.addExperienceTo(env);
            }
        }
        CpInternalEnvironmentResponse response = new CpInternalEnvironmentResponse();
        Set<CpInternalCluster> nonDeletedClusters = experienceStoreService.get(env).getResults()
                .stream()
                .filter(c -> !"DELETED".equals(c.getStatus()))
                .collect(Collectors.toSet());
        response.setResults(nonDeletedClusters);
        return response;
    }

    @DeleteMapping(value = "/{crn}", produces = MediaType.APPLICATION_JSON)
    public DeleteCommonExperienceWorkspaceResponse deleteCluster(@PathVariable("crn") String env) throws Exception {
        experienceStoreService.deleteById(env);
        DeleteCommonExperienceWorkspaceResponse deleteCommonExperienceWorkspaceResponse = new DeleteCommonExperienceWorkspaceResponse();
        deleteCommonExperienceWorkspaceResponse.setName(env);
        deleteCommonExperienceWorkspaceResponse.setStatusReason("Delete success");
        deleteCommonExperienceWorkspaceResponse.setStatus("DELETED");
        return deleteCommonExperienceWorkspaceResponse;
    }

    //create new
    @PostMapping(value = "/mocksupport/{crn}", produces = MediaType.APPLICATION_JSON)
    public CpInternalEnvironmentResponse addExperience(@PathVariable("crn") String env) throws Exception {
        experienceStoreService.addExperienceTo(env);
        return experienceStoreService.get(env);
    }

    @PutMapping(value = "/mocksupport/experience/{id}", produces = MediaType.APPLICATION_JSON)
    public CpInternalCluster changeExperience(@PathVariable("id") String id, @RequestBody CpInternalCluster cluster) throws Exception {
        return experienceStoreService.put(id, cluster);
    }

    @PostMapping(value = "/mocksupport/experience/{id}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.TEXT_PLAIN)
    public CpInternalCluster addExperience(@PathVariable("id") String id, @RequestBody String status) throws Exception {
        return experienceStoreService.changeStatusById(id, status);
    }
}
