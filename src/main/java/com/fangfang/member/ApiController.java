package com.fangfang.member;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.security.Principal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api")
public class ApiController {
 private final JdbcTemplate db;private final MemberService service;
 public ApiController(JdbcTemplate db,MemberService service){this.db=db;this.service=service;}
 public record MemberInput(@NotBlank @Size(max=80)String name,@Pattern(regexp="1\\d{10}") @NotNull String phone,@NotBlank String level,@Size(max=500)String note){}
 public record StatusInput(@NotBlank String status){}
 public record TradeInput(@NotBlank String type,@Min(1) @Max(10000000) long amount,@NotBlank @Size(max=30)String channel,@Size(max=500)String note,@NotBlank @Pattern(regexp="[a-zA-Z0-9-]{8,60}")String requestKey){}
 public record RoleInput(@NotNull Set<String> permissions){}
 @GetMapping("/members")public List<Map<String,Object>> members(){return db.queryForList("SELECT * FROM members ORDER BY created_at,id").stream().map(service::memberDto).toList();}
 @GetMapping("/members/{id}")public Map<String,Object> member(@PathVariable String id){return service.member(id);}
 @PostMapping("/members")@PreAuthorize("hasAuthority('members')")public Map<String,Object> create(@Valid @RequestBody MemberInput body,Principal p){return service.create(body,p.getName());}
 @PutMapping("/members/{id}")@PreAuthorize("hasAuthority('members')")public Map<String,Object> update(@PathVariable String id,@Valid @RequestBody MemberInput body,Principal p){return service.edit(id,body,p.getName());}
 @PatchMapping("/members/{id}/status")@PreAuthorize("hasAuthority('members')")public Map<String,String> status(@PathVariable String id,@Valid @RequestBody StatusInput body,Principal p){service.status(id,body.status(),p.getName());return Map.of("message","状态已更新");}
 @PostMapping("/members/{id}/transactions")@PreAuthorize("(#body.type() == '充值' and hasAuthority('recharge')) or (#body.type() == '消费' and hasAuthority('spend'))")public Map<String,Object> trade(@PathVariable String id,@Valid @RequestBody TradeInput body,Principal p){return service.transact(id,body,p.getName());}
 @GetMapping("/transactions")@PreAuthorize("hasAuthority('transactions')")public List<Map<String,Object>> transactions(){return db.queryForList("SELECT t.*,m.name FROM transactions t JOIN members m ON m.id=t.member_id ORDER BY t.created_at,t.id").stream().map(service::tradeDto).toList();}
 @GetMapping("/logs")@PreAuthorize("hasAuthority('audit')")public List<Map<String,Object>> logs(){return db.queryForList("SELECT * FROM audit_logs ORDER BY created_at,id");}
 @GetMapping("/roles")public List<Map<String,Object>> roles(){return db.queryForList("SELECT * FROM roles ORDER BY id").stream().map(r->Map.<String,Object>of("id",r.get("id"),"name",r.get("name"),"permissions",String.valueOf(r.get("permissions")).split(","))).toList();}
 @PutMapping("/roles/{id}")@PreAuthorize("hasAuthority('roles')")@Transactional public Map<String,String> role(@PathVariable String id,@Valid @RequestBody RoleInput body,Principal p){if("admin".equals(id))throw new IllegalArgumentException("管理员权限固定");if(!Set.of("clerk","viewer").contains(id))throw new IllegalArgumentException("角色不存在");if(!Set.of("members","recharge","spend","transactions","export","audit").containsAll(body.permissions()))throw new IllegalArgumentException("权限值无效");db.update("UPDATE roles SET permissions=? WHERE id=?",String.join(",",new TreeSet<>(body.permissions())),id);service.audit("修改角色权限",id,String.join(",",body.permissions()),p.getName());return Map.of("message","角色权限已更新");}
}
