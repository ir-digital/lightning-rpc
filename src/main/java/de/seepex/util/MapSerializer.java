package de.seepex.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public class MapSerializer extends StdSerializer<Map> {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public static final String SEPARATOR = "##SPLIT##";
    public static final String KVS = "##KVS##";
    private static final Gson GSON = new Gson();

    public MapSerializer() {
        super(Map.class);
    }

    @Override
    public void serialize(Map map, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        final Set<Map.Entry> entrySet = map.entrySet();
        StringBuilder builder = new StringBuilder();
        String csv = "";
        for (Map.Entry entry : entrySet) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            if(value == null) {
                LOG.error("Receive Map with null value for key {} to custom serialize -> {}", key, map);
                continue;
            }
            // There are to many : and , in Json to parse the fckng string
            // We split each key-value pair with KVS    (key value separator)
            builder.append(csv).append(
                    Base64.getEncoder().encodeToString(
                            GSON.toJson(key).getBytes()
                    )).append(KVS).append(
                    Base64.getEncoder().encodeToString(
                            GSON.toJson(value).getBytes()
                    ));
            // each row with SEPARATOR
            csv = SEPARATOR;
        }

        gen.writeStartObject();
        gen.writeStringField(map.getClass().getName(), builder.toString());
        gen.writeEndObject();
    }
}
