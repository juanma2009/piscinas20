package com.bolsadeideas.springboot.app.apisms.d2fa;

import javax.mail.MessagingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

import com.bolsadeideas.springboot.app.models.entity.User;
import com.bolsadeideas.springboot.app.models.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TwoFactorAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = Logger.getLogger(TwoFactorAuthenticationFilter.class.getName());

    @Autowired
    private TwoFactorMessageService twoFactorMessageService;

    @Autowired
    private UserService userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            HttpSession session = request.getSession();

            String requestURI = request.getRequestURI();
            if (requestURI.contains("/verify_2fa")) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = auth.getName();
            User user = userRepository.findByUsername(username);

            // Verificar si el usuario tiene 2FA activado
            if (user != null && user.isTwoFactorEnabled()) {
                Boolean isTwoFactorVerified = (Boolean) session.getAttribute("2FA_VERIFIED");
                if (Boolean.TRUE.equals(isTwoFactorVerified)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Boolean isCodeSent = (Boolean) session.getAttribute("2FA_CODE_SENT");
                if (isCodeSent == null || !isCodeSent) {
                    String email = user.getEmail();
                    if (email != null) {
                        String generatedCode = twoFactorMessageService.generateVerificationCode();
                        user.setSecret(generatedCode);
                        userRepository.save(user);

                        try {
                            twoFactorMessageService.sendVerificationCode(email, generatedCode);
                        } catch (MessagingException e) {
                            log.severe("Error al enviar el código de verificación: " + e.getMessage());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al enviar el código de verificación.");
                            return;
                        }

                        session.setAttribute("2FA_CODE_SENT", true);
                    }
                }

                response.sendRedirect("/verify_2fa");
                return;
            }

            // Si 2FA no está habilitado, continuar sin redirigir
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }


    private boolean isTwoFactorVerified(HttpServletRequest request) {
        // Revisamos la cookie para ver si el 2FA está verificado
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("2FA_STATUS".equals(cookie.getName()) && "VERIFIED".equals(cookie.getValue())) {
                    return true; // El 2FA está verificado
                }
            }
        }
        return false; // El 2FA no está verificado
    }


}

