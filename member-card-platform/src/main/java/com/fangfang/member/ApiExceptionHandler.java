package com.fangfang.member;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class)ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<?> validation(MethodArgumentNotValidException e){String text=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("参数无效");return ResponseEntity.badRequest().body(Map.of("message",text));}
 @ExceptionHandler(DataIntegrityViolationException.class)ResponseEntity<?> conflict(DataIntegrityViolationException e){return ResponseEntity.status(409).body(Map.of("message","数据冲突，请检查手机号或重复请求"));}
 @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)ResponseEntity<?> forbidden(Exception e){return ResponseEntity.status(403).body(Map.of("message","没有此操作权限"));}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"请求失败":e.getReason()));}
}
