package net.ajay.handShulkers.paper.manager.shulkerbox;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.ajay.handShulkers.bukkit.core.AbstractManager;
import net.ajay.handShulkers.paper.util.InventoryUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
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

public final class ShulkerBoxManager extends AbstractManager 
{
    private static ShulkerBoxManager instance;
    private static final Sound OpenSound;
    private static final Sound CloseSound;
    private static final HashSet<ShulkerBoxInfo> shulkerBoxInstances;
    private final NamespacedKey ShulkerIDKey;
    
    private ShulkerBoxManager(JavaPlugin plugin) {
        super(plugin);
        ShulkerIDKey = NamespacedKey.fromString("shulker.id", plugin);
    }
    
    private static void OpenShulker(HumanEntity player, Inventory inventory, ItemStack itemStack) {
        if (isViewingShulker(player) || isOpen(itemStack))
            return;
        
        getShulkerBox(itemStack, shulkerBox -> {

            TextColor color = shulkerBox.getColor() != null ? TextColor.color(shulkerBox.getColor().getColor().asRGB()) : NamedTextColor.LIGHT_PURPLE; 
            
            player.sendMessage(Component.text("Opening ", color).append(itemStack.effectiveName().color(color)).append(Component.text("...", color)));
            player.playSound(OpenSound);
            var taggedInvInfo = InventoryUtils.OpenInventory(player, itemStack, InventoryType.SHULKER_BOX, instance.ShulkerIDKey);
            ShulkerBoxInfo.create(taggedInvInfo.uuid, taggedInvInfo.newInventory, inventory, player);
        });
    }
    private static void CloseShulker(HumanEntity player) {
        if (!isViewingShulker(player)) {
            return;
        }
        GetShulkerInfo(player, ShulkerBoxInfo::close);
    }
    
    private static boolean isShulker(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && itemStack.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox;
    }
    private static boolean isOpen(ItemStack itemStack) {
        return itemStack.getPersistentDataContainer().has(instance.ShulkerIDKey);
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
        if (event.getAction() == Action.RIGHT_CLICK_AIR && !isViewingShulker(event.getPlayer()) && isShulker(event.getItem())) {
            OpenShulker(event.getPlayer(), event.getPlayer().getInventory(), event.getItem());
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void inventoryOpen(InventoryOpenEvent event) {
        if (isViewingShulker(event.getPlayer())) {
            GetShulkerInfo(event.getPlayer(), info -> {
                if(!info.hasShulkerStack())
                    CloseShulker(event.getPlayer());
            });
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
        
        if (isViewingShulker(event.getWhoClicked())) {
            GetShulkerInfo(event.getWhoClicked(), info -> {
                if (!info.hasShulkerStack()) {
                    CloseShulker(event.getWhoClicked());
                    event.setCancelled(true);
                }
            });
            
            return;
        }
        
        if (currentItem == null || currentItem.isEmpty())
            return;
        
        if  (event.isRightClick() && !isViewingShulker(event.getWhoClicked()) && isShulker(event.getCurrentItem())) {
            OpenShulker(event.getWhoClicked(), event.getClickedInventory(), event.getCurrentItem());
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void itemDropped(PlayerDropItemEvent event) {
        if (isViewingShulker(event.getPlayer()) && isShulker(event.getItemDrop().getItemStack()))
            GetShulkerInfo(event.getPlayer(), shulkerBoxInfo -> {
                ItemStack itemStack = event.getItemDrop().getItemStack();
                if (shulkerBoxInfo.isShulkerStack(itemStack))
                    shulkerBoxInfo.onDropped(itemStack);
            });
    }
    
    static {
        OpenSound = Sound.sound(Key.key("block.shulker_box.open"), Sound.Source.PLAYER, 1, 1.25f);
        CloseSound = Sound.sound(Key.key("block.shulker_box.close"), Sound.Source.PLAYER, 1, 1.25f);
        
        shulkerBoxInstances = new HashSet<>();
    }
    public static void create(JavaPlugin plugin) {
        if (instance == null)
            instance = new ShulkerBoxManager(plugin);

    }
    private static class ShulkerBoxInfo {
        public final String uuid;
        public final Inventory shulkerInventory, ownerInventory;
        public final HumanEntity viewer;
        
        public boolean isShulkerInventory(Inventory inventory) {
            return shulkerInventory.equals(inventory);
        }
        
        private boolean closed = false;
        
        public boolean isShulkerStack(ItemStack itemStack) {
            if (itemStack == null || itemStack.isEmpty())
                return false;
            
            var dataHolder = itemStack.getItemMeta().getPersistentDataContainer();
            assert instance.ShulkerIDKey != null;
            return dataHolder.has(instance.ShulkerIDKey) && Objects.equals(dataHolder.get(instance.ShulkerIDKey, PersistentDataType.STRING), uuid);
        }
        public boolean hasShulkerStack() {
            return getShulkerStack().isPresent();
        }
        public Optional<ItemStack> getShulkerStack() {
            for (@Nullable ItemStack itemStack : ownerInventory.getContents()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    if (isShulkerStack(itemStack))
                        return Optional.of(itemStack);
                }
            }
            
            if (isShulkerStack(viewer.getItemOnCursor()))
                return Optional.of(viewer.getItemOnCursor());
            
            return Optional.empty();
        }

        
        public void close() {
            if (closed)
                return;
            
            getShulkerStack().ifPresent(itemStack -> {
                save(itemStack);
                cleanupStack(itemStack);
            });
            this.closeInventory();
        }
        void closeInventory() {
            if (closed)
                return;
            
            viewer.playSound(CloseSound);
            shulkerBoxInstances.remove(this);
            shulkerInventory.close();
            closed = true;
        }
        
        public void onDropped(ItemStack droppedStack) {
            if (closed)
                return;
            
            save(droppedStack);
            cleanupStack(droppedStack);
            this.closeInventory();
        }
        
        private void cleanupStack(ItemStack itemStack) {
            if (closed)
                return;
            

            assert instance.ShulkerIDKey != null;
            if (itemStack.getPersistentDataContainer().has(instance.ShulkerIDKey))
                itemStack.editPersistentDataContainer(pdc -> pdc.remove(instance.ShulkerIDKey));
            
            
        }
        private void save(ItemStack itemStack) {
            itemStack.setData(DataComponentTypes.CONTAINER, this.ItemContainerContents());
        }
        
        public ItemContainerContents ItemContainerContents() {
            ItemContainerContents.Builder contentsBuilder = ItemContainerContents.containerContents();

            for (@Nullable ItemStack itemStack : shulkerInventory.getContents()) {
                contentsBuilder.add(Objects.requireNonNullElseGet(itemStack, ItemStack::empty));
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