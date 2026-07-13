package bg.rezerv.cas.web.error;

import java.time.Instant;

/** Единен формат на грешките за всички REZERV сервизи (REZERV.md, секция 2.6). */
public record ErrorResponse(
        int status,
        String code,
        String message,
        String correlationId,
        Instant timestamp) {
}
