package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.player.SbPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static icu.sdsjmc.polang.notime.NoTime.config;


public class NoTimeAPI {
    static NoTime notime = NoTime.instance;

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
            if (string.contains("一")) return 1;
            if (string.contains("二")) return 2;
            if (string.contains("三")) return 3;
            if (string.contains("四")) return 4;
            if (string.contains("五")) return 5;
            if (string.contains("六")) return 6;
            if (string.contains("日")) return 7;
            notime.getLogger().info(NoTime.notimeTitle + "§c日判断的时间格式不对！");
        }
        return 0;
    }

    public static int getMonthly(String string) {
        try {
            int day = Integer.parseInt(string);
            if (day >= 1 && day <= 31) {
                return day;
            } else {
                notime.getLogger().info(NoTime.notimeTitle + day + " §c是几号？这是哪个国家的日历");
                return 0;
            }
        } catch (NumberFormatException e) {
            notime.getLogger().info(NoTime.notimeTitle + " §c这是数字吗...");
            return 0;
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

    /**
     * 执行操作的方法
     */
    public static void operate(String command) {
        String[] subcommands = command.split(";");
        LocalDate today = LocalDate.now(); // 获取当前日期
        int DAY = today.getDayOfWeek().getValue();
        int number = today.getDayOfMonth();
        for (String subcommand : subcommands) {
            if (subcommand.startsWith("week=") || subcommand.startsWith("周=")) {
                String day = subcommand.substring(subcommand.indexOf("=") + 1);
                if (DAY == getWeekInt(day)) {
                    continue;
                } else {
                    break;
                }
            }
            if (subcommand.startsWith("月=")) {
                String day = subcommand.substring(subcommand.indexOf("=") + 1);
                if (number == getMonthly(day)) {
                    continue;
                } else {
                    break;
                }
            }
            if (subcommand.equalsIgnoreCase("@s")) {
                notime.getServer().shutdown();
            } else {
                //没空格就直接输出后台命令
                if (!subcommand.contains(" ")) {

                    console(subcommand);
                    continue;
                }
                String papiCommand = subcommand;
                String condition = subcommand.substring(0, subcommand.indexOf(' '));
                int q = 0;
                if (NoTime.papi) // 如果有papi插件就把文本改为解析papi的
                {
                    if (condition.equals("@papi")) {
                        q = 6;
                        String cq = papiCommand.substring(6);
                        condition = cq.substring(0, cq.indexOf(' '));
                        papiCommand = cq;
                    } else {
                        papiCommand = PlaceholderAPI.setPlaceholders(SbPlayer.player, subcommand);
                    }
                }
                switch (condition) {
                    case "@m": {
                        String msg = papiCommand.substring(3 + q);
                        notime.getServer().broadcastMessage(msg.replace("&", "§").replace("§§", "&"));
                        break;
                    }
                    case "@all_command": {
                        String msg = papiCommand.substring(13 + q);
                        for (Player player : notime.getServer().getOnlinePlayers()) {
                            console(msg, player);
                        }
                        break;
                    }
                    case "@all": {
                        String com = papiCommand.substring(5 + q);
                        for (Player player : notime.getServer().getOnlinePlayers()) {
                            console(com.replace("@name@", player.getName()));
                        }
                        break;
                    }
                    case "@k": {
                        String com = papiCommand.substring(3 + q);
                        kickPlayers(com);
                        break;
                    }
                    default: {
                        console(papiCommand);
                    }
                }
            }
        }
    }

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
                    throw new IllegalArgumentException(NoTime.notimeTitle + " §c错误的时间单位: §e" + unit);
            }
        }

        // 确保整个字符串被成功解析，否则抛出异常
        if (!matcher.hitEnd()) {
            throw new IllegalArgumentException(NoTime.notimeTitle + " §c无效的时间格式: §e" + timeString);
        }
        return totalDelay;
    }

    public static void kickPlayers(String string) //踢出在线非白名单玩家
    {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!config.getList("notime.whitelist").contains(p.getName())) {
                p.kickPlayer(string);
            }
        }
    }

    public static void console(String command) // 后台执行命令
    {
        notime.getServer().getScheduler().runTask(notime, () ->
                notime.getServer().dispatchCommand(notime.getServer().getConsoleSender(), command.replace("&", "§").replace("§§", "&")));
    }

    public static void console(String command, Player player) // 后台执行命令
    {
        notime.getServer().getScheduler().runTask(notime, () ->
                notime.getServer().dispatchCommand(player, command.replace("&", "§").replace("§§", "&")));
    }

    /**
     * 计算距离指定任务下次执行的时间（秒）
     *
     * @param taskName 任务名称
     * @return 距离下次执行的秒数
     */
    public static long getTimeUntilNextExecution(String taskName) {
        String path = "run." + taskName;

        // 处理单时间任务
        if (config.isString(path + ".time") && !config.isList(path + ".time")) {
            String timeString = config.getString(path + ".time");
            LocalTime taskTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("[HH:mm:ss][HH:mm]"));
            long timeDiff = taskTime.toSecondOfDay() - LocalTime.now().toSecondOfDay();

            // 如果时间已过，则加上一天
            if (timeDiff < 0) {
                timeDiff += 24 * 60 * 60;
            }

            return timeDiff;
        }

        // 处理多时间任务 - 返回最近的一次时间
        if (config.isList(path + ".time")) {
            List<String> timeStrings = config.getStringList(path + ".time");
            long minDiff = Long.MAX_VALUE;

            for (String timeString : timeStrings) {
                LocalTime taskTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("[HH:mm:ss][HH:mm]"));
                long timeDiff = taskTime.toSecondOfDay() - LocalTime.now().toSecondOfDay();

                // 如果时间已过，则加上一天
                if (timeDiff < 0) {
                    timeDiff += 24 * 60 * 60;
                }

                if (timeDiff < minDiff) {
                    minDiff = timeDiff;
                }
            }

            return minDiff == Long.MAX_VALUE ? 0 : minDiff;
        }

        // 处理循环任务
        if (config.isString(path + ".fortime")) {
            // 对于循环任务，返回下一次执行的时间间隔
            String fortime = config.getString(path + ".fortime");
            return TimeUnit.NANOSECONDS.toSeconds(parseTime(fortime));
        }

        return 0;
    }
}
