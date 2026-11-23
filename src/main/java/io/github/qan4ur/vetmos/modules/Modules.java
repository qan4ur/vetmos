package io.github.qan4ur.vetmos.modules;

import io.github.qan4ur.vetmos.module.ModuleManager;

/** Registers every module. Order does not matter (the GUI sorts by title). */
public final class Modules {
    private Modules() {}

    public static void registerAll() {
        // registered by category files below
    }

    @SuppressWarnings("unused")
    private static void r(io.github.qan4ur.vetmos.module.Module m) {
        ModuleManager.register(m);
    }
}
