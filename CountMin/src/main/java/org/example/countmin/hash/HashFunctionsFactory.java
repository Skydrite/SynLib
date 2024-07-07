package org.example.countmin.hash;

public class HashFunctionsFactory {

    public static HashFunctions<?> createHashFunctions(String dataType) {
        return switch (dataType) {
            case "Long" -> new HashFunctionsLong();
            case "String" -> new HashFunctionsString();
            default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
        };
    }
}
