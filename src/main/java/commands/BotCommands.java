package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotCommands extends ListenerAdapter {
    EmbedBuilder eb = new EmbedBuilder();

    VoiceChannel pug;
    VoiceChannel blu;
    VoiceChannel red;

    List<Member> pugChannelMembers;
    List<Member> redTeam;
    List<Member> bluTeam;
    List<Member> fatKids;

    OptionData size;

    OptionMapping format;
    OptionMapping channel;
    OptionMapping customSize;

    int pugSize = 0;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws NullPointerException {
        assert event.getGuild() != null;
        assert event.getMember() != null;

        if (event.getName().equals("run")) {
            format = event.getOption("format");
            assert format != null;
            channel = event.getOption("channel");
            assert channel != null;

            pug = channel.getAsChannel().asVoiceChannel();
            blu = event.getGuild().getVoiceChannelById(1099095690640105502L);
            red = event.getGuild().getVoiceChannelById(1099095742003548160L);


            String formatOption = format.getAsString();

            try {
                switch (formatOption.toLowerCase()) {
                    case "hl" -> pugSize = 18;
                    case "pl" -> pugSize = 14;
                    case "c" -> {
                        size.setRequired(true);
                        customSize = event.getOption("pugsize");
                        assert customSize != null;

                        pugSize = customSize.getAsInt();
                    }
                }
                if (pugSize < 2) {
                    event.reply("Cannot start custom pug without at least two players!").setEphemeral(true).queue();
                } else if (pugSize % 2 != 0) {
                    event.reply("Cannot start custom pug with uneven teams!").setEphemeral(true).queue();
                } else {
                    setupPug(event);
                }
            } catch (NullPointerException e) {
                event.reply("Custom size required for \"custom\" format!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        assert event.getGuild() != null;

        try {
            if (event.getComponentId().equals("startpug")) {
                moveToTeams(event);
            } else if (event.getComponentId().equals("reroll")) {
                refreshPug(event);
            }
        } catch (NullPointerException e) {
            event.getInteraction().deferReply().setEphemeral(true).queue();
        }
    }

    public void createList() {
        eb.clear();
        pugChannelMembers = new ArrayList<>(pug.getMembers());
        redTeam = new ArrayList<>();
        bluTeam = new ArrayList<>();
        fatKids = new ArrayList<>();

        Collections.shuffle(pugChannelMembers);

        for (Member m : pugChannelMembers) {
            if (pugChannelMembers.indexOf(m) < (pugSize / 2)) {
                bluTeam.add(m);
            } else if (pugChannelMembers.indexOf(m) >= (pugSize / 2) && pugChannelMembers.indexOf(m) < pugSize) {
                redTeam.add(m);
            } else if (pugChannelMembers.indexOf(m) >= pugSize) {
                fatKids.add(m);
            }
        }

        eb.setColor(new Color(220, 20, 60));
        eb.addField("BLU:", listMembers(bluTeam, false, true), true);
        eb.addField("RED:", listMembers(redTeam, false, true), true);
        if (fatKids.size() > 0) {
            eb.addField("FKs:", listMembers(fatKids, false, true), false);
        }
    }

    @NotNull
    private String getNames(List<Member> members, boolean mentionable, boolean sort) {
        String messageOfNames;

        if (sort) {
            runComparator(members);
        }

        StringBuilder messageOfNamesBuilder = new StringBuilder();

        if (mentionable) {
            for (Member m : members) {
                if (m.equals(members.get(members.size() - 1))) {
                    messageOfNamesBuilder.append(m.getAsMention());
                } else {
                    messageOfNamesBuilder.append(m.getAsMention()).append("\n");
                }
            }
        } else {
            for (Member m : members) {
                if (m.equals(members.get(members.size() - 1))) {
                    messageOfNamesBuilder.append(m.getEffectiveName());
                } else {
                    messageOfNamesBuilder.append(m.getEffectiveName()).append("\n");
                }
            }
        }
        messageOfNames = messageOfNamesBuilder.toString();
        return messageOfNames;
    }

    public String listMembers(List<Member> members, boolean mentionable, boolean sort) {
        return getNames(members, mentionable, sort);
    }
    private void setupPug(SlashCommandInteractionEvent event) {
        createList();

        MessageEmbed embed = eb.build();

        if (!(pug.getMembers().size() >= pugSize)) {
            event.reply("Not enough players added for pug!\n" + "Number added: " + pug.getMembers().size() + ", number required: " + pugSize).setEphemeral(true).queue();
        } else {
            event.reply("").setEmbeds(embed)
                    .addActionRow(
                            Button.success("startpug", "Start"),
                            Button.primary("reroll", "Reroll")
                    )
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void refreshPug(ButtonInteractionEvent buttonEvent) {
        createList();

        if (pug.getMembers().size() < pugSize) {
            buttonEvent.reply("Could not start pug! Someone must have left!\n"
                            + "Number added: " + pug.getMembers().size()
                            + ", number required: " + pugSize).setEphemeral(true)
                    .queue();
        } else {
            MessageEmbed embed = eb.build();
            buttonEvent.editMessage("").setEmbeds(embed)
                    .queue();
        }
    }

    public void runComparator(List<Member> members) {
        members.sort((m1, m2) -> String.CASE_INSENSITIVE_ORDER.compare(m1.getEffectiveName(), m2.getEffectiveName()));
    }

    public void moveToTeams(ButtonInteractionEvent event) {
        assert event.getGuild() != null;

        if (!(redTeam.size() == (pugSize / 2) || bluTeam.size() == (pugSize / 2))) {
            event.reply("Could not start pug! Someone may have left the channel.\nRerun to create new pug.").setEphemeral(true).queue();
        } else {
            if (pugSize > 10) {
                event.reply("Pug started!\nDiscord allows only ten people to be moved at once, so please give me time to move the rest of the players.").setEphemeral(true).queue();
            } else {
                event.reply("Pug started!").setEphemeral(true).queue();
            }

            moveBlu(event);
            moveRed(event);
        }
        bluTeam.clear();
        redTeam.clear();
    }

    public void moveBlu(ButtonInteractionEvent event) {
        assert event.getGuild() != null;
        for (Member m : bluTeam) {
            event.getGuild().moveVoiceMember(m, blu).queue();
        }
    }

    public void moveRed(ButtonInteractionEvent event) {
        assert event.getGuild() != null;
        for (Member m : redTeam) {
            event.getGuild().moveVoiceMember(m, red).queue();
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        OptionData vc = new OptionData(OptionType.CHANNEL, "channel", "Name of voice channel to start pug in", true).setChannelTypes(ChannelType.VOICE);
        OptionData format = new OptionData(OptionType.STRING, "format", "Competitive format", true)
                .addChoice("Highlander", "hl")
                .addChoice("Prolander", "pl")
                .addChoice("Custom", "c");
        size = new OptionData(OptionType.INTEGER, "pugsize", "Custom pug size. Only required when pug format is set to \"custom\".");

        commandData.add(Commands.slash("run", "Build a pug.").addOptions(format, vc, size).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        for (CommandData c : commandData) {
            event.getGuild().upsertCommand(c).queue();
        }
    }
}