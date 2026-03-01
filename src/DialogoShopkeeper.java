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

    // ── Disegno ───────────────────────────────────────────────────────────────

    /**
     * Disegna il dialogo centrato nello schermo.
     * Chiamare da RenderEngine solo se isVisibile() == true.
     */
    public void disegna(Graphics2D g2) {
        if (!isVisibile()) return;

        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        // Pannello dialogo
        int dw = 420, dh = 130;
        int dx = W/2 - dw/2;
        int dy = H/2 - dh/2 - 20;

        // Sfondo
        g2.setColor(new Color(15, 10, 25, 230));
        g2.fillRoundRect(dx, dy, dw, dh, 12, 12);
        g2.setColor(new Color(180, 100, 220));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(dx, dy, dw, dh, 12, 12);
        g2.setStroke(new BasicStroke(1f));

        // Testo domanda
        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        g2.setColor(Color.WHITE);
        String domanda = "Attaccare il negoziante?";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(domanda, dx + (dw - fm.stringWidth(domanda))/2, dy + 40);

        g2.setFont(new Font("Arial", Font.ITALIC, 13));
        g2.setColor(new Color(200, 150, 150));
        String avviso = "Otterrai 20 monete ma perderai lo shop!";
        g2.drawString(avviso, dx + (dw - g2.getFontMetrics().stringWidth(avviso))/2, dy + 58);

        // Bottoni Sì / No
        String[] scelte = { "NO", "SÌ" };
        Color[]  coloriNorm = { new Color(30, 60, 30), new Color(60, 20, 20) };
        Color[]  coloriSel  = { new Color(50, 130, 50), new Color(160, 40, 40) };

        int btnW = 100, btnH = 36;
        int gap  = 30;
        int totalW = btnW * 2 + gap;
        int btnX0  = dx + (dw - totalW)/2;
        int btnY   = dy + dh - btnH - 15;

        for (int i = 0; i < 2; i++) {
            boolean sel = (i == sceltaAttuale);
            int bx = btnX0 + i * (btnW + gap);

            // Ombra
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(bx + 3, btnY + 3, btnW, btnH, 8, 8);

            // Sfondo
            g2.setColor(sel ? coloriSel[i] : coloriNorm[i]);
            g2.fillRoundRect(bx, btnY, btnW, btnH, 8, 8);

            // Bordo
            g2.setColor(sel ? Color.WHITE : new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(bx, btnY, btnW, btnH, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Testo
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.setColor(sel ? Color.WHITE : new Color(180, 180, 180));
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(scelte[i],
                    bx + (btnW - fm2.stringWidth(scelte[i]))/2,
                    btnY + (btnH + fm2.getAscent() - fm2.getDescent())/2);
        }

        // Hint tasti
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(new Color(120, 120, 150));
        g2.drawString("← → per scegliere   INVIO per confermare   ESC per annullare",
                dx + 20, dy + dh - 3);
    }
}