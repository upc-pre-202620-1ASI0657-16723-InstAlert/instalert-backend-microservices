package com.instalert_backend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@SpringBootApplication(exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
public class CommunitiesApplication {public static void main(String[] args){SpringApplication.run(CommunitiesApplication.class,args);}}
