package co.basin.betterbosses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Mod.EventBusSubscriber(modid = MultiplayerBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SHOULD_SCALE_BOSS_HEALTH = BUILDER
            .comment("If boss health should be scaled")
            .define("Should Scale Boss Health", true);

    private static final ForgeConfigSpec.BooleanValue SHOULD_SCALE_BOSS_DROPS = BUILDER
            .comment("If boss drops should be rolled an additional time per player")
            .define("Should Scale Boss Drops", true);

    private static final ForgeConfigSpec.DoubleValue MULTIPLIER_PER_PLAYER = BUILDER
            .comment("The amount to scale boss health per player after the first")
            .defineInRange("Health Multiplier Per Player", 1.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue FLAT_HEALTH_MULTIPLIER = BUILDER
            .comment("A flat value to increase boss health. Will override \"Health Multiplier Per Player\" if it is not 0")
            .defineInRange("Flat Health Multiplier", 0.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue FLAT_DROPS_MULTIPLIER = BUILDER
            .comment("A flat value to increase boss. If this value is not 0 player count will be ignored")
            .defineInRange("Flat Drop Multiplier", 0, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue SHOULD_USE_FORGE_TAGS = BUILDER
            .comment("Whether to use the forge tag \"forge:bosses\" to detect bosses")
            .define("Should Use Forge Tags", true);

    private static final ForgeConfigSpec.BooleanValue SHOULD_DROP_LOOT_BAGS = BUILDER
            .comment("Whether to drop loot bags filled with boss items or just drop the plain items")
            .define("Should Drop Loot Bags", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_NAMES = BUILDER
            .comment("All bosses that should be affected by this mod. Most are usually in the \"forge:bosses\" tag")
            .defineListAllowEmpty("Boss Entities", List.of("minecraft:wither", "minecraft:ender_dragon"), Config::validateBossName);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean shouldScaleBossHealth;
    public static boolean shouldScaleBossDrops;
    public static double multiplierPerPlayer;
    public static double flatHealthMultiplier;
    public static int flatDropsMultiplier;
    public static boolean shouldUseForgeTags;
    public static boolean shouldDropLootBags;
    public static Set<EntityType> bosses;

    private static boolean validateBossName(final Object obj)
    {
        return obj instanceof final String bossName && ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(bossName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        shouldScaleBossHealth = SHOULD_SCALE_BOSS_HEALTH.get();
        multiplierPerPlayer = MULTIPLIER_PER_PLAYER.get();
        shouldScaleBossDrops = SHOULD_SCALE_BOSS_DROPS.get();
        flatHealthMultiplier = FLAT_HEALTH_MULTIPLIER.get();
        flatDropsMultiplier = FLAT_DROPS_MULTIPLIER.get();
        shouldUseForgeTags = SHOULD_USE_FORGE_TAGS.get();
        shouldDropLootBags = SHOULD_DROP_LOOT_BAGS.get();
        bosses = BOSS_NAMES.get().stream()
                .map(bossName -> ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(bossName)))
                .collect(Collectors.toSet());
    }
}
