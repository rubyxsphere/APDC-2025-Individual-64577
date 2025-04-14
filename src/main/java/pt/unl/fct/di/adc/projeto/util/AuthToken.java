package pt.unl.fct.di.adc.projeto.util;

import com.google.cloud.datastore.*;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;

public class AuthToken {

    private static final String KIND_AUTH_TOKEN = "AuthToken";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_VALID_FROM = "validFrom";
    private static final String PROPERTY_VALID_TO = "validTo";

    private static final Datastore datastore = DatastoreService.get();
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(KIND_AUTH_TOKEN);

    private final String email;
    private final String role;
    private final long validFrom;
    private final long validTo;
    private final String token;

    public AuthToken(String email, String role, long validFrom, long validTo) {
        this(email, role, validFrom, validTo, java.util.UUID.randomUUID().toString());
    }

    private AuthToken(String email, String role, long validFrom, long validTo, String token) {
        this.email = email;
        this.role = role;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.token = token;
    }

    public Entity toDatastoreEntity() {
        Key key = tokenKeyFactory.newKey(this.token);
        return Entity.newBuilder(key)
                .set(PROPERTY_EMAIL, this.email)
                .set(PROPERTY_ROLE, this.role)
                .set(PROPERTY_VALID_FROM, this.validFrom)
                .set(PROPERTY_VALID_TO, this.validTo)
                .build();
    }

    public static AuthToken verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Key key = tokenKeyFactory.newKey(token);
        Entity entity = datastore.get(key);

        if (entity == null) {
            return null;
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        long validFromSeconds = entity.getLong(PROPERTY_VALID_FROM);
        long validToSeconds = entity.getLong(PROPERTY_VALID_TO);

        if (nowSeconds < validFromSeconds || nowSeconds > validToSeconds) {
            return null;
        }

        return new AuthToken(
                entity.getString(PROPERTY_EMAIL),
                entity.getString(PROPERTY_ROLE),
                validFromSeconds,
                validToSeconds,
                token
        );
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public long getValidFrom() {
        return validFrom;
    }

    public long getValidTo() {
        return validTo;
    }

}