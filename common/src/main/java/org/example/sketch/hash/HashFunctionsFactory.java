package org.example.sketch.hash;

public class HashFunctionsFactory {

    public static HashFunctions createHashFunctions(String dataType) {
        return switch (dataType.toLowerCase()) {
            case "long" -> new LongHashFunctions();
            case "string" -> new StringHashFunctions();
            default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
        };
    }
}
