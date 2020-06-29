package com.sequenceiq.cloudbreak.auth;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class CrnUser extends CloudbreakUser implements UserDetails {

    private String role;

    public CrnUser(String userId, String userCrn, String username, String email, String tenant, String role) {
        super(userId, userCrn, username, email, tenant);
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(role));
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrnUser)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CrnUser crnUser = (CrnUser) o;
        return Objects.equals(role, crnUser.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), role);
    }
}
