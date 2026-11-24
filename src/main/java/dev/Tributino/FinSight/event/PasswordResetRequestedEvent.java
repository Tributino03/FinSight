package dev.Tributino.FinSight.event;

import java.time.Duration;

public record PasswordResetRequestedEvent(
        String email,
        String rawToken,
        Duration ttl
) {
    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[email=" + email + ", ttl=" + ttl + "]";
    }
}