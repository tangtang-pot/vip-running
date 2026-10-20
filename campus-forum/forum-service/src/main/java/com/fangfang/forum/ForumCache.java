package com.fangfang.forum;
import java.util.*;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
public class ForumCache {
 final Support support;final JdbcTemplate db;final ObjectMapper json;
 public ForumCache(Support support,JdbcTemplate db,ObjectMapper json){this.support=support;this.db=db;this.json=json;}
 public void requirePublished(String id){if(db.queryForObject("SELECT COUNT(*) FROM posts WHERE id=? AND status='published'",Integer.class,id)==0)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"帖子不存在或不可见");}
 public Map<String,Object> post(String id){requirePublished(id);String key="forum:post:"+id;if(!support.infra)return load(id);String text=support.redis.opsForValue().get(key);if(text!=null)try{return json.readValue(text,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}catch(Exception ignored){}String lock="forum:cache-lock:"+id,token=support.id();if(Boolean.TRUE.equals(support.redis.opsForValue().setIfAbsent(lock,token,Duration.ofSeconds(5)))){try{var result=load(id);support.redis.opsForValue().set(key,json.writeValueAsString(result),Duration.ofSeconds(60+new Random().nextInt(30)));return result;}catch(com.fasterxml.jackson.core.JsonProcessingException e){return load(id);}finally{support.redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>("if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class),List.of(lock),token);}}return load(id);}
 Map<String,Object> load(String id){return db.queryForMap("SELECT p.*,u.username FROM posts p JOIN users u ON u.id=p.user_id WHERE p.id=? AND p.status='published'",id);}
 public void evict(String id){if(support.infra)support.redis.delete("forum:post:"+id);}
 public void bump(String id,double score){if(support.infra)support.redis.opsForZSet().incrementScore("forum:hot",id,score);}
}
