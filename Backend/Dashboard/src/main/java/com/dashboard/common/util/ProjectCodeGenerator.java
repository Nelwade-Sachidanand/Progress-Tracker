package com.dashboard.common.util;

public class ProjectCodeGenerator {

    private ProjectCodeGenerator() {
    }

    public static String generateProjectCode(
            String projectName,
            long count) {

        String prefix =
                projectName.replaceAll("[^A-Za-z]", "")
                           .toUpperCase();

        if (prefix.length() >= 3) {
            prefix = prefix.substring(0, 3);
        } else {
            while (prefix.length() < 3) {
                prefix += "X";
            }
        }

        return prefix +
                String.format("%03d", count + 1);
    }
}