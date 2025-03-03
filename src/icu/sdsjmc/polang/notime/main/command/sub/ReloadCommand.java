package icu.sdsjmc.polang.notime.main.command.sub;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.Handle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, Command cmd, String label, String[] args) {
        NoTime plugin = NoTime.getPlugin(NoTime.class);
        plugin.reloadConfig();
        NoTime.config = plugin.getConfig();
        NoTime.kickMessage = NoTime.config.getString("notime.kick-message")
                .replace("&", "§").replace("§§", "&")
                .replace("%start%", NoTime.config.getString("notime.start"))
                .replace("%end%", NoTime.config.getString("notime.end"));
        NoTime.noTimeEnable = NoTime.config.getBoolean("notime.enable", true);
        if (NoTime.noTimeEnable) {
            if (Handle.checkTime()) {
                NoTime.instance.kickPlayers(NoTime.kickMessage);
            }
        }
        NoTime.instance.runTask();

        if (NoTime.executor != null) {
            NoTime.instance.getLogger().info(NoTime.notime + "§a你已成功注销§c " + NoTime.executor.shutdownNow().size() + " §a个单时间任务");
            NoTime.instance.runTask();
        }
        if (NoTime.executorList != null) {
            NoTime.instance.getLogger().info(NoTime.notime + "§a你已成功注销§c " + NoTime.executorList.shutdownNow().size() + " §a个多时间任务");
            NoTime.instance.runTimes();
        }
        if (NoTime.executorFor != null) {
            NoTime.instance.getLogger().info(NoTime.notime + "§a你已成功注销§c " + NoTime.executorFor.shutdownNow().size() + " §a个循环任务");
            NoTime.instance.forTime();
        }
        sender.sendMessage(NoTime.notime + "§f插件重载成功.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return Collections.emptyList();
    }
}
