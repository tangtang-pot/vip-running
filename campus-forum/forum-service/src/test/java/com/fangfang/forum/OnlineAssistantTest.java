package com.fangfang.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OnlineAssistantTest {
 @Test void sendsGroundedRequestAndReadsAnswerWithoutLeakingKey() throws Exception {
  var request=new AtomicReference<String>();var auth=new AtomicReference<String>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",exchange->{auth.set(exchange.getRequestHeaders().getFirst("Authorization"));request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));byte[] reply="{\"choices\":[{\"message\":{\"content\":\"依据【测试资料】，请在论坛发帖。\"}}]}".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,reply.length);exchange.getResponseBody().write(reply);exchange.close();});server.start();
  try{var json=new ObjectMapper();var adapter=new OnlineAssistant(json,true,"test-only-key","http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions","test-model");
   assertThat(adapter.answer("怎么发帖？",List.of(Map.of("title","测试资料","content","使用发布按钮")))).contains("依据【测试资料】");
   assertThat(auth.get()).isEqualTo("Bearer test-only-key");var body=json.readTree(request.get());assertThat(body.path("model").asText()).isEqualTo("test-model");assertThat(body.path("messages").get(1).path("content").asText()).contains("使用发布按钮","怎么发帖");assertThat(request.get()).doesNotContain("test-only-key");
  }finally{server.stop(0);}
 }
 @Test void masksProviderErrorBodies() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{byte[] body="private-provider-debug-secret".getBytes();exchange.sendResponseHeaders(401,body.length);exchange.getResponseBody().write(body);exchange.close();});server.start();
  try{var adapter=new OnlineAssistant(new ObjectMapper(),true,"test-key","http://127.0.0.1:"+server.getAddress().getPort(),"test");assertThatThrownBy(()->adapter.answer("问题",List.of())).isInstanceOf(IllegalStateException.class).hasMessageContaining("授权失败").hasMessageNotContaining("private-provider");}finally{server.stop(0);}
 }
 @Test void requiresKeyAndSecureRemoteEndpoint(){var json=new ObjectMapper();assertThatThrownBy(()->new OnlineAssistant(json,true,"","https://example.test/chat","model")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new OnlineAssistant(json,true,"key","http://example.test/chat","model")).isInstanceOf(IllegalArgumentException.class);}
}
