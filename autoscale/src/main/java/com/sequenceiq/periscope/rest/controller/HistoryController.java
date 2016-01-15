package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.rest.converter.HistoryConverter;
import com.sequenceiq.periscope.rest.json.HistoryJson;
import com.sequenceiq.periscope.service.HistoryService;

@RestController
@RequestMapping("/clusters/{clusterId}/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;
    @Autowired
    private HistoryConverter historyConverter;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<HistoryJson>> getHistory(@PathVariable long clusterId) {
        List<History> history = historyService.getHistory(clusterId);
        return new ResponseEntity<>(historyConverter.convertAllToJson(history), HttpStatus.OK);
    }

    @RequestMapping(value = "/{historyId}", method = RequestMethod.GET)
    public ResponseEntity<HistoryJson> getHistory(@PathVariable long clusterId, @PathVariable long historyId) {
        History history = historyService.getHistory(clusterId, historyId);
        return new ResponseEntity<>(historyConverter.convert(history), HttpStatus.OK);
    }
}
