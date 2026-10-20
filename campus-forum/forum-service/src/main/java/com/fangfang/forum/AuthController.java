package com.fangfang.forum;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.security.SecureRandom;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/auth") @ConditionalOnExpression("'${app.service:all}' == 'all' or '${app.service:all}' == 'auth'")
public class AuthController {
 final JdbcTemplate db;final Support support;final PasswordEncoder passwords;final JwtEncoder encoder;final boolean mailbox;
 public AuthController(JdbcTemplate db,Support support,PasswordEncoder passwords,JwtEncoder encoder,@Value("${app.dev-mailbox}")boolean mailbox){this.db=db;this.support=support;this.passwords=passwords;this.encoder=encoder;this.mailbox=mailbox;}
 public record Code(@Email @NotBlank @Size(max=180)String email){}
 public record Register(@Pattern(regexp="[a-zA-Z0-9_]{3,30}")@NotNull String username,@Email @NotBlank @Size(max=180)String email,@Size(min=12,max=72)@NotNull String password,@Pattern(regexp="[0-9]{6}")@NotNull String code){}
 public record Login(@NotBlank @Size(max=40)String username,@NotBlank @Size(max=72)String password){}
 @PostMapping("/code")@Transactional public Map<String,Object> code(@Valid @RequestBody Code input,HttpServletRequest request){String email=input.email().trim().toLowerCase(Locale.ROOT);support.rate("code-ip:"+request.getRemoteAddr(),10,600);support.rate("code-email:"+email,1,60);String code=String.format("%06d",new SecureRandom().nextInt(1000000));db.update("DELETE FROM email_codes WHERE email=?",email);db.update("INSERT INTO email_codes(email,code_hash,expires_at,last_sent) VALUES (?,?,?,CURRENT_TIMESTAMP)",email,passwords.encode(code),Timestamp.from(Instant.now().plusSeconds(300)));db.update("INSERT INTO mail_outbox(id,email,code,state) VALUES (?,?,?,?)",support.id(),email,code,mailbox?"dev":"pending");Map<String,Object> result=new LinkedHashMap<>();result.put("message",mailbox?"开发邮箱模式：验证码仅用于本地体验":"验证码已进入异步邮件队列");result.put("expiresIn",300);if(mailbox)result.put("devCode",code);return result;}
 @PostMapping("/register")public Map<String,String> register(@Valid @RequestBody Register input){String email=input.email().trim().toLowerCase(Locale.ROOT);support.rate("register:"+email,5,300);return registerTransaction(input,email);}
 // TransactionTemplate avoids proxy self-invocation; locks protect single-use verification codes.
 private Map<String,String> registerTransaction(Register input,String email){return new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(Objects.requireNonNull(db.getDataSource()))).execute(status->{var codes=db.queryForList("SELECT * FROM email_codes WHERE email=? FOR UPDATE",email);if(codes.isEmpty()||((Timestamp)codes.get(0).get("expires_at")).toInstant().isBefore(Instant.now())||!passwords.matches(input.code(),String.valueOf(codes.get(0).get("code_hash"))))throw new IllegalArgumentException("验证码错误或已过期");String id=support.id();db.update("INSERT INTO users(id,username,email,password_hash,role) VALUES (?,?,?,?,?)",id,input.username(),email,passwords.encode(input.password()),"student");db.update("DELETE FROM email_codes WHERE email=?",email);support.audit(input.username(),"注册账号",id);return Map.of("message","注册成功，请登录");});}
 @PostMapping("/login")public Map<String,Object> login(@Valid @RequestBody Login input,HttpServletRequest request){support.rate("login:"+request.getRemoteAddr(),20,60);var users=db.queryForList("SELECT * FROM users WHERE username=?",input.username());if(users.isEmpty()||!passwords.matches(input.password(),String.valueOf(users.get(0).get("password_hash"))))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码错误");Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer("campus-forum").subject(input.username()).claim("uid",users.get(0).get("id")).issuedAt(now).expiresAt(now.plusSeconds(3600)).id(support.id()).build();return Map.of("token",encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue(),"expiresIn",3600);}
 @GetMapping("/me")public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt){return support.user(jwt.getSubject());}
 @PostMapping("/logout")public Map<String,String> logout(@AuthenticationPrincipal Jwt jwt){db.update("DELETE FROM revoked_tokens WHERE expires_at<CURRENT_TIMESTAMP");db.update("INSERT INTO revoked_tokens VALUES (?,?)",jwt.getId(),Timestamp.from(jwt.getExpiresAt()));if(support.infra)support.redis.opsForValue().set("forum:blacklist:"+jwt.getId(),"1",Duration.ofSeconds(Math.max(1,Duration.between(Instant.now(),jwt.getExpiresAt()).getSeconds())));return Map.of("message","已退出登录");}
}
