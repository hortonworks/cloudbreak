package com.sequenceiq.caas.controller;

import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.caas.model.AltusKey;
import com.sequenceiq.caas.model.CaasUser;
import com.sequenceiq.caas.model.CaasUserList;
import com.sequenceiq.caas.model.IntrospectRequest;
import com.sequenceiq.caas.model.IntrospectResponse;
import com.sequenceiq.caas.model.TokenRequest;
import com.sequenceiq.caas.model.TokenResponse;
import com.sequenceiq.caas.service.MockCaasService;
import com.sequenceiq.caas.util.CrnHelper;

@RestController
public class MockCaasController {

    @Inject
    private MockCaasService mockCaasService;

    @GetMapping("/caas/api/users")
    public CaasUserList getUsers(HttpServletRequest httpServletRequest) {
        return mockCaasService.getUsers(httpServletRequest);
    }

    @PostMapping("/oidc/token")
    public TokenResponse postToken(@RequestBody TokenRequest tokenRequest) {
        return mockCaasService.getTokenResponse(tokenRequest.getAuthorizationCode(), tokenRequest.getRefreshToken());
    }

    @GetMapping("/oidc/authorize")
    public String authorize(HttpServletResponse httpServletResponse,
            @RequestParam("redirect_uri") Optional<String> redirectUri,
            @RequestParam("username") Optional<String> userName,
            @RequestParam("tenant") Optional<String> tenant,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active) {
        return mockCaasService.authorize(httpServletResponse, tenant, userName, redirectUri, active);
    }

    @GetMapping("/oidc/userinfo")
    public CaasUser getUserInfo(HttpServletRequest request) {
        return mockCaasService.getUserInfo(request);
    }

    @PostMapping("/oidc/introspect")
    public IntrospectResponse introSpect(@RequestBody IntrospectRequest introspectRequest) {
        return mockCaasService.introSpect(introspectRequest);
    }

    @GetMapping("/auth/in")
    public void auth(HttpServletRequest httpServletRequest,
                HttpServletResponse httpServletResponse,
                @RequestParam("redirect_uri") String redirectUri,
                @RequestParam("tenant") Optional<String> tenant,
                @RequestParam("username") Optional<String> userName,
                @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active) {
        mockCaasService.auth(httpServletRequest, httpServletResponse, tenant, userName, redirectUri, active);
    }

    @GetMapping("/auth/out")
    public void out(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        mockCaasService.out(httpServletRequest, httpServletResponse);
    }

    @GetMapping("/auth/mockkey/{tenant}/{user}")
    @ResponseBody
    public AltusKey getKeys(@PathVariable("tenant") String tenant, @PathVariable("user") String user) {
        return CrnHelper.generateAltusApiKey(tenant, user);
    }
}
