package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.command.CommandManager;
import io.github.qan4ur.vetmos.macro.Macro;
import io.github.qan4ur.vetmos.macro.MacroManager;
import io.github.qan4ur.vetmos.module.Module;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.MacrosModule;
import io.github.qan4ur.vetmos.util.Keys;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;

import java.util.*;

/** Editor of the macros list (cards with switch, key, text, cooldown, name, test and delete) for the settings window. */
public class MacroEditor extends Widget {
    private static final int GAP = 4, ADD_H = 16;
    private static final float SMALL = 0.75f;

    private final MacrosModule module;
    private final Map<Macro, Card> cards = new IdentityHashMap<>();
    private float addHover, addFlash;
    private int addY;

    public MacroEditor(MacrosModule module) {
        this.module = module;
    }

    private List<Card> cards() {
        List<Card> list = new ArrayList<>();
        Set<Macro> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Macro m : MacroManager.all()) {
            alive.add(m);
            list.add(cards.computeIfAbsent(m, Card::new));
        }
        cards.keySet().retainAll(alive);
        return list;
    }

    private List<String> emptyLines() {
        String text = Lang.ui("macro-empty", "Макрос отправляет текст по нажатию клавиши. Три вида текста:") + "\n"
            + "• " + Lang.ui("macro-empty-chat", "Привет всем! — сообщение в чат") + "\n"
            + "• " + Lang.ui("macro-empty-cmd", "/spawn — команда сервера") + "\n"
            + "• " + Lang.ui("macro-empty-vetmos", ".toggle zoom — команда Vetmos");
        List<String> out = new ArrayList<>();
        for (String part : text.split("\n")) {
            for (OrderedText l : Paint.wrap(part, Math.round((w - 16) / SMALL))) {
                StringBuilder sb = new StringBuilder();
                l.accept((index, style, cp) -> {
                    sb.appendCodePoint(cp);
                    return true;
                });
                out.add(sb.toString());
            }
        }
        return out;
    }

    private int emptyHeight() {
        return 10 + emptyLines().size() * 8;
    }

    @Override
    public int height() {
        int h = 2;
        List<Card> list = cards();
        for (Card c : list) h += c.height() + GAP;
        if (list.isEmpty()) h += emptyHeight() + GAP;
        return h + ADD_H + 4;
    }

    // ---------------------------------------------------------------- focus

    public TextInput focused() {
        for (Card c : cards.values()) {
            if (c.text.isFocused()) return c.text;
            if (c.name.isFocused()) return c.name;
            if (c.cdField.isFocused()) return c.cdField;
        }
        return null;
    }

    public void blurAll() {
        for (Card c : cards.values()) {
            c.text.setFocused(false);
            c.name.setFocused(false);
            c.cdField.setFocused(false);
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mx, int my, Tooltip tip) {
        List<Card> list = cards();
        int cy = y + 2;
        for (Card c : list) {
            c.render(ctx, x, cy, w, mx, my, tip);
            cy += c.height() + GAP;
        }
        if (list.isEmpty()) {
            int eh = emptyHeight();
            Paint.slot(ctx, x, cy, w, eh, 0, 0);
            int ly = cy + 5;
            for (String l : emptyLines()) {
                Paint.textScaled(ctx, l, x + 8, ly, SMALL, l.startsWith("•") ? Palette.TEXT_SECONDARY : Palette.TEXT_MUTED);
                ly += 8;
            }
            cy += eh + GAP;
        }
        addY = cy;
        boolean ah = Paint.inside(mx, my, x, addY, w, ADD_H - 2);
        addHover = Anim.approach(addHover, ah ? 1 : 0, 18);
        addFlash = Anim.approach(addFlash, 0, 6);
        ButtonWidget.draw(ctx, x, addY, w, ADD_H - 2, "+ " + Lang.ui("macro-add", "Добавить макрос"), addHover, addFlash);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean hit = false;
        for (Card c : new ArrayList<>(cards())) if (c.mouseClicked(mx, my, button)) hit = true;
        if (hit) return true;
        if (button == 0 && Paint.inside(mx, my, x, addY, w, ADD_H - 2)) {
            Macro m = MacroManager.add();
            MacroManager.changed();
            addFlash = 1;
            click(1.2f);
            Card c = cards.computeIfAbsent(m, Card::new);
            blurAll();
            c.text.setFocused(true);
            return true;
        }
        return isHovered(mx, my);
    }

    @Override
    public void mouseDragged(double mx, double my, int button) {
        for (Card c : cards.values()) c.drag(mx);
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        for (Card c : cards.values()) c.cdDrag = false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!Screen.hasShiftDown()) return false;
        for (Card c : cards.values()) {
            if (Paint.inside(mx, my, c.trackX - 2, c.y + 38, c.trackW + 4, 14)) {
                c.setCooldown(c.m.cooldown + (amount > 0 ? 0.1 : -0.1));
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- warnings

    private record Warning(String text, boolean severe) {}

    private List<Warning> warnings(Macro m) {
        List<Warning> out = new ArrayList<>();
        if (m.text.isBlank()) out.add(new Warning(Lang.ui("macro-warn-empty", "Пустой текст — макрос ничего не отправит."), false));
        if (m.key == Keys.NONE) {
            out.add(new Warning(Lang.ui("macro-warn-nokey", "Клавиша не назначена."), false));
            return out;
        }
        if (m.enabled) {
            for (Macro o : MacroManager.all()) {
                if (o != m && o.enabled && o.key == m.key) {
                    out.add(new Warning(Lang.ui("macro-warn-dup", "Эта клавиша уже у другого макроса — сработают оба."), true));
                    break;
                }
            }
        }
        for (Module mod : ModuleManager.all()) {
            if (mod.getKey() == m.key) {
                out.add(new Warning(Lang.ui("macro-warn-module", "Клавиша также переключает модуль") + " «" + mod.displayTitle() + "».", true));
                break;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- card

    private final class Card {
        final Macro m;
        final TextField name = new TextField(), text = new TextField(), cdField = new TextField();
        final KeybindWidget key;
        float swT, swHover, testHover, testFlash, delHover, yesHover, noHover, cdHover;
        boolean confirm, cdDrag, cdEditing;
        int x, y, w, trackX, trackW, testX, testW, keyX;

        Card(Macro m) {
            this.m = m;
            swT = m.enabled ? 1 : 0;
            key = new KeybindWidget(() -> m.key, k -> {
                m.key = k;
                MacroManager.changed();
            });
            key.compact = true;
            key.showLabel = false;

            text.maxLen = MacroManager.MAX_TEXT;
            text.placeholder = () -> Lang.ui("macro-text-ph", "Привет! или /spawn или .toggle zoom");
            text.setText(m.text);
            text.onChange = t -> {
                m.text = t;
                MacroManager.changed();
            };
            text.onBlur = () -> {
                m.text = MacroManager.trim(text.text());
                text.setText(m.text);
                MacroManager.changed();
            };
            text.onEnter = () -> text.setFocused(false);

            name.maxLen = 32;
            name.placeholder = () -> Lang.ui("macro-name-ph", "Название (необязательно)");
            name.setText(m.name);
            name.onChange = t -> {
                m.name = t;
                MacroManager.changed();
            };
            name.onBlur = () -> {
                m.name = name.text().trim();
                MacroManager.changed();
            };
            name.onEnter = () -> name.setFocused(false);

            cdField.maxLen = 6;
            cdField.onEnter = () -> cdField.setFocused(false);
            cdField.onBlur = () -> {
                try {
                    setCooldown(Double.parseDouble(cdField.text().trim().replace(',', '.')));
                } catch (NumberFormatException ignored) {}
                cdEditing = false;
            };
        }

        void setCooldown(double v) {
            m.cooldown = Math.round(Math.max(0, Math.min(60, v)) * 10) / 10.0;
            MacroManager.changed();
        }

        int height() {
            return 59 + warnings(m).size() * 8 + 4;
        }

        String kind() {
            String t = m.text;
            if (t.startsWith(CommandManager.PREFIX) && !t.startsWith(CommandManager.PREFIX + CommandManager.PREFIX)) return "Vetmos";
            if (t.startsWith("/")) return Lang.ui("macro-kind-cmd", "команда");
            return Lang.ui("macro-kind-chat", "чат");
        }

        void render(DrawContext ctx, int x, int y, int w, int mx, int my, Tooltip tip) {
            this.x = x;
            this.y = y;
            this.w = w;
            int h = height();
            // keep fields in sync with the model (profile switch / code import)
            if (!text.isFocused() && !text.text().equals(m.text)) text.setText(m.text);
            if (!name.isFocused() && !name.text().equals(m.name)) name.setText(m.name);

            Paint.slot(ctx, x, y, w, h, 0, m.enabled ? 0.25f : 0);
            if (m.enabled) Paint.rect(ctx, x + 1, y + 2, 2, h - 4, Theme.accentAt((y % 300) / 300f) | 0xFF000000);

            // row 1: switch, name, key, test, delete
            int r1 = y + 5;
            boolean swHov = Paint.inside(mx, my, x + 4, r1, 22, 14);
            swHover = Anim.approach(swHover, swHov ? 1 : 0, 18);
            swT = Anim.approach(swT, m.enabled ? 1 : 0, 16);
            float se = Anim.easeInOutCubic(swT);
            int sx = x + 6, sy = r1 + 3;
            Paint.toggle(ctx, sx, sy, 16, 8, se, swHover);
            if (swHov) tip.offer(this, m.enabled ? Lang.ui("macro-on", "Макрос включён — нажмите, чтобы выключить.") : Lang.ui("macro-off", "Макрос выключен — нажмите, чтобы включить."));

            int delX = x + w - 16;
            boolean dh = Paint.inside(mx, my, delX, r1, 12, 14);
            delHover = Anim.approach(delHover, dh ? 1 : 0, 18);
            String test = "▶ " + Lang.ui("macro-test", "тест");
            testW = Paint.width(test) + 10;
            testX = delX - 3 - testW;
            keyX = testX - 3 - 44;
            int nameX = x + 28, nameW = keyX - 4 - nameX;

            if (confirm) {
                Paint.text(ctx, Paint.trim(Lang.ui("macro-delete-confirm", "Удалить макрос?"), Math.max(10, delX - 64 - nameX)), nameX, r1 + 3, Theme.WARNING);
                int yx = delX - 60;
                boolean yh = Paint.inside(mx, my, yx, r1, 28, 14), nh = Paint.inside(mx, my, yx + 30, r1, 28, 14);
                yesHover = Anim.approach(yesHover, yh ? 1 : 0, 18);
                noHover = Anim.approach(noHover, nh ? 1 : 0, 18);
                Paint.button(ctx, yx, r1, 28, 14, Paint.styled(Lang.ui("yes", "Да")), yesHover, 0, true);
                Paint.rect(ctx, yx, r1 + 12, 28, 2, Theme.WARNING);
                ButtonWidget.draw(ctx, yx + 30, r1, 28, 14, Lang.ui("no", "Нет"), noHover, 0);
            } else {
                name.x = nameX;
                name.y = r1;
                name.w = Math.max(20, nameW);
                name.render(ctx, mx, my);
                key.x = keyX;
                key.y = r1 - 1;
                key.w = 44;
                key.render(ctx, mx, my, tip);
                boolean th = Paint.inside(mx, my, testX, r1, testW, 14);
                testHover = Anim.approach(testHover, th ? 1 : 0, 18);
                testFlash = Anim.approach(testFlash, 0, 6);
                ButtonWidget.draw(ctx, testX, r1, testW, 14, test, testHover, testFlash);
                if (th) tip.offer(this.toString() + "test", Lang.ui("macro-test-desc", "Отправить макрос сейчас (с учётом перезарядки)."));
                Paint.text(ctx, "✕", delX + 3, r1 + 3, Anim.mix(Palette.TEXT_MUTED, Theme.WARNING, delHover));
                if (dh) tip.offer(this.toString() + "del", Lang.ui("macro-delete", "Удалить макрос"));
            }

            // row 2: text with a kind tag
            int r2 = y + 22;
            String kind = kind();
            int kindW = Math.round(Paint.width(kind) * 0.7f) + 6;
            text.x = x + 6;
            text.y = r2;
            text.w = w - 12;
            text.rightPad = kindW;
            text.render(ctx, mx, my);
            if (!m.text.isEmpty()) Paint.textScaled(ctx, kind, x + w - 8 - kindW + 3, r2 + 5, 0.7f, Palette.accentA(200));

            // row 3: cooldown slider
            int r3 = y + 40;
            String lbl = Lang.ui("macro-cooldown-label", "Перезарядка");
            int lw = Math.round(Paint.width(lbl) * SMALL);
            Paint.textScaled(ctx, lbl, x + 6, r3 + 3, SMALL, Palette.TEXT_MUTED);
            String val = String.format(Locale.ROOT, "%.1f %s", m.cooldown, Lang.ui("seconds-short", "с"));
            int vw = 36;
            trackX = x + 6 + lw + 6;
            trackW = Math.max(10, x + w - 6 - vw - 4 - trackX);
            boolean sh = Paint.inside(mx, my, trackX - 2, r3, trackW + 4, 10);
            cdHover = Anim.approach(cdHover, sh || cdDrag ? 1 : 0, 18);
            float t = (float) Math.min(1, m.cooldown / 60.0);
            Paint.slider(ctx, trackX, r3 + 1, trackW, 7, t, cdHover);
            int vx = x + w - 6 - vw;
            if (cdEditing) {
                cdField.x = vx;
                cdField.y = r3 - 1;
                cdField.w = vw;
                cdField.h = 12;
                cdField.render(ctx, mx, my);
            } else {
                boolean vh = Paint.inside(mx, my, vx, r3, vw, 10);
                Paint.textRight(ctx, val, vx + vw, r3 + 1, vh ? Palette.accent() : Palette.TEXT_SECONDARY);
                if (vh) tip.offer(this.toString() + "cd", Lang.ui("macro-cooldown-desc", "Пауза между срабатываниями (0–60 с). Нажмите на число, чтобы ввести точное значение; Shift + колёсико — шаг 0.1."));
            }

            // live cooldown bar
            double left = m.cooldownLeft();
            if (left > 0 && m.cooldown > 0) {
                Paint.rect(ctx, x + 6, y + 55, w - 12, 2, 0x26FFFFFF);
                Paint.rect(ctx, x + 6, y + 55, (int) Math.round((w - 12) * (left / m.cooldown)), 2, Palette.accent());
            }

            // warnings
            int wy = y + 60;
            for (Warning wn : warnings(m)) {
                Paint.circle(ctx, x + 9, wy + 3, 1, wn.severe() ? Theme.WARNING : Palette.TEXT_FAINT);
                Paint.textScaled(ctx, Paint.trim(wn.text(), Math.round((w - 20) / SMALL)), x + 13, wy, SMALL, Palette.TEXT_MUTED);
                wy += 8;
            }
        }

        boolean mouseClicked(double mx, double my, int button) {
            // every field sees the click so the others lose focus
            boolean inName = !confirm && name.mouseClicked(mx, my, button);
            boolean inText = text.mouseClicked(mx, my, button);
            boolean inCd = cdEditing && cdField.mouseClicked(mx, my, button);
            if (!Paint.inside(mx, my, x, y, w, height())) return false;
            if (inName || inText || inCd) return true;
            int r1 = y + 5;
            int delX = x + w - 16;
            if (confirm) {
                int yx = delX - 60;
                if (button == 0 && Paint.inside(mx, my, yx, r1, 28, 14)) {
                    MacroManager.remove(m);
                    MacroManager.changed();
                    click(0.8f);
                } else if (button == 0 && Paint.inside(mx, my, yx + 30, r1, 28, 14)) {
                    confirm = false;
                }
                return true;
            }
            if (button == 0 && Paint.inside(mx, my, x + 4, r1, 22, 14)) {
                m.enabled = !m.enabled;
                MacroManager.changed();
                click(m.enabled ? 1.25f : 0.9f);
                return true;
            }
            if (key.mouseClicked(mx, my, button)) return true;
            if (button == 0 && Paint.inside(mx, my, testX, r1, testW, 14)) {
                testFlash = 1;
                click(1.1f);
                test();
                return true;
            }
            if (button == 0 && Paint.inside(mx, my, delX, r1, 12, 14)) {
                confirm = true;
                click(0.9f);
                return true;
            }
            int r3 = y + 40;
            int vx = x + w - 6 - 36;
            if (button == 0 && Paint.inside(mx, my, vx, r3, 36, 10)) {
                cdEditing = true;
                cdField.setText(String.format(Locale.ROOT, "%.1f", m.cooldown));
                cdField.setFocused(true);
                return true;
            }
            if (button == 0 && Paint.inside(mx, my, trackX - 2, r3, trackW + 4, 10)) {
                cdDrag = true;
                drag(mx);
                return true;
            }
            return true;
        }

        void drag(double mx) {
            if (!cdDrag) return;
            setCooldown(Math.max(0, Math.min(1, (mx - trackX) / trackW)) * 60);
        }

        void test() {
            MinecraftClient mc = MinecraftClient.getInstance();
            text.setFocused(false);
            if (mc.player == null) {
                Toasts.show(Lang.ui("macro-test-noworld", "Зайдите в мир, чтобы проверить макрос"), true);
                return;
            }
            if (m.text.isBlank()) {
                Toasts.show(Lang.ui("macro-warn-empty", "Пустой текст — макрос ничего не отправит."), true);
                return;
            }
            if (MacroManager.run(m, module)) Toasts.show(Lang.ui("macro-sent", "Макрос отправлен"));
            else Toasts.show(Lang.ui("macro-on-cooldown", "Макрос перезаряжается"), true);
        }
    }
}
