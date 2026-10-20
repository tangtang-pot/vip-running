package com.fangfang.forum;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.time.Duration;
@Service
public class Support {
 final JdbcTemplate db;final StringRedisTemplate redis;final boolean infra;
 private record Bucket(long expires,AtomicInteger count){}
 private final Map<String,Bucket> limits=new ConcurrentHashMap<>();
 Support(JdbcTemplate db,StringRedisTemplate redis,@Value("${app.infra-enabled}")boolean infra){this.db=db;this.redis=redis;this.infra=infra;}
 String id(){return UUID.randomUUID().toString();}
 Map<String,Object> user(String username){return db.queryForMap("SELECT id,username,email,role FROM users WHERE username=?",username);}
 void audit(String actor,String action,String target){db.update("INSERT INTO audit_logs(id,actor,action,target) VALUES (?,?,?,?)",id(),actor,action,target);}
 void rate(String key,int max,int seconds){if(infra){String script="local n=redis.call('INCR',KEYS[1]);if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]);end;return n";Long n=redis.execute(new DefaultRedisScript<>(script,Long.class),List.of("forum:rate:"+key),String.valueOf(seconds));if(n!=null&&n>max)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"操作太频繁，请稍后再试");}else{long now=System.currentTimeMillis();if(limits.size()>10000)limits.entrySet().removeIf(e->e.getValue().expires<now);Bucket b=limits.compute(key,(k,old)->old==null||old.expires<now?new Bucket(now+seconds*1000L,new AtomicInteger()):old);if(b.count.incrementAndGet()>max)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"操作太频繁，请稍后再试");}}
}
