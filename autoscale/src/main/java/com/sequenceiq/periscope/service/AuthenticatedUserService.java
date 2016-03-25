package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.repository.UserRepository;

@Service
public class AuthenticatedUserService {

    @Autowired
    private UserRepository userRepository;

    public PeriscopeUser getPeriscopeUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            OAuth2Authentication oauth = (OAuth2Authentication) authentication;
            if (oauth.getUserAuthentication() != null) {
                String username = (String) authentication.getPrincipal();
                return userRepository.findOneByName(username);
            }
        }
        return null;
    }
}
