package com.fangfang.agent;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="auth-service",url="${app.auth-url}")
public interface AuthClient {@GetMapping("/api/auth/me")Map<String,Object> me(@RequestHeader("Authorization")String authorization);}
