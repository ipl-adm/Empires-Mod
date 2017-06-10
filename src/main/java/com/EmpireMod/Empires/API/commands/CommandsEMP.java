package com.EmpireMod.Empires.API.commands;

import com.EmpireMod.Empires.Empires;
import com.EmpireMod.Empires.exceptions.EmpiresCommandException;
import com.EmpireMod.Empires.utils.EmpireUtils;
import com.EmpireMod.Empires.utils.StringUtils;
import com.EmpireMod.Empires.entities.Flags.FlagType;
import com.EmpireMod.Empires.entities.Empire.Citizen;
import com.EmpireMod.Empires.entities.Empire.Empire;
import com.EmpireMod.Empires.entities.Empire.EmpireBlock;
import com.EmpireMod.Empires.entities.Empire.Plot;
import com.EmpireMod.Empires.entities.Empire.Rank;
import com.EmpireMod.Empires.entities.Flags.Flag;
import com.EmpireMod.Empires.Proxies.EconomyProxy;
import com.EmpireMod.Empires.Datasource.EmpiresUniverse;
import com.EmpireMod.Empires.Datasource.EmpiresDatasource;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all classes that hold command methods... Mostly for some utils
 */
public abstract class CommandsEMP {

    protected CommandsEMP() {

    }

    public static EmpiresDatasource getDatasource() {
        return Empires.instance.datasource;
    }
    public static EmpiresUniverse getUniverse() {
        return EmpiresUniverse.instance;
    }
    public static Local getLocal() {
        return Empires.instance.LOCAL;
    }

    /**
     * Populates the tab completion map.
     */
    public static void populateCompletionMap() {

        List<String> populator = new ArrayList<String>();
        for(Empire empire : getUniverse().empires) {
            populator.add(empire.getName());
        }

        CommandCompletion.addCompletions("empireCompletionAndAll", populator);
        CommandCompletion.addCompletion("empireCompletionAndAll", "@a");

        CommandCompletion.addCompletions("empireCompletion", populator);

        populator = new ArrayList<String>();
        for (Citizen res : getUniverse().citizens) {
            populator.add(res.getPlayerName());
        }
        CommandCompletion.addCompletions("citizenCompletion", populator);

        populator = new ArrayList<String>();
        for (FlagType flag : FlagType.values()) {
            populator.add(flag.name.toLowerCase());
        }
        CommandCompletion.addCompletions("flagCompletion", populator);

        populator = new ArrayList<String>();
        for (FlagType flag : FlagType.values()) {
            if (flag.isWhitelistable)
                populator.add(flag.name.toLowerCase());
        }
        CommandCompletion.addCompletions("flagCompletionWhitelist", populator);

        populator = new ArrayList<String>();
        for(Plot plot : EmpiresUniverse.instance.plots) {
            populator.add(plot.toString());
        }
        CommandCompletion.addCompletions("plotCompletion", populator);

        populator = new ArrayList<String>();
        for(Rank rank : Rank.defaultRanks) {
            populator.add(rank.getName());
        }
        CommandCompletion.addCompletions("rankCompletion", populator);
    }

    /* ---- HELPERS ---- */

    public static Empire getEmpireFromCitizen(Citizen res) {
        Empire empire = res.empiresContainer.getMainEmpire();
        if (empire == null)
            throw new EmpiresCommandException("Empires.cmd.err.partOfEmpire");
        return empire;
    }

    public static Empire getEmpireFromName(String name) {
        Empire empire = getUniverse().empires.get(name);
        if (empire == null)
            throw new EmpiresCommandException("Empires.cmd.err.empire.missing", name);
        return empire;
    }

    public static Citizen getCitizenFromName(String playerName) {
        Citizen res = EmpiresUniverse.instance.citizens.get(playerName);
        if (res == null)
            throw new EmpiresCommandException("Empires.cmd.err.citizen.missing", playerName);
        return res;
    }

    public static Plot getPlotAtCitizen(Citizen res) {
        Empire empire = getEmpireFromCitizen(res);
        Plot plot = empire.plotsContainer.get(res);
        if (plot == null)
            throw new EmpiresCommandException("Empires.cmd.err.plot.notInPlot");
        return plot;
    }

    public static List<Empire> getInvitesFromCitizen(Citizen res) {
        if (res.empireInvitesContainer.isEmpty())
            throw new EmpiresCommandException("Empires.cmd.err.invite.missing");
        return res.empireInvitesContainer;
    }

    public static Flag getFlagFromType(Flag.Container flagsContainer, FlagType flagType) {
        Flag flag = flagsContainer.get(flagType);
        if (flag == null)
            throw new EmpiresCommandException("Empires.cmd.err.flagNotExists", flagType.toString());
        return flag;
    }

    public static Flag getFlagFromName(Flag.Container flagsContainer, String name) {
        Flag flag;
        try {
            flag = flagsContainer.get(FlagType.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new EmpiresCommandException("Empires.cmd.err.flagNotExists", ex, name);
        }
        if (flag == null)
            throw new EmpiresCommandException("Empires.cmd.err.flagNotExists", name);
        return flag;
    }

    public static EmpireBlock getBlockAtCitizen(Citizen res) {
        EmpireBlock block = getUniverse().blocks.get(res.getPlayer().dimension, ((int) res.getPlayer().posX) >> 4, ((int) res.getPlayer().posZ >> 4));
        if (block == null)
            throw new EmpiresCommandException("Empires.cmd.err.claim.missing", res.empiresContainer.getMainEmpire().getName());
        return block;
    }

    public static Rank getRankFromEmpire(Empire empire, String rankName) {
        Rank rank = empire.ranksContainer.get(rankName);
        if (rank == null) {
            throw new EmpiresCommandException("Empires.cmd.err.rank.missing", rankName, empire.getName());
        }
        return rank;
    }

    public static Rank getRankFromCitizen(Citizen res) {
        Empire empire = res.empiresContainer.getMainEmpire();
        if (empire == null) {
            throw new EmpiresCommandException("Empires.cmd.err.partOfEmpire");
        }
        return empire.citizensMap.get(res);
    }

    public static Plot getPlotAtPosition(int dim, int x, int y, int z) {
        Empire empire = EmpireUtils.getEmpireAtPosition(dim, x >> 4, z >> 4);
        if (empire == null)
            throw new EmpiresCommandException("Empires.cmd.err.blockNotInPlot");
        Plot plot = empire.plotsContainer.get(dim, x, y, z);
        if (plot == null)
            throw new EmpiresCommandException("Empires.cmd.err.blockNotInPlot");
        return plot;
    }

    public static Plot getPlotFromName(Empire empire, String name) {
        Plot plot = empire.plotsContainer.get(name);
        if(plot == null)  {
            throw new EmpiresCommandException("Empires.cmd.err.plot.notExists", name);
        }
        return plot;
    }

    public static FlagType getFlagTypeFromName(String name) {
        try {
            return FlagType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EmpiresCommandException("Empires.cmd.err.flagNotExists", e, name);
        }
    }

    public static Rank.Type getRankTypeFromString(String name) {
        try {
            return Rank.Type.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EmpiresCommandException("Empires.cmd.err.ranks.type.missing", e, name);
        }
    }

    public static void makePayment(EntityPlayer player, int amount) {
        if(amount == 0)
            return;
        Citizen res = EmpiresUniverse.instance.getOrMakeCitizen(player);
        if(!EconomyProxy.getEconomy().takeMoneyFromPlayer(player, amount)){
            throw new EmpiresCommandException("Empires.cmd.err.citizen.payment", EconomyProxy.getCurrency(amount));
        }
        ChatManager.send(player, "Empires.notification.citizen.payment", EconomyProxy.getCurrency(amount));
    }

    public static void makeRefund(EntityPlayer player, int amount) {
        if(amount == 0)
            return;
        Citizen res = EmpiresUniverse.instance.getOrMakeCitizen(player);
        EconomyProxy.getEconomy().giveMoneyToPlayer(player, amount);
        ChatManager.send(player, "Empires.notification.citizen.refund", EconomyProxy.getCurrency(amount));
    }

    public static void makeBankPayment(ICommandSender sender, Empire empire, int amount) {
        if(amount == 0)
            return;

        if(empire.bank.getAmount() < amount)
            throw new EmpiresCommandException("Empires.cmd.err.empire.payment", EconomyProxy.getCurrency(amount));

        empire.bank.addAmount(-amount);
        getDatasource().saveEmpireBank(empire.bank);
        ChatManager.send(sender, "Empires.notification.empire.payment", EconomyProxy.getCurrency(amount));
    }

    public static void makeBankRefund(ICommandSender sender, Empire empire, int amount) {
        if(amount == 0)
            return;

        empire.bank.addAmount(amount);
        getDatasource().saveEmpireBank(empire.bank);
        ChatManager.send(sender, "Empires.notification.empire.refund", EconomyProxy.getCurrency(amount));
    }

    public static void checkPositiveInteger(String integerString) {
        if(!StringUtils.tryParseInt(integerString) || Integer.parseInt(integerString) < 0) {
            throw new EmpiresCommandException("Empires.cmd.err.notPositiveInteger", integerString);
        }
    }
}