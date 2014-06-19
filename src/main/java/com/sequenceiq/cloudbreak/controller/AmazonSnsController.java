package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.service.aws.SnsMessageHandler;
import com.sequenceiq.cloudbreak.service.aws.SnsMessageParser;

@Controller
@RequestMapping("sns")
public class AmazonSnsController {

    @Autowired
    private SnsMessageParser snsMessageParser;

    @Autowired
    private SnsMessageHandler snsMessageHandler;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> receiveSnsMessage(@RequestBody String request) {
        try {
            SnsRequest snsRequest = snsMessageParser.parseRequest(request);
            snsMessageHandler.handleMessage(snsRequest);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            throw new InternalServerException("Failed to parse Amazon SNS message.", e);
        }
    }
}
