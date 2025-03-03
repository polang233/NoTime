package icu.sdsjmc.polang.notime;

import icu.sdsjmc.polang.notime.hook.Metrics;
import icu.sdsjmc.polang.notime.main.Listener;
import icu.sdsjmc.polang.notime.main.command.main.NotimeCommand;
import icu.sdsjmc.polang.notime.main.data.ScheduleData;
import icu.sdsjmc.polang.notime.main.data.ScheduleDataTimes;
import icu.sdsjmc.polang.notime.main.player.SbPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static icu.sdsjmc.polang.notime.main.Handle.*;
import static icu.sdsjmc.polang.notime.main.Handle.parseTime;


public class NoTime extends JavaPlugin {
    public static NoTime instance;
    public static boolean noTimeEnable;
    public static ScheduledExecutorService executor;
    public static ScheduledExecutorService executorList;
    public static ScheduledExecutorService executorFor;
    public static FileConfiguration config;
    public static boolean papi;

    public static String kickMessage = null;

    public final static String notime = "§7[§2§lNo§a§lTime§7] ";

    {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        kickMessage = config.getString("notime.kick-message")
                .replace("&", "§").replace("§§", "&")
                .replace("%start%", config.getString("notime.start"))
                .replace("%end%", config.getString("notime.end"));
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = true;
            new icu.sdsjmc.polang.notime.hook.PlaceholderAPI().register();
            getLogger().info(notime + "§3检测到 PlaceholderAPI 已挂钩");
        }

        noTimeEnable = config.getBoolean("notime.enable", true);
        getServer().getPluginManager().registerEvents(new Listener(), this);
        if (config.getBoolean("notime.kick-old", true) && noTimeEnable) {
            if (checkTime()) {
                kickPlayers(kickMessage);
            }
        }
        runTask();
        run();
        runTimes();
        forTime();
//        getServer().getPluginCommand("notime").setExecutor(new Commander());
//        getServer().getPluginCommand("notime").setTabCompleter(new Commander());
        getServer().getPluginCommand("notime").setExecutor(new NotimeCommand());
        getServer().getPluginCommand("notime").setTabCompleter(new NotimeCommand());

        // 开服后输入指令
        new BukkitRunnable() {
            @Override
            public void run() {
                startCommands();
            }
        }.runTaskLater(this, 20L); // 延迟任务执行一秒钟（20 ticks）

        new Metrics(this, 19955);
    }

    @Override
    public void onDisable() {
        // 关闭 ScheduledExecutorService
        if (executor != null) {
            executor.shutdownNow();
        }
        if (executorList != null) {
            executorList.shutdownNow();
        }
        if (executorFor != null) {
            executorFor.shutdownNow();
        }
    }

    public void startCommands() {
        if (!config.getBoolean("startcommand.enable")) {
            return;
        }
        List<String> commands = config.getStringList("startcommand.commands");
        if (commands == null) {
            return;
        }
        for (String command : commands) {
            console(command);
        }
    }

    // 用于存储定时任务的实例
    BukkitTask task;

    /**
     * 定时任务运行方法。
     * 该方法用于根据配置启动一个异步任务，任务将在特定时间执行，例如踢出过期玩家。
     * 如果不满足启动条件，任务将不会被调度。
     */
    public void runTask() {
        // 检查是否启用了定时任务，如果没有启用，则直接返回不执行任何操作
        if (!noTimeEnable) return;
        // 如果当前存在任务实例，则尝试取消当前任务，避免重复执行
        if (task != null) {
            task.cancel();
        }
        // 调用服务器调度器，异步执行任务
        task = getServer().getScheduler().runTaskAsynchronously(this, () ->
        {
            // 计算任务执行时间，如果当前时间晚于配置的起始时间，则计算为明天的相同时间
            int time = LocalTime.parse(config.getString("notime.start")).toSecondOfDay() - LocalTime.now().toSecondOfDay();
            if (LocalTime.now().toSecondOfDay() >= LocalTime.parse(config.getString("notime.start")).toSecondOfDay()) {
                time += 24 * 60 * 60;
            }
            // 暂停当前线程，直到到达任务执行时间
            try {
                Thread.sleep(time * 1000L);
            } catch (InterruptedException e) {
                // 如果线程被中断，抛出运行时异常
                throw new RuntimeException(e);
            }
            // 任务执行完毕后，重置任务实例
            task = null;
            // 如果配置启用，踢出过期玩家
            if (config.getBoolean("notime.kick-old", true)) {
                getServer().getScheduler().runTask(this, () -> kickPlayers(kickMessage));
            }
            // 递归调用自身，重新安排任务，实现周期性执行
            runTask();
        });
    }


    /**
     * 运行时间组的计时器
     */
    public void runTimes() {
        Map<String, ScheduleDataTimes> schedulesTimes = new HashMap<>();

        // 遍历配置文件中的每个定时任务
        for (String key : config.getConfigurationSection("run").getKeys(false)) {
            String path = "run." + key;
            if (config.isList(path + ".time") && !config.isString(path + ".fortime")) {
                List<String> times = config.getStringList(path + ".time");
                List<LocalTime> timeList = times.stream()
                        .map(s -> LocalTime.parse(s, DateTimeFormatter.ofPattern("[HH:mm:ss][HH:mm]")))
                        .collect(Collectors.toList());
                boolean enable = config.getBoolean(path + ".enable");
                List<String> commands = config.getStringList(path + ".command");
                ScheduleDataTimes scheduleDataTimes = new ScheduleDataTimes(timeList, enable, commands);
                schedulesTimes.put(key, scheduleDataTimes);
            }
        }
        // 创建 ScheduledExecutorService 对象
        executorList = Executors.newSingleThreadScheduledExecutor();

        // 针对每个指定的时间列表创建定时任务
        schedulesTimes.forEach((key, data) ->
        {
            if (data.enable) {
                for (int i = 0; data.times.size() > i; i++) {
                    // 下一次执行要多少毫秒
                    long time = data.times.get(i).toNanoOfDay() - LocalTime.now().toNanoOfDay();
                    // 如果时间已过，则加上一天
                    if (time < 0) {
                        time += TimeUnit.DAYS.toNanos(1);
                    }
                    // 创建任务
                    int finalI = i;
                    executorList.scheduleAtFixedRate(() ->
                            operate(data.commands.get(finalI)), time, TimeUnit.DAYS.toNanos(1), TimeUnit.NANOSECONDS);
                }
            }
        });
    }

    /**
     * 主要运行的计时器
     */
    public void run() {
        Map<String, ScheduleData> schedules = new HashMap<>();

        // 遍历配置文件中的每个定时任务
        for (String key : config.getConfigurationSection("run").getKeys(false)) {
            String path = "run." + key;
            if (!config.isString(path + ".fortime")) {
                data(schedules, key, 1);
            }
        }

        // 创建 ScheduledExecutorService 对象
        executor = Executors.newSingleThreadScheduledExecutor();

        // 针对每个指定的时间创建定时任务
        schedules.forEach((key, data) ->
        {
            if (data.enable) {
                // 下一次执行要多少毫秒
                long time2 = data.time.toNanoOfDay() - LocalTime.now().toNanoOfDay();

                // 如果时间已过，则加上一天
                if (time2 < 0) {
                    time2 += TimeUnit.DAYS.toNanos(1);
                }

                // 创建任务
                executor.scheduleAtFixedRate(() ->
                {
                    for (String command : data.commands) {
                        operate(command);
                    }
                }, time2, TimeUnit.DAYS.toNanos(1), TimeUnit.NANOSECONDS);
            }
        });
    }

    /**
     * 循环执行的计时器
     */

    public void forTime() {
        Map<String, ScheduleData> schedules2 = new HashMap<>();
        for (String key : config.getConfigurationSection("run").getKeys(false)) {
            String path = "run." + key;
            if (config.isString(path + ".fortime")) {
                data(schedules2, key, 2);
            }
        }

        executorFor = Executors.newSingleThreadScheduledExecutor();

        // 针对每个指定的时间创建定时任务
        schedules2.forEach((key, data) ->
        {
            if (data.enable) {
                long delay = parseTime(data.timeString);
                if (delay != 0) // 如果延迟解析不成不创建任务
                {
                    // 创建任务
                    executorFor.scheduleAtFixedRate(() ->
                    {
                        for (String command : data.commands) {
                            operate(command);
                        }
                    }, delay, delay, TimeUnit.NANOSECONDS);
                }
            }
        });

    }

    public void data(Map<String, ScheduleData> map, String key, int mark) {
        String path = "run." + key;
        if (mark == 1) {

            String timeString = path + ".time";
            if (!config.isList(timeString) && !config.isString(path + ".fortime")) {
                LocalTime time = LocalTime.parse(config.getString(timeString),
                        DateTimeFormatter.ofPattern("[HH:mm:ss][HH:mm]"));
                boolean enable = config.getBoolean(path + ".enable");
                List<String> commands = config.getStringList(path + ".command");
                ScheduleData scheduleData = new ScheduleData(time, enable, commands);
                map.put(key, scheduleData);
            }
        } else {
            String timeString = path + ".fortime";
            if (config.getString(timeString) != null) {
                boolean enable = config.getBoolean(path + ".enable");
                List<String> commands = config.getStringList(path + ".command");
                ScheduleData scheduleData = new ScheduleData(config.getString(timeString), enable, commands);
                map.put(key, scheduleData);
            }
        }
    }

    /**
     * 执行操作的方法
     */
    public void operate(String command) {
        String[] subcommands = command.split(";");
        LocalDate today = LocalDate.now(); // 获取当前日期
        int DAY = today.getDayOfWeek().getValue();
        int number = today.getDayOfMonth();
        for (String subcommand : subcommands) {
            if (subcommand.startsWith("week=") || subcommand.startsWith("周=")) {
                String day =  subcommand.substring(subcommand.indexOf("=") + 1);
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
                getServer().shutdown();
            } else {
                if (!subcommand.contains(" ")) {
                    console(subcommand);
                    continue;
                }
                String papiCommand = subcommand;
                String condition = subcommand.substring(0, subcommand.indexOf(' '));
                int q = 0;
                if (papi) // 如果有papi插件就把文本改为解析papi的
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
                        getServer().broadcastMessage(msg.replace("&", "§").replace("§§", "&"));
                        break;
                    }
                    case "@all": {
                        String com = papiCommand.substring(5 + q);
                        for (Player player : getServer().getOnlinePlayers()) {
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

    public void console(String command) // 后台执行命令
    {

        getServer().getScheduler().runTask(this, () ->
                getServer().dispatchCommand(getServer().getConsoleSender(), command.replace("&", "§").replace("§§", "&")));
    }

    public void kickPlayers(String string) //踢出在线非白名单玩家
    {
        for (Player p : getServer().getOnlinePlayers()) {
            if (!config.getList("notime.whitelist").contains(p.getName())) {
                p.kickPlayer(string);
            }
        }
    }
}
