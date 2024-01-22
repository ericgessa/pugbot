import io.github.cdimascio.dotenv.Dotenv;

import commands.BotCommands;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;

public class DiscordBot {
    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();

        JDABuilder.createDefault(dotenv.get("DISCORD_TOKEN"))
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("Creating pugs"))
                .addEventListeners(new BotCommands())
                .build().awaitReady();
    }
}