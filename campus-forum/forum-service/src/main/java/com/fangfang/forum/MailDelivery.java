package com.fangfang.forum;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
@Service
public class MailDelivery {
 final Support support;final RabbitTemplate rabbit;final JavaMailSender mail;final String from;
 MailDelivery(Support support,RabbitTemplate rabbit,JavaMailSender mail,@Value("${app.mail-from}")String from){this.support=support;this.rabbit=rabbit;this.mail=mail;this.from=from;}
 @Bean org.springframework.amqp.core.Queue mailQueue(){return new org.springframework.amqp.core.Queue("forum.mail",true);}
 @Scheduled(fixedDelay=5000)public void publish(){if(!support.infra)return;support.db.update("UPDATE mail_outbox SET state='pending' WHERE state IN ('sending','queued') AND updated_at<?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)));support.db.update("UPDATE mail_outbox SET state='expired',code='' WHERE state IN ('pending','dev') AND created_at<?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(300)));}
 @Scheduled(fixedDelay=3000)public void sendPending(){if(!support.infra)return;var rows=support.db.queryForList("SELECT * FROM mail_outbox WHERE state='pending' AND attempts<5 ORDER BY created_at LIMIT 20");for(var row:rows){String id=String.valueOf(row.get("id"));if(support.db.update("UPDATE mail_outbox SET state='sending',attempts=attempts+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND state='pending'",id)==0)continue;try{rabbit.convertAndSend("forum.mail",id);support.db.update("UPDATE mail_outbox SET state='queued',updated_at=CURRENT_TIMESTAMP WHERE id=? AND state='sending'",id);}catch(Exception e){support.db.update("UPDATE mail_outbox SET state='pending',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);}}}
 @RabbitListener(queues="forum.mail")public void deliver(String id){if(support.db.queryForObject("SELECT COUNT(*) FROM mail_deliveries WHERE id=?",Integer.class,id)>0)return;var rows=support.db.queryForList("SELECT * FROM mail_outbox WHERE id=?",id);if(rows.isEmpty())return;var row=rows.get(0);if(((java.sql.Timestamp)row.get("created_at")).toInstant().isBefore(java.time.Instant.now().minusSeconds(300))){support.db.update("UPDATE mail_outbox SET state='expired',code='' WHERE id=?",id);return;}try{SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(String.valueOf(row.get("email")));message.setSubject("校园论坛注册验证码");message.setText("你的验证码为 "+row.get("code")+"，有效期5分钟。若非本人申请，请忽略。");mail.send(message);support.db.update("INSERT INTO mail_deliveries(id) VALUES (?)",id);support.db.update("UPDATE mail_outbox SET state='sent',code='' WHERE id=?",id);}catch(Exception e){support.db.update("UPDATE mail_outbox SET state='pending',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);/* Outbox retries are bounded by attempts, avoiding poison-message redelivery loops. */}}
}
