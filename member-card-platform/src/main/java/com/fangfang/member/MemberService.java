package com.fangfang.member;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class MemberService {
 final JdbcTemplate db;
 public MemberService(JdbcTemplate db){this.db=db;}
 public void audit(String action,String target,String detail,String operator){db.update("INSERT INTO audit_logs(id,action,target_id,detail,operator_name,result) VALUES (?,?,?,?,?,?)",UUID.randomUUID().toString(),action,target,detail,operator,"成功");}
 public Map<String,Object> member(String id){var rows=db.queryForList("SELECT * FROM members WHERE id=?",id);if(rows.isEmpty())throw new IllegalArgumentException("会员不存在");return memberDto(rows.get(0));}
 public Map<String,Object> memberDto(Map<String,Object> m){Map<String,Object> result=new LinkedHashMap<>();for(String s:List.of("id","name","phone","level","points","status","note"))result.put(s,m.get(s));result.put("balance",m.get("balance_cents"));result.put("created",String.valueOf(m.get("created_at")));return result;}
 public Map<String,Object> tradeDto(Map<String,Object> t){Map<String,Object> result=new LinkedHashMap<>();for(String s:List.of("id","type","channel","note"))result.put(s,t.get(s));result.put("name",t.get("name"));result.put("memberId",t.get("member_id"));result.put("amount",t.get("amount_cents"));result.put("before",t.get("before_cents"));result.put("balance",t.get("after_cents"));result.put("points",t.get("points_delta"));result.put("operator",t.get("operator_name"));result.put("time",String.valueOf(t.get("created_at")));return result;}
 @Transactional public Map<String,Object> create(ApiController.MemberInput input,String actor){String id=UUID.randomUUID().toString();validateLevel(input.level());db.update("INSERT INTO members(id,name,phone,level,note) VALUES (?,?,?,?,?)",id,input.name().trim(),input.phone(),input.level(),input.note()==null?"":input.note());audit("新增会员",id,input.name(),actor);return member(id);}
 @Transactional public Map<String,Object> edit(String id,ApiController.MemberInput input,String actor){validateLevel(input.level());member(id);db.update("UPDATE members SET name=?,phone=?,level=?,note=? WHERE id=?",input.name().trim(),input.phone(),input.level(),input.note()==null?"":input.note(),id);audit("编辑会员",id,input.name(),actor);return member(id);}
 @Transactional public void status(String id,String status,String actor){if(!Set.of("正常","停用").contains(status))throw new IllegalArgumentException("状态无效");var rows=db.queryForList("SELECT id FROM members WHERE id=? FOR UPDATE",id);if(rows.isEmpty())throw new IllegalArgumentException("会员不存在");db.update("UPDATE members SET status=? WHERE id=?",status,id);audit("更新账户状态",id,status,actor);}
 @Transactional public Map<String,Object> transact(String memberId,ApiController.TradeInput input,String actor){
  if(!Set.of("充值","消费").contains(input.type()))throw new IllegalArgumentException("交易类型无效");
  String requestKey=actor+":"+input.requestKey();
  // Lock the member row before balance calculation and idempotency inspection.
  var locked=db.queryForList("SELECT * FROM members WHERE id=? FOR UPDATE",memberId);if(locked.isEmpty())throw new IllegalArgumentException("会员不存在");
  var existing=db.queryForList("SELECT t.*,m.name FROM transactions t JOIN members m ON m.id=t.member_id WHERE request_key=?",requestKey);
  if(!existing.isEmpty()){var t=existing.get(0);if(!memberId.equals(t.get("member_id"))||!input.type().equals(t.get("type"))||((Number)t.get("amount_cents")).longValue()!=input.amount())throw new IllegalArgumentException("幂等键已用于其他交易");return tradeDto(t);}
  var m=locked.get(0);if(!"正常".equals(m.get("status")))throw new IllegalArgumentException("账户已停用");long before=((Number)m.get("balance_cents")).longValue();boolean recharge="充值".equals(input.type());if(!recharge&&before<input.amount())throw new IllegalArgumentException("账户余额不足");long after=recharge?Math.addExact(before,input.amount()):before-input.amount();long points=recharge?0:input.amount()/100;String id=UUID.randomUUID().toString();
  db.update("UPDATE members SET balance_cents=?,points=points+? WHERE id=?",after,points,memberId);
  db.update("INSERT INTO transactions(id,member_id,type,amount_cents,before_cents,after_cents,points_delta,channel,note,operator_name,request_key) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,memberId,input.type(),input.amount(),before,after,points,recharge?input.channel():"会员余额",input.note()==null?"":input.note(),actor,requestKey);
  audit(input.type(),memberId,id+" / "+input.amount()+"分 / "+before+" → "+after,actor);
  return tradeDto(db.queryForMap("SELECT t.*,m.name FROM transactions t JOIN members m ON m.id=t.member_id WHERE t.id=?",id));
 }
 private void validateLevel(String level){if(!Set.of("金卡会员","银卡会员","普通会员").contains(level))throw new IllegalArgumentException("会员等级无效");}
}
