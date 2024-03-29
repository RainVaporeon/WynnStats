package com.spiritlight.wynnstats;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault @ParametersAreNonnullByDefault
public class statCommand extends CommandBase {
    public static final List<String> a_0 = Arrays.asList("player", "guild");
    public static final String ERR_INVALID_ARGS = "Invalid arguments, try /stat for more info.";
    public static final String ERR_REQUEST_FAIL = "Request failed, please check your spellings, or try again after some while.";
    @Override
    public String getName() {
        return "stat";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/stat player|guild <name>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("stats");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        final PagingSpirit pagingSpirit = new PagingSpirit();
        if(args.length == 0) {
            AnnouncerSpirit.send("Usage: /stat [player/guild] <params>");
            return;
        }
        switch(args[0].toLowerCase(Locale.ROOT)) {
            case "player":
                if(args.length < 2) {
                    AnnouncerSpirit.send(ERR_INVALID_ARGS);
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    AnnouncerSpirit.send("§aChecking player §b" + args[1] + "§a...");
                    List<TextComponentString> message;
                    boolean exists = false;
                    if (MainMod.playerMap.containsKey(args[1])) {
                        AnnouncerSpirit.send("Using local cache for this player...");
                        message = MainMod.playerMap.get(args[1]);
                        exists = true;
                    } else {
                        message = LookupSpirit.searchPlayer(args[1]);
                    }
                    if (message == null) {
                        AnnouncerSpirit.send(ERR_REQUEST_FAIL);
                        return;
                    } else if (!exists) {
                        MainMod.playerMap.put(args[1], new ArrayList<>(message));
                    }
                    for (TextComponentString iTextComponents : message)
                        AnnouncerSpirit.send(iTextComponents);
                }).exceptionally(e -> {
                    AnnouncerSpirit.send("§cThis player doesn't exist.");
                    e.printStackTrace();
                    return null;
                });
                break;
            case "guild":
                if(args.length < 2) {
                    AnnouncerSpirit.send(ERR_INVALID_ARGS);
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    String[] nameArr = Arrays.copyOfRange(args, 1, args.length);
                    String guild = String.join(" ", nameArr);
                    AnnouncerSpirit.send("§aChecking guild §b" + guild + "§a...");
                    List<TextComponentString> message;
                    if(MainMod.guildMaps.containsKey(guild)) {
                        guild = MainMod.guildMaps.get(guild);
                    }
                    if(MainMod.cachedGuildInfo.containsKey(guild)) {
                        message = MainMod.cachedGuildInfo.get(guild);
                    } else {
                        message = LookupSpirit.searchGuild(guild);
                    }
                    if(message == null) {
                        AnnouncerSpirit.send("§cThis guild doesn't exist.");
                        return;
                    }
                    pagingSpirit.fetchPage(message, 1, guild); // Safely assume there are at least 1 member in guild
                }).exceptionally(e -> {
                    AnnouncerSpirit.send("§cThis guild doesn't exist.");
                    e.printStackTrace();
                    return null;
                });
                break;
            case "_showguildpage":
                if(args.length < 3) break;
                try {
                    String[] nameArr = Arrays.copyOfRange(args, 2, args.length);
                    String guild = String.join(" ", nameArr);
                    pagingSpirit.fetchPage(MainMod.cachedGuildInfo.get(guild), Integer.parseInt(args[1]), guild);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if(args.length == 0) return a_0;
        if(args[0].toLowerCase(Locale.ROOT).equals("player")) {
            try {
                Collection<NetworkPlayerInfo> players = Minecraft.getMinecraft().getConnection().getPlayerInfoMap();
                List<String> playerNames = new ArrayList<>();
                if(args.length == 2) {
                    for (NetworkPlayerInfo info : players) {
                        String name = info.getGameProfile().getName().toLowerCase(Locale.ROOT);
                        if(name.matches("^[A-Za-z0-9_]+$") && name.startsWith(args[1].toLowerCase(Locale.ROOT)))
                            playerNames.add(info.getGameProfile().getName());
                    }
                } else {
                    for (NetworkPlayerInfo info : players) {
                        if(info.getGameProfile().getName().matches("^[A-Za-z0-9_]+$"))
                            playerNames.add(info.getGameProfile().getName());
                    }
                }
                return playerNames;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
