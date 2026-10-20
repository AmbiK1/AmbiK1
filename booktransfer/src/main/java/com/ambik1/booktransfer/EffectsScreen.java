package com.ambik1.booktransfer;

import com.ambik1.booktransfer.text.MarkupParser;
import com.ambik1.booktransfer.text.StyledRun;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Эффекты текста и пользовательские шаблоны стилей. */
public class EffectsScreen extends Screen {
	private static final int MAX_PRESETS_SHOWN = 8;
	private final TransferScreen parent;
	private EditBox nameBox;
	private EditBox codesBox;
	private Button addButton;

	public EffectsScreen(TransferScreen parent) {
		super(Component.translatable("booktransfer.effects.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		// Эффекты: по две колонки.
		String[][] effects = {
				{"rainbow", "[rainbow]", "[/rainbow]"},
				{"wide", "[wide]", "[/wide]"},
				{"small", "[small]", "[/small]"},
				{"sup", "[sup]", "[/sup]"},
				{"mirror", "[mirror]", "[/mirror]"},
				{"flip", "[flip]", "[/flip]"},
				{"center", "[center]", "[/center]"},
				{"heading", "\n# ", ""},
		};
		int colW = 120;
		int y = 30;
		for (int i = 0; i < effects.length; i++) {
			String[] e = effects[i];
			int x = i % 2 == 0 ? cx - colW - 2 : cx + 2;
			this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.effect." + e[0]), b -> pick(e[1], e[2]))
					.pos(x, y).size(colW, 20)
					.tooltip(Tooltip.create(Component.translatable("booktransfer.effect." + e[0] + ".tooltip")))
					.build());
			if (i % 2 == 1) y += 22;
		}

		// Шаблоны.
		y += 14;
		ModConfig cfg = ModConfig.get();
		List<Map.Entry<String, String>> presets = new ArrayList<>(cfg.presets.entrySet());
		for (int i = 0; i < Math.min(presets.size(), MAX_PRESETS_SHOWN); i++) {
			Map.Entry<String, String> p = presets.get(i);
			int x = i % 2 == 0 ? cx - colW - 2 : cx + 2;
			this.addRenderableWidget(Button.builder(sample(p.getKey(), p.getValue()), b -> pick("[" + p.getKey() + "]", "[/]"))
					.pos(x, y).size(colW - 22, 20)
					.tooltip(Tooltip.create(Component.literal("[" + p.getKey() + "]…[/]   " + p.getValue())))
					.build());
			this.addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), b -> {
						cfg.presets.remove(p.getKey());
						cfg.save();
						this.rebuildWidgets();
					})
					.pos(x + colW - 20, y).size(20, 20)
					.tooltip(Tooltip.create(Component.translatable("booktransfer.preset.delete")))
					.build());
			if (i % 2 == 1) y += 22;
		}
		if (Math.min(presets.size(), MAX_PRESETS_SHOWN) % 2 == 1) y += 22;

		// Новый шаблон: имя + коды.
		y += 4;
		this.nameBox = new EditBox(this.font, cx - colW - 2, y, 90, 20, Component.translatable("booktransfer.preset.name"));
		this.nameBox.setMaxLength(16);
		this.nameBox.setHint(Component.translatable("booktransfer.preset.name"));
		this.nameBox.setResponder(v -> updateAdd());
		this.addRenderableWidget(this.nameBox);
		this.codesBox = new EditBox(this.font, cx - colW + 92, y, 80, 20, Component.translatable("booktransfer.preset.codes"));
		this.codesBox.setMaxLength(24);
		this.codesBox.setHint(Component.literal("&6&l"));
		this.codesBox.setResponder(v -> updateAdd());
		this.addRenderableWidget(this.codesBox);
		this.addButton = this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.preset.add"), b -> {
					String name = this.nameBox.getValue().trim().toLowerCase(Locale.ROOT);
					cfg.presets.put(name, this.codesBox.getValue().trim());
					cfg.save();
					this.rebuildWidgets();
				})
				.pos(cx + 54, y).size(colW - 52, 20).build());
		updateAdd();

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
				.pos(cx - 75, Math.max(y + 30, this.height - 28)).size(150, 20).build());
	}

	private void updateAdd() {
		String name = this.nameBox.getValue().trim();
		String codes = this.codesBox.getValue().trim();
		this.addButton.active = name.matches("[\\p{L}\\p{N}_]+") && codes.matches("([&§][0-9a-fk-orA-FK-OR])+");
	}

	/** Название шаблона, нарисованное его же стилем. */
	private static Component sample(String name, String codes) {
		List<StyledRun> runs = MarkupParser.parse(codes + name, true);
		StringBuilder sb = new StringBuilder();
		for (StyledRun r : runs) sb.append(r.style().fullCodes()).append(r.text());
		return Component.literal(sb.toString());
	}

	private void pick(String before, String after) {
		this.minecraft.gui.setScreen(this.parent);
		this.parent.insert(before, after);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
