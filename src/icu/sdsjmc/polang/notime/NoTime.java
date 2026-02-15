package icu.sdsjmc.polang.notime;

import com.tcoded.folialib.FoliaLib;
import icu.sdsjmc.polang.notime.hook.Metrics;
import icu.sdsjmc.polang.notime.hook.PlaceholderAPI;
import icu.sdsjmc.polang.notime.main.Events;
import icu.sdsjmc.polang.notime.main.NoTimeAPI;
import icu.sdsjmc.polang.notime.main.command.main.NotimeCommand;
import icu.sdsjmc.polang.notime.main.command.sub.TestCommand;
import icu.sdsjmc.polang.notime.main.data.ScheduleData;
import icu.sdsjmc.polang.notime.main.data.ScheduleDataTimes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static icu.sdsjmc.polang.notime.main.NoTimeAPI.*;


public class NoTime extends JavaPlugin {
    public static NoTime instance;
    public static boolean noTimeEnable;
    public static boolean papi;
    private static FoliaLib foliaLib;
    public static boolean isFolia = false;

    public static FileConfiguration config;
    public static String kickMessage = " ";
    public final static String notimeTitle = "§7[§2§lNo§a§lTime§7] ";

    public static ScheduledThreadPoolExecutor executor;
    public static ScheduledThreadPoolExecutor executorList;
    public static ScheduledThreadPoolExecutor executorFor;


    public void onLoad() {
        instance = this;
        try {
            isFolia = Class.forName("io.papermc.paper.threadedregions.RegionizedServer") != null;
        } catch (Exception ignored) {
        }

        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        kickMessage = config.getString("notime.kick-message")
                .replace("&", "§").replace("§§", "&")
                .replace("%start%", config.getString("notime.start"))
                .replace("%end%", config.getString("notime.end"));
        noTimeEnable = config.getBoolean("notime.enable", true);
        getLogger().info("§b配置文件已重载.");
        if (config.getBoolean("notime.kick-old", true) && noTimeEnable) {
            if (checkTime()) {
                kickPlayers(kickMessage);
            }
        }

        TestCommand.list.clear();
        TestCommand.list.addAll(NoTime.config.getConfigurationSection("run").getKeys(false));
    }

    @Override
    public void onEnable() {
        onLoad();
        foliaLib = new FoliaLib(this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = true;
            new PlaceholderAPI().register();
            getLogger().info(notimeTitle + "§3检测到 PlaceholderAPI 已挂钩");
        }
        getServer().getPluginManager().registerEvents(new Events(), this);

        runKickTask();
        if (kickTask != null) getLogger().info(notimeTitle + "§e已成功加载了防沉迷");
        else getLogger().info(notimeTitle + "§8§n未启用防沉迷功能");

        run();
        if (executor != null)
            getLogger().info(notimeTitle + "§e你已成功加载§c " + executor.getTaskCount() + " §e个单时间任务");

        runTimes();
        if (executorList != null)
            getLogger().info(notimeTitle + "§e你已成功加载§c " + executorList.getTaskCount() + " §e条多时间任务");

        forTime();
        if (executorFor != null)
            getLogger().info(notimeTitle + "§e你已成功加载§c " + executorFor.getTaskCount() + " §e个循环任务");
        NotimeCommand notimeCommand = new NotimeCommand();
        getServer().getPluginCommand("notime").setExecutor(notimeCommand);
        getServer().getPluginCommand("notime").setTabCompleter(notimeCommand);
        // 延迟任务执行一秒钟（20 tick）
        // 使用普通线程代替Bukkit scheduler以支持Folia
        Thread startCommandThread = new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待1秒
                startCommands();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        startCommandThread.start();
//        // 延迟任务执行一秒钟（20 tick
//        getServer().getScheduler().runTaskLater(this, this::startCommands, 20L);

        new Metrics(this, 19955);
        if (isFolia) {
            getLogger().info(notimeTitle + "§a您使用的是 Folia，正常运行中。");
        }
        getLogger().info(notimeTitle + "§6问题反馈、插件交流请加群：§d620224543");
        getLogger().info(" ");
        getLogger().info(notimeTitle + "§f插件成功加载！");
    }

    @Override
    public void onDisable() {
        getLogger().info(notimeTitle + "§7插件正在卸载...");
        // 关闭 ScheduledExecutorService
        if (executor != null) executor.shutdownNow();
        if (executorList != null) executorList.shutdownNow();
        if (executorFor != null) executorFor.shutdownNow();
        getLogger().info(notimeTitle + "§3插件已经卸载咯！");
    }

    public void startCommands() {
        if (!config.getBoolean("startcommand.enable")) return;
        List<String> commands = config.getStringList("startcommand.commands");
        if (commands == null) return;
        for (String command : commands) NoTimeAPI.console(command);
    }

    // 用于存储定时任务的实例
    public static BukkitTask kickTask;

    /**
     * 定时任务运行方法。
     * 该方法用于根据配置启动一个异步任务，任务将在特定时间执行，例如踢出过期玩家。
     * 如果不满足启动条件，任务将不会被调度。
     */
    //定时踢出
    public void runKickTask() {
        // 检查是否启用了定时任务，如果没有启用，则直接返回不执行任何操作
        if (!noTimeEnable) return;
        // 如果当前存在任务实例，则尝试取消当前任务，避免重复执行
        if (kickTask != null) {
            kickTask.cancel();
        }

        // Folia支持，不能用Bukkit scheduler
        Thread kickThread = new Thread(() -> {
            while (noTimeEnable) {
                // 计算任务执行时间，如果当前时间晚于配置的起始时间，则计算为明天的相同时间
                int time = LocalTime.parse(config.getString("notime.start")).toSecondOfDay() - LocalTime.now().toSecondOfDay();
                if (LocalTime.now().toSecondOfDay() >= LocalTime.parse(config.getString("notime.start")).toSecondOfDay()) {
                    time += 24 * 60 * 60;
                }
                // 暂停当前线程，直到到达任务执行时间
                try {
                    Thread.sleep(time * 1000L);
                } catch (InterruptedException e) {
                    // 如果线程被中断，退出线程
                    Thread.currentThread().interrupt();
                    return;
                }

                // 如果配置启用，踢出过期玩家，需要确保在主线程执行
                if (config.getBoolean("notime.kick-old", true)) {

                    foliaLib.getScheduler().runNextTick((wrappedTask) -> kickPlayers(kickMessage));
                }
            }
        });
        kickThread.setDaemon(true);
        kickThread.start();
    }


    /**
     * 组时间的计时器
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
        executorList = new ScheduledThreadPoolExecutor(1);

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
     * 单时间的计时器
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
        executor = new ScheduledThreadPoolExecutor(1);
        ;

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
     * 定时循环执行的计时器
     */

    public static Map<String, Long> lastRunTimeForTasks = new HashMap<>();

    public void forTime() {
        Map<String, ScheduleData> schedules2 = new HashMap<>();
        for (String key : config.getConfigurationSection("run").getKeys(false)) {
            String path = "run." + key;
            if (config.isString(path + ".fortime")) {
                data(schedules2, key, 2);
            }
        }

        executorFor = new ScheduledThreadPoolExecutor(1);
        ;

        // 针对每个指定的时间创建定时任务
        schedules2.forEach((key, data) ->
        {
            if (data.enable) {
                long delay = parseTime(data.timeString);
                if (delay != 0) // 如果延迟解析不成不创建任务
                {
                    // 初始化任务的上次执行时间为当前时间
                    lastRunTimeForTasks.put(key, System.nanoTime());
                    // 创建任务
                    executorFor.scheduleAtFixedRate(() ->
                    {
                        // 更新任务的上次执行时间
                        lastRunTimeForTasks.put(key, System.nanoTime());
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

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }
}
