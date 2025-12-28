package net.ajay.betterInventories.manager;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.ajay.betterInventories.core.AbstractManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ShulkerBoxManager extends AbstractManager 
{
    private static ShulkerBoxManager instance;
    private static final Sound OpenSound;
    private static final Sound CloseSound;
    private static HashSet<ShulkerBoxInfo> shulkerBoxInstances;
    private final NamespacedKey ShulkerIDKey;
    
    private ShulkerBoxManager(JavaPlugin plugin) {
        super(plugin);
        ShulkerIDKey = NamespacedKey.fromString("shulker.id", plugin);
    }

    
    private static void OpenShulker(HumanEntity player, Inventory inventory, ItemStack itemStack) {
        if (isViewingShulker(player))
            return;
        
        getShulkerBox(itemStack, shulkerBox -> {

            TextColor color = shulkerBox.getColor() != null ? TextColor.color(shulkerBox.getColor().getColor().asRGB()) : NamedTextColor.LIGHT_PURPLE; 
            
            player.sendMessage(Component.text("Opening ", color).append(itemStack.effectiveName().color(color)).append(Component.text("...", color)));
            
            Inventory ShulkerInventory = Bukkit.createInventory(player, InventoryType.SHULKER_BOX, itemStack.effectiveName().color(NamedTextColor.DARK_GRAY));
            ShulkerInventory.setContents(shulkerBox.getSnapshotInventory().getContents());
            player.openInventory(ShulkerInventory);
            player.playSound(OpenSound);
            String uuid = UUID.randomUUID().toString();
            itemStack.editPersistentDataContainer(pdc -> pdc.set(instance.ShulkerIDKey, PersistentDataType.STRING, uuid));
            ShulkerBoxInfo.create(uuid, ShulkerInventory, inventory, player);
        });
    }
    private static void CloseShulker(HumanEntity player) {
        if (!isViewingShulker(player))
            return;
        
        GetShulkerInfo(player, shulkerBoxInfo -> {
            shulkerBoxInfo.onClose();
        });
    }
    
    
    private static boolean isShulker(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && itemStack.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox;
    }
    private static boolean isViewingShulker(HumanEntity humanEntity) {
        for (ShulkerBoxInfo shulkerBoxInstance : shulkerBoxInstances) {
            if (humanEntity == shulkerBoxInstance.viewer)
                return true;
        }
        
        return false;
    }
    private static void getShulkerBox(ItemStack itemStack, Consumer<ShulkerBox> shulkerBoxConsumer) {
        if (itemStack.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulkerBox) {
            shulkerBoxConsumer.accept(shulkerBox);
            return;
        }
    }
    
    private static void GetShulkerInfo(HumanEntity player, Consumer<ShulkerBoxInfo> shulkerBoxInfoConsumer) {
        for (ShulkerBoxInfo shulkerBoxInstance : shulkerBoxInstances) {
            if (player == shulkerBoxInstance.viewer) {
                shulkerBoxInfoConsumer.accept(shulkerBoxInstance);
                return;
            }
        }
    }
    
    @EventHandler
    public void playerInteraction(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && isShulker(event.getItem())) {
            OpenShulker(event.getPlayer(), event.getPlayer().getInventory(), event.getItem());
        }
    }
    
    @EventHandler
    public void inventoryClosed(InventoryCloseEvent event) {
        if (isViewingShulker(event.getPlayer()))
            CloseShulker(event.getPlayer());
    }
    
    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.isEmpty())
            return;
        
        if  (event.isRightClick() && !isViewingShulker(event.getWhoClicked()) && isShulker(event.getCurrentItem())) {
            OpenShulker(event.getWhoClicked(), event.getClickedInventory(), event.getCurrentItem());
        }
    }
    
    @EventHandler
    public void itemDropped(PlayerDropItemEvent event) {
        if (isViewingShulker(event.getPlayer()) && isShulker(event.getItemDrop().getItemStack()))
            GetShulkerInfo(event.getPlayer(), shulkerBoxInfo -> shulkerBoxInfo.onDropped(event.getItemDrop().getItemStack()));
    }
    
    static {
        OpenSound = Sound.sound(Key.key("block.shulker_box.open"), Sound.Source.PLAYER, 1, 1.25f);
        CloseSound = Sound.sound(Key.key("block.shulker_box.close"), Sound.Source.PLAYER, 1, 1.25f);
        
        shulkerBoxInstances = new HashSet<>();
    }
    public static ShulkerBoxManager create(JavaPlugin plugin) {
        if (instance == null)
            instance = new ShulkerBoxManager(plugin);

        return instance;
    }
    private static class ShulkerBoxInfo {
        public final String uuid;
        public final Inventory shulkerInventory;
        
        public final HumanEntity viewer;
        public final Inventory ownerInventory;
        
        private boolean closed = false;
        
        public Optional<ItemStack> getShulkerStack() {
            for (@Nullable ItemStack itemStack : ownerInventory.getContents()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    var dataHolder = itemStack.getItemMeta().getPersistentDataContainer();
                    
                    if (dataHolder.has(instance.ShulkerIDKey))
                        return dataHolder.get(instance.ShulkerIDKey, PersistentDataType.STRING) == uuid ? Optional.of(itemStack) : Optional.empty();
                }
            }
            
            return Optional.empty();
        }
        public void save() {
            getShulkerStack().ifPresent(itemStack -> itemStack.setData(DataComponentTypes.CONTAINER, this.ItemContainerContents()));
        }
        
        public void onClose() {
            if (closed)
                return;
            
            this.save();
            getShulkerStack().ifPresent(itemStack -> itemStack.editPersistentDataContainer(pdc -> pdc.remove(instance.ShulkerIDKey)));
            close();
        }
        public void onDropped(ItemStack droppedStack) {
            if (closed)
                return;
            
            droppedStack.editPersistentDataContainer(pdc -> pdc.remove(instance.ShulkerIDKey));
            droppedStack.setData(DataComponentTypes.CONTAINER, this.ItemContainerContents());
            shulkerInventory.close();
            close();
        }
        private void close() {
            if (closed)
                return;
            viewer.playSound(CloseSound);
            shulkerBoxInstances.remove(this);
            closed = true;
        }
        
        public ItemContainerContents ItemContainerContents() {
            ItemContainerContents.Builder contentsBuilder = ItemContainerContents.containerContents();

            for (@Nullable ItemStack itemStack : shulkerInventory.getContents()) {
                if (itemStack != null)
                    contentsBuilder.add(itemStack);
                else
                    contentsBuilder.add(ItemStack.empty());
            }

            return contentsBuilder.build();
        }
        
        private ShulkerBoxInfo(String uuid, Inventory shulkerInventory, Inventory inventory, HumanEntity viewer, boolean openPreviousOnClose) {
            this.uuid = uuid;
            this.shulkerInventory = shulkerInventory;
            this.ownerInventory = inventory;
            this.viewer = viewer;
        }
        
        protected static ShulkerBoxInfo create(String uuid, Inventory shulkerInventory, Inventory currentInventory, HumanEntity opener) {
            ShulkerBoxInfo shulkerBoxInfo = new ShulkerBoxInfo(uuid, shulkerInventory, currentInventory, opener, false);
            shulkerBoxInstances.add(shulkerBoxInfo);
            return shulkerBoxInfo;
        }
        protected static ShulkerBoxInfo create(String uuid, Inventory shulkerInventory, HumanEntity opener) {
            return create(uuid, shulkerInventory, opener.getInventory(), opener);
        }
    }
}