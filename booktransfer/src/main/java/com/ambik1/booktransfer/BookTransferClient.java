package com.ambik1.booktransfer;

import com.ambik1.booktransfer.text.UpsideDown;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class BookTransferClient implements ClientModInitializer {
	public static final String MOD_ID = "booktransfer";

	@Override
	public void onInitializeClient() {
		UpsideDown.loadOverrides(FabricLoader.getInstance().getConfigDir().resolve("booktransfer-flip.txt"));

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof BookEditScreen bookScreen) || client.player == null) return;

			InteractionHand hand = findBookHand(client.player);
			if (hand == null) return;

			// Книга нарисована по центру экрана, кнопку ставим слева от неё.
			int bookLeft = (width - BookEditScreen.IMAGE_WIDTH) / 2;
			int buttonWidth = 110;
			int x = Math.max(4, bookLeft - buttonWidth - 6);
			Button button = Button.builder(Component.translatable("booktransfer.button.open"),
							b -> client.gui.setScreen(new TransferScreen(bookScreen, hand)))
					.pos(x, 20)
					.size(buttonWidth, 20)
					.tooltip(Tooltip.create(Component.translatable("booktransfer.button.open.tooltip")))
					.build();
			Screens.getWidgets(screen).add(button);
		});
	}

	/** Книга с пером открывается из основной руки, если она там есть, иначе из второй. */
	static InteractionHand findBookHand(Player player) {
		if (player.getMainHandItem().is(Items.WRITABLE_BOOK)) return InteractionHand.MAIN_HAND;
		if (player.getOffhandItem().is(Items.WRITABLE_BOOK)) return InteractionHand.OFF_HAND;
		return null;
	}
}
