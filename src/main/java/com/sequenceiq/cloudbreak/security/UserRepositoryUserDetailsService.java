package com.sequenceiq.cloudbreak.security;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Component
public class UserRepositoryUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Could not find user: " + email);
        }
        user.setLastLogin(new Date());
        User updatedUser = userRepository.save(user);
        return new UserRepositoryUserDetails(user);
    }

    private static final class UserRepositoryUserDetails extends User implements UserDetails {
        private Set<GrantedAuthority> authorities = new HashSet<>();

        private UserRepositoryUserDetails(User user) {
            super(user);
            for (UserRole role : getUserRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.role()));
            }
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getUsername() {
            return getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return !UserStatus.EXPIRED.equals(getStatus());
        }

        @Override
        public boolean isAccountNonLocked() {
            return !UserStatus.DISABLED.equals(getStatus());
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return UserStatus.ACTIVE.equals(getStatus());
        }

    }

}
