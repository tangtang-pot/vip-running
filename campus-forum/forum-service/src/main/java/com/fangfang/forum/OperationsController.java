package com.fangfang.forum;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
@RestController @ConditionalOnExpression("'${app.service:all}' == 'all' or '${app.service:all}' == 'operations'")
public class OperationsController {
 final Support support;final ForumCache cache;
 OperationsController(Support support,ForumCache cache){this.support=support;this.cache=cache;}
 @GetMapping("/api/hot")public List<Map<String,Object>> hot(){if(support.infra){Set<String> ids=support.redis.opsForZSet().reverseRange("forum:hot",0,19);if(ids!=null&&!ids.isEmpty()){var result=new ArrayList<Map<String,Object>>();for(String id:ids){var rows=support.db.queryForList("SELECT id,title,category,views FROM posts WHERE id=? AND status='published'",id);if(!rows.isEmpty())result.add(rows.get(0));if(result.size()>=5)break;}if(!result.isEmpty())return result;}}return support.db.queryForList("SELECT p.id,p.title,p.category,p.views,(p.views+3*(SELECT COUNT(*) FROM reactions r WHERE r.post_id=p.id AND r.kind='like' AND r.active=TRUE)+4*(SELECT COUNT(*) FROM comments c WHERE c.post_id=p.id)) AS score FROM posts p WHERE status='published' ORDER BY score DESC,p.created_at DESC LIMIT 5");}
 @GetMapping("/api/admin/summary")@PreAuthorize("hasRole('admin')")public Map<String,Object> summary(){return Map.of("users",support.db.queryForObject("SELECT COUNT(*) FROM users",Long.class),"posts",support.db.queryForObject("SELECT COUNT(*) FROM posts WHERE status='published'",Long.class),"comments",support.db.queryForObject("SELECT COUNT(*) FROM comments",Long.class),"likes",support.db.queryForObject("SELECT COUNT(*) FROM reactions WHERE kind='like' AND active=TRUE",Long.class));}
 @GetMapping("/api/admin/posts")@PreAuthorize("hasRole('admin')")public List<Map<String,Object>> posts(){return support.db.queryForList("SELECT p.id,p.title,p.category,p.status,u.username FROM posts p JOIN users u ON u.id=p.user_id ORDER BY p.created_at DESC");}
 public record Moderation(@Pattern(regexp="published|hidden")@NotNull String status){}
 @PatchMapping("/api/admin/posts/{id}")@PreAuthorize("hasRole('admin')")@Transactional public Map<String,String> moderate(@PathVariable String id,@Valid @RequestBody Moderation body,@AuthenticationPrincipal Jwt jwt){int changed=support.db.update("UPDATE posts SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'deleted'",body.status(),id);if(changed==0)throw new IllegalArgumentException("帖子不存在或已删除");cache.evict(id);support.audit(jwt.getSubject(),"帖子状态: "+body.status(),id);return Map.of("message","状态已更新");}
}
