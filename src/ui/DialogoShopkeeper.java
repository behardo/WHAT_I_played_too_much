package ui;

import java.awt.*;

/**
 * DialogoShopkeeper.java
 * Gestisce il dialogo "Vuoi attaccare il negoziante?" che appare
 * quando il giocatore si avvicina allo shopkeeper nella stanza shop.
 *
 * Stato:
 *  - NASCOSTO       → dialogo non visibile
 *  - MOSTRA_DIALOGO → mostra la scelta Sì/No
 *  - ATTACCO        → il giocatore ha scelto Sì (processare l'azione)
 *  - RIFIUTO        → il giocatore ha scelto No (chiude il dialogo)
 *
 * Il dialogo si mostra automaticamente quando il giocatore è vicino
 * allo shopkeeper e non è già stato mostrato in questa visita.
 */
public class DialogoShopkeeper {

    public enum Stato { NASCOSTO, MOSTRA_DIALOGO, ATTACCO, RIFIUTO }

    private Stato  stato        = Stato.NASCOSTO;
    private int    sceltaAttuale = 0;   // 0 = No (default sicuro), 1 = Sì
    private boolean giaVisto    = false; // Non mostrarlo più di una volta per visita

    // Distanza in pixel per triggerare il dialogo
    private static final float DISTANZA_TRIGGER = 90f;

    // ── Aggiornamento ─────────────────────────────────────────────────────────

    /**
     * Controlla se il giocatore è vicino allo shopkeeper e mostra il dialogo.
     * Da chiamare ogni frame in GameLoop.
     */
    public void aggiorna(float pgX, float pgY, float skX, float skY) {
        if (giaVisto || stato != Stato.NASCOSTO) return;

        float dist = (float) Math.sqrt(
                Math.pow(pgX - skX, 2) + Math.pow(pgY - skY, 2));

        if (dist < DISTANZA_TRIGGER) {
            stato = Stato.MOSTRA_DIALOGO;
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /** Naviga orizzontalmente tra le scelte (← Sì, → No) */
    public void spostaScelta(int delta) {
        if (stato != Stato.MOSTRA_DIALOGO) return;
        sceltaAttuale = (sceltaAttuale + delta + 2) % 2;
    }

    /** Conferma la scelta attuale */
    public void conferma() {
        if (stato != Stato.MOSTRA_DIALOGO) return;
        stato = (sceltaAttuale == 1) ? Stato.ATTACCO : Stato.RIFIUTO;
        giaVisto = true;
    }

    /** Premi ESC per chiudere senza scegliere (equivale a No) */
    public void annulla() {
        if (stato != Stato.MOSTRA_DIALOGO) return;
        stato    = Stato.RIFIUTO;
        giaVisto = true;
    }

    // ── Stato ─────────────────────────────────────────────────────────────────

    public Stato  getStato()        { return stato; }
    public int    getScelta()       { return sceltaAttuale; }
    public boolean isVisibile()     { return stato == Stato.MOSTRA_DIALOGO; }

    /** Consuma lo stato ATTACCO o RIFIUTO dopo che è stato processato */
    public void consuma() {
        stato = Stato.NASCOSTO;
    }

    /** Reset per nuova visita allo shop */
    public void reset() {
        stato        = Stato.NASCOSTO;
        sceltaAttuale = 0;
        giaVisto     = false;
    }

    // Il rendering è gestito da RenderEngine.disegnaSceltaShopkeeper()
    // per poter usare res.fontCustom e lo stesso stile del dialogo narrazione.
    public void disegna(Graphics2D g2) { /* no-op */ }
}
