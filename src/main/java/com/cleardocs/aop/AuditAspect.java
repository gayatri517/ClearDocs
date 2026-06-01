package com.cleardocs.aop;

import com.cleardocs.model.mongo.AuditLog;
import com.cleardocs.repository.mongo.AuditLogRepository;
import com.cleardocs.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Audited {
        String action();
        String resourceType() default "";
        String description() default "";
    }

    @Pointcut("@annotation(audited)")
    public void auditedMethods(Audited audited) {}

    @Around("auditedMethods(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long start = System.currentTimeMillis();
        AuditLog auditLog = buildBaseAuditLog(audited.action(), audited.resourceType(), audited.description());
        try {
            Object result = joinPoint.proceed();
            auditLog.setSuccess(true);
            return result;
        } catch (Throwable ex) {
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            auditLog.setExecutionTimeMs(System.currentTimeMillis() - start);
            saveAuditLogAsync(auditLog);
        }
    }

    @Around("execution(* com.cleardocs.controller..*(..)) && !@annotation(com.cleardocs.aop.AuditAspect.Audited)")
    public Object auditControllerCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String action = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        AuditLog auditLog = buildBaseAuditLog(action, "CONTROLLER", "Controller call: " + action);
        try {
            Object result = joinPoint.proceed();
            auditLog.setSuccess(true);
            return result;
        } catch (Throwable ex) {
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            auditLog.setExecutionTimeMs(System.currentTimeMillis() - start);
            saveAuditLogAsync(auditLog);
        }
    }

    private AuditLog buildBaseAuditLog(String action, String resourceType, String description) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuditLog auditLog = new AuditLog();
        auditLog.setTimestamp(Instant.now());
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setDescription(description);

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            auditLog.setUserId(principal.getId());
            auditLog.setUsername(principal.getUsername());
        }

        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestPath(request.getRequestURI());
            }
        } catch (Exception ignored) {
            // non-HTTP context
        }
        return auditLog;
    }

    @Async
    protected void saveAuditLogAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
