package com.enterprise.iam.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.iam.identity.service.TotpService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TotpServiceTests {

    @Test
    void generatesAndVerifiesCodeForFixedClockAndRejectsInvalidCode() {
        Clock clock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneOffset.UTC);
        TotpService totp = new TotpService(clock);
        String secret = totp.generateSecret();
        String code = totp.currentCode(secret);

        assertThat(secret).isNotBlank().doesNotContain(" ");
        assertThat(code).matches("\\d{6}");
        assertThat(totp.verify(secret, code)).isTrue();
        assertThat(totp.verify(secret, "000000")).isFalse();
        assertThat(totp.otpauthUri("Enterprise IAM", "alice", secret))
                .startsWith("otpauth://totp/")
                .contains(secret)
                .contains("alice");
    }

    @Test
    void acceptsCodesFromAdjacentTimeWindows() {
        Instant now = Instant.parse("2024-01-15T12:00:00Z");
        TotpService current = new TotpService(Clock.fixed(now, ZoneOffset.UTC));
        String secret = current.generateSecret();
        String previousWindow = new TotpService(Clock.fixed(now.minusSeconds(30), ZoneOffset.UTC)).currentCode(secret);
        String nextWindow = new TotpService(Clock.fixed(now.plusSeconds(30), ZoneOffset.UTC)).currentCode(secret);

        assertThat(current.verify(secret, previousWindow)).isTrue();
        assertThat(current.verify(secret, nextWindow)).isTrue();
        assertThat(current.verify(secret, new TotpService(Clock.fixed(now.plusSeconds(120), ZoneOffset.UTC)).currentCode(secret)))
                .isFalse();
    }
}
