package com.sequenceiq.thunderhead.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MockThunderheadIAMController {

    @PostMapping("/iam/getUser")
    public Map<String, String> getUser(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new HashMap<>();
    }

    @PostMapping("/iam/listGroups")
    public List<String> listGroups(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new ArrayList<>();
    }

    @PostMapping("/iam/listUsers")
    public List<String> listUsers(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new ArrayList<>();
    }

    @PostMapping("/iam/listMachineUsers")
    public List<String> listMachineUsers(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new ArrayList<>();
    }
}
