package org.soroth.springauthorizationserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // 1. Protocol Endpoints Filter Chain (OAuth2 / OIDC)
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer.oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
                )
                .authorizeHttpRequests((authorize) ->
                        authorize.anyRequest().authenticated()
                )
                // Redirect unauthenticated requests to the default login page
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    // 2. Default Web Security Filter Chain (Handles user login and Bearer tokens for API)
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf((csrf) -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/public-key").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // 3. User Authentication (In-Memory for demonstration)
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user1")
                .password("password")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    // 4. Seed initial client into JpaRegisteredClientRepository
    @Bean
    public ApplicationRunner registeredClientRepositorySeeder(RegisteredClientRepository registeredClientRepository) {
        return args -> {
            if (registeredClientRepository.findByClientId("oidc-client") == null) {
                RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("oidc-client")
                        .clientSecret("{noop}secret") // {noop} disables encoding for plain text
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                        .redirectUri("https://oauth.pstmn.io/v1/callback") // Useful for Postman testing
                        .postLogoutRedirectUri("http://127.0.0.1:8080/")
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope("read")
                        .scope("write")
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(true) // Display consent screen
                                .requireProofKey(true) // Enforce PKCE
                                .build())
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(30))
                                .refreshTokenTimeToLive(Duration.ofDays(1))
                                .reuseRefreshTokens(false)
                                .build())
                        .build();

                registeredClientRepository.save(oidcClient);
            }
        };
    }

    // 5. JWK Signing Key Pair (Persistent RSA 2048-bit key)
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = loadRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static RSAKey loadRsaKey() {
        try (InputStream is = AuthorizationServerConfig.class.getResourceAsStream("/auth-server-key.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return RSAKey.parse(json);
            }
        } catch (Exception ignored) {
        }
        return generateRsaKey();
    }

    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

    // 6. JWT Decoder (for Resource Server Bearer Token validation)
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // 7. Authorization Server Endpoint Configuration
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
