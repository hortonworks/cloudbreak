package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/events", description = ControllerDescription.EVENT_DESCRIPTION, position = 7)
public class CloudbreakEventController {

    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @ApiOperation(value = EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES)
    @RequestMapping(method = RequestMethod.GET, value = "/events")
    @ResponseBody
    public ResponseEntity<List<CloudbreakEventsJson>> events(@ModelAttribute("user") CbUser user, @RequestParam(value = "since", required = false) Long since) {
        MDCBuilder.buildMdcContext(user);
        List<CloudbreakEventsJson> cloudbreakEvents = cloudbreakEventsFacade.retrieveEvents(user.getUserId(), since);
        return new ResponseEntity<>(cloudbreakEvents, HttpStatus.OK);
    }
}
