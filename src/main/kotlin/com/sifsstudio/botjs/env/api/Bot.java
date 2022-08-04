package com.sifsstudio.botjs.env.api;

import com.sifsstudio.botjs.env.ability.Ability;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.annotations.JSFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bot extends ScriptableObject {
    private Map<String, Ability> nameToAbility = new HashMap<>();

    private Map<Object, Object> memories = new HashMap<>();

    public Bot(Set<Ability> abilities) {
        abilities.forEach(it -> nameToAbility.put(it.getId(), it));
    }

    @Override
    public String getClassName() {
        return "Bot";
    }

    @JSFunction
    public Ability getAbility(String id) {
        return nameToAbility.get(id);
    }

    @JSFunction
    public Object getMemory(Object memId) {
        return memories.get(memId);
    }

    @JSFunction
    public void setMemory(Object memId, Object mem) {
        memories.put(memId, mem);
    }
}
