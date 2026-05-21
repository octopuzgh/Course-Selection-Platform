package com.octopuz.selectionservice.context;

public class UserContext {

    private static final ThreadLocal<String> OPERATOR = new ThreadLocal<>();

    public static void setOperator(String operator) {
        OPERATOR.set(operator);
    }

    public static String getOperator() {
        return OPERATOR.get();
    }

    public static void clear() {
        OPERATOR.remove();
    }
}