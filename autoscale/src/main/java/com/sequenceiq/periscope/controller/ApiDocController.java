package com.sequenceiq.periscope.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Controller
public class ApiDocController {

    @RequestMapping(value = "/apidocs", method = RequestMethod.GET)
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ModelAndView apiDocs() {
        return new ModelAndView("redirect:swagger-ui.html");
    }
}