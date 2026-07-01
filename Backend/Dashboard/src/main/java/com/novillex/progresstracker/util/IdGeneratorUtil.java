package com.novillex.progresstracker.util;

import java.util.UUID;

public final class IdGeneratorUtil {

    private IdGeneratorUtil() {
    }

    public static String generatePhaseId() {
        return "PH-" + UUID.randomUUID();
    }

    public static String generateMilestoneId() {
        return "MS-" + UUID.randomUUID();
    }

    public static String generateTaskId() {
        return "TK-" + UUID.randomUUID();
    }

    public static String generateSubTaskId() {
        return "ST-" + UUID.randomUUID();
    }

    public static String generateActivityId() {
        return "ACT-" + UUID.randomUUID();
    }
}