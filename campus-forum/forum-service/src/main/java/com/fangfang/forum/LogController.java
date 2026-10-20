package com.fangfang.forum;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
@RestController @ConditionalOnExpression("'${app.service:all}' == 'all' or '${app.service:all}' == 'log'")
public class LogController {
 final Support support;LogController(Support support){this.support=support;}
 @GetMapping("/api/admin/logs")@PreAuthorize("hasRole('admin')")public List<Map<String,Object>> logs(){return support.db.queryForList("SELECT * FROM audit_logs ORDER BY created_at DESC,id LIMIT 200");}
}
