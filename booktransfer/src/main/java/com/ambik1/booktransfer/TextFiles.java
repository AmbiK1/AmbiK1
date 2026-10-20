package com.ambik1.booktransfer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Загрузка текста из файлов. */
public final class TextFiles {
	private static final long MAX_SIZE = 4L * 1024 * 1024;

	private TextFiles() {
	}

	/** Читает текстовый файл: UTF-8, а если не получилось — Windows-1251 (частый случай для русских .txt). */
	public static String read(Path path) throws IOException {
		if (Files.size(path) > MAX_SIZE) throw new IOException("файл больше 4 МБ");
		byte[] bytes = Files.readAllBytes(path);
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
		} catch (CharacterCodingException e) {
			return new String(bytes, Charset.forName("windows-1251"));
		}
	}

	/** Открывает системный диалог выбора файла в отдельном потоке. */
	public static void chooseFile(Consumer<Path> onChosen) {
		Thread thread = new Thread(() -> {
			String result;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer filters = stack.mallocPointer(2);
				filters.put(stack.UTF8("*.txt"));
				filters.put(stack.UTF8("*.md"));
				filters.flip();
				result = TinyFileDialogs.tinyfd_openFileDialog("Book Transfer", null, filters, "Text (*.txt, *.md)", false);
			} catch (Throwable t) {
				BookTransferClient.LOGGER.warn("Не удалось открыть диалог выбора файла", t);
				return;
			}
			if (result != null) onChosen.accept(Path.of(result));
		}, "BookTransfer file dialog");
		thread.setDaemon(true);
		thread.start();
	}
}
