package icu.sdsjmc.polang.notime.main.data;

import java.time.LocalTime;
import java.util.List;

/**
 * 定义存储定时任务数据时间为List的类
 */
public class ScheduleDataTimes
{
    public final List<LocalTime> times;
    public final boolean enable;
    public final List<String> commands;

    public ScheduleDataTimes(List<LocalTime> times, boolean enable, List<String> commands)
    {
        this.times = times;
        this.enable = enable;
        this.commands = commands;
    }
}
