/**
 * StatNemico.java
 * Calcola le statistiche dei nemici in base al mondo attuale.
 *
 * Scala lineare per storia (M1→M4), esponenziale leggera per infinita.
 *
 * Mondo 1: base
 * Mondo 2: +40% vita, +25% velocità, +1 danno
 * Mondo 3: +100% vita, +50% velocità, +2 danno
 * Mondo 4: +180% vita, +80% velocità, +3 danno
 * Infinita: continua oltre M4 con curva più ripida
 */
public class StatNemico {

    // ── Nemico normale ────────────────────────────────────────────────────────

    public static int vitaNemico(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 8;
            case 4 -> 12;
            default -> 3;
        } + bonusInfinita(mondo, 2);
    }

    public static float velocitaNemico(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 1.6f;
            case 2 -> 1.9f;
            case 3 -> 2.2f;
            case 4 -> 2.6f;
            default -> 1.6f;
        } + bonusVelInfinita(mondo);
    }

    // ── Nemico forte ──────────────────────────────────────────────────────────

    public static int vitaNemicoForte(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 6;
            case 2 -> 10;
            case 3 -> 16;
            case 4 -> 24;
            default -> 6;
        } + bonusInfinita(mondo, 4);
    }

    public static float velocitaNemicoForte(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 2.8f;
            case 2 -> 3.2f;
            case 3 -> 3.7f;
            case 4 -> 4.3f;
            default -> 2.8f;
        } + bonusVelInfinita(mondo);
    }

    // ── Boss ──────────────────────────────────────────────────────────────────

    public static int vitaBoss(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 80;
            case 2 -> 130;
            case 3 -> 200;
            case 4 -> 300;
            default -> 80;
        } + bonusInfinita(mondo, 60);
    }

    public static float velocitaBoss(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 1.1f;
            case 2 -> 1.4f;
            case 3 -> 1.8f;
            case 4 -> 2.2f;
            default -> 1.1f;
        } + bonusVelInfinita(mondo) * 0.5f;
    }

    // ── Quanti nemici per stanza ──────────────────────────────────────────────

    public static int quantiNemici(int mondo, int stanza, java.util.Random rng) {
        int base = 2 + (stanza / 2) + Math.min(mondo - 1, 3);
        int extra = rng.nextInt(3);   // 0-2 random
        return Math.min(base + extra, 10); // cap a 10
    }

    // ── Probabilità NemicoForte (cresce col mondo) ────────────────────────────

    public static float probNemicoForte(int mondo) {
        return switch (((mondo - 1) % 4) + 1) {
            case 1 -> 0.0f;   // M1: solo normali
            case 2 -> 0.35f;  // M2: 35% forti
            case 3 -> 0.55f;  // M3: 55% forti
            case 4 -> 0.70f;  // M4: 70% forti
            default -> 0.0f;
        };
    }

    // ── Bonus per modalità infinita (oltre M4) ────────────────────────────────

    /** Bonus vita flat ogni ciclo di 4 mondi oltre il primo. */
    private static int bonusInfinita(int mondo, int perCiclo) {
        int cicli = (mondo - 1) / 4;  // 0 per M1-M4, 1 per M5-M8, ecc.
        return cicli * perCiclo;
    }

    private static float bonusVelInfinita(int mondo) {
        int cicli = (mondo - 1) / 4;
        return cicli * 0.3f;
    }
}
