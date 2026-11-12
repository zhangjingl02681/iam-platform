package com.enterprise.iam;

import com.enterprise.iam.support.EmbeddedPostgresSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IamPlatformApplicationTests extends EmbeddedPostgresSupport {

    @Test
    void contextLoads() {
    }
}
