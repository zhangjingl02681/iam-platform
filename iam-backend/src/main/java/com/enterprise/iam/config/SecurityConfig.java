package com.enterprise.iam.config;

import com.enterprise.iam.oidc.FormLoginFailureHandler;
import com.enterprise.iam.oidc.FormLoginSuccessHandler;
import com.enterprise.iam.oidc.IamSessionLogoutHandler;
import com.enterprise.iam.session.IamLoginSessionService;
import com.enterprise.iam.session.IamSessionValidationFilter;
import com.enterprise.iam.web.CsrfCookieRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FormLoginSuccessHandler formLoginSuccessHandler,
            FormLoginFailureHandler formLoginFailureHandler,
            IamSessionLogoutHandler sessionLogoutHandler,
            IamLoginSessionService loginSessionService,
            SecurityContextRepository securityContextRepository)
            throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(csrfHandler)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Browser UI still sends the XSRF-TOKEN cookie on /login and /logout.
                        // /api/v1/** is ignored so HTTP Basic and bearer clients do not need a CSRF header.
                        .ignoringRequestMatchers("/api/v1/**", "/actuator/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(sessionLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/login",
                                "/login/mfa",
                                "/csrf",
                                "/error",
                                "/index.html",
                                "/assets/**",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/robots.txt")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> {
                    LoginUrlAuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");
                    HttpStatusEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
                    exceptions.authenticationEntryPoint((request, response, exception) -> {
                        String accept = request.getHeader("Accept");
                        if (accept != null
                                && accept.contains(MediaType.TEXT_HTML_VALUE)
                                && !request.getRequestURI().startsWith("/api/")) {
                            login.commence(request, response, exception);
                        } else {
                            unauthorized.commence(request, response, exception);
                        }
                    });
                });
        http.addFilterAfter(new CsrfCookieRequestFilter(), CsrfFilter.class);
        http.addFilterAfter(new IamSessionValidationFilter(loginSessionService), SecurityContextHolderFilter.class);
        return http.build();
    }
}
