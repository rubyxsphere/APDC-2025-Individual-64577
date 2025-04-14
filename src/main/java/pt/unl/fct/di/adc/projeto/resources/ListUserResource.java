package pt.unl.fct.di.adc.projeto.resources;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.projeto.core.AuthValidator;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;
import pt.unl.fct.di.adc.projeto.util.AuthToken;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/listUsers")
@Produces(MediaType.APPLICATION_JSON)
public class ListUserResource {
    private final Datastore datastore = DatastoreService.get();

    private static final String NOT_DEFINED = "NOT DEFINED";
    private static final String KIND_USER = "User";
    private static final String ROLE_ENDUSER = "ENDUSER";
    private static final String ROLE_BACKOFFICE = "BACKOFFICE";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String STATE_ACTIVE = "ACTIVATED";
    private static final String PROFILE_PUBLIC = "PUBLIC";

    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_USERNAME = "username";
    private static final String PROPERTY_FULL_NAME = "fullName";
    private static final String PROPERTY_PHONE = "phone";
    private static final String PROPERTY_PROFILE = "profile";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_STATE = "state";
    private static final String PROPERTY_CC_NUMBER = "ccNumber";
    private static final String PROPERTY_NIF = "nif";
    private static final String PROPERTY_EMPLOYER = "employer";
    private static final String PROPERTY_JOB = "job";
    private static final String PROPERTY_ADDRESS = "address";
    private static final String PROPERTY_EMPLOYER_NIF = "employerNIF";
    private static final String PROPERTY_PHOTO = "photo";

    @POST
    public Response listUsers(@Context HttpHeaders headers) {
        AuthToken token;
        try {
            token = AuthValidator.extractFromHeader(headers);
            if (token == null) {
                throw new WebApplicationException("Invalid or missing token", Response.Status.UNAUTHORIZED);
            }
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }

        EntityQuery query;
        EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder().setKind(KIND_USER);
        String requesterRole = token.getRole();
        StructuredQuery.Filter roleFilter;

        try {
            switch (requesterRole) {
                case ROLE_ENDUSER:
                    roleFilter = CompositeFilter.and(
                            PropertyFilter.eq(PROPERTY_ROLE, ROLE_ENDUSER),
                            PropertyFilter.eq(PROPERTY_STATE, STATE_ACTIVE),
                            PropertyFilter.eq(PROPERTY_PROFILE, PROFILE_PUBLIC)
                    );
                    queryBuilder.setFilter(roleFilter);
                    break;

                case ROLE_BACKOFFICE:
                    roleFilter = PropertyFilter.eq(PROPERTY_ROLE, ROLE_ENDUSER);
                    queryBuilder.setFilter(roleFilter);
                    break;

                case ROLE_ADMIN:
                    break;

                default:
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("success", false, "error", "User has an invalid role for this operation"))
                            .build();
            }

            query = queryBuilder.build();

            QueryResults<Entity> results = datastore.run(query);

            List<Map<String, Object>> userList = new ArrayList<>();
            while (results.hasNext()) {
                Entity userEntity = results.next();
                Map<String, Object> userData = new LinkedHashMap<>();

                if (ROLE_ENDUSER.equals(requesterRole)) {
                    addPropertyIfExists(userData, userEntity, PROPERTY_USERNAME);
                    addPropertyIfExists(userData, userEntity, PROPERTY_EMAIL);
                    addPropertyIfExists(userData, userEntity, PROPERTY_FULL_NAME);
                } else {
                    addPropertyIfExists(userData, userEntity, PROPERTY_EMAIL);
                    addPropertyIfExists(userData, userEntity, PROPERTY_USERNAME);
                    addPropertyIfExists(userData, userEntity, PROPERTY_FULL_NAME);
                    addPropertyIfExists(userData, userEntity, PROPERTY_PHONE);
                    addPropertyIfExists(userData, userEntity, PROPERTY_PROFILE);
                    addPropertyIfExists(userData, userEntity, PROPERTY_ROLE);
                    addPropertyIfExists(userData, userEntity, PROPERTY_STATE);
                    addPropertyIfExists(userData, userEntity, PROPERTY_CC_NUMBER);
                    addPropertyIfExists(userData, userEntity, PROPERTY_NIF);
                    addPropertyIfExists(userData, userEntity, PROPERTY_EMPLOYER);
                    addPropertyIfExists(userData, userEntity, PROPERTY_JOB);
                    addPropertyIfExists(userData, userEntity, PROPERTY_ADDRESS);
                    addPropertyIfExists(userData, userEntity, PROPERTY_EMPLOYER_NIF);
                    addPropertyIfExists(userData, userEntity, PROPERTY_PHOTO);
                }
                userList.add(userData);
            }

            return Response.ok(Map.of("success", true, "users", userList)).build();

        } catch (DatastoreException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to retrieve user list due to a server error."))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "An unexpected error occurred."))
                    .build();
        }
    }

    private void addPropertyIfExists(Map<String, Object> map, Entity entity, String propertyName) {
        if (entity.contains(propertyName)) {
            Value<?> value = entity.getValue(propertyName);
            if (value instanceof StringValue) {
                String strVal = entity.getString(propertyName);
                map.put(propertyName, strVal == null || strVal.isEmpty() ? NOT_DEFINED : strVal);
            } else if (value instanceof LongValue) {
                map.put(propertyName, entity.getLong(propertyName));
            } else if (value instanceof BooleanValue) {
                map.put(propertyName, entity.getBoolean(propertyName));
            } else if (value instanceof TimestampValue) {
                map.put(propertyName, entity.getTimestamp(propertyName).toString());
            } else if (value instanceof NullValue || value == null) {
                map.put(propertyName, NOT_DEFINED);
            } else {
                try {
                    Object objValue = value.get();
                    map.put(propertyName, objValue != null ? objValue.toString() : NOT_DEFINED);
                } catch (Exception e) {
                    map.put(propertyName, NOT_DEFINED);
                }
            }
        } else {
            map.put(propertyName, NOT_DEFINED);
        }
    }
}