package com.sequenceiq.mock.legacy.freeipa;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;

@RestController
@RequestMapping("/ipa")
public class FreeipaLegacyController {

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @PostMapping("/session/login_password")
    public Object login(HttpServletResponse httpServletResponse) {
        Cookie cookie = new Cookie("ipa_session", "dummysession");
        cookie.setPath("");
        cookie.setDomain("");
        cookie.setMaxAge(-1);
        cookie.setSecure(false);
        cookie.setHttpOnly(false);
        httpServletResponse.addCookie(cookie);
        return "";
    }

    @PostMapping("/session/json")
    public Object postSessionJson(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping("/session/json")
    public Object getSessionJson(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/user_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object userFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/user_mod", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object userMod(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/role_add_member", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object roleAddMember(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/cert_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object certFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/host_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object hostFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/service_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object serviceFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/dnszone_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/dnszone_add", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneAdd(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/dnszone_mod", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneMod(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/dnszone_del", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneDel(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/dnsrecord_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsRecordFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/host_del", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object hostDel(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/role_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object roleFind(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }

    @GetMapping(value = "/server_conncheck", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object serverConncheck(@RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(body);
    }
}
