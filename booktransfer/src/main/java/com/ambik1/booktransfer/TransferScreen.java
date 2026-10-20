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

import java.nio.file.Path;
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
	private static final int YELLOW = 0xFFFFD060;
	private static final int MAX_PAGES = WritableBookContent.MAX_PAGES;
	private static final String COLOR_CODES = "0123456789abcdef";
	private static final int[] COLOR_RGB = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
	};

	// Сохраняется между открытиями экрана в рамках одной игры.
	private static String draft = "";
	private static boolean continued;
	// Позиция курсора и выделение, чтобы вернуться к ним после экрана эффектов/настроек.
	private static int selectionStart = -1;
	private static int selectionEnd = -1;

	private final Screen parent;
	private final InteractionHand hand;
	private final int existingPages;
	private final int spareBooks;

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
	private Component notice;

	public TransferScreen(Screen parent, InteractionHand hand) {
		super(Component.translatable("booktransfer.screen.title"));
		this.parent = parent;
		this.hand = hand;
		LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
		this.existingPages = player == null ? 0 : existingPageTexts(player.getItemInHand(hand)).size();
		this.spareBooks = player == null ? 0 : findSpareBooks(player).size();
		if (!ModConfig.get().rememberDraft && !continued) draft = "";
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

	private int bookSlot(LocalPlayer player) {
		return this.hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
	}

	/** Пустые книги с пером на хотбаре и во второй руке — для следующих томов. */
	private List<Integer> findSpareBooks(LocalPlayer player) {
		List<Integer> slots = new ArrayList<>();
		int current = bookSlot(player);
		Inventory inv = player.getInventory();
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (slot != current && isEmptyBook(inv.getItem(slot))) slots.add(slot);
		}
		if (current != Inventory.SLOT_OFFHAND && isEmptyBook(player.getOffhandItem())) slots.add(Inventory.SLOT_OFFHAND);
		return slots;
	}

	private static boolean isEmptyBook(ItemStack stack) {
		return stack.is(Items.WRITABLE_BOOK) && existingPageTexts(stack).isEmpty();
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
		ModConfig cfg = ModConfig.get();
		int leftW = leftWidth();

		// Ряд 1: стили и инструменты.
		int x = MARGIN;
		x = addToolButton(x, Component.literal("B").withStyle(ChatFormatting.BOLD), "booktransfer.format.bold", () -> insert("**", "**"));
		x = addToolButton(x, Component.literal("I").withStyle(ChatFormatting.ITALIC), "booktransfer.format.italic", () -> insert("*", "*"));
		x = addToolButton(x, Component.literal("S").withStyle(ChatFormatting.STRIKETHROUGH), "booktransfer.format.strike", () -> insert("~~", "~~"));
		x = addToolButton(x, Component.literal("U").withStyle(ChatFormatting.UNDERLINE), "booktransfer.format.underline", () -> insert("__", "__"));
		x = addToolButton(x, Component.literal("K").withStyle(ChatFormatting.OBFUSCATED), "booktransfer.format.obfuscated", () -> insert("||", "||"));
		x = addToolButton(x, Component.literal("R"), "booktransfer.format.reset", () -> insert("&r", ""));
		x = addToolButton(x, Component.literal("¶"), "booktransfer.format.pagebreak", () -> insert("\n" + BookPaginator.PAGE_BREAK + "\n", ""));
		x = addToolButton(x, Component.literal("★").withStyle(ChatFormatting.GOLD), "booktransfer.button.effects",
				() -> this.minecraft.gui.setScreen(new EffectsScreen(this)));
		x = addToolButton(x, Component.literal("⚙"), "booktransfer.button.settings",
				() -> this.minecraft.gui.setScreen(new ConfigScreen(this)));
		this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.file"), b -> openFileDialog())
				.pos(x, 6).size(40, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.button.file.tooltip")))
				.build());

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
					cfg.appendMode = !cfg.appendMode;
					cfg.save();
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN, rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.mode.tooltip")))
				.build());
		this.formatButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
					cfg.formatting = !cfg.formatting;
					cfg.save();
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN + (optW + 2), rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.format.tooltip")))
				.build());
		this.flipButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
					cfg.upsideDown = !cfg.upsideDown;
					cfg.save();
					updateOptionLabels();
					markDirty();
				})
				.pos(MARGIN + (optW + 2) * 2, rowA).size(optW, 20)
				.tooltip(Tooltip.create(Component.translatable("booktransfer.option.flip.tooltip")))
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("booktransfer.button.clear"), b -> {
					this.input.setValue("");
					continued = false;
					this.notice = null;
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
		restoreSelection();
		this.setInitialFocus(this.input);
	}

	private MultilineTextField field() {
		return ((MultiLineEditBoxAccessor) this.input).booktransfer$getTextField();
	}

	private void restoreSelection() {
		if (selectionStart < 0) return;
		int len = this.input.getValue().length();
		MultilineTextField field = field();
		field.seekCursor(Whence.ABSOLUTE, Math.min(selectionStart, len));
		if (selectionEnd != selectionStart) {
			field.setSelecting(true);
			field.seekCursor(Whence.ABSOLUTE, Math.min(selectionEnd, len));
			field.setSelecting(false);
		}
		selectionStart = selectionEnd = -1;
	}

	private int addToolButton(int x, Component label, String tooltipKey, Runnable action) {
		this.addRenderableWidget(Button.builder(label, b -> action.run())
				.pos(x, 6).size(20, 20)
				.tooltip(Tooltip.create(Component.translatable(tooltipKey)))
				.build());
		return x + 22;
	}

	private void updateOptionLabels() {
		ModConfig cfg = ModConfig.get();
		this.modeButton.setMessage(Component.translatable(cfg.appendMode ? "booktransfer.option.mode.append" : "booktransfer.option.mode.replace"));
		this.formatButton.setMessage(Component.translatable(cfg.formatting ? "booktransfer.option.format.on" : "booktransfer.option.format.off"));
		this.flipButton.setMessage(Component.translatable(cfg.upsideDown ? "booktransfer.option.flip.on" : "booktransfer.option.flip.off"));
	}

	private void updateSignButton() {
		this.signButton.active = !this.titleBox.getValue().isBlank();
	}

	// ---------- редактирование ----------

	/** Вставляет разметку в позицию курсора; выделенный текст оборачивается. */
	public void insert(String before, String after) {
		MultilineTextField field = field();
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

	/** Перетаскивание .txt файла в окно игры. */
	@Override
	public void onFilesDrop(List<Path> files) {
		for (Path file : files) {
			if (loadFile(file)) return;
		}
	}

	private void openFileDialog() {
		TextFiles.chooseFile(path -> this.minecraft.execute(() -> loadFile(path)));
	}

	private boolean loadFile(Path path) {
		try {
			String text = TextFiles.read(path);
			this.input.setValue(text);
			selectionStart = selectionEnd = -1;
			continued = false;
			this.notice = Component.translatable("booktransfer.file.loaded", path.getFileName().toString());
			this.setFocused(this.input);
			return true;
		} catch (Exception e) {
			this.notice = Component.translatable("booktransfer.file.error", e.getMessage() == null ? e.toString() : e.getMessage());
			return false;
		}
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
		ModConfig cfg = ModConfig.get();
		List<StyledRun> runs = MarkupParser.parse(this.input.getValue(), cfg.formatting, cfg.presetsForParser());
		if (cfg.upsideDown) runs = UpsideDown.flip(runs);
		BookPaginator.Measurer measurer = new BookPaginator.Measurer() {
			@Override
			public int lines(String text) {
				return TransferScreen.this.font.getSplitter().splitLines(text, PAGE_WIDTH, Style.EMPTY).size();
			}

			@Override
			public int width(String text) {
				return TransferScreen.this.font.width(text);
			}
		};
		BookPaginator.Options options = new BookPaginator.Options(cfg.pageNumbers, cfg.tableOfContents,
				cfg.chapterNewPage, cfg.appendMode ? this.existingPages : 0,
				Component.translatable("booktransfer.toc.title").getString());
		return new BookPaginator(measurer, options).paginate(runs);
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

	private static boolean isBlank(List<String> pages) {
		return pages.size() == 1 && pages.get(0).isEmpty();
	}

	private int totalPages() {
		return (ModConfig.get().appendMode ? this.existingPages : 0) + (isBlank(this.pages) ? 0 : this.pages.size());
	}

	// ---------- перенос ----------

	private void transfer(Optional<String> title) {
		LocalPlayer player = this.minecraft.player;
		if (player == null || this.minecraft.getConnection() == null) return;
		ModConfig cfg = ModConfig.get();
		ItemStack stack = player.getItemInHand(this.hand);
		if (!stack.is(Items.WRITABLE_BOOK)) {
			player.sendOverlayMessage(Component.translatable("booktransfer.message.no_book").withStyle(ChatFormatting.RED));
			this.minecraft.gui.setScreen(null);
			return;
		}

		List<String> newPages = buildPages();
		List<String> all = new ArrayList<>(cfg.appendMode ? existingPageTexts(stack) : List.of());
		if (!isBlank(newPages)) all.addAll(newPages);
		if (all.isEmpty()) all.add("");

		// Делим на тома по 100 страниц.
		List<List<String>> volumes = new ArrayList<>();
		for (int i = 0; i < all.size(); i += MAX_PAGES) {
			volumes.add(new ArrayList<>(all.subList(i, Math.min(all.size(), i + MAX_PAGES))));
		}
		List<Integer> slots = new ArrayList<>();
		slots.add(bookSlot(player));
		if (cfg.multiVolume && volumes.size() > 1) slots.addAll(findSpareBooks(player));
		int written = Math.min(volumes.size(), slots.size());

		for (int v = 0; v < written; v++) {
			int slot = slots.get(v);
			List<String> volume = volumes.get(v);
			Optional<String> volumeTitle = title.map(t -> written > 1 ? volumeTitle(t, v + 1) : t);
			this.minecraft.getConnection().send(new ServerboundEditBookPacket(slot, List.copyOf(volume), volumeTitle));
			if (title.isEmpty()) {
				ItemStack target = slot == Inventory.SLOT_OFFHAND ? player.getOffhandItem() : player.getInventory().getItem(slot);
				target.set(DataComponents.WRITABLE_BOOK_CONTENT,
						new WritableBookContent(volume.stream().map(Filterable::passThrough).toList()));
			}
		}

		List<String> leftover = new ArrayList<>();
		for (int v = written; v < volumes.size(); v++) leftover.addAll(volumes.get(v));

		if (leftover.isEmpty()) {
			draft = "";
			continued = false;
			Component msg = written > 1
					? Component.translatable("booktransfer.message.done_volumes", all.size(), written)
					: Component.translatable("booktransfer.message.done", all.size());
			player.sendOverlayMessage(msg.copy().withStyle(ChatFormatting.GREEN));
		} else {
			// Остаток уже разложен и перевёрнут (если нужно) — дальше переносим как есть.
			draft = MarkupParser.toAmpersand(String.join(" ", leftover));
			continued = true;
			cfg.upsideDown = false;
			cfg.formatting = true;
			cfg.save();
			this.minecraft.keyboardHandler.setClipboard(draft);
			player.sendSystemMessage(Component.translatable("booktransfer.message.overflow", leftover.size()).withStyle(ChatFormatting.GOLD));
		}

		if (title.isPresent()) {
			this.minecraft.gui.setScreen(null);
			return;
		}
		WritableBookContent content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		this.minecraft.gui.setScreen(new BookEditScreen(player, stack, this.hand,
				content != null ? content : WritableBookContent.EMPTY));
	}

	/** «Название (2)» — не длиннее 32 символов. */
	private static String volumeTitle(String title, int number) {
		String suffix = " (" + number + ")";
		int max = 32 - suffix.length();
		return (title.length() > max ? title.substring(0, max).trim() : title) + suffix;
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
		boolean multi = ModConfig.get().multiVolume;
		int books = (total + MAX_PAGES - 1) / MAX_PAGES;
		int canWrite = multi ? 1 + this.spareBooks : 1;
		graphics.text(this.font, Component.translatable("booktransfer.info.pages", total, MAX_PAGES),
				px, 8, books > canWrite ? RED : WHITE);
		graphics.text(this.font, Component.translatable("booktransfer.info.chars", this.input.getValue().length()), px, 19, GRAY);
		if (books > 1) {
			Component line = multi
					? Component.translatable("booktransfer.info.books", books, 1 + this.spareBooks)
					: Component.translatable("booktransfer.info.overflow", total - MAX_PAGES);
			graphics.text(this.font, line, px, 30, books > canWrite ? RED : YELLOW);
		} else if (this.notice != null) {
			graphics.text(this.font, this.notice, px, 30, YELLOW);
		} else if (continued) {
			graphics.text(this.font, Component.translatable("booktransfer.info.continued"), px, 30, YELLOW);
		}
	}

	@Override
	public void removed() {
		if (this.input != null) {
			draft = this.input.getValue();
			MultilineTextField field = field();
			if (field.hasSelection()) {
				MultilineTextField.StringView sel = field.getSelected();
				selectionStart = sel.beginIndex();
				selectionEnd = sel.endIndex();
			} else {
				selectionStart = selectionEnd = field.cursor();
			}
		}
		super.removed();
	}
}
