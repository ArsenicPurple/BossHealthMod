package co.basin.betterbosses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
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

    private static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER_PER_PLAYER = BUILDER
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

    private static final ForgeConfigSpec.BooleanValue USE_PROXIMITY_SCALING = BUILDER
            .comment("Whether to scale health and loot off of the number of players in the configured range or use the global player count")
            .define("Use Proximity Scaling", true);

    private static final ForgeConfigSpec.IntValue PROXIMITY_SCALING_RANGE = BUILDER
            .comment("How far from a boss spawn position to detect players")
            .defineInRange("Proximity Scaling Range", 100, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_NAMES = BUILDER
            .comment("All bosses that should be affected by this mod. Most are usually in the \"forge:bosses\" tag")
            .defineList("Boss Entities", List.of(
                    "minecraft:wither",
                    "minecraft:ender_dragon",
                    "cataclysm:ancient_remnant",
                    "cataclysm:ignis",
                    "cataclysm:maledictus",
                    "cataclysm:ender_golem",
                    "cataclysm:ender_guardian",
                    "cataclysm:the_leviathan",
                    "cataclysm:the_harbinger",
                    "cataclysm:netherite_monstrosity",
                    "bosses_of_mass_destruction:void_blossom",
                    "bosses_of_mass_destruction:gauntlet",
                    "bosses_of_mass_destruction:lich",
                    "bosses_of_mass_destruction:obsidilith"
            ), Config::validateString);

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<? extends String>>> LOOT_BAG_TINTS = BUILDER
            .comment("Tint colors used for loot bags dropped from bosses. Uses minecrafts integer encoded rgb format. Use -1 for no tint")
            .defineList("Loot Bag Tints", List.of(
                    List.of("minecraft:wither", "7561558", "13882367"),
                    List.of("minecraft:ender_dragon", "2171169", "9830655"),
                    List.of("cataclysm:ancient_remnant", "15789718", "16760084"),
                    List.of("cataclysm:ignis", "5384240", "4766957"),
                    List.of("cataclysm:maledictus", "10387251", "2220943"),
                    List.of("cataclysm:ender_golem", "2363210", "7217407"),
                    List.of("cataclysm:ender_guardian", "12905869", "1049638"),
                    List.of("cataclysm:the_leviathan", "1050911", "6690047"),
                    List.of("cataclysm:the_harbinger", "13617352", "12523030"),
                    List.of("cataclysm:netherite_monstrosity", "3092275", "7995392"),
                    List.of("bosses_of_mass_destruction:void_blossom", "2511372", "4653074"),
                    List.of("bosses_of_mass_destruction:lich", "2700357", "6984916"),
                    List.of("bosses_of_mass_destruction:obsidilith", "328201", "3089731")
            ), Config::validateLootbagTints);

    private static boolean validateLootbagTints(final Object obj) {
        return obj instanceof final List<?> list && list.size() == 3;
    }

    private static boolean validateString(final Object obj) {
        return obj instanceof String;
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean shouldScaleBossHealth;
    public static boolean shouldScaleBossDrops;
    public static double multiplierPerPlayer;
    public static double flatHealthMultiplier;
    public static int flatDropsMultiplier;
    public static boolean shouldUseForgeTags;
    public static boolean shouldDropLootBags;
    public static boolean useProximityScaling;
    public static int proximityScalingRange;
    public static List<EntityType<?>> bosses;

    public static Map<EntityType<?>, Tuple<Integer, Integer>> lootBagTints;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        shouldScaleBossHealth = SHOULD_SCALE_BOSS_HEALTH.get();
        multiplierPerPlayer = HEALTH_MULTIPLIER_PER_PLAYER.get();
        shouldScaleBossDrops = SHOULD_SCALE_BOSS_DROPS.get();
        flatHealthMultiplier = FLAT_HEALTH_MULTIPLIER.get();
        flatDropsMultiplier = FLAT_DROPS_MULTIPLIER.get();
        shouldUseForgeTags = SHOULD_USE_FORGE_TAGS.get();
        shouldDropLootBags = SHOULD_DROP_LOOT_BAGS.get();
        useProximityScaling = USE_PROXIMITY_SCALING.get();
        proximityScalingRange = PROXIMITY_SCALING_RANGE.get();

        bosses = BOSS_NAMES.get().stream()
                .filter(bossName -> ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(bossName)))
                .map(bossName -> ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(bossName)))
                .collect(Collectors.toList());
        lootBagTints = LOOT_BAG_TINTS.get().stream()
                .filter((list) -> ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(list.get(0))))
                .collect(Collectors.toMap(
                        (list) -> ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(list.get(0))),
                        (list) -> new Tuple<>(Integer.parseInt(list.get(1)), Integer.parseInt(list.get(2)))
                ));
    }
}
