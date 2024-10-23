package co.basin.betterbosses;

import co.basin.betterbosses.Item.LootBagItem;
import co.basin.betterbosses.Item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(MultiplayerBosses.MODID)
public class MultiplayerBosses
{
    public static final String MODID = "multiplayerbosses";
    private static final Logger LOGGER = LogUtils.getLogger();
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
        int playerCount = event.getLevel().players().size();
        if (playerCount <= 1 && Config.flatHealthMultiplier == 0) { return; }
        double scalar = playerCount - 1 * Config.multiplierPerPlayer;
        if (Config.flatHealthMultiplier > 0) { scalar = Config.flatHealthMultiplier - 1; }
        if (!isBoss(livingEntity)) { return; }
        AttributeInstance maxHealthAttribute = livingEntity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            LOGGER.error(event.getEntity().getName() + " has no max health attribute, skipping entity");
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
        if (Config.flatDropsMultiplier > 0) { playerCount = Config.flatDropsMultiplier; }
        if (playerCount <= 1) { return; }
        event.setCanceled(true);
        ServerLevel serverLevel = (ServerLevel) event.getEntity().level();
        for (int i = 0; i < playerCount - 1; i++) {
            if (Config.shouldDropLootBags) {
                createLootBag(event.getEntity(), event.getSource(), serverLevel);
            } else {
                createLoot(event.getEntity(), event.getSource(), serverLevel);
            }
        }
    }

    private void createLootBag(LivingEntity livingEntity, DamageSource damageSource, ServerLevel serverLevel) {
        if (livingEntity instanceof WitherBoss) {
            ItemStack stack = new ItemStack(ModItems.LOOTBAGITEM.get());
            LootBagItem.setBoss(stack, livingEntity);
            LootBagItem.setItem(stack, Items.NETHER_STAR);
            ItemEntity itementity = livingEntity.spawnAtLocation(stack);
            if (itementity != null) { itementity.setExtendedLifetime(); }
            return;
        }

        ResourceLocation resourcelocation = livingEntity.getLootTable();
        LootTable loottable = serverLevel.getServer().getLootData().getLootTable(resourcelocation);
        ItemStack stack = new ItemStack(ModItems.LOOTBAGITEM.get());
        LootBagItem.setBoss(stack, livingEntity);
        LootBagItem.setItems(stack, loottable.getRandomItems(createLootParameters(livingEntity, damageSource, serverLevel), livingEntity.getLootTableSeed()));
        livingEntity.spawnAtLocation(stack);
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
        for (EntityType entityType : Config.bosses) {
            if (entity.getType().equals(entityType)) {
                return true;
            }
        }

        return false;
    }
}
