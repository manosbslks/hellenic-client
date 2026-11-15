package gr.hellenic.client.scripting.types;

import gr.hellenic.client.scripting.HiddenMethod;
import com.zergatul.scripting.type.CustomType;

import java.util.UUID;

@CustomType(name = "UUID")
public class UUIDWrapper {

    private final UUID id;

    public UUIDWrapper(UUID id) {
        this.id = id;
    }

    @HiddenMethod
    public UUID getRaw() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}