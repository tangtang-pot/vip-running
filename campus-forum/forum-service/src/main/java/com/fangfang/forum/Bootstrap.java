package com.fangfang.forum;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${app.service:all}' == 'all' or '${app.service:all}' == 'auth'")
public class Bootstrap implements CommandLineRunner {
 final Support support;final PasswordEncoder passwords;final String admin,student;
 public Bootstrap(Support support,PasswordEncoder passwords,@Value("${app.admin-password}")String admin,@Value("${app.demo-password}")String student){this.support=support;this.passwords=passwords;this.admin=admin;this.student=student;}
 @Override @Transactional public void run(String...args){if(support.db.queryForObject("SELECT COUNT(*) FROM users",Integer.class)>0)return;if(admin.length()<12||student.length()<12)throw new IllegalStateException("Development passwords must be at least 12 characters");String aid=support.id(),sid=support.id();support.db.update("INSERT INTO users(id,username,email,password_hash,role) VALUES (?,?,?,?,?)",aid,"admin","admin@example.test",passwords.encode(admin),"admin");support.db.update("INSERT INTO users(id,username,email,password_hash,role) VALUES (?,?,?,?,?)",sid,"student","student@example.test",passwords.encode(student),"student");String[][] posts={{"新同学，来这里打个招呼吧","这是校园论坛演示区。分享你的学习计划和想认识的同学吧！\n请勿填写真实的个人敏感信息。","校园交流"},{"高等数学复习搭子招募","准备从极限、导数开始一起复习，有兴趣的同学可以评论交流。\n演示帖子，不代表实际活动。","学习求助"},{"校园资讯板块使用说明","这里可以发布校园活动资讯。重要通知请以学校官方渠道为准。","校园资讯"},{"分享一个 Java 学习路线","Java 基础 → 集合与并发 → Spring Boot → 数据库 → 动手完成一个项目。欢迎补充你的学习经验。","学习求助"},{"闲置图书交流区开放啦","发布闲置书籍时请说明书名和成色。平台示例不提供真实交易担保。","二手闲置"},{"你的图书馆学习仪式感是什么","准备好水杯，列出今天的小目标。欢迎聊聊你的学习习惯。","校园交流"}};for(int i=0;i<posts.length;i++)support.db.update("INSERT INTO posts(id,user_id,title,body,category) VALUES (?,?,?,?,?)",support.id(),i%2==0?aid:sid,posts[i][0],posts[i][1],posts[i][2]);support.db.update("INSERT INTO knowledge(id,title,content) VALUES (?,?,?)",support.id(),"演示资料：论坛账号与使用说明","这是应用演示资料，不是学校正式政策。注册论坛账号需要邮箱验证码，验证码有效期5分钟。登录后可发帖、评论、点赞及收藏。账号退出后原令牌会失效。涉及学籍、奖学金、宿舍等校园办理事项，请以学校官方通知为准。");}
}
