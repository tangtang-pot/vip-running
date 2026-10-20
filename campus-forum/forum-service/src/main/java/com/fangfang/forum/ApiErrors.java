package com.fangfang.forum;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class)ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("参数错误")));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("message","数据已存在或发生冲突，请刷新后重试"));}
 @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)ResponseEntity<?> forbidden(Exception e){return ResponseEntity.status(403).body(Map.of("message","没有此操作权限"));}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"请求失败":e.getReason()));}
}
