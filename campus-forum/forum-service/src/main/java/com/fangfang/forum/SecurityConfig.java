package com.fangfang.forum;
import java.util.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder();}
 @Bean SecretKeySpec key(@Value("${app.jwt-secret}")String secret){byte[] bytes;if(secret.isBlank()){bytes=new byte[32];new SecureRandom().nextBytes(bytes);}else{bytes=secret.getBytes(StandardCharsets.UTF_8);if(bytes.length<32)throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");}return new SecretKeySpec(bytes,"HmacSHA256");}
 @Bean JwtEncoder encoder(SecretKeySpec key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
 @Bean JwtDecoder decoder(SecretKeySpec key,JdbcTemplate db){var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();OAuth2TokenValidator<Jwt> revoked=jwt->db.queryForObject("SELECT COUNT(*) FROM revoked_tokens WHERE token_id=?",Integer.class,jwt.getId())>0?OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")):OAuth2TokenValidatorResult.success();decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("campus-forum"),revoked));return decoder;}
 @Bean SecurityFilterChain security(HttpSecurity http,JdbcTemplate db)throws Exception{return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/app.js","/style.css","/favicon.ico","/api/auth/login","/api/auth/code","/api/auth/register").permitAll().requestMatchers(HttpMethod.GET,"/api/posts","/api/posts/*","/api/categories","/api/hot").permitAll().anyRequest().authenticated()).oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwt->{var rows=db.queryForList("SELECT id,role FROM users WHERE username=?",jwt.getSubject());if(rows.isEmpty())throw new org.springframework.security.authentication.BadCredentialsException("User missing");return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+rows.get(0).get("role"))),jwt.getSubject());}))).build();}
}
