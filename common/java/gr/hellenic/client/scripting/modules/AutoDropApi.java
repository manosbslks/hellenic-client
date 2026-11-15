package gr.hellenic.client.scripting.modules;

import gr.hellenic.client.scripting.ApiType;
import gr.hellenic.client.scripting.ApiVisibility;
import gr.hellenic.client.configs.AutoDropConfig;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.utils.InventorySlot;
import gr.hellenic.client.utils.InventoryUtils;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class AutoDropApi {

    @MethodDescription("""
            Drops configured items on the ground
            """)
    @ApiVisibility(ApiType.ACTION)
    public void dropItems() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            AutoDropConfig config = ConfigStore.instance.getConfig().autoDropConfig;
            List<InventorySlot> slots = new ArrayList<>();
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = mc.player.getInventory().getItem(i);
                if (!itemStack.isEmpty() && config.items.contains(itemStack.getItem())) {
                    slots.add(new InventorySlot(i));
                }
            }
            InventoryUtils.dropItemStacks(slots);
        }
    }
}