package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Param {

    private String name;
    private Object value;
    private Boolean forceObjectType;
    private String objectType;
    private String objectMapperCoding;

    private final Logger logger = LoggerFactory.getLogger(Param.class);

    public Param() {}

    public Param(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * when this method is used, receiver side will cast to the exact object that was submitted
     * this is required, when the method signature accepts an Interface and we want to cast to a specific
     * implementation of the interface
     *
     * Be aware the parameter, that you are submitting might need a
     *
     * @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")*
     *
     * annotation - especially if you are working with abstract classes
     *
     * @param name
     * @param value
     * @param forceObjectType
     */
    public Param(String name, Object value, boolean forceObjectType) {
        this.name = name;
        this.value = value;
        this.objectType = value.getClass().getName();
        this.forceObjectType = forceObjectType;

        try {
            this.objectMapperCoding = new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.error("Failed to encode", e);
        }
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public String getObjectMapperCoding() {
        return objectMapperCoding;
    }

    public Boolean getForceObjectType() {
        return forceObjectType != null ? forceObjectType : Boolean.FALSE;
    }

    public String getObjectType() {
        return objectType;
    }

    @Override
    public String toString() {
        return "Param{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", forceObjectType=" + forceObjectType +
                ", objectType='" + objectType + '\'' +
                ", objectMapperCoding='" + objectMapperCoding  + '\'' +
                '}';
    }
}
