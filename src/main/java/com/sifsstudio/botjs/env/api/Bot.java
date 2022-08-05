package com.sifsstudio.botjs.env.api;

import com.sifsstudio.botjs.env.ability.Ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bot {
    private final Map<String, Ability> nameToAbility = new HashMap<>();

    private final Map<Object, Object> memories = new HashMap<>();

    public Bot(Set<Ability> abilities) {
        abilities.forEach(it -> nameToAbility.put(it.getId(), it));
    }

    @SuppressWarnings("unused")
    public Ability getAbility(String id) {
        return nameToAbility.get(id);
    }

    @SuppressWarnings("unused")
    public Object getMemory(Object memId) {
        return memories.get(memId);
    }

    @SuppressWarnings("unused")
    public void setMemory(Object memId, Object mem) {
        memories.put(memId, mem);
    }
}
