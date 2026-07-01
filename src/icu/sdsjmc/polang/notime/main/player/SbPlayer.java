package icu.sdsjmc.polang.notime.main.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * 为 PlaceholderAPI 提供虚拟玩家实例。
 * 使用动态代理替代手动实现 Player 接口的所有方法，
 * 所有方法调用默认返回 null / 0 / false / 空字符串等默认值。
 */
public class SbPlayer {

    /**
     * 通过动态代理生成的虚拟 OfflinePlayer 实例，
     * 用于在没有真实玩家时解析 PlaceholderAPI 变量。
     */
    public static final OfflinePlayer player = createProxyPlayer();

    private static OfflinePlayer createProxyPlayer() {
        // 使用 JDK 动态代理创建 Player 接口的虚拟实现
        // 所有方法调用根据返回类型给出合理默认值，避免手动实现 1700+ 个接口方法
        return (OfflinePlayer) Proxy.newProxyInstance(
                SbPlayer.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    String methodName = method.getName();
                    if ("toString".equals(methodName)) return "NoTimePlaceholderPlayer";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    if ("getName".equals(methodName)) return "NoTime";
                    if ("getUniqueId".equals(methodName)) return new UUID(0L, 0L);

                    if (returnType == boolean.class) return false;     // 布尔类型返回 false
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == short.class) return (short) 0;
                    if (returnType == byte.class) return (byte) 0;
                    if (returnType == float.class) return 0F;
                    if (returnType == double.class) return 0D;
                    if (returnType == char.class) return '\0';          // 字符类型返回空字符
                    if (returnType == String.class) return "";          // 字符串类型返回空串
                    return null;                                        // 对象类型返回 null
                }
        );
    }
}
