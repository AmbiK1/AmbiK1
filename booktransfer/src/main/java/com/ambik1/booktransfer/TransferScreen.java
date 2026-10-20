package com.ambik1.booktransfer;

import com.ambik1.booktransfer.mixin.MultiLineEditBoxAccessor;
import com.ambik1.booktransfer.text.BookPaginator;
import com.ambik1.booktransfer.text.MarkupParser;
import com.ambik1.booktransfer.text.StyledRun;
import com.ambik1.booktransfer.text.UpsideDown;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Экран, куда вставляется текст для переноса в книгу. */
public class TransferScreen extends Screen {
	private static final int PAGE_WIDTH = BookEditScreen.TEXT_WIDTH;
	private static final int LINE_HEIGHT = 9;
	private static final int PANEL_PADDING = 6;
	private static final int PANEL_WIDTH = PAGE_WIDTH + PANEL_PADDING * 2;
	private static final int PANEL_HEIGHT = BookPaginator.MAX_LINES * LINE_HEIGHT + PANEL_PADDING * 2;
	private static final int MARGIN = 8;
	private static final int TOP = 50;
	private static final int PAPER = 0xFFF4EBD3;
	private static final int PAPER_EDGE = 0xFF8B6B3E;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF6060;
	private static final String COLOR_CODES = "0123456789abcdef";
	private static final int[] COLOR_RGB = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
	};

	// Сохраняется между открытиями экрана в рамках одной игры.
	private static String draft = "";
	private static boolean appendMode;
	private static boolean formatting = true;
	private static boolean upsideDown;
	private static boolean continued;

	private final Screen parent;
	private final InteractionHand hand;
	private final int existingPages;

	private MultiLineEditBox input;
	private EditBox titleBox;
	private Button modeButton;
	private Button formatButton;
	private Button flipButton;
	private Button signButton;
	private Button prevButton;
	private Button nextButton;

	private List<String> pages = List.of("");
	private int previewPage;
	private boolean dirty = true;
	private int idleTicks;

	public TransferScreen(Screen parent, InteractionHand hand) {
		super(Component.translatable("booktransfer.screen.title"));
		this.parent = parent;
		this.hand = hand;
		this.existingPages = countExistingPages();
	}

	private int countExistingPages() {
		LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
		if (player == null) return 0;
		return existingPageTexts(player.getItemInHand(hand)).size();
	}

	private static List<String> existingPageTexts(ItemStack stack) {
		List<String> result = new ArrayList<>();
		WritableBookContent content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		if (content != null) {
			content.pages().forEach(p -> result.add(p.raw()));
		}
		while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	// ---------- раскладка ----------

	private int panelX() {
		return this.width - MARGIN - PANEL_WIDTH;
	}

	private int leftWidth() {
		return panelX() - MARGIN * 2;
	}

	@Override
	protected void init() {
		int leftW = leftWidth();

		// Ряд 1: стили.
		int x = MARGIN;
		x = addFormatButton(x, "B", ChatFormatting.BOLD, "booktransfer.format.bold", "**", "**");
		x = addFormatButton(x, "I", ChatFormatting.ITALIC, "booktransfer.format.italic", "*", "*");
		x = addFormatButton(x, "S", ChatFormatting.STRIKETHROUGH, "booktransfer.format.strike", "~~", "~~");
		x = addFormatButton(x, "U", ChatFormatting.UNDERLINE, "booktransfer.format.underline", "__", "__");
		x = addFormatButton(x, "K", ChatFormatting.OBFUSCATED, "booktransfer.format.obfuscated", "||", "||");
		x = addFormatButton(x, "R", ChatFormatting.RESET, "booktransfer.format.reset", "&r", "");
		addFormatButton(x, "¶", ChatFormatting.RESET, "booktransfer.format.pagebreak", "\n" + BookPaginator.PAGE_BREAK + "\n", "");

		// Ряд 2: цвета.
		int colorSize = 14;
		int colorStep = Math.min(16, Math.max(colorSize, leftW / 16));
		for (int i = 0; i < COLOR_CODES.length(); i++) {
			char code = COLOR_CODES.charAt(i);
			this.addRenderableWidget(Button.builder(Component.literal("■").withColor(COLOR_RGB[i]),
							b -> insert("&" + code, ""))
					.pos(MARGIN + i * colorStep, 28)
					.size(colorSize, colorSize)
					.tooltip(Tooltip.create(Component.translatable("booktransfer.format.color", String.valueOf(code))))
					.build());
		}

		// Поле ввода.
		int inputHeight = Math.max(40, this.height - TOP - 56);
		this.input = MultiLineEditBox.builder()
				.setX(MARGIN)
				.setY(TOP)
				.setPlaceholder(Component.translatable("booktransfer.input.placeholder").withStyle(ChatFormatting.DARK_GRAY))
				.build(this.font, leftW, inputHeight, Component.translatable("booktransfer.screen.title"));
		this.input.setCharacterLimit(Integer.MAX_VALUE);
		this.input.setValue(draft);
		this.input.setValueListener(value -> {
			draft = value;
			markDirty();
		});
		this.addRenderableWidget(this.input);

		// Ряд настроек.
		int rowA = this.height - 50;
		int optW = (leftW - 6) / 4;
		this.modeButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
					appendMode = !appendMode;
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN, rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.mode.tooltip")))
				.build());
		this.formatButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
					formatting = !formatting;
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN + (optW + 2), rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.format.tooltip")))
				.build());
		this.flipButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
					upsideDown = !upsideDown;
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN + (optW + 2) * 2, rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.flip.tooltip")))
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.clear"), b -> {
					this.input.setValue("");
					continued = false;
					this.setFocused(this.input);
				})
				.pos(MARGIN + (optW + 2) * 3, rowA).size(optW, 20)
				.build());

		// Нижний ряд слева: название + «Подписать».
		int rowB = this.height - 26;
		int signW = 70;
		this.titleBox = new EditBox(this.font, MARGIN, rowB, leftW - signW - 4, 20,
				Component.translatable("booktransfer.title.placeholder"));
		this.titleBox.setMaxLength(32);
		this.titleBox.setHint(Component.translatable("booktransfer.title.placeholder"));
		this.titleBox.setResponder(value -> updateSignButton());
		this.addRenderableWidget(this.titleBox);
		this.signButton = this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.sign"),
						b -> transfer(Optional.of(this.titleBox.getValue().trim())))
				.pos(MARGIN + leftW - signW, rowB).size(signW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.button.sign.tooltip")))
				.build());

		// Справа: листание предпросмотра и главные кнопки.
		int px = panelX();
		int navY = TOP + PANEL_HEIGHT + 4;
		this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> turnPreview(-1))
				.pos(px, navY).size(20, 20).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> turnPreview(1))
				.pos(px + PANEL_WIDTH - 20, navY).size(20, 20).build());

		int half = (PANEL_WIDTH - 4) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.cancel"), b -> this.onClose())
				.pos(px, rowB).size(half, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.transfer").withStyle(ChatFormatting.BOLD),
						b -> transfer(Optional.empty()))
				.pos(px + half + 4, rowB).size(half, 20).build());

		updateOptionLabels();
		updateSignButton();
		recompute();
		this.setInitialFocus(this.input);
	}

	private int addFormatButton(int x, String label, ChatFormatting style, String tooltipKey, String before, String after) {
		Component text = style == ChatFormatting.RESET ? Component.literal(label) : Component.literal(label).withStyle(style);
		this.addRenderableWidget(Button.builder(text, b -> insert(before, after))
				.pos(x, 6).size(20, 20)
				.tooltip(Tooltip.create(Component.translatable(tooltipKey)))
				.build());
		return x + 22;
	}

	private void updateOptionLabels() {
		this.modeButton.setMessage(Component.translatable(appendMode ? "booktransfer.option.mode.append" : "booktransfer.option.mode.replace"));
		this.formatButton.setMessage(Component.translatable(formatting ? "booktransfer.option.format.on" : "booktransfer.option.format.off"));
		this.flipButton.setMessage(Component.translatable(upsideDown ? "booktransfer.option.flip.on" : "booktransfer.option.flip.off"));
	}

	private void updateSignButton() {
		this.signButton.active = !this.titleBox.getValue().isBlank();
	}

	// ---------- редактирование ----------

	/** Вставляет разметку в позицию курсора; выделенный текст оборачивается. */
	private void insert(String before, String after) {
		MultilineTextField field = ((MultiLineEditBoxAccessor) this.input).booktransfer$getTextField();
		String selected = field.hasSelection() ? field.getSelectedText() : "";
		String closing = after;
		// Цвет для выделенного куска закрываем сбросом, чтобы он не «протёк» дальше.
		if (after.isEmpty() && !selected.isEmpty() && before.startsWith("&") && !before.equals("&r")) {
			closing = "&r";
		}
		field.insertText(before + selected + closing);
		if (selected.isEmpty() && !closing.isEmpty()) {
			field.seekCursor(Whence.RELATIVE, -closing.length());
		}
		this.setFocused(this.input);
		draft = this.input.getValue();
		markDirty();
	}

	private void markDirty() {
		this.dirty = true;
		this.idleTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		// Пересчитываем страницы с небольшой задержкой, чтобы не тормозить при наборе.
		if (this.dirty && ++this.idleTicks >= 3) {
			recompute();
		}
	}

	private List<String> buildPages() {
		List<StyledRun> runs = MarkupParser.parse(this.input.getValue(), formatting);
		if (upsideDown) runs = UpsideDown.flip(runs);
		BookPaginator paginator = new BookPaginator(
				text -> this.font.getSplitter().splitLines(text, PAGE_WIDTH, Style.EMPTY).size());
		return paginator.paginate(runs);
	}

	private void recompute() {
		this.pages = buildPages();
		this.dirty = false;
		this.previewPage = Math.max(0, Math.min(this.previewPage, this.pages.size() - 1));
		updateNavButtons();
	}

	private void turnPreview(int delta) {
		this.previewPage = Math.max(0, Math.min(this.previewPage + delta, this.pages.size() - 1));
		updateNavButtons();
	}

	private void updateNavButtons() {
		if (this.prevButton == null) return;
		this.prevButton.active = this.previewPage > 0;
		this.nextButton.active = this.previewPage < this.pages.size() - 1;
	}

	private boolean hasText() {
		return !(this.pages.size() == 1 && this.pages.get(0).isEmpty());
	}

	private int totalPages() {
		return (appendMode ? this.existingPages : 0) + (hasText() ? this.pages.size() : 0);
	}

	// ---------- перенос ----------

	private void transfer(Optional<String> title) {
		LocalPlayer player = this.minecraft.player;
		if (player == null || this.minecraft.getConnection() == null) return;
		ItemStack stack = player.getItemInHand(this.hand);
		if (!stack.is(Items.WRITABLE_BOOK)) {
			player.sendOverlayMessage(Component.translatable("booktransfer.message.no_book").withStyle(ChatFormatting.RED));
			this.minecraft.gui.setScreen(null);
			return;
		}

		List<String> newPages = buildPages();
		List<String> all = new ArrayList<>(appendMode ? existingPageTexts(stack) : List.of());
		if (!(newPages.size() == 1 && newPages.get(0).isEmpty())) all.addAll(newPages);

		int max = WritableBookContent.MAX_PAGES;
		List<String> leftover = List.of();
		if (all.size() > max) {
			leftover = new ArrayList<>(all.subList(max, all.size()));
			all = new ArrayList<>(all.subList(0, max));
		}
		if (all.isEmpty()) all.add("");

		int slot = this.hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
		this.minecraft.getConnection().send(new ServerboundEditBookPacket(slot, List.copyOf(all), title));

		if (leftover.isEmpty()) {
			draft = "";
			continued = false;
			player.sendOverlayMessage(Component.translatable("booktransfer.message.done", all.size()).withStyle(ChatFormatting.GREEN));
		} else {
			// Остаток уже разложен и перевёрнут (если нужно) — дальше переносим как есть.
			draft = MarkupParser.toAmpersand(String.join(" ", leftover));
			continued = true;
			upsideDown = false;
			formatting = true;
			this.minecraft.keyboardHandler.setClipboard(draft);
			player.sendSystemMessage(Component.translatable("booktransfer.message.overflow", leftover.size()).withStyle(ChatFormatting.GOLD));
		}

		if (title.isPresent()) {
			this.minecraft.gui.setScreen(null);
			return;
		}
		WritableBookContent content = new WritableBookContent(all.stream().map(Filterable::passThrough).toList());
		stack.set(DataComponents.WRITABLE_BOOK_CONTENT, content);
		this.minecraft.gui.setScreen(new BookEditScreen(player, stack, this.hand, content));
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	// ---------- отрисовка ----------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int px = panelX();
		graphics.fill(px - 1, TOP - 1, px + PANEL_WIDTH + 1, TOP + PANEL_HEIGHT + 1, PAPER_EDGE);
		graphics.fill(px, TOP, px + PANEL_WIDTH, TOP + PANEL_HEIGHT, PAPER);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int px = panelX();

		// Предпросмотр страницы так, как она будет выглядеть в книге.
		String page = this.pages.get(this.previewPage);
		List<FormattedCharSequence> lines = this.font.split(Component.literal(page), PAGE_WIDTH);
		int shown = Math.min(lines.size(), BookPaginator.MAX_LINES);
		for (int i = 0; i < shown; i++) {
			graphics.text(this.font, lines.get(i), px + PANEL_PADDING, TOP + PANEL_PADDING + i * LINE_HEIGHT, 0xFF000000, false);
		}

		int navY = TOP + PANEL_HEIGHT + 4;
		graphics.centeredText(this.font,
				Component.translatable("booktransfer.preview.page", this.previewPage + 1, this.pages.size()),
				px + PANEL_WIDTH / 2, navY + 6, WHITE);

		// Сводка справа сверху.
		int total = totalPages();
		int max = WritableBookContent.MAX_PAGES;
		graphics.text(this.font, Component.translatable("booktransfer.info.pages", total, max), px, 8, total > max ? RED : WHITE);
		graphics.text(this.font, Component.translatable("booktransfer.info.chars", this.input.getValue().length()), px, 19, GRAY);
		if (total > max) {
			graphics.text(this.font, Component.translatable("booktransfer.info.overflow", total - max), px, 30, RED);
		} else if (continued) {
			graphics.text(this.font, Component.translatable("booktransfer.info.continued"), px, 30, 0xFFFFD060);
		}
	}

	@Override
	public void removed() {
		draft = this.input != null ? this.input.getValue() : draft;
		super.removed();
	}
}
