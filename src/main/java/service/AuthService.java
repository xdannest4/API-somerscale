package service;

import dto.AuthRequest;
import dto.AuthResponse;
import dto.RefreshTokenRequest;
import security.JwtTokenProvider;
import security.UserPrincipal;
import security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(userPrincipal.getRole())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (tokenProvider.validateToken(request.getRefreshToken())) {
            String username = tokenProvider.getUsernameFromJWT(request.getRefreshToken());
            
            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
            
            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(request.getRefreshToken())
                    .role(userPrincipal.getRole())
                    .build();
        }
        throw new RuntimeException("Refresh token inválido");
    }
}