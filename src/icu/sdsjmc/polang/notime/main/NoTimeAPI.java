package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.player.SbPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

    /**
     * 判断玩家是否应该被拦截（踢出/禁止登录）。
     * 根据黑名单/白名单配置进行判断。
     *
     * @param playerName 玩家名称
     * @return true 表示应该被拦截
     */
    public static boolean shouldBlockPlayer(String playerName) {
        List<?> blacklist = config.getList("notime.blacklist", Collections.emptyList());
        boolean useBlacklist = blacklist != null && !blacklist.isEmpty()
                && !String.valueOf(blacklist.get(0)).trim().isEmpty();

        if (useBlacklist) {
            // 黑名单模式：仅黑名单中的玩家被拦截
            return blacklist.contains(playerName);
        } else {
            // 白名单模式：不在白名单中的玩家被拦截
            return !config.getStringList("notime.whitelist").contains(playerName);
        }
    }



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
                if (DAY == getWeekInt(day)) continue;
                else break;
            }
            if (subcommand.startsWith("月=")) {
                String day = subcommand.substring(subcommand.indexOf("=") + 1);
                if (number == getMonthly(day)) continue;
                else break;
            }
            if (subcommand.equalsIgnoreCase("@s")) {
                notime.getServer().shutdown();
                return;
            }
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
            // 修复：非 @ 前缀的普通命令用 break 而非 return，
            // 避免跳过当前子命令后面的所有分号分隔的命令
            if (!condition.startsWith("@"))
            {
                console(papiCommand);
                continue; // 只跳过当前子命令，不影响后续命令
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
            }
        }
    }

    // 正则表达式预编译为静态常量，避免每次调用 parseTime() 时重复编译
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public static long parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            throw new IllegalArgumentException(NoTime.notimeTitle + " §c无效的时间格式: §e" + timeString);
        }
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        long totalDelay = 0;
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                throw new IllegalArgumentException(NoTime.notimeTitle + " §c无效的时间格式: §e" + timeString);
            }
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
            lastEnd = matcher.end();
        }

        // 确保整个字符串被成功解析，否则抛出异常
        if (lastEnd != timeString.length()) {
            throw new IllegalArgumentException(NoTime.notimeTitle + " §c无效的时间格式: §e" + timeString);
        }
        return totalDelay;
    }

    // 使用共享的 shouldBlockPlayer 方法统一判断黑白名单，避免重复代码
    public static void kickPlayers(String string) //踢出在线非白名单玩家
    {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (shouldBlockPlayer(p.getName())) {
                p.kickPlayer(string);
            }
        }
    }

    public static void console(String command) // 后台执行命令
    {
        NoTime.getFoliaLib().getScheduler().runNextTick((wrappedTask ->
                notime.getServer().dispatchCommand(
                        notime.getServer().getConsoleSender(),
                        command.replace("&", "§").replace("§§", "&"))));
    }

    public static void console(String command, Player player) // 后台执行命令
    {
        NoTime.getFoliaLib().getScheduler().runNextTick((wrappedTask ->
                notime.getServer().dispatchCommand(player, command
                        .replace("&", "§")
                        .replace("§§", "&"))
        ));
    }

    /**
     * 计算距离指定任务下次执行的时间（秒）
     *
     * @param taskName 任务名称
     * @return 距离下次执行的秒数
     */
    public static long getTimeNextExecution(String taskName) {
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
            // 对于循环任务，计算剩余时间直到下一次执行
            String fortime = config.getString(path + ".fortime");
            long interval = parseTime(fortime);
            if (interval == 0) return 0;

            // 获取任务上次执行的时间戳
            Long lastRunTime = NoTime.lastRunTimeForTasks.get(taskName);
            if (lastRunTime == null) {
                // 如果没有记录上次执行时间，返回总间隔时间
                return TimeUnit.NANOSECONDS.toSeconds(interval);
            }

            // 计算从上次执行到现在经过的时间
            long elapsed = System.nanoTime() - lastRunTime;
            // 计算剩余时间
            long remaining = interval - elapsed;
            // 如果剩余时间小于等于0，说明即将执行或刚执行完，返回0
            if (remaining <= 0) {
                return 0;
            }

            // 将纳秒转换为秒
            return TimeUnit.NANOSECONDS.toSeconds(remaining);
        }

        return 0;
    }
}
