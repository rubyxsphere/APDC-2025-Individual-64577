package pt.unl.fct.di.adc.projeto.core;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.projeto.util.AuthToken;

public class AuthValidator {

    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ERROR_MISSING_TOKEN = "Missing token";
    private static final String ERROR_INVALID_TOKEN = "Invalid or expired token";
    private static final String ERROR_INVALID_HEADER = "Missing or invalid Authorization header (must start with 'Bearer ')";

    private AuthValidator() {
        throw new IllegalStateException("Utility class cannot be instantiated.");
    }

    public static AuthToken validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new WebApplicationException(ERROR_MISSING_TOKEN, Response.Status.UNAUTHORIZED);
        }

        AuthToken authToken = AuthToken.verifyToken(token);
        if (authToken == null) {
            throw new WebApplicationException(ERROR_INVALID_TOKEN, Response.Status.UNAUTHORIZED);
        }

        return authToken;
    }

    public static AuthToken extractFromHeader(HttpHeaders headers) {
        String authorizationHeader = headers.getHeaderString(AUTH_HEADER_NAME);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new WebApplicationException(ERROR_INVALID_HEADER, Response.Status.UNAUTHORIZED);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        return validateToken(token);
    }
}