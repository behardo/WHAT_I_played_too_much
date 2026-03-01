/*/**
 * Impostazioni.java
 * Contiene tutti i dati delle impostazioni di gioco modificabili dal giocatore.
 * Viene passato per riferimento a RenderEngine e GameLoop.
 * In futuro può essere serializzato su file per la persistenza.
 */
public class Impostazioni {

    // ── Volume ────────────────────────────────────────────────────────────────
    /** Volume musica di sottofondo: 0–100 */
    public int volumeMusica  = 70;

    /** Volume effetti sonori: 0–100 */
    public int volumeEffetti = 80;

    // ── Difficoltà ────────────────────────────────────────────────────────────
    public enum Difficolta { FACILE, NORMALE, DIFFICILE }
    public Difficolta difficolta = Difficolta.NORMALE;

    // ── Metodi helper ─────────────────────────────────────────────────────────

    /** Incrementa/decrementa volume musica con clamp 0-100 */
    public void cambiaVolumeMusica(int delta) {
        volumeMusica = Math.max(0, Math.min(100, volumeMusica + delta));
    }

    public void cambiaVolumeEffetti(int delta) {
        volumeEffetti = Math.max(0, Math.min(100, volumeEffetti + delta));
    }

    public void cicladifficolta() {
        difficolta = switch (difficolta) {
            case FACILE    -> Difficolta.NORMALE;
            case NORMALE   -> Difficolta.DIFFICILE;
            case DIFFICILE -> Difficolta.FACILE;
        };
    }

    /** Modificatori di gameplay in base alla difficoltà */
    public float getMultiplicatoreNemici() {
        return switch (difficolta) {
            case FACILE    -> 0.7f;
            case NORMALE   -> 1.0f;
            case DIFFICILE -> 1.5f;
        };
    }

    public int getViteBonusFacile() {
        return difficolta == Difficolta.FACILE ? 1 : 0;
    }

    public String getNomeDifficolta() {
        return switch (difficolta) {
            case FACILE    -> "FACILE";
            case NORMALE   -> "NORMALE";
            case DIFFICILE -> "DIFFICILE";
        };
    }

    public java.awt.Color getColoreDifficolta() {
        return switch (difficolta) {
            case FACILE    -> new java.awt.Color(80, 200, 80);
            case NORMALE   -> new java.awt.Color(200, 200, 80);
            case DIFFICILE -> new java.awt.Color(200, 60, 60);
        };
    }
}