package com.kyluua.verity.registry;

import com.kyluua.verity.Verity;
import com.kyluua.verity.item.VerityBoxItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Items and the creative tab for Verity.
 *
 * <p>There is exactly one item in the first version: the <b>Verity Box</b> - the
 * cardboard box (with a mail-slot) that Verity ships inside. Right-clicking it
 * "opens" the box and releases the companion. See {@link VerityBoxItem}.</p>
 */
public final class VerityItems {

    private VerityItems() {}

    /** Deferred register for all items in this mod. */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Verity.MOD_ID);

    /** Deferred register for the mod's creative tab. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Verity.MOD_ID);

    /** The box Verity arrives in. One per player at world start (handed out on first join). */
    public static final RegistryObject<Item> VERITY_BOX = ITEMS.register("verity_box",
            () -> new VerityBoxItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .fireResistant()));

    /** Creative tab that surfaces the box so builders/admins can grab one. */
    public static final RegistryObject<CreativeModeTab> VERITY_TAB = CREATIVE_TABS.register("verity_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.verity"))
                    .icon(() -> VERITY_BOX.get().getDefaultInstance())
                    .displayItems((params, output) -> output.accept(VERITY_BOX.get()))
                    .build());
}
