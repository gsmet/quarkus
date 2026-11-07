package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.StdSerializer;

@Singleton
public class GeneratedSerializersRegister implements JsonMapperBuilderCustomizer {

    private static volatile SimpleModule mappingModule;
    private static volatile ExactSerializers serializers;

    static {
        reset();
    }

    /**
     * Drops the registered serializers and deserializers, they are registered again when the application starts.
     * Required as this class may be shared by several applications (dev mode restarts, tests).
     */
    public static void reset() {
        ExactSerializers newSerializers = new ExactSerializers();
        SimpleModule newModule = new SimpleModule();
        // Use a custom SimpleSerializers to use a json serializer only if it has been generated for that
        // exact class and not one of its sublclasses. This is already the default behaviour for deserializers.
        newModule.setSerializers(newSerializers);
        serializers = newSerializers;
        mappingModule = newModule;
    }

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.addModule(mappingModule);
    }

    public static void addSerializer(Class<? extends StdSerializer> serClass) {
        try {
            StdSerializer serializer = serClass.getConstructor().newInstance();
            serializers.addExactSerializer(serializer.handledType(), serializer);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addDeserializer(Class<? extends StdDeserializer> deserClass) {
        try {
            StdDeserializer deserializer = deserClass.getConstructor().newInstance();
            mappingModule.addDeserializer(deserializer.handledType(), deserializer);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExactSerializers extends SimpleSerializers {

        private final Map<Class<?>, ValueSerializer<?>> exactSerializers = new HashMap<>();

        public <T> void addExactSerializer(Class<? extends T> type, ValueSerializer<T> ser) {
            exactSerializers.put(type, ser);
        }

        @Override
        public ValueSerializer<?> findSerializer(SerializationConfig config,
                JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides) {
            ValueSerializer<?> exactSerializer = exactSerializers.get(type.getRawClass());
            return exactSerializer != null ? exactSerializer : super.findSerializer(config, type, beanDescRef, formatOverrides);
        }
    }
}
