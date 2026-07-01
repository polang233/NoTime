package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import static icu.sdsjmc.polang.notime.NoTime.kickMessage;
import static icu.sdsjmc.polang.notime.main.NoTimeAPI.checkTime;
import static icu.sdsjmc.polang.notime.main.NoTimeAPI.shouldBlockPlayer;

public class Events implements Listener {

    @EventHandler
    public void noLogin(AsyncPlayerPreLoginEvent event) {
        if (!NoTime.noTimeEnable) return;

        if (!checkTime()) return;
        // 使用 NoTimeAPI 中统一的 shouldBlockPlayer 方法判断黑白名单
        if (shouldBlockPlayer(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
        }
    }
}
