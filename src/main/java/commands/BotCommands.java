package commands;

import io.github.cdimascio.dotenv.Dotenv;

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
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BotCommands extends ListenerAdapter {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    Dotenv dotenv;

    public BotCommands() {
        dotenv = Dotenv.load();
    }

    private VoiceChannel pugVoiceChannel;
    private VoiceChannel bluVoiceChannel;
    private VoiceChannel redVoiceChannel;

    private List<Member> pugChannelMembers = new ArrayList<>();
    private List<Member> redTeam = new ArrayList<>();
    private List<Member> bluTeam = new ArrayList<>();
    private List<Member> fatKids = new ArrayList<>();

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

            pugVoiceChannel = channel.getAsChannel().asVoiceChannel();
            bluVoiceChannel = event.getGuild().getVoiceChannelById(dotenv.get("BLU_CHANNEL_ID"));
            redVoiceChannel = event.getGuild().getVoiceChannelById(dotenv.get("RED_CHANNEL_ID"));

            pugChannelMembers = pugVoiceChannel.getMembers();
            int memberSize = pugChannelMembers.size();

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
                } else if (memberSize < pugSize) {
                    event.reply("Not enough players added for pug!\n" + "Number added: " + memberSize
                    + ", number required: " + pugSize).setEphemeral(true).queue();
                }else {
                    setupPug(pugChannelMembers, event);
                }
            } catch (NullPointerException e) {
                event.reply("Custom size required for \"custom\" format!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        assert event.getGuild() != null;

        System.out.println("button pressed");
        System.out.println(event.getComponentId());
        System.out.println(event.getComponentId().equals("startpug"));

        try {
            System.out.println("in try block");
            if (event.getComponentId().equals("startpug")) {
                if (pugVoiceChannel.getMembers().size() < pugSize) {
                    event.reply("Could not start pug! Someone may have left the channel.\nReroll to create new pug.")
                            .setEphemeral(true).queue();
                    return;
                }
                moveToTeams(event);
            } else if (event.getComponentId().equals("reroll")) {
                int memberSize = pugChannelMembers.size();
                if(memberSize < pugSize) {
                    event.reply("Not enough players added for pug!\n" + "Number added: " + memberSize
                            + ", number required: " + pugSize).setEphemeral(true).queue();
                    return;
                }
                setupPug(pugChannelMembers, event);
            }
        } catch (NullPointerException e) {
            event.getInteraction().deferReply().setEphemeral(true).queue();
        }
    }

    public void createList() {
        embedBuilder.clear();
        pugChannelMembers = new ArrayList<>(pugVoiceChannel.getMembers());
        redTeam.clear();
        bluTeam.clear();
        fatKids.clear();
    
        Collections.shuffle(pugChannelMembers);
    
        for (int i = 0; i < pugChannelMembers.size(); i++) {
            Member m = pugChannelMembers.get(i);
            if (redTeam.size() < (pugSize / 2)) {
                redTeam.add(m);
            } else if (bluTeam.size() < (pugSize / 2)) {
                bluTeam.add(m);
            } else {
                fatKids.add(m);
            }
        }

        embedBuilder.setColor(new Color(220, 20, 60));
        embedBuilder.addField("BLU:", getNames(bluTeam), true);
        embedBuilder.addField("RED:", getNames(redTeam), true);
        if (fatKids.size() > 0) {
            embedBuilder.addField("FKs:", getNames(fatKids), false);
        }
    }

    @NotNull
    private String getNames(List<Member> members) {
        sortMembersByName(members);

        return members.stream()
                .map(m -> m.getEffectiveName())
                .collect(Collectors.joining("\n"));
    }

    private void setupPug(List<Member> members, Interaction event) {
        createList();

        MessageEmbed embed = embedBuilder.build();
    
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).reply("").setEmbeds(embed)
                    .addActionRow(
                            Button.success("startpug", "Start"),
                            Button.primary("reroll", "Reroll")
                    )
                    .setEphemeral(true)
                    .queue();
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).replyEmbeds(embed)
                    .addActionRow(
                            Button.success("startpug", "Start"),
                            Button.primary("reroll", "Reroll")
                    )
                    .setEphemeral(true)
                    .queue();
        }
    }
    
    public void sortMembersByName(List<Member> members) {
        members.sort((m1, m2) -> String.CASE_INSENSITIVE_ORDER.compare(m1.getEffectiveName(), m2.getEffectiveName()));
    }

    public void moveToTeams(ButtonInteractionEvent event) {
        assert event.getGuild() != null;
    
        if (pugSize > 10) {
            event.reply(
                    "Pug started!\nDiscord allows only ten people to be moved at once, so please give me time to move the rest of the players.")
                    .setEphemeral(true).queue();
        } else {
            event.reply("Pug started!").setEphemeral(true).queue();
        }
    
        moveMembers(event, bluTeam, bluVoiceChannel);
        moveMembers(event, redTeam, redVoiceChannel);
    
        bluTeam.clear();
        redTeam.clear();
    }

    public void moveMembers(ButtonInteractionEvent event, List<Member> team, VoiceChannel destination) {
        assert event.getGuild() != null;
        for (Member m : team) {
            event.getGuild().moveVoiceMember(m, destination).queue(null, throwable -> {
                // Handle error here
                System.out.println("Failed to move member: " + throwable.getMessage());
            });
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        OptionData vc = new OptionData(OptionType.CHANNEL, "channel", "Name of voice channel to start pug in", true)
                .setChannelTypes(ChannelType.VOICE);
        OptionData format = new OptionData(OptionType.STRING, "format", "Competitive format", true)
                .addChoice("Highlander", "hl")
                .addChoice("Prolander", "pl")
                .addChoice("Custom", "c");
        size = new OptionData(OptionType.INTEGER, "pugsize",
                "Custom pug size. Only required when pug format is set to \"custom\".");

        commandData.add(Commands.slash("run", "Build a pug.").addOptions(format, vc, size)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        for (CommandData c : commandData) {
            event.getGuild().upsertCommand(c).queue();
        }
    }
}