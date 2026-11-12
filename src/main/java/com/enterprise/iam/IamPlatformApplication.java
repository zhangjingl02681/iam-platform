package com.enterprise.iam;

import com.enterprise.iam.config.IamProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IamProperties.class)
public class IamPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamPlatformApplication.class, args);
    }
}
