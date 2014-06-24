package com.sequenceiq.cloudbreak.conf;

import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandshakeInterceptor.class);

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        HttpHeaders headers = request.getHeaders();

        StringWriter writer = new StringWriter();
        IOUtils.copy(request.getBody(), writer, "UTF-8");
        String theString = writer.toString();
        /*String userName = headers.get("USERNAME").get(0);
        String password = headers.get("PASSWORD").get(0);
        try {
            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
        } catch (Exception e) {
            LOGGER.warn("Exception occurred during the handshake", e);
            return false;
        }*/
        System.out.println("call:" + theString);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        System.out.print("");
    }

}
