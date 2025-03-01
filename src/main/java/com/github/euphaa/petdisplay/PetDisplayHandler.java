package com.github.euphaa.petdisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Level;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * handles everything related to the pet dislplay including detection and rendering.
 */
public class PetDisplayHandler
{
    /**
     * the name of the currently equiped pet.
     */
    String activePet = null;

    /**
     * stores the color (i.e. the rarity) of the pet. this value is stored as the character
     * of the minecraft color formatting code.
     * @see <a href=https://minecraft.fandom.com/wiki/Formatting_codes>https://minecraft.fandom.com/wiki/Formatting_codes</a>
     */
    private char petColor = '6';

    /**
     * stores the level of the pet. can be negative if the level is not yet known.
     */
    private int petLevel = 100;

    /**
     * pattern to extract the pet data from the tab list.
     */
    PetPattern tablistPattern = new PetPattern(3, 2, 1, "\\s*§r\\s*§r§7\\[Lvl (\\d+)\\]\\s*§r§(.)([A-Za-z ]+)§r");

    /**
     * patterns to detect pet related chat messages.
     */
    PetPattern[] patterns = new PetPattern[]
    {
            new PetPattern(3, 2, 1, "§cAutopet §eequipped your §7\\[Lvl (\\d+)\\] §(.)([A-Za-z ]+)§e! §a§lVIEW RULE"),
            new PetPattern(2, 1, -1, "§r§aYou summoned your §r§(.)([A-Za-z ]+)§r§a!§r"),
    };
    PetPattern chatDespawnPattern = new PetPattern(1, 1, -1, "§r§aYou despawned your §r§(.)([A-Za-z ]+)§r§a!§r");

    /**
     * the time until which tablist detection will be disabled. it is set when a pet is detected using another
     * method such as a chat message. if this occurrs it is necessary to timeout tablist detection
     * because it updates very late.
     */
    private long tablistWidgetTimeout;

    /**
     * amount of time in ms in which the tablist should not be checked in the event that a
     * chat message is detected that determines the active pet.
     */
    private final long TABLIST_TIMEOUT_LENGTH = 3_000;
    /**
     * tracks tick number so that certain features can only be called on certain tick intervals.
     */
    private short tick = 0;
    public static int[] guiPos = new int[]{15, 15};


    /**
     * scans tablist for active pet.
     * @param event
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        // only check every 10 ticks
        if (tick++ % 10 != 0) return;
        // dont check if tablist is under timeout
        if (System.currentTimeMillis() < tablistWidgetTimeout) return;

        // get tablist information from the gui
        NetHandlerPlayClient netHandlerPlayClient;
        List<NetworkPlayerInfo> tablist;
        try
        {
            netHandlerPlayClient = Minecraft.getMinecraft().thePlayer.sendQueue;
            tablist = GuiPlayerTabOverlay.field_175252_a.sortedCopy(netHandlerPlayClient.getPlayerInfoMap());
        }
        catch (NullPointerException e)
        {
            Init.LOGGER.log(Level.ERROR, "erm, what the-");
            return;
        }

        // find the active pet tab widget and then extract the pet data from it
        for (int i = 0; i < tablist.size(); i++)
        {
            IChatComponent displayName;
            try
            {
                displayName = tablist.get(i).getDisplayName();
            }
            catch (NullPointerException e)
            {
                return;
            }

            if (displayName == null || !displayName.getFormattedText().equals("§r§e§lPet:§r"))
            {
                continue;
            }

            String text = tablist.get(i+1).getDisplayName().getFormattedText();
            Matcher matcher = tablistPattern.regexPattern.matcher(text);
            if (!matcher.find()) return;
            Init.LOGGER.log(Level.DEBUG, "MATCH FOUND: " + matcher.group(tablistPattern.nameGroup));
            activePet = matcher.group(tablistPattern.nameGroup);
            petColor = matcher.group(tablistPattern.colorGroup).toCharArray()[0];
            petLevel = Integer.parseInt(matcher.group(tablistPattern.levelGroup));
            return;
        }
    }

    /**
     * renders the overlay GUI every frame.
     * @param event
     */
    @SubscribeEvent
    public void onGuiRender(RenderGameOverlayEvent.Text event)
    {
        if (activePet == null)
        {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("No pet equiped", guiPos[0], guiPos[1], 0xDD3322);
        }
        else
        {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(String.format("§7[Lvl %d] §%c%s", petLevel, petColor, activePet), guiPos[0], guiPos[1], 0xFFFFFF);
        }
    }

    /**
     * looks in the chat for changes in pet.
     * @param event
     */
    @SubscribeEvent
    public void onClientChatRecieved(ClientChatReceivedEvent event)
    {
        // check normal chat patterns
        for (PetPattern pattern : patterns)
        {
            Matcher matcher = pattern.regexPattern.matcher(event.message.getFormattedText());
            if (!matcher.find()) continue;

            activePet = matcher.group(pattern.nameGroup);
            petColor = matcher.group(pattern.colorGroup).toCharArray()[0];
            tablistWidgetTimeout = System.currentTimeMillis() + TABLIST_TIMEOUT_LENGTH;

            if (pattern.levelGroup > 0) petLevel = Integer.parseInt(matcher.group(pattern.levelGroup));
            else petLevel = -1;

            return;
        }

        // check unequip message pattern
        Matcher matcher = chatDespawnPattern.regexPattern.matcher(event.message.getFormattedText());
        if (!matcher.find()) return;

        activePet = null;
        tablistWidgetTimeout = System.currentTimeMillis() + TABLIST_TIMEOUT_LENGTH;
    }

    /**
     * stores a regex pattern used to detect messages in chat along with which regex group
     * in the provided patter that contains the name of the pet.
     */
    private static class PetPattern
    {
        /**
         * regular expression which used to find chat messages which denote a change in equiped pet.
         */
        Pattern regexPattern;
        /**
         * the group number where the name of the pet can be found.
         */
        int nameGroup;
        /**
         * the group number where the color of the pet can be found.
         */
        int colorGroup;
        /**
         * the group number where the level of the pet can be found. can be negative if the pattern does not
         * provide a pet level.
         */
        int levelGroup;

        /**
         *
         * @param nameGroup the group number where the name of the pet can be found.
         * @param regex regular expression which is then compiled and used to find chat messages which
         *              denote a change in equipped pet.
         * @param colorGroup the group number where the color of the pet can be found.
         * @param levelGroup the group number where the level of the pet can be found. use -1
         *                   when a pattern does not provide the level number.
         */
        public PetPattern(int nameGroup, int colorGroup, int levelGroup, String regex)
        {
            this.regexPattern = Pattern.compile(regex);
            this.nameGroup = nameGroup;
            this.colorGroup = colorGroup;
            this.levelGroup = levelGroup;
        }
    }
}
