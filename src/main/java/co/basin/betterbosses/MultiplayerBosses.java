package co.basin.betterbosses;

import co.basin.betterbosses.item.LootBagItem;
import co.basin.betterbosses.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.stream.Collectors;

@Mod(MultiplayerBosses.MODID)
public class MultiplayerBosses
{
    public static final String MODID = "multiplayerbosses";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MULTIPLIER_TAG_KEY = "lhmulti";

    public MultiplayerBosses()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Multiplayer Bosses Initiated");
    }

    @SubscribeEvent
    public void onLivingSpawn(EntityJoinLevelEvent event) {
        if (!Config.shouldScaleBossHealth) { return; }
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) { return; }
        if (!isBoss(livingEntity)) { return; }
        double playerCount = event.getLevel().players().size();
        if (Config.useProximityScaling) {
            Vec3 entityPosition = livingEntity.position();
            AABB range = new AABB(entityPosition.x - Config.proximityScalingRange, entityPosition.y - Config.proximityScalingRange, entityPosition.z - Config.proximityScalingRange, entityPosition.x + Config.proximityScalingRange, entityPosition.y + Config.proximityScalingRange, entityPosition.z + Config.proximityScalingRange);
            playerCount = event.getLevel().getEntities(livingEntity, range, entity -> entity instanceof Player).stream()
                    .filter(entity -> entityPosition.distanceTo(entity.position()) <= Config.proximityScalingRange)
                    .collect(Collectors.toSet())
                    .size();
            livingEntity.getPersistentData().putInt(MULTIPLIER_TAG_KEY, (int) playerCount);
        }
        if (Config.flatHealthMultiplier > 0) { playerCount = Config.flatHealthMultiplier; }
        if (playerCount <= 1) { return; }
        double scalar = (playerCount - 1) * Config.multiplierPerPlayer;

        AttributeInstance maxHealthAttribute = livingEntity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            LOGGER.error("{} has no max health attribute, skipping entity", event.getEntity().getName());
            return;
        }

        AttributeModifier attributeModifier = new AttributeModifier("playerCountScalar", scalar, AttributeModifier.Operation.MULTIPLY_BASE);
        if (!maxHealthAttribute.hasModifier(attributeModifier)) {
            maxHealthAttribute.addPermanentModifier(new AttributeModifier("playerCountScalar", scalar, AttributeModifier.Operation.MULTIPLY_BASE));
            livingEntity.setHealth(livingEntity.getMaxHealth());
        }
    }

    @SubscribeEvent
    public void onLivingDrop(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) { return; }
        if (!isBoss(event.getEntity())) { return; }
        if (!Config.shouldScaleBossDrops) { return; }
        MinecraftServer server;
        if ((server = event.getEntity().getServer()) == null) { return; }
        int playerCount = server.getPlayerCount();
        if (Config.useProximityScaling) {
            playerCount = event.getEntity().getPersistentData().getInt(MULTIPLIER_TAG_KEY);
        }
        if (Config.flatDropsMultiplier > 0) { playerCount = Config.flatDropsMultiplier; }
        if (playerCount <= 1) { return; }
        event.setCanceled(true);
        ServerLevel serverLevel = (ServerLevel) event.getEntity().level();
        for (int i = 0; i < playerCount; i++) {
            if (Config.shouldDropLootBags) {
                createLootBag(event.getEntity());
            } else {
                createLoot(event.getEntity(), event.getSource(), serverLevel);
            }
        }
    }

    private void createLootBag(LivingEntity livingEntity) {
        ItemStack stack = new ItemStack(ModItems.LOOTBAG.get());
        LootBagItem.setBoss(stack, livingEntity);
        ItemEntity itementity = livingEntity.spawnAtLocation(stack);
        if (itementity != null) { itementity.setExtendedLifetime(); }
    }

    private static void createLoot(LivingEntity livingEntity, DamageSource damageSource, ServerLevel serverLevel) {
        if (livingEntity instanceof WitherBoss) {
            ItemEntity itementity = livingEntity.spawnAtLocation(Items.NETHER_STAR);
            if (itementity != null) { itementity.setExtendedLifetime(); }
            return;
        }

        ResourceLocation resourcelocation = livingEntity.getLootTable();
        LootTable loottable = serverLevel.getServer().getLootData().getLootTable(resourcelocation);
        loottable.getRandomItems(createLootParameters(livingEntity, damageSource, serverLevel), livingEntity.getLootTableSeed(), livingEntity::spawnAtLocation);
    }

    private static LootParams createLootParameters(LivingEntity livingEntity, DamageSource damageSource, ServerLevel serverLevel) {
        LootParams.Builder lootparams$builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, livingEntity)
                .withParameter(LootContextParams.ORIGIN, livingEntity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, damageSource.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, damageSource.getDirectEntity());
        return lootparams$builder.create(LootContextParamSets.ENTITY);
    }

    private static boolean isBoss(LivingEntity entity) {
        if (Config.shouldUseForgeTags && entity.getTags().contains("forge:bosses")) { return true; }
        return Config.bosses.contains(entity.getType());
    }

    public static void logInfo(String info) {
        LOGGER.info(info);
    }
}
