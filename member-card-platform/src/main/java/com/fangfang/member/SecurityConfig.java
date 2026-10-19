package com.fangfang.member;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecretKeySpec signingKey(@Value("${app.jwt-secret}") String secret){
  byte[] bytes;if(secret.isBlank()){bytes=new byte[32];new SecureRandom().nextBytes(bytes);}else{bytes=secret.getBytes(StandardCharsets.UTF_8);if(bytes.length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");}return new SecretKeySpec(bytes,"HmacSHA256");
 }
 @Bean JwtEncoder encoder(SecretKeySpec key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
 @Bean JwtDecoder decoder(SecretKeySpec key,JdbcTemplate jdbc){
  NimbusJwtDecoder decoder= NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  OAuth2TokenValidator<Jwt> revoked=jwt->{Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM revoked_tokens WHERE token_id=?",Integer.class,jwt.getId());return count!=null&&count>0?OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Token revoked",null)):OAuth2TokenValidatorResult.success();};
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("member-platform"),revoked));return decoder;
 }
 @Bean SecurityFilterChain filterChain(HttpSecurity http,JdbcTemplate jdbc)throws Exception{
  http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/","/index.html","/app.js","/styles.css","/favicon.ico").permitAll().anyRequest().authenticated())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwt->{
    var rows=jdbc.queryForList("SELECT r.permissions FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=?",jwt.getSubject());
    if(rows.isEmpty())throw new org.springframework.security.authentication.BadCredentialsException("User no longer exists");
    String permissions=String.valueOf(rows.get(0).get("permissions"));
    var authorities=Arrays.stream(permissions.split(",")).filter(x->!x.isBlank()).map(SimpleGrantedAuthority::new).toList();
    return new JwtAuthenticationToken(jwt,authorities,jwt.getSubject());
   })));
  return http.build();
 }
}
