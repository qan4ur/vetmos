package io.github.qan4ur.vetmos.gui;

import io.github.qan4ur.vetmos.config.ConfigManager;
import io.github.qan4ur.vetmos.module.ModuleManager;
import io.github.qan4ur.vetmos.modules.client.InterfaceModule;
import io.github.qan4ur.vetmos.util.Lang;
import io.github.qan4ur.vetmos.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Supplier;

/** First-run guide: a few short steps over the ClickGUI, with the RU/EN switch right on the card. */
public class WelcomeOverlay {
    private record Step(String icon, Supplier<String> title, Supplier<String> body) {}

    private static final List<Step> STEPS = List.of(
        new Step("☾", () -> Lang.ui("welcome1-title", "Добро пожаловать в Vetmos"),
            () -> Lang.ui("welcome1-body", "Меню открывается клавишей «правый Ctrl» (её можно сменить в настройках управления Minecraft). Язык интерфейса можно выбрать прямо здесь, справа вверху.")),
        new Step("⚔", () -> Lang.ui("welcome2-title", "Модули"),
            () -> Lang.ui("welcome2-body", "ЛКМ по модулю включает и выключает его. ПКМ или значок ⚙ открывает окно настроек: вкладки слева, описание под каждой настройкой, ↺ сбрасывает значение, справа предпросмотр.")),
        new Step("▣", () -> Lang.ui("welcome3-title", "Редактор HUD"),
            () -> Lang.ui("welcome3-body", "Кнопка «Редактор HUD» вверху: перетаскивайте элементы мышью, колёсиком меняйте размер, ПКМ открывает настройки элемента.")),
        new Step("◐", () -> Lang.ui("welcome4-title", "Стиль и профили"),
            () -> Lang.ui("welcome4-body", "Кружок палитры меняет акцентный цвет и градиент. В меню профилей можно хранить несколько наборов настроек и делиться ими через код. «?» снова покажет эту справку."))
    );

    private final Anim.Toggle open = new Anim.Toggle(0.25f, false);
    private int step;
    private float stepAnim = 1;
    private int x, y, w, h;
    private final float[] hovers = new float[5]; // back, skip, next, ru, en

    public void show() {
        step = 0;
        stepAnim = 0;
        open.set(true);
    }

    public boolean isOpen() {
        return open.on();
    }

    public boolean isVisible() {
        return open.raw() > 0;
    }

    public void finish() {
        open.set(false);
        ConfigManager.setWelcomeShown(true);
    }

    private int[] btn(int i) {
        // 0 back, 1 skip, 2 next
        int by = y + h - 22;
        return switch (i) {
            case 0 -> new int[] { x + 10, by, 60, 15 };
            case 1 -> new int[] { x + w / 2 - 35, by, 70, 15 };
            default -> new int[] { x + w - 80, by, 70, 15 };
        };
    }

    private int[] langBtn(int i) {
        return new int[] { x + w - 56 + i * 24, y + 8, 22, 13 };
    }

    public void render(DrawContext ctx, int mx, int my, int vw, int vh) {
        open.update();
        if (open.raw() <= 0) return;
        float e = open.on() ? Anim.easeOutCubic(open.raw()) : Anim.easeInOutCubic(open.raw());
        stepAnim = Anim.approach(stepAnim, 1, 10);
        if (!open.on()) mx = my = Widget.NO_MOUSE;

        float old = Paint.alpha;
        Paint.alpha = old * e;
        Paint.rect(ctx, -2000, -2000, vw + 4000, vh + 4000, 0xA0000000);

        w = Math.min(vw - 30, 280);
        h = 168;
        x = (vw - w) / 2;
        y = (vh - h) / 2;
        var m = ctx.getMatrices();
        m.pushMatrix();
        float s = 0.9f + 0.1f * e;
        m.translate(x + w / 2f, y + h / 2f);
        m.scale(s, s);
        m.translate(-(x + w / 2f), -(y + h / 2f));

        Paint.panel(ctx, x, y, w, h, Paint.MODAL_BG);

        // language switch
        boolean en = Lang.isEnglish();
        for (int i = 0; i < 2; i++) {
            int[] b = langBtn(i);
            boolean hov = Paint.inside(mx, my, b[0], b[1], b[2], b[3]);
            hovers[3 + i] = Anim.approach(hovers[3 + i], hov ? 1 : 0, 18);
            boolean sel = (i == 1) == en;
            Paint.button(ctx, b[0], b[1], b[2], b[3], Paint.styled(i == 0 ? "RU" : "EN"), Math.max(hovers[3 + i], sel ? 1 : 0), 0, true);
        }

        // step content (slides in)
        Step st = STEPS.get(step);
        float a = Anim.easeOutCubic(stepAnim);
        float oldInner = Paint.alpha;
        Paint.alpha = oldInner * a;
        m.pushMatrix();
        m.translate((1 - a) * 12, 0);
        int cx = x + 26, cy = y + 30;
        Paint.slot(ctx, cx - 11, cy - 11, 22, 22, 0, 0.6f);
        Paint.textCentered(ctx, st.icon(), cx, cy - 4, Palette.TEXT);
        String title = st.title().get();
        int tx = x + 44;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int c = Theme.accentAt((float) i / Math.max(1, title.length()));
            Paint.text(ctx, Text.literal(ch).formatted(Formatting.BOLD), tx, cy - 4, Anim.mix(Palette.TEXT, c | 0xFF000000, 0.5f));
            tx += Paint.width(Text.literal(ch).formatted(Formatting.BOLD));
        }
        List<OrderedText> lines = Paint.wrap(st.body().get(), w - 28);
        int ly = y + 52;
        for (OrderedText l : lines) {
            Paint.line(ctx, l, x + 14, ly, 0xFFDADADA);
            ly += 11;
        }
        m.popMatrix();
        Paint.alpha = oldInner;

        // step dots
        int dotsW = STEPS.size() * 10;
        for (int i = 0; i < STEPS.size(); i++) {
            int dx = x + (w - dotsW) / 2 + i * 10 + 5, dy = y + h - 32;
            if (i == step) Paint.rect(ctx, dx - 4, dy - 1, 8, 2, Palette.accent());
            else Paint.rect(ctx, dx - 2, dy - 1, 4, 2, 0x50FFFFFF);
        }

        // buttons
        String[] labels = {
            Lang.ui("back", "Назад"),
            Lang.ui("skip", "Пропустить"),
            step == STEPS.size() - 1 ? Lang.ui("start", "Начать") : Lang.ui("next", "Далее")
        };
        for (int i = 0; i < 3; i++) {
            if (i == 0 && step == 0) continue;
            if (i == 1 && step == STEPS.size() - 1) continue;
            int[] b = btn(i);
            boolean hov = Paint.inside(mx, my, b[0], b[1], b[2], b[3]);
            hovers[i] = Anim.approach(hovers[i], hov ? 1 : 0, 18);
            ButtonWidget.draw(ctx, b[0], b[1], b[2], b[3], labels[i], i == 2 ? Math.max(0.45f, hovers[i]) : hovers[i], 0);
        }

        m.popMatrix();
        Paint.alpha = old;
    }

    /** Consumes every click while open. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!open.on()) return false;
        if (button != 0) return true;
        InterfaceModule ui = ModuleManager.get(InterfaceModule.class);
        for (int i = 0; i < 2; i++) {
            int[] b = langBtn(i);
            if (ui != null && Paint.inside(mx, my, b[0], b[1], b[2], b[3])) {
                ui.language.set(i == 0 ? InterfaceModule.Language.RU : InterfaceModule.Language.EN);
                Widget.click(1.2f);
                return true;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (i == 0 && step == 0) continue;
            if (i == 1 && step == STEPS.size() - 1) continue;
            int[] b = btn(i);
            if (!Paint.inside(mx, my, b[0], b[1], b[2], b[3])) continue;
            Widget.click(1.1f);
            switch (i) {
                case 0 -> {
                    step--;
                    stepAnim = 0;
                }
                case 1 -> finish();
                default -> {
                    if (step == STEPS.size() - 1) finish();
                    else {
                        step++;
                        stepAnim = 0;
                    }
                }
            }
            return true;
        }
        return true;
    }
}
