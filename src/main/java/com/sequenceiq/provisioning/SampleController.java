package com.sequenceiq.provisioning;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

    @RequestMapping("/")
    @ResponseBody
    public ResponseEntity<SampleJson> home() {
        return new ResponseEntity<>(new SampleJson("name", "OK"), HttpStatus.OK);
    }
}