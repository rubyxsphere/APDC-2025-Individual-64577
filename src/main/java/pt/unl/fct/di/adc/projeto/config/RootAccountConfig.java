package pt.unl.fct.di.adc.projeto.config;

import com.google.cloud.datastore.*;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.projeto.core.DatastoreService;

@WebListener
public class RootAccountConfig implements ServletContextListener {

    private static final String ROOT_EMAIL = "admin@root.com";
    private static final String ROOT_USERNAME = "root";
    private static final String ROOT_PASSWORD = "admin";
    private static final String ROOT_FULLNAME = "Administrator";
    private static final String ROOT_PROFILE = "PUBLIC";
    private static final String ROOT_ROLE = "ADMIN";
    private static final String ROOT_STATE = "ACTIVATED";

    private final Datastore datastore = DatastoreService.get();
    private boolean runInitialization = true;


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String environment = System.getProperty("com.google.appengine.runtime.environment");

        if ("Development".equals(environment)) {
            this.runInitialization = false;
        } else {
            this.runInitialization = true;
            initRootAccount();
        }
    }

    private void initRootAccount() {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(ROOT_EMAIL);
        Entity existingUser = datastore.get(userKey);

        if (existingUser == null) {
            String hashedPassword = DigestUtils.sha512Hex(ROOT_PASSWORD);

            Entity.Builder builder = Entity.newBuilder(userKey)
                    .set("email", ROOT_EMAIL)
                    .set("username", ROOT_USERNAME)
                    .set("fullName", ROOT_FULLNAME)
                    .set("password", hashedPassword)
                    .set("profile", ROOT_PROFILE)
                    .set("role", ROOT_ROLE)
                    .set("state", ROOT_STATE);

            datastore.put(builder.build());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}