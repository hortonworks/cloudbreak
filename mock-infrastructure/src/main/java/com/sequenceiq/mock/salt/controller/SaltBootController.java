package com.sequenceiq.mock.salt.controller;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintRequest;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.mock.salt.SaltStoreService;
import com.sequenceiq.mock.service.HostNameService;

@RestController
@RequestMapping("/{mock_uuid}/saltboot/")
public class SaltBootController {

    private static final Logger LOGGER = getLogger(SaltBootController.class);

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private HostNameService hostNameService;

    @PostMapping(value = "file/distribute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponses fileDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestParam("file") MultipartFile body) {
        saltStoreService.saltbootFileDistribute(mockUuid, body);
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @PostMapping(value = "salt/server/pillar")
    public GenericResponse saltServerPillar(@PathVariable("mock_uuid") String mockUuid, @RequestBody Pillar pillar) {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/server/pillar/distribute")
    public GenericResponses saltServerPillarDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody Pillar pillar) {
        saltStoreService.addPillar(mockUuid, pillar);
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @GetMapping(value = "health")
    public GenericResponse health(@PathVariable("mock_uuid") String mockUuid) {
        saltStoreService.read(mockUuid);
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/action/distribute")
    public GenericResponses saltActionDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody SaltAction saltAction) {
        saltStoreService.setSaltAction(mockUuid, saltAction);
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(new ArrayList<>());
        return genericResponses;
    }

    @PostMapping(value = "hostname/distribute")
    public GenericResponses saltHostnameDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody Map<String, List<String>> privateIps) {
        GenericResponses genericResponses = new GenericResponses();
        List<GenericResponse> responses = new ArrayList<>();

        List<String> strings = privateIps.get("clients");
        List<Minion> minions = saltStoreService.read(mockUuid).getMinions();
        for (String address : strings) {
            Optional<Minion> minion = minions.stream().filter(m -> address.equals(m.getAddress())).findFirst();
            if (minion.isPresent()) {
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(address);
                genericResponse.setStatus(hostNameService.generateHostNameByIp(address, minion.get().getDomain()));
                genericResponse.setStatusCode(HttpStatus.OK.value());
                responses.add(genericResponse);
            } else {
                LOGGER.info("Cannot find minion with ip: {}", address);
            }
        }
        genericResponses.setResponses(responses);
        return genericResponses;
    }

    @PostMapping(value = "salt/minion/fingerprint/distribute")
    public GenericResponses saltMinionDistributeDistribute(@PathVariable("mock_uuid") String mockUuid, @RequestBody FingerprintRequest request) {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }
}
