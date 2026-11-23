package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Profile list (click to switch, ✕ to delete with confirmation), "new profile" field and share-code buttons. */
public class ProfilePopover extends Popover {
    private static final int ROW = 14, MAX_ROWS = 7;
    private final Runnable onChanged;
    private final SearchField nameField = new SearchField();
    private List<String> profiles = new ArrayList<>();
    private final Map<String, float[]> hovers = new HashMap<>();
    private String confirmDelete;
    private int first;
    private float createHover, createFlash, expHover, expFlash, impHover, impFlash;

    public ProfilePopover(Runnable onChanged) {
        this.onChanged = onChanged;
        w = 176;
        nameField.icon = false;
        nameField.maxLen = 32;
        nameField.placeholder = () -> Lang.ui("profile-name", "Имя профиля…");
        nameField.onEnter = this::create;
    }

    @Override
    public void show(int anchorX, int anchorY) {
        super.show(anchorX, anchorY);
        refresh();
    }

    @Override
    protected void onHide() {
        nameField.setFocused(false);
        confirmDelete = null;
    }

    private void refresh() {
        profiles = ConfigManager.listProfiles();
        first = Math.max(0, Math.min(first, profiles.size() - MAX_ROWS));
    }

    private int rows() {
        return Math.min(MAX_ROWS, profiles.size());
    }

    private int listY() {
        return y + 16;
    }

    private int newY() {
        return listY() + rows() * ROW + 6;
    }

    @Override
    protected int contentHeight() {
        return newY() - y + 10 + 18 + 22;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mx, int my, Tooltip tip) {
        caption(ctx, Lang.ui("profiles", "Профили"), x + 8, y + 6);
        String current = ConfigManager.currentProfile();
        int ry = listY();
        for (int i = first; i < Math.min(profiles.size(), first + MAX_ROWS); i++) {
            String p = profiles.get(i);
            float[] hv = hovers.computeIfAbsent(p, k -> new float[2]);
            boolean hov = Paint.inside(mx, my, x + 4, ry, w - 8, ROW);
            boolean xHov = hov && Paint.inside(mx, my, x + w - 18, ry, 14, ROW);
            hv[0] = Anim.approach(hv[0], hov ? 1 : 0, 18);
            hv[1] = Anim.approach(hv[1], xHov ? 1 : 0, 18);
            boolean cur = p.equalsIgnoreCase(current);
            if (cur) Paint.round(ctx, x + 4, ry, w - 8, ROW, 3, Palette.accentA(40));
            if (hv[0] > 0.01f) Paint.round(ctx, x + 4, ry, w - 8, ROW, 3, Anim.withAlpha(0xFFFFFFFF, Math.round(14 * hv[0])));

            if (p.equals(confirmDelete)) {
                Paint.text(ctx, Paint.trim(Lang.ui("delete-confirm", "Удалить") + " «" + p + "»?", w - 70), x + 9, ry + 3, 0xFFE5484D);
                drawSmall(ctx, x + w - 58, ry + 1, 26, Lang.ui("yes", "Да"), Paint.inside(mx, my, x + w - 58, ry + 1, 26, ROW - 2), true);
                drawSmall(ctx, x + w - 30, ry + 1, 26, Lang.ui("no", "Нет"), Paint.inside(mx, my, x + w - 30, ry + 1, 26, ROW - 2), false);
            } else {
                if (cur) Paint.circle(ctx, x + 10, ry + ROW / 2, 2, Palette.accent());
                Paint.text(ctx, Paint.trim(p, w - 40), x + 16, ry + 3, cur ? Palette.TEXT : Anim.mix(Palette.TEXT_MUTED, Palette.TEXT_IDLE, hv[0]));
                if (!cur && hv[0] > 0.02f) {
                    Paint.text(ctx, "✕", x + w - 14, ry + 3, Anim.alpha(Anim.mix(Palette.TEXT_MUTED, 0xFFE5484D, hv[1]), hv[0]));
                }
                if (xHov && !cur) tip.offer("del:" + p, Lang.ui("delete-profile", "Удалить профиль"));
                else if (hov) tip.offer("prof:" + p, cur ? Lang.ui("profile-current", "Текущий профиль") : Lang.ui("profile-switch", "Переключиться на этот профиль"));
            }
            ry += ROW;
        }
        if (profiles.size() > MAX_ROWS) {
            int trackH = MAX_ROWS * ROW;
            int thumbH = Math.max(10, trackH * MAX_ROWS / profiles.size());
            int thumbY = listY() + (trackH - thumbH) * first / Math.max(1, profiles.size() - MAX_ROWS);
            Paint.rect(ctx, x + w - 3, thumbY, 1, thumbH, Palette.accentA(160));
        }

        // new profile
        int ny = newY();
        Paint.rect(ctx, x + 8, ny - 3, w - 16, 1, 0x18FFFFFF);
        caption(ctx, Lang.ui("new-profile", "Новый профиль (копия текущего)"), x + 8, ny + 1);
        int fy = ny + 9;
        int bw = Paint.width(Lang.ui("create", "Создать")) + 12;
        nameField.x = x + 8;
        nameField.y = fy;
        nameField.w = w - 16 - bw - 4;
        nameField.h = 14;
        nameField.render(ctx, mx, my);
        boolean ch = Paint.inside(mx, my, x + w - 8 - bw, fy, bw, 14);
        createHover = Anim.approach(createHover, ch ? 1 : 0, 18);
        createFlash = Anim.approach(createFlash, 0, 6);
        ButtonWidget.draw(ctx, x + w - 8 - bw, fy, bw, 14, Lang.ui("create", "Создать"), createHover, createFlash);

        // share codes
        int cy = fy + 20;
        int half = (w - 20) / 2;
        boolean eh = Paint.inside(mx, my, x + 8, cy, half, 14), ih = Paint.inside(mx, my, x + 12 + half, cy, half, 14);
        expHover = Anim.approach(expHover, eh ? 1 : 0, 18);
        impHover = Anim.approach(impHover, ih ? 1 : 0, 18);
        expFlash = Anim.approach(expFlash, 0, 6);
        impFlash = Anim.approach(impFlash, 0, 6);
        ButtonWidget.draw(ctx, x + 8, cy, half, 14, Lang.ui("export-code", "Экспорт кода"), expHover, expFlash);
        ButtonWidget.draw(ctx, x + 12 + half, cy, half, 14, Lang.ui("import-code", "Импорт кода"), impHover, impFlash);
        if (eh) tip.offer("export", Lang.ui("export-desc", "Скопировать все настройки и HUD в виде кода, чтобы отправить другу."));
        if (ih) tip.offer("import", Lang.ui("import-desc", "Применить код из буфера обмена к текущему профилю."));
    }

    private static void drawSmall(DrawContext ctx, int bx, int by, int bw, String label, boolean hov, boolean danger) {
        int c = danger ? 0xFFE5484D : Palette.CONTROL_LINE_HOVER;
        Paint.round(ctx, bx, by, bw, ROW - 2, 3, hov ? Anim.withAlpha(c, 90) : Palette.CONTROL);
        Paint.outline(ctx, bx, by, bw, ROW - 2, 3, Anim.withAlpha(c, hov ? 255 : 150));
        Paint.textCentered(ctx, label, bx + bw / 2, by + 2, Palette.TEXT);
    }

    private void create() {
        String name = nameField.text().trim();
        if (!ConfigManager.isValidProfileName(name)) {
            Toasts.show(Lang.ui("toast-bad-name", "Недопустимое имя профиля"), true);
            return;
        }
        if (ConfigManager.profileExists(name) || !ConfigManager.createProfile(name, true)) {
            Toasts.show(Lang.ui("toast-exists", "Такой профиль уже есть"), true);
            return;
        }
        ConfigManager.switchProfile(name);
        nameField.setText("");
        nameField.setFocused(false);
        createFlash = 1;
        refresh();
        onChanged.run();
        Toasts.show(Lang.ui("toast-profile-created", "Профиль создан") + ": " + name);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOver(mx, my)) return false;
        String current = ConfigManager.currentProfile();
        int ry = listY();
        for (int i = first; i < Math.min(profiles.size(), first + MAX_ROWS); i++) {
            String p = profiles.get(i);
            if (Paint.inside(mx, my, x + 4, ry, w - 8, ROW) && button == 0) {
                if (p.equals(confirmDelete)) {
                    if (Paint.inside(mx, my, x + w - 58, ry + 1, 26, ROW - 2)) {
                        if (ConfigManager.deleteProfile(p)) Toasts.show(Lang.ui("toast-deleted", "Профиль удалён") + ": " + p);
                        confirmDelete = null;
                        refresh();
                    } else if (Paint.inside(mx, my, x + w - 30, ry + 1, 26, ROW - 2)) confirmDelete = null;
                    return true;
                }
                boolean cur = p.equalsIgnoreCase(current);
                if (!cur && Paint.inside(mx, my, x + w - 18, ry, 14, ROW)) {
                    confirmDelete = p;
                    Widget.click(0.9f);
                    return true;
                }
                if (!cur) {
                    ConfigManager.switchProfile(p);
                    Widget.click(1.2f);
                    onChanged.run();
                    Toasts.show(Lang.ui("toast-switched", "Профиль") + ": " + p);
                }
                return true;
            }
            ry += ROW;
        }
        confirmDelete = null;
        if (nameField.mouseClicked(mx, my, button)) return true;
        int ny = newY(), fy = ny + 9;
        int bw = Paint.width(Lang.ui("create", "Создать")) + 12;
        if (button == 0 && Paint.inside(mx, my, x + w - 8 - bw, fy, bw, 14)) {
            Widget.click(1.1f);
            create();
            return true;
        }
        int cy = fy + 20, half = (w - 20) / 2;
        if (button == 0 && Paint.inside(mx, my, x + 8, cy, half, 14)) {
            expFlash = 1;
            Widget.click(1.1f);
            try {
                MinecraftClient.getInstance().keyboard.setClipboard(ConfigManager.exportCode());
                Toasts.show(Lang.ui("toast-code-copied", "Код скопирован"));
            } catch (RuntimeException e) {
                Toasts.show(Lang.ui("toast-code-error", "Не удалось создать код"), true);
            }
            return true;
        }
        if (button == 0 && Paint.inside(mx, my, x + 12 + half, cy, half, 14)) {
            impFlash = 1;
            Widget.click(1.1f);
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (ConfigManager.importCode(clip)) {
                onChanged.run();
                Toasts.show(Lang.ui("toast-code-imported", "Настройки из кода применены"));
            } else {
                Toasts.show(Lang.ui("toast-code-invalid", "В буфере нет кода Vetmos"), true);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) return false;
        if (profiles.size() > MAX_ROWS) first = Math.max(0, Math.min(profiles.size() - MAX_ROWS, first - (int) Math.signum(amount)));
        return true;
    }

    @Override
    public SearchField focusedField() {
        return nameField.isFocused() ? nameField : null;
    }
}
