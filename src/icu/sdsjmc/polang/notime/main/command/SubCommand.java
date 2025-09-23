package icu.sdsjmc.polang.notime.main.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String getName();

    boolean execute(CommandSender sender, Command cmd, String label, String[] args);

    List<String> tabComplete(CommandSender sender, Command cmd, String label, String[] args);
}
