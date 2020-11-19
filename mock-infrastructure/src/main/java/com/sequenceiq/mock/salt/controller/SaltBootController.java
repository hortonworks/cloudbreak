package com.sequenceiq.mock.salt.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.mock.HostNameUtil;
import com.sequenceiq.mock.salt.SaltStoreService;

@RestController
@RequestMapping("/{mock_uuid}/saltboot/")
public class SaltBootController {

    @Inject
    private SaltStoreService saltStoreService;

    @PostMapping(value = "file")
    public GenericResponses file(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @PostMapping(value = "file/distribute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponses fileDistribute(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @PostMapping(value = "salt/server/pillar")
    public GenericResponse saltServerPillar(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/server/pillar/distribute")
    public GenericResponses saltServerPillarDistribute(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @GetMapping(value = "health")
    public GenericResponse health(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/action/distribute")
    public GenericResponses saltActionDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody SaltAction saltAction) {
        saltStoreService.setMinions(mockUuid, saltAction.getMinions());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(new ArrayList<>());
        return genericResponses;
    }

    @PostMapping(value = "hostname/distribute")
    public GenericResponses saltHostnameDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody Map<String, List<String>> privateIps) {
        GenericResponses genericResponses = new GenericResponses();
        List<GenericResponse> responses = new ArrayList<>();

        List<String> strings = privateIps.get("clients");

        for (String address : strings) {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setAddress(address);
            genericResponse.setStatus(HostNameUtil.generateHostNameByIp(address));
            genericResponse.setStatusCode(HttpStatus.OK.value());
            responses.add(genericResponse);
        }
        genericResponses.setResponses(responses);
        return genericResponses;
    }

    @PostMapping(value = "salt/minion/fingerprint/distribute")
    public GenericResponses saltMinionDistributeDistribute(@PathVariable("mock_uuid") String mockUuid) {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }
}
