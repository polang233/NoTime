package icu.sdsjmc.polang.notime.main.command.sub;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.command.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

import static icu.sdsjmc.polang.notime.NoTime.kickTask;

public class ReloadCommand implements SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, Command cmd, String label, String[] args) {
        //覆盖进去

        NoTime plugin = NoTime.instance;
        plugin.onLoad();

        plugin.runKickTask();
        if (NoTime.kickTask != null && NoTime.noTimeEnable) {
            sender.sendMessage(NoTime.notimeTitle + "§e已成功加载了防沉迷");
        } else {
            sender.sendMessage(NoTime.notimeTitle + "§8§n未启用防沉迷功能");
        }

        if (NoTime.executor != null) {
            sender.sendMessage(NoTime.notimeTitle + "§7当前卸载了§c " + NoTime.executor.shutdownNow().size() + " §7个单时间任务");
            plugin.run();
            sender.sendMessage(NoTime.notimeTitle + "§e重新加载了§c " + NoTime.executor.getTaskCount() + " §e个循环任务");
        }

        if (NoTime.executorList != null) {
            sender.sendMessage(NoTime.notimeTitle + "§7当前卸载了§c " + NoTime.executorList.shutdownNow().size() + " §7个多时间任务");
            plugin.runTimes();
            sender.sendMessage(NoTime.notimeTitle + "§e重新加载了§c " + NoTime.executorList.getTaskCount() + " §e个循环任务");
        }

        if (NoTime.executorFor != null) {
            sender.sendMessage(NoTime.notimeTitle + "§7当前卸载了§c " + NoTime.executorFor.shutdownNow().size() + " §7个循环任务");
            plugin.forTime();
            sender.sendMessage(NoTime.notimeTitle + "§e重新加载了§c " + NoTime.executorFor.getTaskCount() + " §e个循环任务");
        }
        sender.sendMessage(NoTime.notimeTitle);
        sender.sendMessage(NoTime.notimeTitle + "§f插件重载成功.");
        if (sender instanceof Player) {
            plugin.getLogger().info("§f插件由 §c" + sender.getName() + " §f重载.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return Collections.emptyList();
    }
}
