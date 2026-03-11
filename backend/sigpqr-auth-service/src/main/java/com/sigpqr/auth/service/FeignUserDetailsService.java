package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.dto.UserCredentialsDto;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeignUserDetailsService implements UserDetailsService {

    private final UserServiceClient userServiceClient;

    public FeignUserDetailsService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserCredentialsDto credentials;
        try {
            credentials = userServiceClient.findByEmail(email);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found: " + email, e);
        }

        if (credentials == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        List<SimpleGrantedAuthority> authorities = buildAuthorities(credentials.profileId());

        return User.builder()
                .username(credentials.email())
                .password(credentials.passwordHash())
                .disabled(!credentials.enabled())
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .authorities(authorities)
                .build();
    }

    private List<SimpleGrantedAuthority> buildAuthorities(String profileId) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile:read"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile:write"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_pqr:read"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_pqr:write"));

        if ("ADMIN".equalsIgnoreCase(profileId) || "COORDINATOR".equalsIgnoreCase(profileId)) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_pqr:respond"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_admin:read"));
        }

        if ("ADMIN".equalsIgnoreCase(profileId)) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_admin:write"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_user:manage"));
        }

        return authorities;
    }
}
