package org.soroth.springauthorizationserver.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    private final JWKSource<SecurityContext> jwkSource;

    public HomeController(JWKSource<SecurityContext> jwkSource) {
        this.jwkSource = jwkSource;
    }

    @GetMapping("/")
    public Map<String, Object> home(Principal principal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("message", "Spring Authorization Server is running");
        response.put("user", principal != null ? principal.getName() : "anonymous");
        response.put("openid_configuration", "http://localhost:9000/.well-known/openid-configuration");
        response.put("jwks", "http://localhost:9000/oauth2/jwks");
        response.put("public_key_pem", "http://localhost:9000/public-key");
        return response;
    }

    @GetMapping(value = "/public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPublicKeyPem() throws Exception {
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
        List<JWK> keys = this.jwkSource.get(selector, null);
        if (!keys.isEmpty() && keys.get(0) instanceof RSAKey rsaKey) {
            byte[] encoded = rsaKey.toRSAPublicKey().getEncoded();
            String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
            return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
        }
        return "No RSA key found";
    }
}
