package com.fangfang.forum;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.script.DefaultRedisScript;
@RestController @ConditionalOnExpression("'${app.service:all}' == 'all' or '${app.service:all}' == 'interaction'")
public class InteractionController {
 final Support support;final ReactionStore store;final ForumCache cache;
 public InteractionController(Support support,ReactionStore store,ForumCache cache){this.support=support;this.store=store;this.cache=cache;}
 public record Reaction(@Pattern(regexp="like|bookmark")@NotNull String kind,@NotNull Boolean active){}
 @PutMapping("/api/posts/{id}/reactions")public Map<String,Object> set(@PathVariable String id,@Valid @RequestBody Reaction input,@AuthenticationPrincipal Jwt jwt){support.rate("reaction:"+jwt.getSubject(),120,60);cache.requirePublished(id);String uid=String.valueOf(support.user(jwt.getSubject()).get("id"));if(support.infra){String key="forum:reaction:"+uid+":"+id+":"+input.kind();Long baseline=support.db.queryForObject("SELECT COALESCE(MAX(version),0) FROM reactions WHERE post_id=? AND user_id=? AND kind=?",Long.class,id,uid,input.kind());String script="if redis.call('HEXISTS',KEYS[1],'version')==0 then redis.call('HSET',KEYS[1],'version',ARGV[2]);end;local v=redis.call('HINCRBY',KEYS[1],'version',1);redis.call('HSET',KEYS[1],'active',ARGV[1]);redis.call('SADD',KEYS[2],KEYS[1]);return v";support.redis.execute(new DefaultRedisScript<>(script,Long.class),List.of(key,"forum:dirty"),input.active()?"1":"0",String.valueOf(baseline));}else{Long old=support.db.queryForObject("SELECT COALESCE(MAX(version),0) FROM reactions WHERE post_id=? AND user_id=? AND kind=?",Long.class,id,uid,input.kind());store.apply(id,uid,input.kind(),input.active(),old+1);}return Map.of("active",input.active(),"sync",support.infra?"pending":"stored");}
 @Scheduled(fixedDelay=2000)public void flush(){if(!support.infra)return;Set<String> keys;try{keys=support.redis.opsForSet().members("forum:dirty");}catch(Exception e){return;}if(keys==null)return;for(String key:keys.stream().limit(200).toList()){try{var value=support.redis.opsForHash().entries(key);if(value.isEmpty())continue;String[] p=key.split(":");long version=Long.parseLong(String.valueOf(value.get("version")));store.apply(p[3],p[2],p[4],"1".equals(value.get("active")),version);support.redis.execute(new DefaultRedisScript<>("if redis.call('HGET',KEYS[1],'version')==ARGV[1] then return redis.call('SREM',KEYS[2],KEYS[1]) else return 0 end",Long.class),List.of(key,"forum:dirty"),String.valueOf(version));}catch(Exception e){/* Keep dirty entry for the next scheduled retry. */}}}
}
