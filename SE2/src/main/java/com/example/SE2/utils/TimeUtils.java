package com.example.SE2.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtils {

    public static String timeAgo(LocalDateTime date) {
        Duration duration = Duration.between(date, LocalDateTime.now());

        long seconds = duration.getSeconds();

        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        if (seconds < 2592000) return (seconds / 86400) + " days ago";

        return (seconds / 2592000) + " months ago";

    }
}
