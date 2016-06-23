package com.sequenceiq.cloudbreak.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView

@Controller
class ApiDocController {

    @RequestMapping(value = "/apidocs", method = RequestMethod.GET)
    fun apiDocs(): ModelAndView {
        return ModelAndView("redirect:/api/index.html")
    }
}
