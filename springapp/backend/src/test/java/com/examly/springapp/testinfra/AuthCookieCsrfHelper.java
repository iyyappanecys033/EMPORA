package com.examly.springapp.testinfra;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.*;

@Component
public class AuthCookieCsrfHelper {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final String DEFAULT_CSRF_VALUE = "valid-test-csrf-token-abc123xyz";

    @Autowired private JwtService jwtService;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    public Role getOrCreateRole(String roleName) {
        return roleRepo.findByRoleName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(roleName);
            return roleRepo.saveAndFlush(r);
        });
    }

    public UserAccount getOrCreateUser(String username, String roleName, String department, String manager, Double salary) {
        Role role = getOrCreateRole(roleName);
        UserAccount account = userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode("Password123!"));
            u.setEmail(username + "@empora.local");
            u.setPhone("9876543210");
            u.setStatus(AccountStatus.ACTIVE);
            u.setFailedLoginAttempts(0);
            return userAccountRepo.saveAndFlush(u);
        });

        if (userRoleMappingRepo.findByUserId(account.getId()).isEmpty()) {
            UserRoleMapping urm = new UserRoleMapping();
            urm.setUser(account);
            urm.setRole(role);
            userRoleMappingRepo.saveAndFlush(urm);
        }

        if (profileRepo.findByUserId(account.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(account);
            p.setEmployeeId("EMP-" + username.toUpperCase().replaceAll("[^A-Z0-9]", ""));
            p.setFirstName(capitalize(username));
            p.setLastName("TestUser");
            p.setDepartment(department != null ? department : "IT");
            p.setDesignation(roleName + " Specialist");
            p.setSalary(salary != null ? salary : 85000.0);
            p.setDateOfJoining(LocalDate.of(2023, 1, 1));
            p.setReportingManager(manager != null ? manager : "admin");
            p.setAddress("Corporate Headquarters 101");
            p.setDob(LocalDate.of(1990, 1, 1));
            p.setGender("Other");
            p.setEmergencyContact("9876543219");
            profileRepo.saveAndFlush(p);
        }

        return account;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + (str.length() > 1 ? str.substring(1) : "");
    }

    public String generateTokenForUser(String username, String roleName) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );
        User userDetails = new User(username, "", authorities);
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", Collections.singletonList(roleName));
        return jwtService.generateToken(claims, userDetails);
    }

    public Cookie createAuthCookie(String username, String roleName) {
        getOrCreateUser(username, roleName, "IT", "admin", 80000.0);
        String token = generateTokenForUser(username, roleName);
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    public RequestPostProcessor withRole(String roleName) {
        String username = "test_" + roleName.toLowerCase();
        return withUser(username, roleName);
    }

    public RequestPostProcessor withUser(String username, String roleName) {
        Cookie authCookie = createAuthCookie(username, roleName);
        return request -> {
            request.setCookies(appendCookie(request.getCookies(), authCookie));
            return request;
        };
    }

    public RequestPostProcessor withCsrf() {
        return withCsrf(DEFAULT_CSRF_VALUE);
    }

    public RequestPostProcessor withCsrf(String tokenValue) {
        Cookie csrfCookie = new Cookie(CSRF_COOKIE_NAME, tokenValue);
        csrfCookie.setPath("/");
        csrfCookie.setHttpOnly(false);
        return request -> {
            request.setCookies(appendCookie(request.getCookies(), csrfCookie));
            request.addHeader(CSRF_HEADER_NAME, tokenValue);
            return request;
        };
    }

    public RequestPostProcessor withMismatchedCsrf() {
        Cookie csrfCookie = new Cookie(CSRF_COOKIE_NAME, "token-in-cookie-111");
        csrfCookie.setPath("/");
        return request -> {
            request.setCookies(appendCookie(request.getCookies(), csrfCookie));
            request.addHeader(CSRF_HEADER_NAME, "mismatched-token-header-222");
            return request;
        };
    }

    public RequestPostProcessor withMissingCsrfHeader() {
        Cookie csrfCookie = new Cookie(CSRF_COOKIE_NAME, DEFAULT_CSRF_VALUE);
        csrfCookie.setPath("/");
        return request -> {
            request.setCookies(appendCookie(request.getCookies(), csrfCookie));
            return request;
        };
    }

    public RequestPostProcessor withAuthAndCsrf(String username, String roleName) {
        RequestPostProcessor authProc = withUser(username, roleName);
        RequestPostProcessor csrfProc = withCsrf();
        return request -> csrfProc.postProcessRequest(authProc.postProcessRequest(request));
    }

    private Cookie[] appendCookie(Cookie[] existing, Cookie newCookie) {
        if (existing == null || existing.length == 0) {
            return new Cookie[]{newCookie};
        }
        List<Cookie> list = new ArrayList<>(Arrays.asList(existing));
        list.removeIf(c -> c.getName().equals(newCookie.getName()));
        list.add(newCookie);
        return list.toArray(new Cookie[0]);
    }
}
