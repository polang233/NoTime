package icu.sdsjmc.polang.notime.hook;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.NoTimeAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.TimeUnit;

public class PlaceholderAPI extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "notime";
    }

    public String getName() {
        return "notime"; //名称
    }

    @Override
    public String getAuthor() {
        return "Polang_";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; //防止 PlaceholderAPI 在扩展重载的时候把它注销
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // 解析参数，格式为: 任务名_时间单位 例如: 测试1_m
        String[] parts = params.split("_");
        if (parts.length < 2) {
            return "0";
        }

        String taskName = parts[0];
        String timeUnit = parts[1];

        // 检查任务是否存在
        String path = "run." + taskName;
        if (!NoTime.config.contains(path)) {
            return "0";
        }
        long nextTime = NoTimeAPI.getTimeNextExecution(taskName);
        switch (timeUnit.toLowerCase()) {
            case "s":
                return String.valueOf(nextTime);
            case "m":
                return String.valueOf(TimeUnit.SECONDS.toMinutes(nextTime));
            case "h":
                return String.valueOf(TimeUnit.SECONDS.toHours(nextTime));
            case "d":
                return String.valueOf(TimeUnit.SECONDS.toDays(nextTime));
            default:
                return "0";
        }
    }
}
