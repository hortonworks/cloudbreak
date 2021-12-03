package com.sequenceiq.mock.freeipa;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{mock_uuid}/ipa")
public class FreeipaController {

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @PostMapping("/session/login_password")
    public Object login(@PathVariable("mock_uuid") String mockUuid, HttpServletResponse httpServletResponse) {
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
    public Object postSessionJson(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping("/session/json")
    public Object getSessionJson(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/user_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object userFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/user_mod", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object userMod(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/role_add_member", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object roleAddMember(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/cert_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object certFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/host_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object hostFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/service_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object serviceFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/dnszone_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/dnszone_add", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneAdd(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/dnszone_mod", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneMod(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/dnszone_del", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsZoneDel(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/dnsrecord_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object dnsRecordFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/host_del", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object hostDel(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/role_find", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object roleFind(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }

    @GetMapping(value = "/server_conncheck", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object serverConncheck(@PathVariable("mock_uuid") String mockUuid, @RequestBody String body) throws Exception {
        return freeIpaRouteHandler.handle(mockUuid, body);
    }
}
