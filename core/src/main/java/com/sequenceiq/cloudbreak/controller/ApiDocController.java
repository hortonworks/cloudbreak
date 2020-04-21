package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.api.CoreApi.API_ROOT_CONTEXT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ApiDocController {

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @RequestMapping(value = "/apidocs", method = RequestMethod.GET)
    public ModelAndView apiDocs() {
        String redirectPath = String.format("redirect:webjars/swagger-ui/3.25.0/index.html?url=%s%s/swagger.json", cbRootContextPath, API_ROOT_CONTEXT);
        return new ModelAndView(redirectPath);
    }
}
