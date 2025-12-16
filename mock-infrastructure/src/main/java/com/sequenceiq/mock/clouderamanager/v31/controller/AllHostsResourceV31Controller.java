package com.sequenceiq.mock.clouderamanager.v31.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.v31.api.AllHostsResourceApi;

@Controller
public class AllHostsResourceV31Controller implements AllHostsResourceApi {

    @Override
    public ResponseEntity<ApiConfigList> readConfig(String mockUuid, String view) {
        return ResponseEntity.ok(new ApiConfigList());
    }

    @Override
    public ResponseEntity<ApiConfigList> updateConfig(String mockUuid, String message, ApiConfigList body) {
        return ResponseEntity.ok(body);
    }
}
