package co.basin.betterbosses.item;

import co.basin.betterbosses.Config;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

public class LootBagItem extends Item {
    public LootBagItem(Properties properties) {
        super(properties);
    }

    public static void setBoss(ItemStack stack, LivingEntity livingEntity) {
        String encodeId = livingEntity.getEncodeId();
        stack.getOrCreateTag().putString("mob", encodeId != null ? encodeId : "");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag nbt = itemStack.getOrCreateTag();

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(nbt.getString("mob")));
        if (entityType == null) { return InteractionResultHolder.fail(itemStack); }

        if (!level.isClientSide()) {
            LivingEntity livingEntity = (LivingEntity) entityType.create(level);
            ServerLevel serverLevel = (ServerLevel) level;
            Inventory inventory = player.getInventory();
            for (ItemStack lootStack : createLoot(player, livingEntity, serverLevel)) {
                inventory.placeItemBackInInventory(lootStack);
            }
        }

        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private ObjectArrayList<ItemStack> createLoot(Player player, LivingEntity livingEntity, ServerLevel serverLevel) {
        if (livingEntity instanceof WitherBoss) { return ObjectArrayList.of(new ItemStack(Items.NETHER_STAR)); }
        ResourceLocation resourcelocation = livingEntity.getLootTable();
        LootTable loottable = serverLevel.getServer().getLootData().getLootTable(resourcelocation);
        LootParams.Builder lootparams$builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, livingEntity)
                .withParameter(LootContextParams.DAMAGE_SOURCE, serverLevel.damageSources().generic())
                .withParameter(LootContextParams.ORIGIN, player.position());
        return loottable.getRandomItems(lootparams$builder.create(LootContextParamSets.ENTITY), livingEntity.getLootTableSeed());
    }

    @Override
    public Component getName(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt == null) { return super.getName(itemStack); }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(nbt.getString("mob")));
        if (entityType == null) { return super.getName(itemStack); }
        return entityType.getDescription().copy().append(" ").append(Component.translatable(this.getDescriptionId(itemStack)));
    }

    public int getColor(ItemStack itemStack, int tintIndex) {
        CompoundTag nbt = itemStack.getTag();
        if (nbt == null) { return -1; }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(nbt.getString("mob")));
        Tuple<?, ?> colors = Config.lootBagTints.get(entityType);
        if (colors == null) { return -1; }
        return (int) (tintIndex == 0 ? colors.getA() : colors.getB());
    }
}
