package co.basin.betterbosses.Item;

import co.basin.betterbosses.MultiplayerBosses;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MultiplayerBosses.MODID);

    public static final RegistryObject<LootBagItem> LOOTBAGITEM = ITEMS.register("lootbagitem", () -> new LootBagItem(new Item.Properties()));
}
