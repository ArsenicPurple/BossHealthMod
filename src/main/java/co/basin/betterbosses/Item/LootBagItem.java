package co.basin.betterbosses.Item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class LootBagItem extends Item {
    public LootBagItem(Properties properties) {
        super(properties);
    }

    public static void setBoss(ItemStack stack, LivingEntity livingEntity) {
        stack.getOrCreateTag().putString("mob", livingEntity.getType().getDescriptionId());
    }

    public static void setItem(ItemStack stack, Item loot) {
        ListTag list = new ListTag();
        list.add(0, new ItemStack(loot).serializeNBT());
        stack.getOrCreateTag().put("inventory", list);
    }

    public static void setItems(ItemStack stack, ObjectArrayList<ItemStack> loot) {
        ListTag list = new ListTag();
        for (int i = 0; i < loot.size(); i++) {
            list.add(i, loot.get(i).serializeNBT());
        }
        stack.getOrCreateTag().put("inventory", list);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag tag = itemStack.getOrCreateTag();
        ListTag list = (ListTag) tag.get("inventory");
        if (list == null) { return InteractionResultHolder.fail(itemStack); }

        for (int i = 0; i < list.size(); i++) {
            player.getInventory().placeItemBackInInventory(ItemStack.of(list.getCompound(i)));
        }

        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
