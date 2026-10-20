package com.ambik1.booktransfer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Экран настроек (открывается кнопкой ⚙ и из Mod Menu). */
public class ConfigScreen extends Screen {
	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("booktransfer.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig cfg = ModConfig.get();
		int w = 220;
		int x = this.width / 2 - w / 2;
		int y = 36;
		y = toggle(x, y, w, "pageNumbers", c -> c.pageNumbers, (c) -> c.pageNumbers = !c.pageNumbers);
		y = toggle(x, y, w, "tableOfContents", c -> c.tableOfContents, (c) -> c.tableOfContents = !c.tableOfContents);
		y = toggle(x, y, w, "chapterNewPage", c -> c.chapterNewPage, (c) -> c.chapterNewPage = !c.chapterNewPage);
		y = toggle(x, y, w, "multiVolume", c -> c.multiVolume, (c) -> c.multiVolume = !c.multiVolume);
		y = toggle(x, y, w, "appendMode", c -> c.appendMode, (c) -> c.appendMode = !c.appendMode);
		y = toggle(x, y, w, "formatting", c -> c.formatting, (c) -> c.formatting = !c.formatting);
		y = toggle(x, y, w, "rememberDraft", c -> c.rememberDraft, (c) -> c.rememberDraft = !c.rememberDraft);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.pos(this.width / 2 - 75, Math.max(y + 8, this.height - 28)).size(150, 20).build());
	}

	private int toggle(int x, int y, int w, String key, Predicate<ModConfig> getter, Consumer<ModConfig> flip) {
		ModConfig cfg = ModConfig.get();
		Button button = Button.builder(label(key, getter.test(cfg)), b -> {
					flip.accept(cfg);
					cfg.save();
					b.setMessage(label(key, getter.test(cfg)));
				})
				.pos(x, y).size(w, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.config." + key + ".tooltip")))
				.build();
		this.addRenderableWidget(button);
		return y + 24;
	}

	private static Component label(String key, boolean value) {
		return Component.translatable("booktransfer.config." + key)
				.append(": ")
				.append(Component.translatable(value ? "options.on" : "options.off"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
