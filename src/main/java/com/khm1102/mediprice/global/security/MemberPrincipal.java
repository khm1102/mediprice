package com.khm1102.mediprice.global.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record MemberPrincipal(Long memberId, String email, String role, String name) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public boolean isGuest() {
        return "GUEST".equals(role);
    }

    public static MemberPrincipal from(Claims claims) {
        String sub = claims.getSubject();
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        String name = claims.get("name", String.class);

        Long memberId = null;
        if (!"GUEST".equals(role) && sub != null) {
            try {
                memberId = Long.parseLong(sub);
            } catch (NumberFormatException ignored) {
            }
        }

        return new MemberPrincipal(memberId, email != null ? email : sub, role, name);
    }
}
