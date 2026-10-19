package com.fangfang.member;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
@Component
public class Bootstrap implements CommandLineRunner {
 private final JdbcTemplate db;private final PasswordEncoder encoder;private final String admin,clerk,viewer;
 public Bootstrap(JdbcTemplate db,PasswordEncoder encoder,@Value("${app.admin-password}")String admin,@Value("${app.clerk-password}")String clerk,@Value("${app.viewer-password}")String viewer){this.db=db;this.encoder=encoder;this.admin=admin;this.clerk=clerk;this.viewer=viewer;}
 @Override @Transactional public void run(String...args){
  if(db.queryForObject("SELECT COUNT(*) FROM roles",Integer.class)>0)return;
  db.update("INSERT INTO roles VALUES (?,?,?)","admin","系统管理员","members,recharge,spend,transactions,export,audit,roles");
  db.update("INSERT INTO roles VALUES (?,?,?)","clerk","门店操作员","members,recharge,spend,transactions");
  db.update("INSERT INTO roles VALUES (?,?,?)","viewer","只读观察员","transactions");
  addUser("admin",admin,"admin");addUser("clerk",clerk,"clerk");addUser("viewer",viewer,"viewer");
  String[] names={"林小满","陈予安","苏沐晴","周亦然","许知夏","沈嘉宁","顾清和","唐以南"};
  for(int i=0;i<names.length;i++)db.update("INSERT INTO members(id,name,phone,level,balance_cents,points,status,note) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),names[i],"1380000"+String.format("%04d",1234+i*537),new String[]{"金卡会员","银卡会员","普通会员"}[i%3],0,0,i==6?"停用":"正常","演示会员；初始余额为0，充值后可消费");
 }
 private void addUser(String name,String password,String role){if(password.length()<12)throw new IllegalStateException("Account passwords must be at least 12 characters");db.update("INSERT INTO app_users VALUES (?,?,?,?)",UUID.randomUUID().toString(),name,encoder.encode(password),role);}
}
