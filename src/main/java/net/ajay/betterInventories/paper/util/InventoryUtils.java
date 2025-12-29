package net.ajay.betterInventories.paper.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class InventoryUtils 
{
    public static void OpenInventory(HumanEntity opener, Inventory inventory) {
        InventoryView oldInventoryView = opener.getOpenInventory();
        ItemStack cursorStack = oldInventoryView.getCursor();
        oldInventoryView.setCursor(ItemStack.empty());
        
        InventoryView newInventoryView = opener.openInventory(inventory);
        newInventoryView.setCursor(cursorStack);
    }
    public static TaggedInventoryInfo OpenInventory(HumanEntity opener, ItemStack itemStack, InventoryType type, NamespacedKey tagKey) {
        ItemContainerContents itemContainerContents = itemStack.getData(DataComponentTypes.CONTAINER);
        Inventory StackInventory = Bukkit.createInventory(opener, type, itemStack.effectiveName().color(NamedTextColor.GRAY));
        StackInventory.setContents(itemContainerContents.contents().toArray(new ItemStack[0]));
        OpenInventory(opener, StackInventory);
        
        String uuid = UUID.randomUUID().toString();
        itemStack.editPersistentDataContainer(pdc -> pdc.set(tagKey, PersistentDataType.STRING, uuid));
        
        return new TaggedInventoryInfo(uuid, StackInventory);
    }
    
    public static class TaggedInventoryInfo {
        public final String uuid;
        public final Inventory newInventory;
        
        protected TaggedInventoryInfo(String uuid, Inventory newInventory) {
            this.uuid = uuid;
            this.newInventory = newInventory;
        }
    }
}
