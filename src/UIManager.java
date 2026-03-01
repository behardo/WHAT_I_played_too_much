import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * UIManager.java
 * Gestisce:
 *  - La lista dei personaggi selezionabili e le loro statistiche
 *  - Le hitbox (Rectangle) di tutti i pulsanti e riquadri di selezione
 *  - L'inizializzazione dei layout dei menu
 *
 * Non disegna nulla: è un gestore di dati UI puro.
 * Il disegno è responsabilità di RenderEngine.
 */
public class UIManager {

    // ── Lista personaggi ──────────────────────────────────────────────────────
    public final List<DatiPersonaggio> listaPersonaggi = new ArrayList<>();

    // ── Hitbox selezione personaggio (4 riquadri) ─────────────────────────────
    public final Rectangle[] rectsSelezionePG = new Rectangle[4];

    // ── Hitbox selezione modalità (2 riquadri) ────────────────────────────────
    public final Rectangle[] rectsSelezioneModalita = new Rectangle[2];

    // ── Pulsanti Game Over ────────────────────────────────────────────────────
    public final Rectangle btnRiprova;
    public final Rectangle btnEsci;

    // ── Pulsante Vittoria ─────────────────────────────────────────────────────
    public final Rectangle btnMenuPrincipale;

    // ─────────────────────────────────────────────────────────────────────────
    public UIManager(ResourceLoader res) {
        inizializzaPersonaggi(res);
        inizializzaRectsSelezionePG();
        inizializzaRectsSelezioneModalita();

        int W = GameState.LARGHEZZA_GIOCO;
        int H = GameState.ALTEZZA_GIOCO;

        btnRiprova       = new Rectangle(W / 2 - 150, H / 2 + 80,  140, 50);
        btnEsci          = new Rectangle(W / 2 + 10,  H / 2 + 80,  140, 50);
        btnMenuPrincipale = new Rectangle(W / 2 - 100, H / 2 + 150, 200, 50);
    }

    // ── Inizializzazione ─────────────────────────────────────────────────────

    private void inizializzaPersonaggi(ResourceLoader res) {
        listaPersonaggi.add(new DatiPersonaggio(
                "BELLGERD", 3, 6.0f, 1,
                res.getIconaPerIndice(0),
                res.imgPersonaggioDefault,
                "Equilibrato."));

        listaPersonaggi.add(new DatiPersonaggio(
                "VLAD", 2, 8.5f, 1,
                res.getIconaPerIndice(1),
                res.imgPersonaggioVeloce,
                "Veloce ma fragile."));

        listaPersonaggi.add(new DatiPersonaggio(
                "PAUL", 3, 4.5f, 2,
                res.getIconaPerIndice(2),
                res.imgPersonaggioForte,
                "Lento ma potente."));

        listaPersonaggi.add(new DatiPersonaggio(
                "JUICY", 5, 3.5f, 1,
                res.getIconaPerIndice(3),
                res.imgPersonaggioTank,
                "Lentissimo, molta vita."));
    }

    private void inizializzaRectsSelezionePG() {
        int startX = GameState.LARGHEZZA_GIOCO / 2 - 320;
        int startY = GameState.ALTEZZA_GIOCO   / 2 - 100;
        int rectW  = 150;
        int rectH  = 200;
        int gap    = 10;
        for (int i = 0; i < 4; i++) {
            rectsSelezionePG[i] = new Rectangle(startX + i * (rectW + gap), startY, rectW, rectH);
        }
    }

    private void inizializzaRectsSelezioneModalita() {
        int startX = GameState.LARGHEZZA_GIOCO / 2 - 250;
        int startY = GameState.ALTEZZA_GIOCO   / 2 - 120;
        int rectW  = 240;
        int rectH  = 240;
        int gap    = 20;
        for (int i = 0; i < 2; i++) {
            rectsSelezioneModalita[i] = new Rectangle(startX + i * (rectW + gap), startY, rectW, rectH);
        }
    }

    // ── Accesso dati ─────────────────────────────────────────────────────────

    public DatiPersonaggio getPersonaggioSelezionato(int indice) {
        return listaPersonaggi.get(indice);
    }

    public int getNumPersonaggi() {
        return listaPersonaggi.size();
    }
}