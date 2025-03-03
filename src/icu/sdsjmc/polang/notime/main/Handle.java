package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static icu.sdsjmc.polang.notime.NoTime.config;


public class Handle {
    static NoTime notime = NoTime.instance;
    static String title = NoTime.notime;

    public static boolean checkTime() //判断是否在防沉迷时间内
    {
        int start = LocalTime.parse(config.getString("notime.start")).toSecondOfDay();
        int end = LocalTime.parse(config.getString("notime.end")).toSecondOfDay();
        int time = LocalTime.now().toSecondOfDay();
        if (config.getBoolean("notime.opposite", false)) {
            //当前时间小于起始时间且当前时间大于结束时间
            return time <= start || time >= end;
        } else {
            //当前时间大于开始时间，且当前时间小于结束时间
            return time >= start && time <= end;
        }
    }

    public static int getWeekInt(String string) {
        if (string.matches("[0-9]*")) {
            return Integer.parseInt(string);
        } else {
            switch (string) {
                case "星期一":
                    return 1;
                case "星期二":
                    return 2;
                case "星期三":
                    return 3;
                case "星期四":
                    return 4;
                case "星期五":
                    return 5;
                case "星期六":
                    return 6;
                case "星期日":
                    return 7;
                default:
                    notime.getLogger().info(NoTime.notime + "§c日判断的时间格式不对！");
            }
        }
        return 0;
    }
    public static int getMonthly(String string) {
        try {
            int day = Integer.parseInt(string);
            if (day >= 1 && day <= 31) {
                return day;
            } else {
                notime.getLogger().info(NoTime.notime + day + " §c是几号？这是哪个国家的日历");
                return 0; // 或者你可以选择抛出异常，或者返回一个特定的错误码
            }
        } catch (NumberFormatException e) {
            notime.getLogger().info(NoTime.notime + " §c这是数字吗...");
            return 0; // 或者你可以选择抛出异常，或者返回一个特定的错误码
        }
    }

//    public static String parseTime(String timeString, String key) {
//        switch (timeString) {
//            case "s":
//                return String.valueOf(NoTime.instance.getTimeUntilNextExecution(key));
//            case "m":
//                return String.valueOf(NoTime.instance.getTimeUntilNextExecution(key).toMinutes());
//            case "h":
//                String.valueOf(NoTime.instance.getTimeUntilNextExecution(key).toHours());
//            case "d":
//                String.valueOf(NoTime.instance.getTimeUntilNextExecution(key).toDays());
//        }
//        return null;
//    }

    public static long parseTime(String timeString) {
        // 使用正则表达式匹配数字加单位的模式，如 10m, 5s 等
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(timeString);
        long totalDelay = 0;

        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1)); // 提取数字部分
            char unit = matcher.group(2).charAt(0); // 提取单位

            switch (unit) {
                case 's':
                    totalDelay += TimeUnit.SECONDS.toNanos(amount);
                    break;
                case 'm':
                    totalDelay += TimeUnit.MINUTES.toNanos(amount);
                    break;
                case 'h':
                    totalDelay += TimeUnit.HOURS.toNanos(amount);
                    break;
                case 'd':
                    totalDelay += TimeUnit.DAYS.toNanos(amount);
                    break;
                // 如果有其他单位，可以在这里添加
                default:
                    throw new IllegalArgumentException(NoTime.notime + " §c错误的时间单位: §e" + unit);
            }
        }

        // 确保整个字符串被成功解析，否则抛出异常
        if (!matcher.hitEnd()) {
            throw new IllegalArgumentException(NoTime.notime + " §c无效的时间格式: §e" + timeString);
        }

        return totalDelay;
    }
}
