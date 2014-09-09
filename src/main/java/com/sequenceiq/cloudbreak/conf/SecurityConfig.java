package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.domain.CbUser;

@Configuration
public class SecurityConfig {

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Value("${cb.client.id}")
        private String clientId;

        @Value("${cb.client.secret}")
        private String clientSecret;

        @Value("${cb.identity.server.url}")
        private String identityServerUrl;

        @Bean
        RemoteTokenServices remoteTokenServices() {
            RemoteTokenServices rts = new RemoteTokenServices();
            rts.setClientId(clientId);
            rts.setClientSecret(clientSecret);
            rts.setCheckTokenEndpointUrl(identityServerUrl + "/check_token");
            return rts;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId(clientId);
            resources.tokenServices(remoteTokenServices());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.csrf()
                    .disable()
                    .headers()
                    .contentTypeOptions()
                    .and()
                    .addFilterAfter(new ScimAccountGroupReaderFilter(clientId, clientSecret, identityServerUrl), AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers("/user/blueprints").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/account/blueprints").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/blueprints/**").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/user/templates").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/account/templates").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/templates/**").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/notification/**").permitAll()
                    .antMatchers("/sns/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/users/**").permitAll()
                    .antMatchers(HttpMethod.GET, "/users/confirm/**").permitAll()
                    .antMatchers(HttpMethod.POST, "/password/reset/**").permitAll()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .antMatchers(HttpMethod.GET, "/stacks/metadata/**").permitAll();
        }
    }

    private static class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

        private static final int ACCOUNT_PART = 2;
        private static final int ROLE_PART = 3;

        private String clientId;
        private String clientSecret;
        private String identityServerUrl;

        public ScimAccountGroupReaderFilter(String clientId, String clientSecret, String identityServerUrl) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.identityServerUrl = identityServerUrl;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException,
                IOException {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                OAuth2AuthenticationDetails authDetails = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();

                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders userInfoHeaders = new HttpHeaders();
                userInfoHeaders.set("Authorization", "Bearer " + authDetails.getTokenValue());

                Map<String, String> userInfoResponse = restTemplate.exchange(
                        identityServerUrl + "/userinfo",
                        HttpMethod.GET,
                        new HttpEntity<>(userInfoHeaders),
                        Map.class).getBody();

                HttpHeaders tokenRequestHeaders = new HttpHeaders();
                tokenRequestHeaders.set("Authorization", getAuthorizationHeader(clientId, clientSecret));

                Map<String, String> tokenResponse = restTemplate.exchange(
                        identityServerUrl + "/oauth/token?grant_type=client_credentials",
                        HttpMethod.POST,
                        new HttpEntity<>(tokenRequestHeaders),
                        Map.class).getBody();

                HttpHeaders scimRequestHeaders = new HttpHeaders();
                scimRequestHeaders.set("Authorization", "Bearer " + tokenResponse.get("access_token"));

                String scimResponse = restTemplate.exchange(
                        identityServerUrl + "/Users/" + userInfoResponse.get("user_id"),
                        HttpMethod.GET,
                        new HttpEntity<>(scimRequestHeaders),
                        String.class).getBody();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(scimResponse);
                List<String> roles = new ArrayList<>();
                String account = null;
                for (Iterator<JsonNode> iterator = root.get("groups").getElements(); iterator.hasNext();) {
                    JsonNode node = iterator.next();
                    String group = node.get("display").asText();
                    if (group.startsWith("cloudbreak.account")) {
                        String[] parts = group.split("\\.");
                        if (account != null && account != parts[2]) {
                            throw new IllegalStateException("A user can belong to only one account.");
                        }
                        account = parts[ACCOUNT_PART];
                        roles.add(parts[ROLE_PART]);
                    }
                }

                CbUser user = new CbUser(username, account, roles);
                request.setAttribute("user", user);
            }
            filterChain.doFilter(request, response);
        }

        private String getAuthorizationHeader(String clientId, String clientSecret) {
            String creds = String.format("%s:%s", clientId, clientSecret);
            try {
                return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Could not convert String");
            }
        }
    }

}