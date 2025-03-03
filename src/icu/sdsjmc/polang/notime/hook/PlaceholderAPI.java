package icu.sdsjmc.polang.notime.hook;

import icu.sdsjmc.polang.notime.NoTime;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

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
    public String onRequest(OfflinePlayer player, String s) {
        List<String> list = new ArrayList<>();
        // 遍历配置文件中的每个定时任务
        for (String key : NoTime.config.getConfigurationSection("run").getKeys(false)) {
            String path = "run." + key;
            if (!NoTime.config.isString(path + ".fortime")) {
                list.add(key);
            }
        }
        for (String str : list)
        {
            String name = s.substring(0, s.indexOf("_"));
            if (str.contains(name))
            {

                String str2 = s.substring(s.indexOf("_") + 1);
                return str2;
//                return Handle.parseTime(str2, name);
            }
        }
    return "0";
    }
}
