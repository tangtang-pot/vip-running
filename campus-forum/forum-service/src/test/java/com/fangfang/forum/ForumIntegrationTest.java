package com.fangfang.forum;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:forumtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","app.jwt-secret=forum-test-secret-over-thirty-two-bytes","app.dev-mailbox=true"})@AutoConfigureMockMvc
class ForumIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean OnlineAssistant online;
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 String text(Object object)throws Exception{return json.writeValueAsString(object);}
 JsonNode node(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));}
 String login(String name,String pass)throws Exception{return node(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(text(Map.of("username",name,"password",pass)))).andExpect(status().isOk()).andReturn()).get("token").asText();}
 String student()throws Exception{return login("student","local-student-change-me");}String admin()throws Exception{return login("admin","local-admin-change-me");}
 String create(String token,String title)throws Exception{return node(mvc.perform(post("/api/posts").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("title",title,"body","集成测试正文 <script>alert('x')</script>","category","学习求助")))).andExpect(status().isOk()).andReturn()).get("id").asText();}
 @Test void publicBrowsingAndAuthBoundaries()throws Exception{mvc.perform(get("/api/posts")).andExpect(status().isOk());mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());String token=student();mvc.perform(get("/api/admin/summary").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());}
 @Test void registrationCodeIsSingleUseAndRateLimited()throws Exception{String code=node(mvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON).content(text(Map.of("email","tester@example.test")))).andExpect(status().isOk()).andReturn()).get("devCode").asText();mvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON).content(text(Map.of("email","tester@example.test")))).andExpect(status().isTooManyRequests());var payload=Map.of("username","new_student","email","tester@example.test","password","my-long-password123","code",code);mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(text(payload))).andExpect(status().isOk());assertThat(login("new_student","my-long-password123")).isNotBlank();mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(text(payload))).andExpect(status().isBadRequest());}
 @Test void postOwnershipCommentsAndIdempotentReactions()throws Exception{String token=student(),id=create(token,"发布与互动集成测试");String authorization="Bearer "+token;mvc.perform(put("/api/posts/"+id).header("Authorization","Bearer "+admin()).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("title","不能代改","body","正文","category","学习求助")))).andExpect(status().isForbidden());for(int i=0;i<2;i++)mvc.perform(put("/api/posts/"+id+"/reactions").header("Authorization",authorization).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("kind","like","active",true)))).andExpect(status().isOk());mvc.perform(get("/api/posts/"+id).header("Authorization",authorization)).andExpect(jsonPath("$.likes").value(1));mvc.perform(post("/api/posts/"+id+"/comments").header("Authorization",authorization).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("body","一条测试评论")))).andExpect(status().isOk());mvc.perform(get("/api/posts/"+id)).andExpect(jsonPath("$.comments.length()").value(1));mvc.perform(put("/api/posts/"+id+"/reactions").header("Authorization",authorization).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("kind","bookmark","active",true)))).andExpect(status().isOk());mvc.perform(get("/api/posts").param("bookmarks","true").header("Authorization",authorization)).andExpect(jsonPath("$.total").value(1));mvc.perform(put("/api/posts/"+id+"/reactions").header("Authorization",authorization).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("kind","like","active",false)))).andExpect(status().isOk());mvc.perform(get("/api/posts/"+id)).andExpect(jsonPath("$.likes").value(0));}
 @Test void moderationHidesPublicContentAndKeepsAudit()throws Exception{String token=admin(),id=create(student(),"待隐藏帖子");mvc.perform(patch("/api/admin/posts/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(text(Map.of("status","hidden")))).andExpect(status().isOk());mvc.perform(get("/api/posts/"+id)).andExpect(status().isNotFound());mvc.perform(get("/api/admin/logs").header("Authorization","Bearer "+token)).andExpect(status().isOk());assertThat(db.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE target=?",Long.class,id)).isGreaterThan(0);}
 @Test void dynamicSearchIsParameterizedAndSizesAreBounded()throws Exception{mvc.perform(get("/api/posts").param("q","' OR 1=1 --").param("size","9999")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.size").value(50));}
 @Test void knowledgeIngestionRequiresAdmin()throws Exception{String body=text(Map.of("title","测试资料","content","演示校园问题咨询请联系官方渠道，不提供未经核实的政策。"));mvc.perform(post("/api/knowledge").header("Authorization","Bearer "+student()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());mvc.perform(post("/api/knowledge").header("Authorization","Bearer "+admin()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());mvc.perform(get("/api/assistant/stream").param("q","完全未知的事项").header("Authorization","Bearer "+student())).andExpect(status().isOk());}
 @Test void onlineAnswerUsesAuthenticatedSseAndSources()throws Exception{
  org.mockito.Mockito.when(online.enabled()).thenReturn(true);
  org.mockito.Mockito.when(online.answer(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyList())).thenReturn("模拟模型回答：请使用发布按钮。");
  String resultId=UUID.randomUUID().toString();db.update("INSERT INTO knowledge(id,title,content) VALUES (?,?,?)",resultId,"独特联调资料","独特联调：使用发布按钮。");
  var started=mvc.perform(get("/api/assistant/stream").param("q","独特联调").header("Authorization","Bearer "+student())).andExpect(request().asyncStarted()).andReturn();
  started.getAsyncResult(5000);
  String response=mvc.perform(asyncDispatch(started)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  assertThat(response).contains("event:mode","在线模型问答","event:source","独特联调资料","模拟模型回答","event:done");
 }

}
