package co.basin.betterbosses.item;

import co.basin.betterbosses.MultiplayerBosses;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MultiplayerBosses.MODID);

    public static final RegistryObject<LootBagItem> LOOTBAG = ITEMS.register("lootbag", () -> new LootBagItem(new Item.Properties().rarity(Rarity.EPIC)));
}
