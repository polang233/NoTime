package icu.sdsjmc.polang.notime.main.command;

import icu.sdsjmc.polang.notime.NoTime;
import icu.sdsjmc.polang.notime.main.command.sub.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {

    private final Map<String, SubCommand> commands = new HashMap<>();

    public void registerCommand(SubCommand command) {
        commands.put(command.getName(), command);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(NoTime.notime + "§7可输入以下命令.");
            for (SubCommand command : commands.values())
            {
                sender.sendMessage(NoTime.notime + "/notime " +command.getName());
            }
            return false;
        }

        String subCommandName = args[0];
        SubCommand subCommand = commands.get(subCommandName);
        if (subCommand != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            return subCommand.execute(sender, cmd, label, subArgs);
        } else {
            sender.sendMessage(NoTime.notime + "§c未知的子命令.");
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(commands.keySet());
        }

        String subCommandName = args[0];
        SubCommand subCommand = commands.get(subCommandName);
        if (subCommand != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            return subCommand.tabComplete(sender, cmd, label, subArgs);
        }

        return new ArrayList<>();
    }

    public Map<String, SubCommand> getCommands() {
        return commands;
    }
}
