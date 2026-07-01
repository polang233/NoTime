package icu.sdsjmc.polang.notime.main.command.sub;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.NoTimeAPI;
import icu.sdsjmc.polang.notime.main.command.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.*;

public class TestCommand implements SubCommand {
    @Override
    public String getName() {
        return "test";
    }

    public static List<String> list = new ArrayList<>();

    @Override
    public boolean execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c请指定一个子命令.");
            return false;
        }

        // 使用 config.contains() 判断任务是否存在，而不是冗余的 != null 检查
        // （getStringList() 永远不返回 null，原代码检查无意义）
        String taskPath = "run." + args[0] + ".command";
        if (NoTime.config.contains(taskPath)) {
            List<String> commands = NoTime.config.getStringList(taskPath);
            for (String command : commands) {
                NoTimeAPI.operate(command);
            }
            sender.sendMessage("§a运行 §2" + args[0] + " §a定时任务成功");
        } else {
            sender.sendMessage("§c找不到该 §2" + args[0] + " §c定时任务.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return list;
    }
}
