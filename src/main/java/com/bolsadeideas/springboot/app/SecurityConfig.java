package com.bolsadeideas.springboot.app;
import com.bolsadeideas.springboot.app.apisms.d2fa.TwoFactorAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf().ignoringAntMatchers("/api/**")
                .and()
                .authorizeRequests()
                .antMatchers("/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/login", "/logout").permitAll()
                .antMatchers("/api/**").permitAll() // Permitir acceso temporal a la API
                .antMatchers("/verify_2fa").permitAll()
                .antMatchers("/roles/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/google/drive/preview/**").authenticated()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/listar", true)
                .failureUrl("/login?error=true")
                .permitAll()
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                .and()
                .headers()
                .contentSecurityPolicy(
  "default-src 'self'; " +
  "script-src 'self' 'unsafe-inline' https://accounts.google.com https://apis.google.com https://cdn.ckeditor.com https://code.jquery.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net/npm/chart.js https://cdn.datatables.net https://stackpath.bootstrapcdn.com; " +
  "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://code.jquery.com https://cdn.datatables.net https://stackpath.bootstrapcdn.com; " +
  "img-src 'self' data: https: blob:; " +
  "font-src 'self' https://cdnjs.cloudflare.com; " +
  "connect-src 'self' https://accounts.google.com https://www.googleapis.com https://www.google.com https://upload.cloudinary.com; " +
  "frame-src 'self' https://accounts.google.com https://apis.google.com https://docs.google.com; " +
  "child-src 'self' https://accounts.google.com https://apis.google.com https://docs.google.com;"
)
                .and()
                .addHeaderWriter((request, response) -> {
                    response.addHeader("Permissions-Policy", "identity-credentials-get=()");
                    response.addHeader("X-Content-Type-Options", "nosniff");
                    response.addHeader("X-Frame-Options", "SAMEORIGIN");
                    response.addHeader("X-XSS-Protection", "1; mode=block");
                })
                .and()
                .addFilterBefore(twoFactorAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    private AuthenticationSuccessHandler successHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/listar");
        return handler;
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error=true");
    }

    @Bean
    public TwoFactorAuthenticationFilter twoFactorAuthenticationFilter() {
        return new TwoFactorAuthenticationFilter();
    }



    @Bean
    public HttpFirewall allowDoubleSlashes() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }
}
