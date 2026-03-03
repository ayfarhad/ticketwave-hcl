package com.ticketwave.common.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Pointcut("@annotation(com.ticketwave.common.audit.Auditable)")
    public void auditedMethods() {}

    @AfterReturning(pointcut = "auditedMethods()", returning = "result")
    public void after(JoinPoint jp, Object result) {
        log.info("AUDIT {} returned {}", jp.getSignature(), result);
    }
}
