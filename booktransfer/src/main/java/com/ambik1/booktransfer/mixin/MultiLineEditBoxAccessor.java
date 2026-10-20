package com.ambik1.booktransfer.mixin;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Доступ к полю ввода, чтобы вставлять разметку прямо в позицию курсора. */
@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {
	@Accessor("textField")
	MultilineTextField booktransfer$getTextField();
}
