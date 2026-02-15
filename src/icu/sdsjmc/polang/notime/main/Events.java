package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import static icu.sdsjmc.polang.notime.NoTime.config;
import static icu.sdsjmc.polang.notime.NoTime.kickMessage;
import static icu.sdsjmc.polang.notime.main.NoTimeAPI.checkTime;

public class Events implements Listener {

    @EventHandler
    public void noLogin(AsyncPlayerPreLoginEvent event) {
        if (!NoTime.noTimeEnable) return;

        if (!checkTime()) return;
        if (config.getList("notime.blacklist").isEmpty() || config.getList("notime.blacklist").get(0) == "") {
            if (!config.getList("notime.whitelist").contains(event.getName())) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
        } else if (config.getList("notime.blacklist").contains(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
        }
    }
}
