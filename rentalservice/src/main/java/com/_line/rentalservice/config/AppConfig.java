package com._line.rentalservice.config;

import com._line.rentalservice.filter.AuthTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
// Serialize Page<T> as a stable {content, page:{size,number,totalElements,totalPages}} shape.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfig {

    /** Kept out of SecurityConfig so services can depend on it without a bean cycle. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Runs the mock bank's delayed settlement and webhook retries. */
    @Bean(name = "mockBankScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler mockBankScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("mock-bank-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * AuthTokenFilter is a @Component so Boot would also register it as a plain servlet filter.
     * It must only run inside the security filter chain, so disable the automatic registration.
     */
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration(AuthTokenFilter filter) {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
