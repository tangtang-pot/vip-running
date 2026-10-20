package com.fangfang.forum;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface PostMapper {
 @SelectProvider(type=Sql.class,method="list")List<Map<String,Object>> list(@Param("q")String q,@Param("category")String category,@Param("offset")int offset,@Param("size")int size,@Param("userId")String userId,@Param("bookmarks")boolean bookmarks);
 @SelectProvider(type=Sql.class,method="count")long count(@Param("q")String q,@Param("category")String category,@Param("userId")String userId,@Param("bookmarks")boolean bookmarks);
 class Sql {
  static String where(Map<String,Object> p){String sql=" WHERE p.status='published'";if(p.get("q")!=null&&!p.get("q").toString().isBlank())sql+=" AND (p.title LIKE CONCAT('%',#{q},'%') OR p.body LIKE CONCAT('%',#{q},'%'))";if(p.get("category")!=null&&!p.get("category").toString().isBlank())sql+=" AND p.category=#{category}";if(Boolean.TRUE.equals(p.get("bookmarks")))sql+=" AND EXISTS(SELECT 1 FROM reactions r WHERE r.post_id=p.id AND r.user_id=#{userId} AND r.kind='bookmark' AND r.active=TRUE)";return sql;}
  public static String list(Map<String,Object> p){return "SELECT p.*,u.username,(SELECT COUNT(*) FROM reactions r WHERE r.post_id=p.id AND r.kind='like' AND r.active=TRUE) AS likes,(SELECT COUNT(*) FROM reactions r WHERE r.post_id=p.id AND r.kind='bookmark' AND r.active=TRUE) AS bookmarks,(SELECT COUNT(*) FROM comments c WHERE c.post_id=p.id) AS comments FROM posts p JOIN users u ON u.id=p.user_id"+where(p)+" ORDER BY p.created_at DESC,p.id LIMIT #{size} OFFSET #{offset}";}
  public static String count(Map<String,Object> p){return "SELECT COUNT(*) FROM posts p"+where(p);}
 }
}
