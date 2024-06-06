package com.sequenceiq.thunderhead.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.thunderhead.entity.Cdl;
import com.sequenceiq.thunderhead.repository.CdlRespository;

@RestController
public class MockCdlController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCdlController.class);

    @Inject
    private CdlRespository cdlRespository;

    @PostMapping("api/v1/cdl/addDatabaseConfig")
    public ResponseEntity<String> addDatabaseConfig(@RequestBody String requestEntity) {
        LOGGER.info("Adding DB config to mock CDL: '{}'", requestEntity);
        try {
            Json requestJson = new Json(requestEntity);
            Map<String, Object> requestJsonMap = requestJson.getMap();
            String cdlCrn = requestJsonMap.get("crn").toString();
            String databaseServerCrn = requestJsonMap.get("databaseServerCrn").toString();
            String hmsDatabaseHost = requestJsonMap.get("hmsDatabaseHost").toString();
            String hmsDatabaseUser = requestJsonMap.get("hmsDatabaseUser").toString();
            String hmsDatabasePassword = requestJsonMap.get("hmsDatabasePassword").toString();
            String hmsDatabaseName = requestJsonMap.get("hmsDatabaseName").toString();

            Optional<Cdl> cdl = cdlRespository.findByCrn(cdlCrn);
            cdl.ifPresent(cdlEntity -> {
                cdlEntity.setDatabaseServerCrn(databaseServerCrn);
                cdlEntity.setHmsDatabaseHost(hmsDatabaseHost);
                cdlEntity.setHmsDatabaseName(hmsDatabaseName);
                cdlEntity.setHmsDatabaseUser(hmsDatabaseUser);
                cdlEntity.setHmsDatabasePassword(hmsDatabasePassword);
                cdlRespository.save(cdlEntity);
            });

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("crn", cdlCrn);
            return new ResponseEntity<>(new Json(responseMap).getValue(), HttpStatus.OK);
        } catch (Exception ex) {
            String msg = "UH-OH something went wrong!";
            LOGGER.warn(msg, ex);
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
