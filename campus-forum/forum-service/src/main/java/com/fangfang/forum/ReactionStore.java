package com.fangfang.forum;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
@Service
public class ReactionStore {
 final JdbcTemplate db;final ForumCache cache;
 ReactionStore(JdbcTemplate db,ForumCache cache){this.db=db;this.cache=cache;}
 @Transactional public void apply(String post,String user,String kind,boolean active,long version){db.queryForList("SELECT id FROM posts WHERE id=? FOR UPDATE",post);var rows=db.queryForList("SELECT version,active FROM reactions WHERE post_id=? AND user_id=? AND kind=?",post,user,kind);boolean changed=rows.isEmpty()?active:!activeEquals(rows.get(0).get("active"),active);if(rows.isEmpty())db.update("INSERT INTO reactions(post_id,user_id,kind,active,version) VALUES (?,?,?,?,?)",post,user,kind,active,version);else if(((Number)rows.get(0).get("version")).longValue()<version)db.update("UPDATE reactions SET active=?,version=? WHERE post_id=? AND user_id=? AND kind=?",active,version,post,user,kind);else return;if(changed&&kind.equals("like"))cache.bump(post,active?3:-3);cache.evict(post);}
 boolean activeEquals(Object value,boolean active){return (value instanceof Boolean b?b:((Number)value).intValue()!=0)==active;}
}
