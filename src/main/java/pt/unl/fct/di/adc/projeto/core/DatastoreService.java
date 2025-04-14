package pt.unl.fct.di.adc.projeto.core;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

public final class DatastoreService {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public static Datastore get() {
        return datastore;
    }
}