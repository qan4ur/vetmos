package io.github.qan4ur.vetmos.util;

/** English translations for the combat / chat tweak modules (see {@link Lang} for the key format). */
final class LangEnTweaks {
    private LangEnTweaks() {}

    static void register() {
        noHurtCam();
        lowFire();
        lowShield();
        hitSounds();
        chatTweaks();
    }

    private static void noHurtCam() {
        Lang.en("no-hurt-cam", "No Hurt Cam");
        Lang.en("no-hurt-cam#desc", "Removes or weakens the camera tilt when you take damage.");
        Lang.en("no-hurt-cam.strength", "Shake strength");
        Lang.en("no-hurt-cam.strength#desc", "How much of the normal shake to keep: 0% - none, 100% - vanilla.");
    }

    private static void lowFire() {
        Lang.en("low-fire", "Low Fire");
        Lang.en("low-fire#desc", "Lowers the on-screen fire while burning and makes it more transparent.");
        Lang.en("low-fire.offset", "Lower by");
        Lang.en("low-fire.offset#desc", "How far to move the fire down on screen.");
        Lang.en("low-fire.opacity", "Opacity");
        Lang.en("low-fire.opacity#desc", "Fire opacity, % (vanilla is 90%).");
        Lang.en("low-fire.hide-fire-resistance", "Hide with Fire Resistance");
        Lang.en("low-fire.hide-fire-resistance#desc", "Hide the fire completely while Fire Resistance is active.");
    }

    private static void lowShield() {
        Lang.en("low-shield", "Low Shield");
        Lang.en("low-shield#desc", "Lowers and shrinks the shield in first person.");
        Lang.en("low-shield.lower", "Lower by");
        Lang.en("low-shield.lower#desc", "How far to move the shield down.");
        Lang.en("low-shield.scale", "Size");
        Lang.en("low-shield.scale#desc", "Size of the shield in your hand.");
        Lang.en("low-shield.while-blocking", "While blocking");
        Lang.en("low-shield.while-blocking#desc", "Also apply while you are blocking with the shield.");
    }

    private static void hitSounds() {
        Lang.en("hit-sounds", "Hit Sounds");
        Lang.en("hit-sounds#desc", "Plays your own sound when you hit a target (only you hear it).");
        Lang.en("hit-sounds.sound", "Sound");
        Lang.en("hit-sounds.sound#desc", "Sound for a normal hit.");
        Lang.en("hit-sounds.volume", "Volume");
        Lang.en("hit-sounds.volume#desc", "Sound volume.");
        Lang.en("hit-sounds.pitch", "Pitch");
        Lang.en("hit-sounds.pitch#desc", "Sound pitch.");
        Lang.en("hit-sounds.only-players", "Only players");
        Lang.en("hit-sounds.only-players#desc", "Play the sound only when hitting players.");
        Lang.en("hit-sounds.crits", "Crit sound");
        Lang.en("hit-sounds.crits#desc", "A separate sound for critical hits.");
        Lang.en("hit-sounds.crit-sound", "Crit sound");
        Lang.en("hit-sounds.crit-sound#desc", "Sound for a critical hit.");
        Lang.en("hit-sounds.crit-pitch", "Crit pitch");
        Lang.en("hit-sounds.crit-pitch#desc", "Pitch of the crit sound.");
        Lang.en("enum:HitSound.ORB", "Experience");
        Lang.en("enum:HitSound.BELL", "Bell");
        Lang.en("enum:HitSound.PLING", "Pling");
        Lang.en("enum:HitSound.CHIME", "Chime");
        Lang.en("enum:HitSound.HAT", "Click");
        Lang.en("enum:HitSound.BASS", "Bass");
        Lang.en("enum:HitSound.BIT", "8-bit");
        Lang.en("enum:HitSound.AMETHYST", "Amethyst");
        Lang.en("enum:HitSound.ARROW", "Arrow hit");
        Lang.en("enum:HitSound.CRIT", "Crit");
        Lang.en("enum:HitSound.STRONG", "Strong hit");
        Lang.en("enum:HitSound.GLASS", "Glass");
        Lang.en("enum:HitSound.ANVIL", "Anvil");
    }

    private static void chatTweaks() {
        Lang.en("chat-tweaks", "Chat");
        Lang.en("chat-tweaks#desc", "Timestamps before messages and stacking of identical consecutive messages.");
        Lang.en("group:Время", "Time");
        Lang.en("group:Сложение", "Stacking");
        Lang.en("chat-tweaks.timestamps", "Timestamps");
        Lang.en("chat-tweaks.timestamps#desc", "Show the time before every message.");
        Lang.en("chat-tweaks.format", "Format");
        Lang.en("chat-tweaks.format#desc", "Time format.");
        Lang.en("chat-tweaks.brackets", "Brackets");
        Lang.en("chat-tweaks.brackets#desc", "What to wrap the time in.");
        Lang.en("chat-tweaks.accent-color", "Accent color");
        Lang.en("chat-tweaks.accent-color#desc", "Color the time with the interface accent.");
        Lang.en("chat-tweaks.color", "Color");
        Lang.en("chat-tweaks.color#desc", "Time color.");
        Lang.en("chat-tweaks.stacking", "Stack repeats");
        Lang.en("chat-tweaks.stacking#desc", "Identical consecutive messages merge into one with a \"×3\" counter.");
        Lang.en("chat-tweaks.stack-color", "Counter color");
        Lang.en("chat-tweaks.stack-color#desc", "Color of the repeat counter.");
        Lang.en("enum:ChatBrackets.SQUARE", "Square [ ]");
        Lang.en("enum:ChatBrackets.ANGLE", "Angle < >");
        Lang.en("enum:ChatBrackets.ROUND", "Round ( )");
        Lang.en("enum:ChatBrackets.PIPE", "Bar |");
        Lang.en("enum:ChatBrackets.NONE", "No brackets");
    }
}
