package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.auth.AuthCookieService;
import com.bfrost.backend.auth.AuthResult;
import com.bfrost.backend.auth.AuthService;
import com.bfrost.backend.user.RegistrationStatus;
import com.bfrost.backend.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthCookieService cookieService;

    @Value("${bfrost.oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Value("${bfrost.oauth2.registration-redirect-uri}")
    private String registrationRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof BFrostPrincipal principal)) {
            throw new IllegalStateException(
                    "Unexpected OAuth2 principal type: " + authentication.getPrincipal().getClass());
        }
        User user = principal.user();

        if (user.getRegistrationStatus() == RegistrationStatus.PENDING) {
            String token = authService.issuePendingRegistrationToken(user);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            response.sendRedirect(registrationRedirectUri + "?token=" + encodedToken);
            return;
        }

        AuthResult result = authService.issueTokensFor(user);
        cookieService.setAccessTokenCookie(response, result.accessToken());
        cookieService.setRefreshCookie(response, result.refreshToken());
        response.sendRedirect(frontendRedirectUri);
    }
}
