package com.sequenceiq.mock.legacy.salt.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.service.HostNameUtil;

@RestController
@RequestMapping("/saltboot/")
public class SaltBootLegacyController {

    @Inject
    private DefaultModelService defaultModelService;

    @PostMapping(value = "file")
    public GenericResponses file() {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @PostMapping(value = "file/distribute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponses fileDistribute() {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @PostMapping(value = "salt/server/pillar")
    public GenericResponse saltServerPillar() {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/server/pillar/distribute")
    public GenericResponses saltServerPillarDistribute() {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }

    @GetMapping(value = "health")
    public GenericResponse health() {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.OK.value());
        return genericResponse;
    }

    @PostMapping(value = "salt/action/distribute")
    public GenericResponses saltActionDistribute(@RequestBody SaltAction saltAction) {
        defaultModelService.setMinions(saltAction.getMinions());
        GenericResponses genericResponses = new GenericResponses();
        genericResponses.setResponses(new ArrayList<>());
        return genericResponses;
    }

    @PostMapping(value = "hostname/distribute")
    public GenericResponses saltHostnameDistribute(@RequestBody String body) {
        GenericResponses genericResponses = new GenericResponses();
        List<GenericResponse> responses = new ArrayList<>();

        JsonObject parsedRequest = new JsonParser().parse(body).getAsJsonObject();
        JsonArray nodeArray = parsedRequest.getAsJsonArray("clients");

        for (int i = 0; i < nodeArray.size(); i++) {
            String address = nodeArray.get(i).getAsString();
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
    public GenericResponses saltMinionDistributeDistribute() {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setStatusCode(HttpStatus.CREATED.value());
        genericResponses.setResponses(Collections.singletonList(genericResponse));
        return genericResponses;
    }
}
