package com.jeju.ormicamp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class APP {

    public static void main(String[] args) {
        SpringApplication.run(APP.class, args);
    }

}
