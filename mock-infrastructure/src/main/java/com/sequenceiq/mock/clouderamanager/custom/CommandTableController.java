package com.sequenceiq.mock.clouderamanager.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;

@Controller
@RequestMapping(value = "/{mockUuid}/cmf/commands")
public class CommandTableController {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @RequestMapping(value = "/activeCommandTable",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getActiveCommandTable(@QueryParam("limit") Integer limit,
            @QueryParam("startTime") Long startTime, @QueryParam("endTime") Long endTime) {
        return responseCreatorComponent.exec(new ArrayList<>());
    }

    @RequestMapping(value = "/commandTable",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getCommandTable(@QueryParam("limit") Integer limit,
            @QueryParam("startTime") Long startTime, @QueryParam("endTime") Long endTime) {
        return responseCreatorComponent.exec(new ArrayList<>());
    }
}