package com.cleardocs.service;

import com.cleardocs.aop.AuditAspect.Audited;
import com.cleardocs.dto.JwtResponse;
import com.cleardocs.dto.LoginRequest;
import com.cleardocs.dto.RegisterRequest;
import com.cleardocs.model.jpa.Role;
import com.cleardocs.model.jpa.User;
import com.cleardocs.repository.jpa.RoleRepository;
import com.cleardocs.repository.jpa.UserRepository;
import com.cleardocs.security.JwtTokenProvider;
import com.cleardocs.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${cleardocs.jwt.expiration}")
    private long jwtExpirationMs;

    @Audited(action = "USER_LOGIN", resourceType = "AUTH", description = "User login attempt")
    @Transactional
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal.getUsername());

        List<String> roles = principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        return JwtResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpirationMs / 1000)
            .userId(principal.getId())
            .username(principal.getUsername())
            .email(principal.getEmail())
            .fullName(user.getFullName())
            .roles(roles)
            .build();
    }

    @Audited(action = "USER_REGISTER", resourceType = "AUTH", description = "New user registration")
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + request.getEmail());
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .department(request.getDepartment())
            .roles(roles)
            .enabled(true)
            .build();

        return userRepository.save(user);
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            roles.add(getRole(Role.ERole.ROLE_USER));
            return roles;
        }
        for (String roleName : roleNames) {
            Role.ERole eRole;
            switch (roleName.toLowerCase()) {
                case "admin":    eRole = Role.ERole.ROLE_ADMIN;    break;
                case "reviewer": eRole = Role.ERole.ROLE_REVIEWER; break;
                case "approver": eRole = Role.ERole.ROLE_APPROVER; break;
                case "auditor":  eRole = Role.ERole.ROLE_AUDITOR;  break;
                default:         eRole = Role.ERole.ROLE_USER;     break;
            }
            roles.add(getRole(eRole));
        }
        return roles;
    }

    private Role getRole(Role.ERole eRole) {
        return roleRepository.findByName(eRole)
            .orElseThrow(() -> new RuntimeException("Role not found: " + eRole));
    }
}
