import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * RenderEngine.java
 * Contiene tutta la logica di disegno del gioco.
 * Riceve per costruttore tutti i dati necessari (GameState, ResourceLoader,
 * RoomManager, UIManager) e non modifica alcuno stato.
 *
 * Il metodo principale è render(Graphics2D, int, int) che viene chiamato
 * da paintComponent() nel JPanel principale.
 */
public class RenderEngine {

    private final GameState      state;
    private final ResourceLoader res;
    private final RoomManager    roomMgr;
    private final UIManager      ui;
    private final FullscreenManager fullscreen;

    // Riferimento ai pugni attivi (gestiti dal GameLoop)
    private List<Pugno> pugniAttivi;

    // ─────────────────────────────────────────────────────────────────────────
    public RenderEngine(GameState state, ResourceLoader res,
                        RoomManager roomMgr, UIManager ui,
                        FullscreenManager fullscreen) {
        this.state      = state;
        this.res        = res;
        this.roomMgr    = roomMgr;
        this.ui         = ui;
        this.fullscreen = fullscreen;
    }

    public void setPugniAttivi(List<Pugno> pugni) {
        this.pugniAttivi = pugni;
    }

    // ── Entry point rendering ─────────────────────────────────────────────────

    /**
     * Punto di ingresso principale. Chiamato da paintComponent().
     * Applica la trasformazione fullscreen e delega al metodo corretto.
     */
    public void render(Graphics2D g2, int panelWidth, int panelHeight) {
        // Hint qualità: bilineare per lo stretch, anti-aliasing per testo/forme
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        // Stretch puro: scaleX e scaleY indipendenti, nessun offset/letterbox
        double[] params = fullscreen.getScaleParams(panelWidth, panelHeight);
        double scaleX   = params[0];
        double scaleY   = params[1];
        // Nessun fillRect di sfondo: il contenuto copre tutto il pannello

        g2.scale(scaleX, scaleY);

        // Dispatch per stato
        switch (state.statoGioco) {
            case MENU                  -> disegnaMenu(g2);
            case IMPOSTAZIONI          -> disegnaImpostazioni(g2);
            case CONTROLLI             -> disegnaControlli(g2);
            case SELEZIONE_PERSONAGGIO -> disegnaSelezionePersonaggio(g2);
            case SELEZIONE_MODALITA    -> disegnaSelezioneModalita(g2);
            case GIOCO                 -> disegnaGioco(g2);
            case PAUSA                 -> { disegnaGioco(g2); disegnaPausa(g2); }
            case GAME_OVER             -> { disegnaGioco(g2); disegnaGameOver(g2); }
            case VITTORIA_STORIA       -> disegnaVittoriaStoria(g2);
        }
    }

    // ── Menu Principale ───────────────────────────────────────────────────────

    private void disegnaMenu(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        // Sfondo con gradiente simulato
        g2.setColor(new Color(12, 12, 28));
        g2.fillRect(0, 0, W, H);
        // Alone centrale
        for (int r = 300; r > 0; r -= 15) {
            int alpha = (int)(18 * (1.0 - r / 300.0));
            g2.setColor(new Color(60, 60, 140, alpha));
            g2.fillOval(W/2 - r, H/2 - r - 80, r*2, r*2);
        }

        // Titolo
        g2.setFont(new Font("Consolas", Font.BOLD, 38));
        String titolo = "WHAT: I'VE PLAYED TOO MUCH";
        FontMetrics fm = g2.getFontMetrics();
        int tx = W/2 - fm.stringWidth(titolo)/2;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(titolo, tx + 3, 60 + 3);
        g2.setColor(new Color(200, 200, 255));
        g2.drawString(titolo, tx, 60);

        // Sottotitolo
        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.setColor(new Color(140, 140, 180));
        String sub = "Un roguelike di muratori e boss corrotti";
        g2.drawString(sub, W/2 - g2.getFontMetrics().stringWidth(sub)/2, 85);

        // Separatore
        g2.setColor(new Color(80, 80, 130));
        g2.fillRect(W/2 - 120, 95, 240, 2);

        // Bottoni menu
        ui.btnGioca.draw(g2);
        ui.btnImpostazioni.draw(g2);
        ui.btnControlli.draw(g2);
        ui.btnEsciMenu.draw(g2);

        // Footer hint
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(90, 90, 120));
        g2.drawString("F11 = Fullscreen", 20, H - 15);
        g2.drawString("v1.0", W - 50, H - 15);
    }

    // ── Impostazioni ─────────────────────────────────────────────────────────

    private void disegnaImpostazioni(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;
        Impostazioni imp = state.impostazioni;

        sfondoOverlay(g2, new Color(10, 10, 30));

        // Titolo
        g2.setFont(new Font("Consolas", Font.BOLD, 32));
        g2.setColor(new Color(200, 200, 255));
        String t = "IMPOSTAZIONI";
        g2.drawString(t, W/2 - g2.getFontMetrics().stringWidth(t)/2, 62);
        g2.setColor(new Color(80, 80, 130));
        g2.fillRect(W/2 - 140, 70, 280, 2);

        // Leggo i parametri di layout calcolati in UIManager
        int LX   = ui._impLabelX;
        int CX2  = ui._impCtrlX;
        int SW   = ui._impSw;
        int s    = ui._impStartY;
        int RH   = ui._impRigaH;
        int SH   = ui._impSh;
        int sliderX = CX2 + SW + 5;
        int sliderW = 145;

        // ── Riga 1: Volume Musica ─────────────────────────────────────────────
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        g2.drawString("VOLUME MUSICA", LX, s + SH/2 + 6);
        ui.btnMusMeno.draw(g2);
        disegnaSlider(g2, sliderX, s + 4, sliderW, SH - 8, imp.volumeMusica,
                new Color(80, 80, 200), new Color(140, 140, 255));
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(new Color(200, 200, 255));
        g2.drawString(imp.volumeMusica + "%", sliderX + sliderW + 8, s + SH/2 + 6);
        ui.btnMusPiu.draw(g2);

        // ── Riga 2: Volume Effetti ────────────────────────────────────────────
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        g2.drawString("VOLUME EFFETTI", LX, s+RH + SH/2 + 6);
        ui.btnEffMeno.draw(g2);
        disegnaSlider(g2, sliderX, s+RH + 4, sliderW, SH - 8, imp.volumeEffetti,
                new Color(80, 180, 80), new Color(120, 220, 120));
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(new Color(150, 230, 150));
        g2.drawString(imp.volumeEffetti + "%", sliderX + sliderW + 8, s+RH + SH/2 + 6);
        ui.btnEffPiu.draw(g2);

        // ── Riga 3: Difficoltà ────────────────────────────────────────────────
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        g2.drawString("DIFFICOLTA'", LX, s+RH*2 + SH/2 + 6);
        ui.btnDifficolta.setColoreTesto(imp.getColoreDifficolta());
        // Aggiorna il label del bottone dinamicamente
        // (MenuButton non supporta cambio label → disegniamo il testo sopra)
        ui.btnDifficolta.draw(g2);
        // Sovrascrivi testo con la difficoltà corrente centrato nel bottone
        Rectangle db = ui.btnDifficolta.bounds;
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(imp.getColoreDifficolta());
        String diff = imp.getNomeDifficolta();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(diff, db.x + (db.width - fm.stringWidth(diff))/2,
                db.y + (db.height + fm.getAscent() - fm.getDescent())/2);

        // ── Indietro ──────────────────────────────────────────────────────────
        ui.btnChiudiImpostazioni.draw(g2);
    }

    /** Disegna una barra slider orizzontale con valore 0-100 */
    private void disegnaSlider(Graphics2D g2, int x, int y, int w, int h,
                               int valore, Color colorePieno, Color coloreLuce) {
        // Sfondo
        g2.setColor(new Color(30, 30, 50));
        g2.fillRoundRect(x, y, w, h, 6, 6);
        // Riempimento
        int riempito = (int)(w * valore / 100.0);
        g2.setColor(colorePieno);
        g2.fillRoundRect(x, y, riempito, h, 6, 6);
        // Highlight
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRoundRect(x, y, riempito, h/2, 6, 6);
        // Bordo
        g2.setColor(new Color(80, 80, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 6, 6);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Controlli ────────────────────────────────────────────────────────────

    private void disegnaControlli(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        sfondoOverlay(g2, new Color(10, 10, 30));

        g2.setFont(new Font("Consolas", Font.BOLD, 38));
        g2.setColor(new Color(200, 200, 255));
        String t = "CONTROLLI";
        g2.drawString(t, W/2 - g2.getFontMetrics().stringWidth(t)/2, 90);
        g2.setColor(new Color(80, 80, 130));
        g2.fillRect(W/2 - 130, 100, 260, 2);

        // Layout a due colonne
        int col1X = W/2 - 320;
        int col2X = W/2 + 30;
        int startY = 160;
        int rigaH  = 52;

        // Colonna 1: movimento e sparo
        disegnaTastoInfo(g2, col1X, startY,           "W A S D",     "Movimento");
        disegnaTastoInfo(g2, col1X, startY + rigaH,   "↑ ↓ ← →",    "Sparo");
        disegnaTastoInfo(g2, col1X, startY + rigaH*2, "ESC",         "Pausa");
        disegnaTastoInfo(g2, col1X, startY + rigaH*3, "F11",         "Fullscreen");

        // Colonna 2: azioni durante pausa
        disegnaTastoInfo(g2, col2X, startY,           "INVIO",       "Conferma");
        disegnaTastoInfo(g2, col2X, startY + rigaH,   "ESC",         "Indietro / Pausa");
        disegnaTastoInfo(g2, col2X, startY + rigaH*2, "Q",           "Esci (in pausa)");
        disegnaTastoInfo(g2, col2X, startY + rigaH*3, "Click",       "Interagisci");

        ui.btnChiudiControlli.draw(g2);
    }

    /** Disegna un singolo riquadro tasto + descrizione */
    private void disegnaTastoInfo(Graphics2D g2, int x, int y, String tasto, String descrizione) {
        int tw = 140, th = 38, raggio = 6;

        // Sfondo tasto
        g2.setColor(new Color(40, 40, 70));
        g2.fillRoundRect(x, y, tw, th, raggio, raggio);
        g2.setColor(new Color(100, 100, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, tw, th, raggio, raggio);
        g2.setStroke(new BasicStroke(1f));

        // Testo tasto
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 200, 255));
        g2.drawString(tasto, x + (tw - fm.stringWidth(tasto))/2, y + (th + fm.getAscent())/2 - 3);

        // Descrizione
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(new Color(180, 180, 200));
        g2.drawString(descrizione, x + tw + 12, y + th/2 + 6);
    }

    // ── Selezione Personaggio ─────────────────────────────────────────────────

    private void disegnaSelezionePersonaggio(Graphics2D g2) {
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 30));
        String titolo = "SCEGLI IL TUO PERSONAGGIO";
        g2.drawString(titolo, GameState.LARGHEZZA_GIOCO/2 - g2.getFontMetrics().stringWidth(titolo)/2, 55);

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(new Color(160, 160, 190));
        g2.drawString("[Frecce/Mouse] naviga  [INVIO/Click] conferma  [ESC] indietro",
                GameState.LARGHEZZA_GIOCO/2 - 220, 78);

        for (int i = 0; i < ui.listaPersonaggi.size(); i++) {
            DatiPersonaggio pg   = ui.listaPersonaggi.get(i);
            Rectangle       rect = ui.rectsSelezionePG[i];
            boolean         sel  = (i == state.indicePersonaggioSelezionato);

            // Sfondo riquadro con angoli arrotondati
            g2.setColor(sel ? new Color(255, 215, 0, 80) : new Color(255, 255, 255, 20));
            g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
            g2.setColor(sel ? Color.YELLOW : new Color(120, 120, 160));
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Icona personaggio centrata in alto
            int imgSize = 80;
            if (pg.imgIcona != null) {
                g2.drawImage(pg.imgIcona,
                        rect.x + (rect.width - imgSize) / 2,
                        rect.y + 15, imgSize, imgSize, null);
            }

            // Nome
            g2.setColor(sel ? Color.YELLOW : Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pg.nome, rect.x + (rect.width - fm.stringWidth(pg.nome))/2, rect.y + 112);

            // Descrizione
            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            g2.setColor(new Color(180, 180, 200));
            g2.drawString(pg.descrizione, rect.x + 8, rect.y + 130);

            // Stats: vita, velocità, danno
            int statY = rect.y + 152;
            if (res.imgCuore != null)
                g2.drawImage(res.imgCuore, rect.x + 8, statY - 14, 16, 16, null);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(new Color(255, 100, 100));
            g2.drawString("" + pg.vitaMax, rect.x + 27, statY);

            g2.setColor(new Color(100, 200, 255));
            g2.drawString("VEL " + pg.velocitaBase, rect.x + 8,  statY + 18);
            g2.setColor(new Color(255, 200, 100));
            g2.drawString("DMG " + pg.dannoBase,    rect.x + 80, statY + 18);
        }
    }

    // ── Selezione Modalità ────────────────────────────────────────────────────

    private void disegnaSelezioneModalita(Graphics2D g2) {
        g2.setColor(new Color(40, 20, 50));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 30));
        String t = "SELEZIONA LA SFIDA";
        g2.drawString(t, GameState.LARGHEZZA_GIOCO/2 - g2.getFontMetrics().stringWidth(t)/2, 55);

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(new Color(160, 160, 190));
        g2.drawString("[Frecce/Mouse] naviga  [INVIO/Click] conferma  [ESC] indietro",
                GameState.LARGHEZZA_GIOCO/2 - 210, 80);

        String[] nomi     = { "STORIA CLASSICA",      "MODALITA INFINITA"    };
        String[] desc1    = { "Sconfiggi 4 Boss",      "Sopravvivi all'infinito!" };
        String[] desc2    = { "per salvare il cantiere", "Nemici sempre più forti." };
        BufferedImage[] icone    = { res.imgIconaStoria, res.imgIconaInfinita };
        String[] fallback = { "S", "∞" };

        for (int i = 0; i < 2; i++) {
            Rectangle rect = ui.rectsSelezioneModalita[i];
            boolean   sel  = (i == state.indiceModalitaSelezionata);

            // Sfondo
            g2.setColor(sel ? new Color(173, 216, 230, 80) : new Color(255, 255, 255, 15));
            g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g2.setColor(sel ? Color.CYAN : new Color(120, 120, 160));
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Icona/fallback centrata
            int imgSize = 90;
            if (icone[i] != null) {
                g2.drawImage(icone[i],
                        rect.x + (rect.width - imgSize) / 2,
                        rect.y + 15, imgSize, imgSize, null);
            } else {
                g2.setColor(sel ? Color.CYAN : Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 70));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fallback[i],
                        rect.x + (rect.width - fm.stringWidth(fallback[i]))/2,
                        rect.y + 90);
            }

            // Nome
            g2.setColor(sel ? Color.CYAN : Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(nomi[i],
                    rect.x + (rect.width - fm.stringWidth(nomi[i]))/2,
                    rect.y + 128);

            // Descrizione
            g2.setFont(new Font("Arial", Font.ITALIC, 14));
            g2.setColor(new Color(190, 190, 210));
            g2.drawString(desc1[i], rect.x + 12, rect.y + 158);
            g2.drawString(desc2[i], rect.x + 12, rect.y + 178);
        }
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void disegnaPausa(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, W, H);

        // Pannello: top=130 (dove partono i bottoni) - 75 (per titolo+separatore)
        // bottom = 154 + 4*44 + 3*10 = 154+206 = 360, +15 padding = 375
        int pw = 300, padX = 20;
        int py = 79;                      // 154 - 75
        int ph = 375 - py;               // 296
        int px = W/2 - pw/2 - padX;

        g2.setColor(new Color(15, 15, 35, 230));
        g2.fillRoundRect(px, py, pw + padX*2, ph, 16, 16);
        g2.setColor(new Color(80, 80, 140));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(px, py, pw + padX*2, ph, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        // Titolo centrato nel pannello
        g2.setFont(new Font("Consolas", Font.BOLD, 30));
        g2.setColor(new Color(200, 200, 255));
        String t = "PAUSA";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(t, W/2 - fm.stringWidth(t)/2, py + 45);
        g2.setColor(new Color(60, 60, 110));
        g2.fillRect(W/2 - 70, py + 52, 140, 2);

        ui.btnRiprendi.draw(g2);
        ui.btnImpostazioniPausa.draw(g2);
        ui.btnMenuPrincipalePausa.draw(g2);
        ui.btnEsciPausa.draw(g2);
    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void disegnaGameOver(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, W, H);

        // Titolo rosso con ombra
        g2.setFont(new Font("Consolas", Font.BOLD, 62));
        FontMetrics fm = g2.getFontMetrics();
        String t = "GAME OVER";
        int tx = W/2 - fm.stringWidth(t)/2;
        g2.setColor(new Color(100, 0, 0));
        g2.drawString(t, tx + 4, H/2 - 55 + 4);
        g2.setColor(new Color(220, 40, 40));
        g2.drawString(t, tx, H/2 - 55);

        // Stats
        g2.setFont(new Font("Consolas", Font.PLAIN, 20));
        g2.setColor(new Color(200, 180, 180));
        String stats = "Mondo " + state.mondoAttuale + "  •  Stanza " + state.stanzaNelMondo
                + "  •  Monete " + state.monete;
        g2.drawString(stats, W/2 - g2.getFontMetrics().stringWidth(stats)/2, H/2 + 10);

        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            g2.setFont(new Font("Arial", Font.ITALIC, 18));
            g2.setColor(new Color(180, 160, 100));
            int tot = (state.mondoAttuale - 1) * GameState.STANZA_BOSS + state.stanzaNelMondo;
            String inf = "Stanze totali superate: " + tot;
            g2.drawString(inf, W/2 - g2.getFontMetrics().stringWidth(inf)/2, H/2 + 35);
        }

        // Bottoni
        ui.btnRiprova.draw(g2);
        ui.btnMenuPrincipaleGO.draw(g2);
        ui.btnEsciGO.draw(g2);
    }

    // ── Vittoria Storia ───────────────────────────────────────────────────────

    private void disegnaVittoriaStoria(Graphics2D g2) {
        final int W = GameState.LARGHEZZA_GIOCO;
        final int H = GameState.ALTEZZA_GIOCO;

        g2.setColor(new Color(10, 40, 10));
        g2.fillRect(0, 0, W, H);
        // Alone dorato
        for (int r = 280; r > 0; r -= 12) {
            int alpha = (int)(20 * (1.0 - r / 280.0));
            g2.setColor(new Color(200, 180, 0, alpha));
            g2.fillOval(W/2 - r, H/2 - r - 40, r*2, r*2);
        }

        g2.setFont(new Font("Consolas", Font.BOLD, 52));
        g2.setColor(new Color(80, 60, 0));
        g2.drawString("STORIA COMPLETATA!", W/2 - 295, H/2 - 45);
        g2.setColor(new Color(255, 220, 0));
        g2.drawString("STORIA COMPLETATA!", W/2 - 298, H/2 - 48);

        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        String sub = "Hai sconfitto tutti i Boss e salvato il cantiere!";
        g2.drawString(sub, W/2 - g2.getFontMetrics().stringWidth(sub)/2, H/2 + 25);

        g2.setFont(new Font("Consolas", Font.BOLD, 24));
        g2.setColor(new Color(255, 215, 0));
        String monete = "Monete raccolte: " + state.monete;
        g2.drawString(monete, W/2 - g2.getFontMetrics().stringWidth(monete)/2, H/2 + 60);

        ui.btnMenuPrincipaleVittoria.draw(g2);
    }

    // ── Gioco ─────────────────────────────────────────────────────────────────

    private void disegnaGioco(Graphics2D g2) {
        // Se siamo nella stanza shop, disegna una schermata shop speciale
        if (roomMgr.inStanzaShop) {
            disegnaStanzaShop(g2);
            disegnaGiocatore(g2);
            disegnaHUD(g2);
            return;
        }

        disegnaAmbiente(g2);
        disegnaOggetti(g2);
        if (pugniAttivi != null) {
            for (Pugno p : pugniAttivi) p.draw(g2);
        }
        disegnaNemici(g2);
        disegnaGiocatore(g2);
        disegnaHUD(g2);
    }

    // ── Stanza Shop ───────────────────────────────────────────────────────────

    private void disegnaStanzaShop(Graphics2D g2) {
        // Pavimento e muri con colore diverso per distinguerla
        BufferedImage imgMuro  = (state.mondoAttuale % 2 != 0) ? res.imgMuroMondo1      : res.imgMuroMondo2;
        BufferedImage imgPavim = (state.mondoAttuale % 2 != 0) ? res.imgPavimentoMondo1 : res.imgPavimentoMondo2;

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                boolean isMuro = i < GameState.OFFSET
                        || i >= GameState.COL_GIOCO + GameState.OFFSET
                        || j < GameState.OFFSET
                        || j >= GameState.RIG_GIOCO + GameState.OFFSET;
                if (isMuro) {
                    if (imgMuro != null) g2.drawImage(imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(40, 20, 10)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (imgPavim != null) g2.drawImage(imgPavim, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(80, 60, 40)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        // Targa "SHOP" in cima
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Consolas", Font.BOLD, 28));
        g2.drawString("✦  NEGOZIO  ✦", GameState.LARGHEZZA_GIOCO / 2 - 110, GameState.TILE_SIZE - 10);

        // Porta sud (per uscire) al centro in basso
        int portaSudX = (GameState.COL_TOTALI / 2) * GameState.TILE_SIZE;
        int portaSudY = (GameState.RIG_TOTALI - 1) * GameState.TILE_SIZE;
        if (res.imgPorta != null) {
            g2.drawImage(res.imgPorta, portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
        } else {
            g2.setColor(new Color(0, 200, 100, 180));
            g2.fillRect(portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE);
        }
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.drawString("ESCI", portaSudX + 15, portaSudY - 5);

        // Oggetti shop
        for (Shopkeeper sk : roomMgr.getShopkeeperShop()) sk.draw(g2);
        for (ShopItem   si : roomMgr.getItemsShop())      si.draw(g2);
    }

    private void disegnaAmbiente(Graphics2D g2) {
        BufferedImage imgMuro  = (state.mondoAttuale % 2 != 0) ? res.imgMuroMondo1      : res.imgMuroMondo2;
        BufferedImage imgPavim = (state.mondoAttuale % 2 != 0) ? res.imgPavimentoMondo1 : res.imgPavimentoMondo2;

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                boolean isMuro = i < GameState.OFFSET
                        || i >= GameState.COL_GIOCO + GameState.OFFSET
                        || j < GameState.OFFSET
                        || j >= GameState.RIG_GIOCO + GameState.OFFSET;
                if (isMuro) {
                    if (imgMuro != null) g2.drawImage(imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(Color.BLACK); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (imgPavim != null) g2.drawImage(imgPavim, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(Color.GRAY); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        final int T = GameState.TILE_SIZE;

        // ── Porta sinistra (torna indietro) ───────────────────────────────────
        int portaY  = (GameState.RIG_TOTALI / 2) * T;
        if (state.indiceStanzaMemoria > 0) {
            if (res.imgPorta != null) g2.drawImage(res.imgPorta, 0, portaY, T, T, null);
            else { g2.setColor(new Color(150, 100, 50)); g2.fillRect(0, portaY, T, T); }
        }

        // ── Porta destra (avanza) — appare SOLO se stanza pulita ──────────────
        int  portaDX    = (GameState.COL_TOTALI - 1) * T;
        boolean stanzaPulita = roomMgr.getNemiciCorrenti().isEmpty();
        boolean bossUscita   = state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.bossSpawnato && state.bossSconfitto;

        if (bossUscita) {
            // Porta speciale cyan dopo il boss
            g2.setColor(new Color(0, 255, 255, 180));
            g2.fillRect(portaDX, portaY, T, T);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("AVANTI", portaDX + 5, portaY - 5);
        } else if (stanzaPulita || state.stanzaNelMondo == 1) {
            // Porta normale visibile solo a stanza pulita (o stanza 1 che è sempre vuota)
            if (res.imgPorta != null) g2.drawImage(res.imgPorta, portaDX, portaY, T, T, null);
            else { g2.setColor(new Color(150, 100, 50)); g2.fillRect(portaDX, portaY, T, T); }
        }
        // Se nemici ancora vivi: non disegna nulla → il muro appare chiuso

        // ── Porta nord SHOP — solo stanza 1 + shopSbloccato ──────────────────
        if (state.stanzaNelMondo == 1 && state.shopSbloccato) {
            int portaShopX = (GameState.COL_TOTALI / 2) * T; // Centro del muro superiore
            if (res.imgShopDoor != null) {
                g2.drawImage(res.imgShopDoor, portaShopX, 0, T, T, null);
            } else {
                g2.setColor(new Color(255, 215, 0, 200)); // Oro
                g2.fillRect(portaShopX, 0, T, T);
            }
            // Freccia indicativa
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("▲ SHOP", portaShopX - 5, 20);
        }
    }

    private void disegnaOggetti(Graphics2D g2) {
        for (Cura       c  : roomMgr.getCureCorrenti())         c.draw(g2);
        for (Moneta     m  : roomMgr.getMoneteCorrenti())       m.draw(g2);
        for (Shopkeeper sk : roomMgr.getShopkeepersCorrenti())  sk.draw(g2);
        for (ShopItem   si : roomMgr.getShopItemsCorrenti())    si.draw(g2);
    }

    private void disegnaNemici(Graphics2D g2) {
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if      (n instanceof Boss)        n.draw(g2, res.imgBoss);
            else if (n instanceof NemicoForte) n.draw(g2, res.imgNemico2);
            else                               n.draw(g2, res.imgNemico);
        }
    }

    private void disegnaGiocatore(Graphics2D g2) {
        // Lampeggio durante invulnerabilità
        if (state.invulnerabile && state.timerInvulnerabilita % 10 >= 5) return;

        BufferedImage imgPG = ui.listaPersonaggi
                .get(state.indicePersonaggioSelezionato).imgGioco;
        if (imgPG != null) {
            g2.drawImage(imgPG, (int) state.x, (int) state.y,
                    GameState.PG_SIZE, GameState.PG_SIZE, null);
        } else {
            g2.setColor(Color.CYAN);
            g2.fillOval((int) state.x, (int) state.y, GameState.PG_SIZE, GameState.PG_SIZE);
        }
    }

    private void disegnaHUD(Graphics2D g2) {
        // Cuori
        for (int i = 0; i < state.vite; i++) {
            if (res.imgCuore != null)
                g2.drawImage(res.imgCuore, 20 + i * 35, 45, 30, 30, null);
        }

        // Monete
        if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, 20, 85, 25, 25, null);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("" + state.monete, 55, 105);

        // Mondo / stanza
        g2.setFont(new Font("Consolas", Font.BOLD, 22));
        if (state.modalitaScelta == GameState.Modalita.STORIA) {
            g2.drawString("STORIA: " + state.mondoAttuale + "/" + GameState.MONDI_STORIA_MAX, 20, 25);
        } else {
            g2.drawString("INFINITA - M: " + state.mondoAttuale, 20, 25);
        }
        g2.drawString("STANZA: " + state.stanzaNelMondo + "/8", 220, 25);

        // UI Boss
        disegnaUIBoss(g2);
    }

    private void disegnaUIBoss(Graphics2D g2) {
        if (state.stanzaNelMondo != GameState.STANZA_BOSS
                || !state.bossSpawnato || state.bossSconfitto) return;

        // Trova il boss nella lista
        Boss bossCorrente = null;
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if (n instanceof Boss b) { bossCorrente = b; break; }
        }
        if (bossCorrente == null) return;

        int uiX  = (GameState.COL_TOTALI * GameState.TILE_SIZE) / 2 - 150;
        int uiY  = (GameState.RIG_TOTALI * GameState.TILE_SIZE) - 60;
        int barW = 300;
        int barH = 20;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(uiX, uiY, barW, barH);
        g2.setColor(Color.RED);
        int larghezzaVita = (int) (((float) bossCorrente.getVita() / bossCorrente.getVitaMax()) * barW);
        g2.fillRect(uiX, uiY, larghezzaVita, barH);
        g2.setColor(Color.BLACK);
        g2.drawRect(uiX, uiY, barW, barH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.drawString("CAPOCANTIERE CORRUTTO", uiX + 40, uiY - 5);
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2.drawString(bossCorrente.getVita() + "/" + bossCorrente.getVitaMax() + " HP",
                uiX + 110, uiY + 15);

        // Timer
        g2.setFont(new Font("Consolas", Font.BOLD, 20));
        g2.setColor(state.tempoRimanenteBoss < 600 ? Color.RED : Color.WHITE);
        g2.drawString("TEMPO LIMITE: " + (state.tempoRimanenteBoss / 60) + "s",
                GameState.LARGHEZZA_GIOCO - 200, GameState.ALTEZZA_GIOCO - 30);
    }

    // ── Utilità UI ────────────────────────────────────────────────────────────

    /** Sfondo scuro pieno con leggero alone centrale */
    private void sfondoOverlay(Graphics2D g2, Color base) {
        g2.setColor(base);
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        int W = GameState.LARGHEZZA_GIOCO, H = GameState.ALTEZZA_GIOCO;
        for (int r = 250; r > 0; r -= 12) {
            int alpha = (int)(15 * (1.0 - r / 250.0));
            g2.setColor(new Color(60, 60, 140, alpha));
            g2.fillOval(W/2 - r, H/2 - r, r*2, r*2);
        }
    }
}