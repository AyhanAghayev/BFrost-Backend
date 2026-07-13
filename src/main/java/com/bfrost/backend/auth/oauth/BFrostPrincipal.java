package com.bfrost.backend.auth.oauth;

import com.bfrost.backend.user.User;

public interface BFrostPrincipal {
    User user();
}
