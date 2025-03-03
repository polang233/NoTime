package icu.sdsjmc.polang.notime.main.data;

import java.time.LocalTime;
import java.util.List;

/**
 * 定义存储定时任务数据的类
 */
public class ScheduleData
{
    public LocalTime time;
    public String timeString;
    public final boolean enable;
    public final List<String> commands;

    public ScheduleData(LocalTime time, boolean enable, List<String> commands)
    {
        this.time = time;
        this.enable = enable;
        this.commands = commands;
    }

    public ScheduleData(String time, boolean enable, List<String> commands)
    {
        this.timeString = time;
        this.enable = enable;
        this.commands = commands;
    }
}
