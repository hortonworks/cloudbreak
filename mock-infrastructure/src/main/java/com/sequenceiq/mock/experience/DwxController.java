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
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.experience.response.common.CpInternalCluster;
import com.sequenceiq.mock.experience.response.common.CpInternalEnvironmentResponse;
import com.sequenceiq.mock.experience.response.common.DeleteCommonExperienceWorkspaceResponse;

@RestController
@RequestMapping("/dwx")
public class DwxController {

    public static final int MAGIC_SIZE = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(DwxController.class);

    @Inject
    private ExperienceStoreService experienceStoreService;

    @GetMapping(value = "/{crn}", produces = MediaType.APPLICATION_JSON)
    public CpInternalEnvironmentResponse listCluster(@PathVariable("crn") String env) throws Exception {
        experienceStoreService.createIfNotExist(env);
        //this should be deleted, when branch rebased to master
        experienceStoreService.addExperienceTo(env);
        return experienceStoreService.get(env);
    }

    @DeleteMapping(value = "/{crn}", produces = MediaType.APPLICATION_JSON)
    public DeleteCommonExperienceWorkspaceResponse deleteCluster(@PathVariable("crn") String env) throws Exception {
        experienceStoreService.deleteById(env);
        DeleteCommonExperienceWorkspaceResponse deleteCommonExperienceWorkspaceResponse = new DeleteCommonExperienceWorkspaceResponse();
        deleteCommonExperienceWorkspaceResponse.setName(env);
        deleteCommonExperienceWorkspaceResponse.setStatusReason("fain");
        deleteCommonExperienceWorkspaceResponse.setStatus("deleted");
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
