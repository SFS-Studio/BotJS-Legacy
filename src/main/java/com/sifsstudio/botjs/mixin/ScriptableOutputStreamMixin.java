package com.sifsstudio.botjs.mixin;

import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ScriptableOutputStream.class)
public abstract class ScriptableOutputStreamMixin {
    @Shadow
    private Map<Object,String> table;

    /**
     * @author switefaster
     * @reason Fix to {@link ScriptableOutputStream#removeExcludedName(String)} working improperly by
     * mismatching key and value. This would be removed if rhino fixed this issue.
     */
    @Overwrite
    public void removeExcludedName(String name) {
        table.values().remove(name);
    }

    /**
     * @author switefaster
     * @reason Fix to {@link ScriptableOutputStream#hasExcludedName(String)} working improperly by
     * mismatching key and value. This would be removed if rhino fixed this issue.
     */
    @Overwrite
    public boolean hasExcludedName(String name) {
        return table.containsValue(name);
    }
}
