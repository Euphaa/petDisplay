package com.github.euphaa.petdisplay;

import com.github.euphaa.petdisplay.commands.PetDIsplayGuiCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "petdisplay", useMetadata = true)
public class Init
{
    public static final String MODID = "petdisplay";
    private final PetDisplayHandler PET_DISPLAY_HANDLER = new PetDisplayHandler();
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        /* event handlers */
        MinecraftForge.EVENT_BUS.register(new Init());

        /* commands */
        ClientCommandHandler.instance.registerCommand(new PetDIsplayGuiCommand());

    }

    /**
     * fires when the player connects to any server. it registers the {@link PetDisplayHandler} if
     * the connected server is both remote and has the ip of mc.hypixel.net.
     * @param event
     */
    @SubscribeEvent
    public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        if (event.isLocal) return;
        ServerData serverData = Minecraft.getMinecraft().getCurrentServerData();
        LOGGER.log(Level.INFO, "SERVER IP: " + serverData.serverIP);
        if (serverData == null || !serverData.serverIP.contains("hypixel.net")) return;


        LOGGER.log(Level.INFO, "REGISTERED PET DISPLAY TO EVENT BUS");
        MinecraftForge.EVENT_BUS.register(PET_DISPLAY_HANDLER);
    }

    /**
     * unregisteres the {@link PetDisplayHandler} any time the client disconnects from the server.
     * @param event
     */
    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        LOGGER.log(Level.INFO, "DE-REGISTERED PET DISPLAY TO EVENT BUS");
        MinecraftForge.EVENT_BUS.unregister(PET_DISPLAY_HANDLER);
    }
}
