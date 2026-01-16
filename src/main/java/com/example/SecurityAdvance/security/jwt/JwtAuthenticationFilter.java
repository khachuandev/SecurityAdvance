package com.example.SecurityAdvance.security.jwt;

import com.example.SecurityAdvance.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Value("${api.prefix}")
    private String apiPrefix;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) {
        try {
            if(isBypassToken(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");

            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            if(!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String emailHash = jwtTokenProvider.extractUsername(token);

            if(emailHash != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserDetails userDetails =
                        (CustomUserDetails) userDetailsService.loadUserByUsername(emailHash);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e){
            log.error("JWT authentication error", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                Pair.of(String.format("%s/auth/login", apiPrefix), "POST"),
                Pair.of(String.format("%s/auth/register", apiPrefix), "POST"),
                Pair.of(String.format("%s/auth/refresh-token", apiPrefix), "POST"),
                Pair.of(String.format("%s/auth/verify-email", apiPrefix), "GET"),
                Pair.of(String.format("%s/auth/resend-verification", apiPrefix), "GET"),
                Pair.of("/v3/api-docs/.*", "GET"),
                Pair.of("/swagger-ui/.*", "GET"),
                Pair.of("/swagger-ui.html", "GET")
        );
        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();
        for (Pair<String, String> token : bypassTokens) {
            String path = token.getFirst();
            String method = token.getSecond();
            String regexPath = path.replace("**", ".*");
            if (requestPath.matches(regexPath)
                    && requestMethod.equalsIgnoreCase(method)) {
                System.out.println("Bypassing for path: " + path + " and method: " + method);
                return true;
            }
        }
        return false;
    }
}
