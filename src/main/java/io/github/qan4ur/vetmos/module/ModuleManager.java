package io.github.qan4ur.vetmos.module;

import net.minecraft.entity.Entity;

import java.util.*;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Class<? extends Module>, Module> BY_CLASS = new HashMap<>();

    private ModuleManager() {}

    public static <M extends Module> M register(M module) {
        if (get(module.name) != null) throw new IllegalStateException("Duplicate module name " + module.name);
        MODULES.add(module);
        BY_CLASS.put(module.getClass(), module);
        return module;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> inCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : MODULES) if (m.category == category) list.add(m);
        list.sort(Comparator.comparing(m -> m.title));
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <M extends Module> M get(Class<M> type) {
        return (M) BY_CLASS.get(type);
    }

    public static Module get(String name) {
        for (Module m : MODULES) if (m.name.equalsIgnoreCase(name) || m.title.equalsIgnoreCase(name)) return m;
        return null;
    }

    public static boolean isEnabled(Class<? extends Module> type) {
        Module m = BY_CLASS.get(type);
        return m != null && m.isEnabled();
    }

    // ---------------------------------------------------------------- dispatch

    public static void tick() {
        for (Module m : MODULES) if (m.isEnabled()) safe(m, m::onTick);
    }

    public static void attack(Entity target) {
        for (Module m : MODULES) if (m.isEnabled()) safe(m, () -> m.onAttack(target));
    }

    public static void entityStatus(Entity entity, byte status) {
        for (Module m : MODULES) if (m.isEnabled()) safe(m, () -> m.onEntityStatus(entity, status));
    }

    public static void join() {
        for (Module m : MODULES) if (m.isEnabled()) safe(m, m::onJoin);
    }

    public static void leave() {
        for (Module m : MODULES) if (m.isEnabled()) safe(m, m::onLeave);
    }

    /** Key / mouse press while no screen is open. */
    public static void onKeyPressed(int key) {
        if (key == io.github.qan4ur.vetmos.util.Keys.NONE) return;
        for (Module m : MODULES) if (m.getKey() == key) m.toggleWithFeedback();
        io.github.qan4ur.vetmos.macro.MacroManager.onKey(key);
    }

    /** Runs a module hook; a crashing module is disabled instead of crashing the game. */
    public static void safe(Module module, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            io.github.qan4ur.vetmos.Vetmos.LOG.error("Module {} crashed, disabling it", module.name, t);
            module.setEnabled(false);
            io.github.qan4ur.vetmos.util.ChatUtil.error(module.title + " crashed and was disabled: " + t);
        }
    }
}
