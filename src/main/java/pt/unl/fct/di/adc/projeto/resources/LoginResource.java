package pt.unl.fct.di.adc.projeto.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;
import pt.unl.fct.di.adc.projeto.requests.LoginRequest;

import java.util.Map;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {

    private static final String KIND_USER = "User";
    private static final String KIND_USER_LOG = "UserLog";
    private static final String KIND_USER_STATS = "UserStats";
    private static final String KIND_AUTH_TOKEN = "AuthToken";

    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_USER_LOGIN_TIME = "user_login_time";
    private static final String PROPERTY_LOG_IP = "user_login_ip";
    private static final String PROPERTY_LOG_HOST = "user_login_host";
    private static final String PROPERTY_LOG_CITY = "user_login_city";
    private static final String PROPERTY_LOG_COUNTRY = "user_login_country";
    private static final String PROPERTY_LOG_LATLON = "user_login_latlon";
    private static final String PROPERTY_LOG_TIME = "user_login_time";
    private static final String PROPERTY_STATS_LOGINS = "user_stats_logins";
    private static final String PROPERTY_STATS_FAILED = "user_stats_failed";
    private static final String PROPERTY_STATS_FIRST_LOGIN = "user_first_login";
    private static final String PROPERTY_STATS_LAST_LOGIN = "user_last_login";
    private static final String PROPERTY_STATS_LAST_ATTEMPT = "user_last_attempt";

    private static final String STATS_KEY_NAME = "counters";

    private static final long TOKEN_VALIDITY_SECONDS = 3600; // 1 hour

    private static final String HEADER_AE_CITY = "X-AppEngine-City";
    private static final String HEADER_AE_COUNTRY = "X-AppEngine-Country";
    private static final String HEADER_AE_LATLON = "X-AppEngine-CityLatLong";

    private static final String ERROR_MISSING_CREDENTIALS = "Missing credentials";
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid credentials";
    private static final String ERROR_DATASTORE_FAILED = "Datastore operation failed";

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind(KIND_USER);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest data,
                          @Context HttpServletRequest request,
                          @Context HttpHeaders headers) {

        if (data == null || data.getEmail() == null || data.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", ERROR_MISSING_CREDENTIALS))
                    .build();
        }

        Key userKey = userKeyFactory.newKey(data.getEmail());
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                txn.rollback();
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("success", false, "error", ERROR_INVALID_CREDENTIALS))
                        .build();
            }

            String storedHashedPassword = user.getString(PROPERTY_PASSWORD);
            if (!storedHashedPassword.equals(DigestUtils.sha512Hex(data.getPassword()))) {
                txn.rollback();
                updateStatsOnFailure(data.getEmail());
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("success", false, "error", ERROR_INVALID_CREDENTIALS))
                        .build();
            }

            Timestamp loginTimestamp = Timestamp.now();

            Entity updatedUser = Entity.newBuilder(user)
                    .set(PROPERTY_USER_LOGIN_TIME, loginTimestamp)
                    .build();

            Key logKey = datastore.allocateId(
                    datastore.newKeyFactory()
                            .addAncestor(PathElement.of(KIND_USER, data.getEmail()))
                            .setKind(KIND_USER_LOG)
                            .newKey()
            );
            Entity logEntity = Entity.newBuilder(logKey)
                    .set(PROPERTY_LOG_IP, request.getRemoteAddr())
                    .set(PROPERTY_LOG_HOST, request.getRemoteHost())
                    .set(PROPERTY_LOG_CITY, headers.getHeaderString(HEADER_AE_CITY))
                    .set(PROPERTY_LOG_COUNTRY, headers.getHeaderString(HEADER_AE_COUNTRY))
                    .set(PROPERTY_LOG_LATLON, headers.getHeaderString(HEADER_AE_LATLON))
                    .set(PROPERTY_LOG_TIME, loginTimestamp)
                    .build();

            Key statsKey = datastore.newKeyFactory()
                    .addAncestor(PathElement.of(KIND_USER, data.getEmail()))
                    .setKind(KIND_USER_STATS)
                    .newKey(STATS_KEY_NAME);

            Entity currentStats = txn.get(statsKey);
            Entity statsEntity;
            if (currentStats != null) {
                statsEntity = Entity.newBuilder(currentStats)
                        .set(PROPERTY_STATS_LOGINS, currentStats.getLong(PROPERTY_STATS_LOGINS) + 1)
                        .set(PROPERTY_STATS_LAST_LOGIN, loginTimestamp)
                        .set(PROPERTY_STATS_FAILED, 0L)
                        .build();
            } else {
                statsEntity = Entity.newBuilder(statsKey)
                        .set(PROPERTY_STATS_LOGINS, 1L)
                        .set(PROPERTY_STATS_FAILED, 0L)
                        .set(PROPERTY_STATS_FIRST_LOGIN, loginTimestamp)
                        .set(PROPERTY_STATS_LAST_LOGIN, loginTimestamp)
                        .build();
            }

            long validFrom = loginTimestamp.getSeconds();
            long validTo = validFrom + TOKEN_VALIDITY_SECONDS;
            AuthToken token = new AuthToken(
                    data.getEmail(),
                    user.getString(PROPERTY_ROLE),
                    validFrom,
                    validTo
            );

            Entity tokenEntity = token.toDatastoreEntity();

            txn.put(updatedUser, logEntity, statsEntity, tokenEntity);
            txn.commit();

            return Response.ok(Map.of("success", true, "token", token)).build();

        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ERROR_DATASTORE_FAILED)).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private void updateStatsOnFailure(String email) {
        Key statsKey = datastore.newKeyFactory()
                .addAncestor(PathElement.of(KIND_USER, email))
                .setKind(KIND_USER_STATS)
                .newKey(STATS_KEY_NAME);

        Transaction txn = datastore.newTransaction();
        try {
            Entity stats = txn.get(statsKey);
            if (stats != null) {
                Entity updatedStats = Entity.newBuilder(stats)
                        .set(PROPERTY_STATS_FAILED, stats.getLong(PROPERTY_STATS_FAILED) + 1)
                        .set(PROPERTY_STATS_LAST_ATTEMPT, Timestamp.now())
                        .build();
                txn.put(updatedStats);
                txn.commit();
            } else {
                txn.rollback();
            }
        } catch (DatastoreException e) {
            if (txn.isActive()) {
                txn.rollback();
            }
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}