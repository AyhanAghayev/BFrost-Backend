package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.user.RegistrationStatus;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        User user = resolveUser(oauth2User.getAttributes());
        return new BFrostOAuth2User(user, oauth2User.getAttributes());
    }

    User resolveUser(Map<String, Object> attributes) {
        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        return userRepository.findByGoogleId(googleId)
                .or(() -> Boolean.TRUE.equals(emailVerified)
                        ? userRepository.findByEmail(email).map(u -> linkGoogleId(u, googleId))
                        : java.util.Optional.empty())
                .orElseGet(() -> createUser(googleId, email, name, picture));
    }

    private User linkGoogleId(User user, String googleId) {
        user.setGoogleId(googleId);
        return userRepository.save(user);
    }

    private User createUser(String googleId, String email, String name, String picture) {
        User user = User.builder()
                .username(generateUniqueUsername(email))
                .email(email)
                .googleId(googleId)
                .emailVerified(true)
                .registrationStatus(RegistrationStatus.PENDING)
                .displayName(name != null ? name : email)
                .profilePictureUrl(picture)
                .build();
        return userRepository.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (base.isEmpty()) base = "user";
        if (base.length() > 40) base = base.substring(0, 40);

        String candidate = base;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
