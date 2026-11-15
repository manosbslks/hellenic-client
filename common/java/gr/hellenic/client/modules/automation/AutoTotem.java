package gr.hellenic.client.modules.automation;

import gr.hellenic.client.common.Events;
import gr.hellenic.client.configs.AutoTotemConfig;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.modules.Module;
import gr.hellenic.client.utils.InventorySlot;
import gr.hellenic.client.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AutoTotem implements Module {

    public static final AutoTotem instance = new AutoTotem();

    private final Minecraft mc = Minecraft.getInstance();

    private AutoTotem() {
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    private void onClientTickEnd() {
        if (mc.player == null) {
            return;
        }

        AutoTotemConfig config = ConfigStore.instance.getConfig().autoTotemConfig;
        if (!config.enabled) {
            return;
        }

        if (config.skipIfUsingItem && mc.player.isUsingItem()) {
            return;
        }

        ItemStack offhand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (offhand.isEmpty()) {
            Inventory inventory = mc.player.getInventory();
            int totemSlot = -1;
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack.has(DataComponents.DEATH_PROTECTION)) {
                    totemSlot = i;
                    break;
                }
            }

            if (totemSlot >= 0) {
                InventoryUtils.moveItemStack(new InventorySlot(totemSlot), new InventorySlot(EquipmentSlot.OFFHAND));
            }
        }
    }
}