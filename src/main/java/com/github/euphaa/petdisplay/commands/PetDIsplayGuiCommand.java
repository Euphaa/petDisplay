package com.github.euphaa.petdisplay.commands;

import com.github.euphaa.petdisplay.PetDisplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class PetDIsplayGuiCommand extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "petdisplaygui";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/petdisplaygui <int posX> <int posY>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        try
        {
            PetDisplayHandler.guiPos[0] = Integer.parseInt(args[0]);
            PetDisplayHandler.guiPos[1] = Integer.parseInt(args[1]);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText("§cToo few arguments given. requires 2 args: <int posX> <int posY>"));
            return;
        }
        catch (NumberFormatException e)
        {
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText("§cThat is not a number! only use chars 0-9."));
            return;
        }

        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText("§aSuccessfully changed gui location."));
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
}
