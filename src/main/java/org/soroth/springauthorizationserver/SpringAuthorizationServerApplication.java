package org.soroth.springauthorizationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SpringBootApplication
public class SpringAuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAuthorizationServerApplication.class, args);
    }



}
