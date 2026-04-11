package icu.sdsjmc.polang.notime.main.command.main;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.command.CommandManager;
import icu.sdsjmc.polang.notime.main.command.sub.ReloadCommand;
import icu.sdsjmc.polang.notime.main.command.SubCommand;
import icu.sdsjmc.polang.notime.main.command.sub.TestCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class NotimeCommand implements SubCommand, CommandExecutor, TabCompleter {

    private final CommandManager subCommandManager = new CommandManager();

    public NotimeCommand() {
        // 注册子命令
        subCommandManager.registerCommand(new ReloadCommand());
        subCommandManager.registerCommand(new TestCommand());
        // 可以继续注册其他子命令
    }

    @Override
    public String getName() {
        return "notime";
    }

    @Override
    public boolean execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(NoTime.notimeTitle + "§7可输入以下命令.");
            for (SubCommand command : subCommandManager.getCommands().values())
            {
                sender.sendMessage(NoTime.notimeTitle + "/notime " +command.getName());
            }
            return false;
        }

        return subCommandManager.onCommand(sender, cmd, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(subCommandManager.getCommands().keySet());
        }

        return subCommandManager.onTabComplete(sender, cmd, label, args);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return execute(sender, cmd, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return tabComplete(sender, cmd, alias, args);
    }
}
