package com.example.SecurityAdvance.security;

import com.example.SecurityAdvance.entities.Role;
import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.enums.UserStatus;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        user.getUserRoles().forEach(ur -> {
            Role role = ur.getRole();

            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role.getName())
            );

            role.getRolePermissions().forEach(rp ->
                    authorities.add(
                            new SimpleGrantedAuthority(rp.getPermission().getCode())
                    )
            );
        });
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
