package com.sequenceiq.datalake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ApiDocController {

    @RequestMapping(value = "/apidocs", method = RequestMethod.GET)
    public ModelAndView apiDocs() {
        return new ModelAndView("redirect:swagger-ui.html");
    }
}
