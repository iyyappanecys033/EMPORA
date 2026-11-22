package com.examly.springapp.security;

import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.UserAccountRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import com.examly.springapp.repository.UserRoleMappingRepo;
import com.examly.springapp.model.UserRoleMapping;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import com.examly.springapp.model.AccountStatus;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepo userAccountRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;

    public CustomUserDetailsService(UserAccountRepo userAccountRepo, UserRoleMappingRepo userRoleMappingRepo) {
        this.userAccountRepo = userAccountRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<UserRoleMapping> mappings = userRoleMappingRepo.findByUserId(user.getId());
        List<GrantedAuthority> authorities = mappings.stream()
                .map(mapping -> {
                    String r = mapping.getRole().getRoleName().toUpperCase();
                    String auth = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                    return new SimpleGrantedAuthority(auth);
                })
                .collect(Collectors.toList());

        boolean enabled = user.getStatus() == AccountStatus.ACTIVE;
        boolean accountNonLocked = user.getLockedUntil() == null || !user.getLockedUntil().isAfter(LocalDateTime.now());
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                true, // accountNonExpired
                true, // credentialsNonExpired
                accountNonLocked,
                authorities
        );
    }
}
