import commands.BotCommands;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;

public class DiscordBot {
    public static void main(String[] args) throws InterruptedException {
        JDABuilder.createDefault("MTEyMjgyODI4MjU1MTg2OTU3MA.GCKFeq.63emcBiDsAS8C1QH-5DPSCtAyiF0w-UJr6nmJE")
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("Making bis teams"))
                .addEventListeners(new BotCommands())
                .build().awaitReady();
    }
}