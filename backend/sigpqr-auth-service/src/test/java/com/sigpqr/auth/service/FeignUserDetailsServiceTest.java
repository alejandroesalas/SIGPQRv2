package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.dto.UserCredentialsDto;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignUserDetailsServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private FeignUserDetailsService service;

    private static final String EMAIL = "user@example.com";

    private UserCredentialsDto user(String profileId) {
        return new UserCredentialsDto(UUID.randomUUID(), EMAIL, "hashedPw", profileId, true, true);
    }

    @Test
    @DisplayName("loadUser - STUDENT profile - returns 6 base authorities")
    void loadUser_studentProfile_returnsBaseAuthorities() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(user("STUDENT"));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertThat(authorities).hasSize(6);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "SCOPE_openid", "SCOPE_profile",
                        "SCOPE_profile:read", "SCOPE_profile:write",
                        "SCOPE_pqr:read", "SCOPE_pqr:write"
                );
    }

    @Test
    @DisplayName("loadUser - COORDINATOR profile - includes respond and admin:read")
    void loadUser_coordinatorProfile_includesRespondAndAdminRead() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(user("COORDINATOR"));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getAuthorities()).hasSize(8);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_pqr:respond", "SCOPE_admin:read");
    }

    @Test
    @DisplayName("loadUser - ADMIN profile - includes all 10 authorities")
    void loadUser_adminProfile_includesAllAuthorities() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(user("ADMIN"));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getAuthorities()).hasSize(10);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_admin:write", "SCOPE_user:manage");
    }

    @Test
    @DisplayName("loadUser - user not found - throws UsernameNotFoundException")
    void loadUser_userNotFound_throwsUsernameNotFound() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUser - feign exception - throws UsernameNotFoundException")
    void loadUser_feignException_throwsUsernameNotFound() {
        when(userServiceClient.findByEmail(EMAIL)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
