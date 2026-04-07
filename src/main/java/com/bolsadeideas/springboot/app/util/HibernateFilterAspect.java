package com.bolsadeideas.springboot.app.util;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Aspect
@Component
public class HibernateFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(HibernateFilterAspect.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.bolsadeideas.springboot.app.models.service.*.*(..))")
    public void beforeServiceMethod() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Si el usuario es SUPER_ADMIN, no filtramos por empresa (ve todo)
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return;
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            log.debug("Enabling tenantFilter for tenant: {}", tenantId);
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
