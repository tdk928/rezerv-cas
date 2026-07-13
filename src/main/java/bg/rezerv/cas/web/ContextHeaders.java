package bg.rezerv.cas.web;

/** Header имена, закачани от rezerv-gateway (виж REZERV.md, секция 2.3). */
public final class ContextHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String COMPANY_ID = "X-Company-Id";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private ContextHeaders() {
    }
}
