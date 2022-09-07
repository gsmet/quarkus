package io.quarkus.it.mongodb.discriminator.jsonb;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import io.quarkus.it.mongodb.discriminator.Car;
import io.quarkus.it.mongodb.discriminator.Moto;
import io.quarkus.it.mongodb.discriminator.Vehicle;

public class VehicleDeserializer implements JsonbDeserializer<Vehicle> {
    @Override
    public Vehicle deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        JsonObject json = parser.getObject();
        String type = json.getString("type");
        switch (type) {
            case "CAR":
                Car car = new Car();
                car.type = type;
                car.seatNumber = json.getInt("seatNumber");
                car.name = json.getString("name");
                return car;
            case "MOTO":
                Moto moto = new Moto();
                moto.type = type;
                moto.name = json.getString("name");
                moto.sideCar = json.getBoolean("sideCar");
                return moto;
            default:
                throw new RuntimeException("Type " + type + "not managed");
        }
    }
}
