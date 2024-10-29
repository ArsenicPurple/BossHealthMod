package co.basin.betterbosses.client;

import co.basin.betterbosses.MultiplayerBosses;
import co.basin.betterbosses.item.LootBagItem;
import co.basin.betterbosses.item.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiplayerBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Events {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(((itemStack, tintIndex) -> ((LootBagItem) itemStack.getItem()).getColor(itemStack, tintIndex)), ModItems.LOOTBAG.get());
    }
}
