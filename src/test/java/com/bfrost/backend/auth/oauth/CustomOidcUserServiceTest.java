package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock private OidcUser delegate;

    @Test
    void bfrostOidcUserDelegatesAttributesAndIdTokenButUsesInternalUserForIdentityAndRole() {
        User user = User.builder().id(UUID.randomUUID()).username("alice")
                .email("alice@example.com").displayName("Alice").build();
        Map<String, Object> attributes = Map.of("sub", "google-1", "email", "alice@example.com");
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", "google-1"));

        org.mockito.Mockito.when(delegate.getAttributes()).thenReturn(attributes);
        org.mockito.Mockito.when(delegate.getIdToken()).thenReturn(idToken);

        BFrostOidcUser oidcUser = new BFrostOidcUser(user, delegate);

        assertThat(oidcUser.getAttributes()).isEqualTo(attributes);
        assertThat(oidcUser.getIdToken()).isEqualTo(idToken);
        assertThat(oidcUser.getName()).isEqualTo(user.getId().toString());
        assertThat(oidcUser.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_" + user.getRole().name());
        assertThat((Object) oidcUser.user()).isEqualTo(user);
    }
}
