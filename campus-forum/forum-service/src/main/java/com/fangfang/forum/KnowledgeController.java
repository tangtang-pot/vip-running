package com.fangfang.forum;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
@RestController @ConditionalOnExpression("'${app.service:all}' == 'all'")
public class KnowledgeController {
 final Support support; final OnlineAssistant online; final java.util.concurrent.Semaphore slots=new java.util.concurrent.Semaphore(3);
 KnowledgeController(Support support,OnlineAssistant online){this.support=support;this.online=online;}
 public record Document(@NotBlank @Size(max=180)String title,@NotBlank @Size(max=30000)String content){}
 @PostMapping("/api/knowledge")@PreAuthorize("hasRole('admin')")public Map<String,String> add(@Valid @RequestBody Document input,@AuthenticationPrincipal Jwt jwt){String id=support.id();support.db.update("INSERT INTO knowledge(id,title,content) VALUES (?,?,?)",id,input.title(),input.content());support.audit(jwt.getSubject(),"新增知识资料",id);return Map.of("id",id);}
 @GetMapping("/api/knowledge")public List<Map<String,Object>> documents(){return support.db.queryForList("SELECT id,title,content FROM knowledge ORDER BY created_at DESC");}
 @GetMapping(value="/api/assistant/stream",produces="text/event-stream")public SseEmitter answer(@RequestParam String q,@AuthenticationPrincipal Jwt jwt){if(q.isBlank()||q.length()>500)throw new IllegalArgumentException("问题长度须在1至500字符之间");support.rate("ask:"+jwt.getSubject(),10,60);SseEmitter emitter=new SseEmitter(75000L);List<Map<String,Object>> docs=support.db.queryForList("SELECT id,title,content FROM knowledge");String question=q.toLowerCase(Locale.ROOT);var matches=docs.stream().map(doc->Map.entry(doc,score(question,String.valueOf(doc.get("title"))+doc.get("content")))).filter(x->x.getValue()>0).sorted((a,b)->Integer.compare(b.getValue(),a.getValue())).limit(3).map(Map.Entry::getKey).toList();
 if(online.enabled()&&!matches.isEmpty()){
  if(!slots.tryAcquire())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"问答繁忙，请稍后再试");
  Thread worker=Thread.ofVirtual().unstarted(()->{try{
   emitter.send(SseEmitter.event().name("mode").data("在线模型问答：正在根据资料生成（完整回答返回）"));
   for(var doc:matches)emitter.send(SseEmitter.event().name("source").data(Map.of("id",doc.get("id"),"title",doc.get("title"))));
   emitter.send(SseEmitter.event().name("token").data(online.answer(q,matches)));
   emitter.send(SseEmitter.event().name("done").data("complete"));emitter.complete();
  }catch(Exception error){try{String message=error instanceof IllegalStateException?error.getMessage():"模型请求失败或超时，请稍后再试。";emitter.send(SseEmitter.event().name("token").data(message));emitter.send(SseEmitter.event().name("mode").data("在线模型调用失败"));emitter.send(SseEmitter.event().name("done").data("failed"));emitter.complete();}catch(Exception ignored){emitter.completeWithError(ignored);}}finally{slots.release();}});
  emitter.onTimeout(worker::interrupt);emitter.onCompletion(worker::interrupt);worker.start();return emitter;
 }
 try{emitter.send(SseEmitter.event().name("mode").data(matches.isEmpty()?"未找到相关资料，本次未调用大模型":"本地资料检索模式：非大模型生成"));if(matches.isEmpty()){emitter.send(SseEmitter.event().name("token").data("当前资料不足，无法根据已有文档回答。请联系学校相关部门，或由管理员补充资料。"));}else{for(var doc:matches){emitter.send(SseEmitter.event().name("source").data(Map.of("id",doc.get("id"),"title",doc.get("title"))));String content=String.valueOf(doc.get("content"));emitter.send(SseEmitter.event().name("token").data("【"+doc.get("title")+"】\n"+content.substring(0,Math.min(content.length(),700))+"\n"));}}emitter.send(SseEmitter.event().name("done").data("complete"));emitter.complete();}catch(Exception e){emitter.completeWithError(e);}return emitter;}
 private int score(String q,String text){int score=0;for(int i=0;i<q.length()-1;i++){String part=q.substring(i,i+2);if(!part.isBlank()&&text.toLowerCase(Locale.ROOT).contains(part))score++;}return score;}
}
