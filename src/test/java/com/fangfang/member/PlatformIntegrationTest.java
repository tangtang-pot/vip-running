package com.fangfang.member;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:platformtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","app.jwt-secret=test-secret-at-least-thirty-two-bytes-long"})
@AutoConfigureMockMvc
class PlatformIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired MemberService service;
 String login(String user,String password)throws Exception{var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",user,"password",password)))).andExpect(status().isOk()).andReturn();return json.readTree(result.getResponse().getContentAsString()).get("token").asText();}
 @Test void loginAuthorizationAndLogout()throws Exception{mvc.perform(get("/api/members")).andExpect(status().isUnauthorized());String token=login("viewer","local-viewer-change-me");mvc.perform(get("/api/members").header("Authorization","Bearer "+token)).andExpect(status().isOk());mvc.perform(post("/api/members").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"无权限\",\"phone\":\"13800009999\",\"level\":\"普通会员\"}")).andExpect(status().isForbidden());mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());mvc.perform(get("/api/members").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());}
 @Test void transactionIdempotencyAndBalanceGuard(){var m=service.create(new ApiController.MemberInput("幂等测试","13899990001","普通会员",""),"test");String id=(String)m.get("id");var request=new ApiController.TradeInput("充值",10000,"微信","测试",UUID.randomUUID().toString());var first=service.transact(id,request,"test");var second=service.transact(id,request,"test");assertThat(first.get("id")).isEqualTo(second.get("id"));assertThat(((Number)service.member(id).get("balance")).longValue()).isEqualTo(10000);assertThatThrownBy(()->service.transact(id,new ApiController.TradeInput("消费",20000,"会员余额","",UUID.randomUUID().toString()),"test")).isInstanceOf(IllegalArgumentException.class);assertThat(((Number)service.member(id).get("balance")).longValue()).isEqualTo(10000);service.status(id,"停用","test");assertThatThrownBy(()->service.transact(id,new ApiController.TradeInput("充值",100,"微信","",UUID.randomUUID().toString()),"test")).hasMessage("账户已停用");}
 @Test void concurrentConsumptionCannotOverdraw()throws Exception{var member=service.create(new ApiController.MemberInput("并发测试","13899990002","普通会员",""),"test");String id=(String)member.get("id");service.transact(id,new ApiController.TradeInput("充值",10000,"微信","",UUID.randomUUID().toString()),"test");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);Callable<Boolean> consume=()->{start.await();try{service.transact(id,new ApiController.TradeInput("消费",8000,"会员余额","",UUID.randomUUID().toString()),"test");return true;}catch(IllegalArgumentException e){return false;}};try{Future<Boolean>a=pool.submit(consume),b=pool.submit(consume);start.countDown();assertThat((a.get(10,TimeUnit.SECONDS)?1:0)+(b.get(10,TimeUnit.SECONDS)?1:0)).isEqualTo(1);assertThat(((Number)service.member(id).get("balance")).longValue()).isEqualTo(2000);}finally{pool.shutdownNow();}}
}
