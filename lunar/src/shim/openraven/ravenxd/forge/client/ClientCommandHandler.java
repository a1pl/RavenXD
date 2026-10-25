package openraven.ravenxd.forge.client;
import java.util.*;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
/** Client-side command registry (Forge's ClientCommandHandler, trimmed). Dispatch is wired by a mixin on EntityPlayerSP.sendChatMessage. */
public class ClientCommandHandler {
    public static final ClientCommandHandler instance = new ClientCommandHandler();
    private final Map<String, ICommand> commands = new HashMap<String, ICommand>();
    /** func_71560_a = registerCommand */
    public ICommand func_71560_a(ICommand command) {
        commands.put(command.func_71517_b(), command);
        try {
            List aliases = command.func_71514_a();
            if (aliases != null) for (Object a : aliases) commands.put(String.valueOf(a), command);
        } catch (Throwable ignored) {}
        return command;
    }
    /** func_71556_a = executeCommand; 1 = handled, 0 = not a client command */
    public int func_71556_a(ICommandSender sender, String message) {
        if (message == null) return 0;
        message = message.trim();
        if (!message.startsWith("/")) return 0;
        message = message.substring(1);
        String[] split = message.split(" ");
        if (split.length == 0 || split[0].isEmpty()) return 0;
        ICommand cmd = commands.get(split[0]);
        if (cmd == null) return 0;
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        try { cmd.func_71515_b(sender, args); }
        catch (Throwable t) { System.err.println("[RavenXD] client command failed: " + t); }
        return 1;
    }
}
