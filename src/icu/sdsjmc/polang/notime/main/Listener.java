package icu.sdsjmc.polang.notime.main;

import icu.sdsjmc.polang.notime.NoTime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import static icu.sdsjmc.polang.notime.NoTime.config;
import static icu.sdsjmc.polang.notime.NoTime.kickMessage;
import static icu.sdsjmc.polang.notime.main.Handle.checkTime;

public class Listener implements org.bukkit.event.Listener
{

    @EventHandler
    public void noLogin(AsyncPlayerPreLoginEvent event)
    {
        if(!NoTime.noTimeEnable)
        {
            return;
        }
        if(checkTime())
        {
            if(!config.getList("notime.whitelist").contains(event.getName()))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
        }
    }
}
