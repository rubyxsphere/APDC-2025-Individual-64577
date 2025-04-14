package pt.unl.fct.di.adc.projeto.resources;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;

import java.util.Map;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON)
public class LogoutResource {

    private static final String KIND_AUTH_TOKEN = "AuthToken";

    private static final String ERROR_INVALID_TOKEN = "Invalid or missing token";

    private static final String SUCCESS_LOGGED_OUT = "Logged out successfully";

    private final Datastore datastore = DatastoreService.get();
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind(KIND_AUTH_TOKEN);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpHeaders headers) {

        AuthToken authToken = AuthValidator.extractFromHeader(headers);
        if (authToken == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", ERROR_INVALID_TOKEN))
                    .build();
        }

        try {
            Key tokenKey = tokenKeyFactory.newKey(authToken.getToken());

            datastore.delete(tokenKey);

            return Response.ok(Map.of("success", true, "message", SUCCESS_LOGGED_OUT)).build();

        } catch (Exception e) {

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to invalidate token"))
                    .build();
        }
    }
}