package com.fangfang.gateway;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
@SpringBootApplication
public class GatewayApplication {
 public static void main(String[]args){SpringApplication.run(GatewayApplication.class,args);}
 @Bean RouteLocator routes(RouteLocatorBuilder builder){return builder.routes()
  .route("auth",r->r.path("/api/auth/**").uri("lb://auth-service"))
  .route("interaction",r->r.path("/api/posts/*/reactions").uri("lb://interaction-service"))
  .route("logs",r->r.path("/api/admin/logs").uri("lb://log-service"))
  .route("operations",r->r.path("/api/admin/**","/api/hot").uri("lb://operations-service"))
  .route("agent",r->r.path("/api/assistant/**","/api/knowledge/**","/api/knowledge").uri("lb://agent-service"))
  .route("content",r->r.path("/**").uri("lb://content-service")).build();}
}
