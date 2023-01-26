package de.seepex.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PersistentCollectionCheckingTypeAdapter extends TypeAdapter<Collection> {

    private final Logger logger = LoggerFactory.getLogger(PersistentCollectionCheckingTypeAdapter.class);


    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (Collection.class.isAssignableFrom(type.getRawType())) {
                TypeAdapter delegate = gson.getDelegateAdapter(this, type);

                return (TypeAdapter<T>) new PersistentCollectionCheckingTypeAdapter(delegate);
            }

            return null;
        }
    };

    private final TypeAdapter delegate;

    PersistentCollectionCheckingTypeAdapter(TypeAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(JsonWriter out, Collection value) throws IOException {

        // this is probably ugly as hell, as we are using reflection to access a private property
        // but currently I have no idea how to get the length of already written json as it is
        // only inside the writer
        try {
            Field field = out.getClass().getDeclaredField("out");
            field.setAccessible(true);
            StringWriter stringWriter = (StringWriter) field.get(out);

            // means we are starting a new json
            if(stringWriter.getBuffer().length() == 0) {
                ContextAwareUtil.clear();
            }
        } catch (Exception e) {
            logger.error("Failed to access writer", e);
        }

        if (value == null) {
            out.nullValue();
            return;
        }

        Set visitedNodes = ContextAwareUtil.getObject();
        if (visitedNodes == null) {
            visitedNodes = new HashSet<>();
            ContextAwareUtil.setObject(visitedNodes);
        }

        // This catches non-proxied collections AND initialized proxied collections
        PersistenceUtil persistenceUtil = Persistence.getPersistenceUtil();
        if (persistenceUtil.isLoaded(value)) {

            if (!value.isEmpty()) {
                Optional first = value.stream().findFirst();
                Object object = first.get();
                Object id;
                try {
                    Method getId = object.getClass().getMethod("getId");
                    id = getId.invoke(object);
                } catch (Exception ex) {
                    delegate.write(out, value);
                    return;
                }

                if (visitedNodes.contains(id)) {
                    out.nullValue();
                    return;
                }

                visitedNodes.add(id);
            }

            delegate.write(out, value);
            return;
        }
        // write out null for uninitialized proxied collections
        out.nullValue();
    }

    @Override
    public Collection read(JsonReader in) throws IOException {
        return (Collection) delegate.read(in);
    }
}
