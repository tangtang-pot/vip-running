package com.fangfang.member;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final JdbcTemplate db;private final PasswordEncoder passwords;private final JwtEncoder encoder;
 public AuthController(JdbcTemplate db,PasswordEncoder passwords,JwtEncoder encoder){this.db=db;this.passwords=passwords;this.encoder=encoder;}
 public record Login(@NotBlank @Size(max=80)String username,@NotBlank @Size(max=200)String password){}
 @PostMapping("/login")public Map<String,Object> login(@Valid @RequestBody Login input){
  var rows=db.queryForList("SELECT password_hash FROM app_users WHERE username=?",input.username());
  if(rows.isEmpty()||!passwords.matches(input.password(),String.valueOf(rows.get(0).get("password_hash"))))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码错误");
  Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer("member-platform").subject(input.username()).issuedAt(now).expiresAt(now.plusSeconds(3600)).id(UUID.randomUUID().toString()).build();
  String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();return Map.of("token",token,"expiresIn",3600);
 }
 @GetMapping("/me")public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt){var row=db.queryForMap("SELECT u.username,r.id AS role_id,r.name,r.permissions FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=?",jwt.getSubject());return Map.of("username",row.get("username"),"roleId",row.get("role_id"),"roleName",row.get("name"),"permissions",String.valueOf(row.get("permissions")).split(","));}
 @PostMapping("/logout")public Map<String,String> logout(@AuthenticationPrincipal Jwt jwt){db.update("DELETE FROM revoked_tokens WHERE expires_at<CURRENT_TIMESTAMP");db.update("INSERT INTO revoked_tokens(token_id,expires_at) VALUES (?,?)",jwt.getId(),java.sql.Timestamp.from(jwt.getExpiresAt()));return Map.of("message","已退出登录");}
}
