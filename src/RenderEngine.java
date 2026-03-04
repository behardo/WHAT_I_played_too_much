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

    public void render(Graphics2D g2, int panelWidth, int panelHeight) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        // Aggiorna volume audio da impostazioni
        state.audio.setVolumeMusica(state.impostazioni.volumeMusica);
        state.audio.setVolumeEffetti(state.impostazioni.volumeEffetti);

        // Gestione musica in base allo stato
        aggiornaMusica();

        // Aggiorna layout bottoni a ogni frame (idempotente se dimensioni invariate)
        ui.ricalcolaBottoni(panelWidth, panelHeight);

        // Disegna direttamente nelle dimensioni reali del pannello — nessuna scala,
        // nessuna banda nera, niente deformazione
        switch (state.statoGioco) {
            case MENU                  -> disegnaMenu(g2, panelWidth, panelHeight);
            case IMPOSTAZIONI          -> disegnaImpostazioni(g2, panelWidth, panelHeight);
            case CONTROLLI             -> disegnaControlli(g2, panelWidth, panelHeight);
            case SELEZIONE_PERSONAGGIO -> disegnaSelezionePersonaggio(g2, panelWidth, panelHeight);
            case SELEZIONE_MODALITA    -> disegnaSelezioneModalita(g2, panelWidth, panelHeight);
            case TETRIS                -> disegnaTetris(g2, panelWidth, panelHeight);
            case GIOCO                 -> disegnaGioco(g2, panelWidth, panelHeight);
            case PAUSA -> {
                java.awt.geom.AffineTransform t1 = g2.getTransform();
                disegnaGioco(g2, panelWidth, panelHeight);
                g2.setTransform(t1);  // ripristina prima di disegnare la pausa
                disegnaPausa(g2, panelWidth, panelHeight);
            }
            case GAME_OVER -> {
                java.awt.geom.AffineTransform t2 = g2.getTransform();
                disegnaGioco(g2, panelWidth, panelHeight);
                g2.setTransform(t2);  // ripristina prima di disegnare game over
                disegnaGameOver(g2, panelWidth, panelHeight);
            }
            case VITTORIA_STORIA       -> disegnaVittoriaStoria(g2, panelWidth, panelHeight);
        }
    }

    // ── Menu Principale ───────────────────────────────────────────────────────

    private void disegnaMenu(Graphics2D g2, int W, int H) {
        // Sfondo personalizzabile: se sfondo_menu.png esiste lo usa, altrimenti procedurale
        if (res.imgSfondoMenu != null) {
            g2.drawImage(res.imgSfondoMenu, 0, 0, W, H, null);
            // nessun overlay grigio
        } else {
            // Sfondo con gradiente simulato
            g2.setColor(new Color(12, 12, 28));
            g2.fillRect(0, 0, W, H);
            for (int r = 300; r > 0; r -= 15) {
                int alpha = (int)(18 * (1.0 - r / 300.0));
                g2.setColor(new Color(60, 60, 140, alpha));
                g2.fillOval(W/2 - r, H/2 - r - 80, r*2, r*2);
            }
        }

        // Bottoni menu
        ui.btnGioca.draw(g2);
        ui.btnImpostazioni.draw(g2);
        ui.btnControlli.draw(g2);
        ui.btnEsciMenu.draw(g2);

        // Footer hint
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(new Color(90, 90, 120));
        g2.drawString("F11 = Fullscreen", 20, H - 15);
        g2.drawString("v1.0", W - 50, H - 15);
    }

    // ── Impostazioni ─────────────────────────────────────────────────────────

    private void disegnaImpostazioni(Graphics2D g2, int W, int H) {
        Impostazioni imp = state.impostazioni;

        sfondoOverlay(g2, W, H, new Color(10, 10, 30));

        // Titolo proporzionale
        int titFs = (int)(H * 0.072);
        g2.setColor(new Color(200, 200, 255));
        drawTextCentered(g2, "IMPOSTAZIONI", W/2, (int)(H * 0.17), titFs);
        g2.setColor(new Color(80, 80, 130));
        g2.fillRect(W/2 - (int)(W*0.13), (int)(H*0.195), (int)(W*0.26), 2);

        // Leggo i parametri di layout calcolati in UIManager
        int LX   = ui._impLabelX;
        int CX2  = ui._impCtrlX;
        int SW   = ui._impSw;
        int s    = ui._impStartY;
        int RH   = ui._impRigaH;
        int SH   = ui._impSh;
        int sliderX = CX2 + SW + 5;
        int sliderW = ui.btnMusPiu.bounds != null
                ? ui.btnMusPiu.bounds.x - sliderX - 5   // si adatta ai bottoni reali
                : (int)(W * 0.13);                        // fallback proporzionale

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

    private void disegnaControlli(Graphics2D g2, int W, int H) {
        sfondoOverlay(g2, W, H, new Color(10, 10, 30));

        int titFs = (int)(H * 0.085);
        g2.setColor(new Color(200, 200, 255));
        drawTextCentered(g2, "CONTROLLI", W/2, (int)(H * 0.22), titFs);
        g2.setColor(new Color(80, 80, 130));
        g2.fillRect(W/2 - (int)(W*0.12), (int)(H*0.245), (int)(W*0.24), 2);

        // Layout a due colonne proporzionale
        int col1X = W/2 - (int)(W*0.294);
        int col2X = W/2 + (int)(W*0.028);
        int startY = (int)(H * 0.36);
        int rigaH  = (int)(H * 0.116);

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

    private void disegnaSelezionePersonaggio(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(0, 0, W, H);

        // Titolo e hint — proporzionali
        int titFs  = Math.max(14, (int)(H * 0.067));
        int hintFs = Math.max(10, (int)(H * 0.031));
        g2.setColor(Color.WHITE);
        drawTextCentered(g2, "SCEGLI IL TUO PERSONAGGIO", W/2, (int)(H * 0.13), titFs);
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(160, 160, 190));
        String hint = "[Frecce/Mouse] naviga  [INVIO/Click] conferma  [ESC] indietro";
        g2.drawString(hint, W/2 - g2.getFontMetrics().stringWidth(hint)/2, (int)(H * 0.19));

        // Hint combo segreto
        int comboB = state.sistemaPersonaggi.getContatoreBCombo();
        if (comboB > 0) {
            g2.setColor(new Color(255, 200, 0, 180));
            g2.setFont(new Font("Consolas", Font.BOLD, hintFs));
            g2.drawString("B x " + comboB + " / 5", W - (int)(W * 0.1), (int)(H * 0.06));
        }

        boolean segretoAttivo = state.sistemaPersonaggi.isSegretoAttivo();
        int numMostra = segretoAttivo ? 5 : 4;

        // Usa ESATTAMENTE i rect calcolati da ricalcolaBottoni per coerenza click/draw
        for (int i = 0; i < numMostra; i++) {
            java.awt.Rectangle r = ui.rectsSelezionePG[i];
            if (r == null) continue;
            int rx = r.x, ry = r.y, rw = r.width, rh = r.height;

            DatiPersonaggio pg      = ui.listaPersonaggi.get(i);
            boolean         lock    = !state.sistemaPersonaggi.isSbloccato(i);
            boolean         segreto = (i == SistemaPersonaggi.INDICE_SEGRETO);
            boolean         sel     = (i == state.indicePersonaggioSelezionato) && !lock;

            // Sfondo card
            Color colBG = segreto ? new Color(100, 60, 0, 80)
                    : lock    ? new Color(15, 15, 35, 120)
                    : sel     ? new Color(255, 215, 0, 80)
                    : new Color(255, 255, 255, 20);
            Color colBordo = segreto ? new Color(255, 200, 0)
                    : lock    ? new Color(50, 50, 70)
                    : sel     ? Color.YELLOW
                    : new Color(100, 100, 140);
            g2.setColor(colBG);
            g2.fillRoundRect(rx, ry, rw, rh, 8, 8);
            g2.setColor(colBordo);
            g2.setStroke(new BasicStroke(sel ? 2.5f : lock ? 1f : 1.5f));
            g2.drawRoundRect(rx, ry, rw, rh, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Proporzioni interne alla card
            int imgSize = (int)(rw * 0.55);
            int imgX    = rx + (rw - imgSize) / 2;
            int imgY    = ry + (int)(rh * 0.06);
            int nomeY   = ry + (int)(rh * 0.55);
            int descY   = ry + (int)(rh * 0.65);
            int statsY  = ry + (int)(rh * 0.75);
            int cardFs  = Math.max(9, (int)(rw * 0.10));
            int smallFs = Math.max(8, (int)(rw * 0.085));

            if (lock) {
                // Icona sbiadita
                if (pg.imgIcona != null) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, 0.25f));
                    g2.drawImage(pg.imgIcona, imgX, imgY, imgSize, imgSize, null);
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, 1f));
                }
                // Nome sbiadito
                g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)cardFs) : new Font("Consolas", Font.BOLD, cardFs));
                g2.setColor(new Color(80, 80, 100));
                FontMetrics fmL = g2.getFontMetrics();
                g2.drawString(pg.nome, rx + (rw - fmL.stringWidth(pg.nome))/2, nomeY);
                // Lucchetto
                int lockFs = (int)(rh * 0.18);
                g2.setFont(new Font("Arial", Font.PLAIN, lockFs));
                g2.setColor(new Color(100, 100, 130));
                g2.drawString("\uD83D\uDD12",
                        rx + rw/2 - lockFs/2, ry + (int)(rh * 0.70));
                // Testo sblocco
                g2.setFont(new Font("Arial", Font.PLAIN, smallFs));
                g2.setColor(new Color(120, 120, 150));
                String[] righe = state.sistemaPersonaggi.testoSblocco(i).split("\n");
                for (int rr = 0; rr < righe.length; rr++) {
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(righe[rr],
                            rx + (rw - fm2.stringWidth(righe[rr]))/2,
                            statsY + rr * (smallFs + 3));
                }
                continue;
            }

            // Card sbloccata — icona
            if (pg.imgIcona != null) {
                g2.drawImage(pg.imgIcona, imgX, imgY, imgSize, imgSize, null);
            } else if (segreto) {
                // Fallback stella solo se icona_p4.png non è presente
                g2.setFont(new Font("Serif", Font.BOLD, (int)(rh * 0.25)));
                g2.setColor(new Color(255, 215, 0));
                g2.drawString("\u2605",
                        rx + rw/2 - (int)(rh * 0.12), imgY + imgSize - (int)(rh*0.03));
            } else {
                g2.setColor(new Color(150, 150, 180));
                g2.fillOval(imgX, imgY, imgSize, imgSize);
            }

            // Nome
            g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)cardFs) : new Font("Consolas", Font.BOLD, cardFs));
            g2.setColor(segreto ? new Color(255, 215, 0) : sel ? Color.YELLOW : Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pg.nome, rx + (rw - fm.stringWidth(pg.nome))/2, nomeY);

            // Descrizione
            g2.setFont(new Font("Arial", Font.PLAIN, smallFs));
            g2.setColor(segreto ? new Color(255, 180, 0) : new Color(170, 170, 200));
            FontMetrics fmD = g2.getFontMetrics();
            g2.drawString(pg.descrizione, rx + (rw - fmD.stringWidth(pg.descrizione))/2, descY);

            // Stats
            g2.setFont(new Font("Consolas", Font.BOLD, smallFs));
            int icoS = smallFs;
            if (res.imgCuore != null)
                g2.drawImage(res.imgCuore, rx + 5, statsY - icoS + 2, icoS, icoS, null);
            g2.setColor(new Color(255, 100, 100));
            g2.drawString("" + pg.vitaMax, rx + 6 + icoS, statsY);
            g2.setColor(new Color(100, 200, 255));
            g2.drawString("V:" + pg.velocitaBase, rx + 5, statsY + smallFs + 3);
            g2.setColor(new Color(255, 200, 100));
            g2.drawString("D:" + pg.dannoBase, rx + rw/2, statsY + smallFs + 3);
        }
    }


    // ── Selezione Modalità ────────────────────────────────────────────────────

    private void disegnaSelezioneModalita(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(40, 20, 50));
        g2.fillRect(0, 0, W, H);

        int titFs  = Math.max(14, (int)(H * 0.067));
        int hintFs = Math.max(10, (int)(H * 0.031));
        g2.setColor(Color.WHITE);
        drawTextCentered(g2, "SELEZIONA LA SFIDA", W/2, (int)(H * 0.13), titFs);
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(160, 160, 190));
        String hint = "[Frecce/Mouse] naviga  [INVIO/Click] conferma  [ESC] indietro";
        g2.drawString(hint, W/2 - g2.getFontMetrics().stringWidth(hint)/2, (int)(H * 0.19));

        String[] nomi  = { "STORIA CLASSICA", "MODALITA INFINITA" };
        String[] desc1 = { "Sconfiggi 4 Boss", "Sopravvivi all'infinito!" };
        String[] desc2 = { "per salvare il cantiere", "Nemici sempre piu forti." };
        BufferedImage[] icone    = { res.imgIconaStoria, res.imgIconaInfinita };
        String[] fallback = { "S", "inf" };

        for (int i = 0; i < 2; i++) {
            java.awt.Rectangle rect = ui.rectsSelezioneModalita[i];
            if (rect == null) continue;
            boolean sel = (i == state.indiceModalitaSelezionata);

            // Sfondo
            g2.setColor(sel ? new Color(173, 216, 230, 80) : new Color(255, 255, 255, 15));
            g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g2.setColor(sel ? Color.CYAN : new Color(120, 120, 160));
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Proporzioni interne
            int imgSize = (int)(rect.width * 0.45);
            int imgX    = rect.x + (rect.width - imgSize) / 2;
            int imgY    = rect.y + (int)(rect.height * 0.07);
            int nomeY   = rect.y + (int)(rect.height * 0.60);
            int d1Y     = rect.y + (int)(rect.height * 0.72);
            int d2Y     = rect.y + (int)(rect.height * 0.83);
            int nomeFs  = Math.max(11, (int)(rect.width * 0.09));
            int descFs  = Math.max(9,  (int)(rect.width * 0.075));

            // Icona
            if (icone[i] != null) {
                g2.drawImage(icone[i], imgX, imgY, imgSize, imgSize, null);
            } else {
                g2.setColor(sel ? Color.CYAN : Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, (int)(rect.height * 0.30)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fallback[i],
                        rect.x + (rect.width - fm.stringWidth(fallback[i]))/2,
                        imgY + imgSize);
            }

            // Nome
            g2.setColor(sel ? Color.CYAN : Color.WHITE);
            g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)nomeFs) : new Font("Consolas", Font.BOLD, nomeFs));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(nomi[i], rect.x + (rect.width - fm.stringWidth(nomi[i]))/2, nomeY);

            // Descrizione
            g2.setFont(new Font("Arial", Font.PLAIN, descFs));
            g2.setColor(new Color(190, 190, 210));
            FontMetrics fmD = g2.getFontMetrics();
            g2.drawString(desc1[i], rect.x + (rect.width - fmD.stringWidth(desc1[i]))/2, d1Y);
            g2.drawString(desc2[i], rect.x + (rect.width - fmD.stringWidth(desc2[i]))/2, d2Y);
        }
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void disegnaPausa(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, W, H);

        // Pannello centrato — tutto in percentuale di W/H
        int BH  = ui.btnRiprendi.bounds.height;
        int GAP = (int)(H * 0.022);
        int panW = (int)(W * 0.32);
        int panX = W/2 - panW/2;
        int btnY = ui.btnRiprendi.bounds.y;          // già calcolato da ricalcolaBottoni
        int titY = btnY - (int)(H * 0.07);
        int panY = titY - (int)(H * 0.06);
        int panH = (BH + GAP) * 4 + (int)(H * 0.13);

        g2.setColor(new Color(15, 15, 35, 235));
        g2.fillRoundRect(panX, panY, panW, panH, 16, 16);
        g2.setColor(new Color(80, 80, 140));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panX, panY, panW, panH, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(200, 200, 255));
        drawTextCentered(g2, "PAUSA", W/2, titY + (int)(H*0.045), (int)(H*0.065));
        g2.setColor(new Color(60, 60, 110));
        int sepW = (int)(panW * 0.5);
        g2.fillRect(W/2 - sepW/2, titY + (int)(H*0.052), sepW, 2);

        // Mouse ha priorita'. Se nessun bottone e' in hover, usa selezione tastiera.
        MenuButton[] btnsPausa = {
                ui.btnRiprendi, ui.btnImpostazioniPausa,
                ui.btnMenuPrincipalePausa, ui.btnEsciPausa
        };
        boolean mouseAttivo = false;
        for (MenuButton b : btnsPausa) if (b.hover) { mouseAttivo = true; break; }
        for (int i = 0; i < btnsPausa.length; i++) {
            if (!mouseAttivo) btnsPausa[i].hover = (i == state.indiceBtnPausa);
            btnsPausa[i].draw(g2);
        }
    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void disegnaGameOver(Graphics2D g2, int W, int H) {
        if (res.imgSfondoGameOver != null) {
            g2.drawImage(res.imgSfondoGameOver, 0, 0, W, H, null);
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, W, H);
        } else {
            g2.setColor(new Color(0, 0, 0, 185));
            g2.fillRect(0, 0, W, H);
        }

        // Titolo rosso con ombra
        g2.setFont(new Font("Consolas", Font.BOLD, 62));
        FontMetrics fm = g2.getFontMetrics();
        String t = "GAME OVER";
        int tx = W/2 - fm.stringWidth(t)/2;
        int goFs = (int)(H * 0.12);
        int goY  = (int)(H * 0.42);
        g2.setColor(new Color(100, 0, 0));
        drawTextCentered(g2, t, W/2 + 4, goY + 4, goFs);
        g2.setColor(new Color(220, 40, 40));
        drawTextCentered(g2, t, W/2, goY, goFs);

        // Stats
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(new Color(200, 180, 180));
        String stats = "Mondo " + state.mondoAttuale + "  |  Stanza " + state.stanzaNelMondo
                + "  |  Monete " + state.monete;
        g2.drawString(stats, W/2 - g2.getFontMetrics().stringWidth(stats)/2, H/2 + 10);

        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
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

    private void disegnaVittoriaStoria(Graphics2D g2, int W, int H) {
        if (res.imgSfondoVittoria != null) {
            g2.drawImage(res.imgSfondoVittoria, 0, 0, W, H, null);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, W, H);
        } else {
            g2.setColor(new Color(10, 40, 10));
            g2.fillRect(0, 0, W, H);
            for (int r = 280; r > 0; r -= 12) {
                int alpha = (int)(20 * (1.0 - r / 280.0));
                g2.setColor(new Color(200, 180, 0, alpha));
                g2.fillOval(W/2 - r, H/2 - r - 40, r*2, r*2);
            }
        }

        int vitFs  = (int)(H * 0.115);
        int vitY   = (int)(H * 0.40);
        g2.setColor(new Color(80, 60, 0));
        drawTextCentered(g2, "STORIA COMPLETATA!", W/2 + 3, vitY + 3, vitFs);
        g2.setColor(new Color(255, 220, 0));
        drawTextCentered(g2, "STORIA COMPLETATA!", W/2, vitY, vitFs);

        int subFs = (int)(H * 0.049);
        g2.setFont(new Font("Arial", Font.PLAIN, subFs));
        g2.setColor(Color.WHITE);
        String sub = "Hai sconfitto tutti i Boss e salvato il cantiere!";
        int subY = vitY + (int)(H * 0.13);
        g2.drawString(sub, W/2 - g2.getFontMetrics().stringWidth(sub)/2, subY);

        int mFs = (int)(H * 0.054);
        g2.setFont(new Font("Consolas", Font.BOLD, mFs));
        g2.setColor(new Color(255, 215, 0));
        String monete = "Monete raccolte: " + state.monete;
        g2.drawString(monete, W/2 - g2.getFontMetrics().stringWidth(monete)/2, subY + (int)(H*0.09));

        ui.btnMenuPrincipaleVittoria.draw(g2);
    }

    // ── Tetris ────────────────────────────────────────────────────────────────

    private void disegnaTetris(Graphics2D g2, int W, int H) {
        TetrisGame t = state.tetris;

        // ── Font helpers ──────────────────────────────────────────────────────
        java.util.function.Function<Float, java.awt.Font> cf  =
                s -> res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD,  s)
                        : new Font("Consolas", Font.BOLD,  s.intValue());
        java.util.function.Function<Float, java.awt.Font> cfl =
                s -> res.fontCustom != null ? res.fontCustom.deriveFont(Font.PLAIN, s)
                        : new Font("Consolas", Font.PLAIN, s.intValue());

        // ── Dimensioni celle proporzionali ────────────────────────────────────
        // Riserviamo il 38% della larghezza per i due pannelli laterali (19% ciascuno)
        // e il resto per shell+griglia. La griglia occupa al max il 90% dell'altezza.
        int availForShell = (int)(W * 0.62f);
        int cellH = Math.min((int)(H * 0.043f),
                Math.min((int)(availForShell * 0.72f / TetrisGame.COLS),
                        (int)(H * 0.9f / TetrisGame.ROWS)));
        int cellW = cellH;
        int gridW = TetrisGame.COLS * cellW;
        int gridH = TetrisGame.ROWS * cellH;

        // ── Gameboy shell ─────────────────────────────────────────────────────
        int shellPadX = (int)(cellW * 1.4f);
        int shellPadT = (int)(cellH * 2.8f);
        int shellPadB = (int)(cellH * 3.2f);
        int shellW    = gridW + shellPadX * 2;
        int shellH    = gridH + shellPadT + shellPadB;
        int shellX    = W / 2 - shellW / 2;
        int shellY    = Math.max(4, H / 2 - shellH / 2);

        // Coordinate griglia dentro il body
        int gridX = shellX + shellPadX;
        int gridY = shellY + shellPadT;

        // Pannelli laterali — usano tutto lo spazio disponibile a sx e dx del body
        int sideGap = Math.max(6, (int)(cellW * 0.7f));
        int sideW   = Math.max((int)(cellW * 4f),
                Math.min((shellX - sideGap),        // spazio a sinistra
                        (W - shellX - shellW - sideGap))); // spazio a destra
        int leftX   = shellX - sideGap - sideW;
        int rightX  = shellX + shellW + sideGap;
        int sideY   = shellY;
        int sideH   = shellH;
        // Clamp: se non c'è spazio, i pannelli si sovrappongono ma non escono dallo schermo
        if (leftX < 2) leftX = 2;
        if (rightX + sideW > W - 2) sideW = W - 2 - rightX;

        // ── Sfondo pagina ─────────────────────────────────────────────────────
        g2.setColor(new Color(8, 8, 18));
        g2.fillRect(0, 0, W, H);

        // ── Gameboy body ──────────────────────────────────────────────────────
        // Ombra profonda
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(shellX + 6, shellY + 8, shellW, shellH, 28, 28);
        // Body principale — grigio scuro stile plastica
        GradientPaint bodyGrad = new GradientPaint(
                shellX, shellY, new Color(52, 52, 68),
                shellX, shellY + shellH, new Color(30, 30, 42));
        g2.setPaint(bodyGrad);
        g2.fillRoundRect(shellX, shellY, shellW, shellH, 28, 28);
        // Bordo esterno
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(90, 90, 120));
        g2.drawRoundRect(shellX, shellY, shellW, shellH, 28, 28);
        // Bordo interno luminoso
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(130, 130, 170, 80));
        g2.drawRoundRect(shellX + 3, shellY + 3, shellW - 6, shellH - 6, 25, 25);
        g2.setStroke(new BasicStroke(1f));

        // Schermino LCD — cornice attorno alla griglia
        int lcdPad = (int)(cellW * 0.35f);
        int lcdX   = gridX - lcdPad;
        int lcdY   = gridY - lcdPad;
        int lcdW   = gridW + lcdPad * 2;
        int lcdH   = gridH + lcdPad * 2;
        // Cornice esterna LCD
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(lcdX - 4, lcdY - 4, lcdW + 8, lcdH + 8, 8, 8);
        g2.setColor(new Color(70, 70, 100));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(lcdX - 4, lcdY - 4, lcdW + 8, lcdH + 8, 8, 8);
        g2.setStroke(new BasicStroke(1f));
        // Schermo LCD (verde verdino tipico gameboy)
        g2.setColor(new Color(15, 22, 18));
        g2.fillRect(lcdX, lcdY, lcdW, lcdH);

        // Logo "GAME BOY" style sotto la griglia dentro il body
        String logo = "WHAT TETRIS";
        int logoFs = Math.max(9, (int)(cellH * 0.55f));
        // Riduci se troppo largo per il body
        g2.setFont(cf.apply((float) logoFs));
        while (logoFs > 8 && g2.getFontMetrics().stringWidth(logo) > shellW - shellPadX * 2) {
            logoFs--;
            g2.setFont(cf.apply((float) logoFs));
        }
        g2.setColor(new Color(100, 100, 140));
        FontMetrics fmLogo = g2.getFontMetrics();
        g2.drawString(logo, shellX + (shellW - fmLogo.stringWidth(logo)) / 2,
                gridY + gridH + (int)(cellH * 1.1f));

        // ── Pannello sinistro (PUNTEGGIO · LIVELLO · RIGHE) ──────────────────
        disegnaPannelloLaterale(g2, leftX,  sideY, sideW, sideH, cellH, cf, cfl, t, false);

        // ── Pannello destro (NEXT · PREMIO) ──────────────────────────────────
        disegnaPannelloLaterale(g2, rightX, sideY, sideW, sideH, cellH, cf, cfl, t, true);

        if (t == null) return;

        // ── Timer riquadro in alto ─────────────────────────────────────────────
        int sec     = t.secondiRimanenti();
        boolean urg = sec < 15;
        int timerBoxH = (int)(shellPadT * 0.72f);
        int timerBoxW = (int)(shellW * 0.62f);
        int timerBoxX = shellX + (shellW - timerBoxW) / 2;
        int timerBoxY = shellY + (shellPadT - timerBoxH) / 2;

        // Sfondo timer
        g2.setColor(urg ? new Color(80, 10, 10, 220) : new Color(20, 20, 40, 220));
        g2.fillRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 10, 10);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(urg ? new Color(255, 60, 60) : new Color(80, 80, 140));
        g2.drawRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 10, 10);
        g2.setStroke(new BasicStroke(1f));

        // Contenuto timer
        int timerLabelFs = Math.max(9, (int)(timerBoxH * 0.30f));
        int timerValFs   = Math.max(12, (int)(timerBoxH * 0.52f));
        String timerStr  = sec / 60 + ":" + String.format("%02d", sec % 60);
        g2.setFont(cfl.apply((float) timerLabelFs));
        FontMetrics fmTL = g2.getFontMetrics();
        g2.setColor(urg ? new Color(255, 120, 120) : new Color(150, 150, 200));
        g2.drawString("TEMPO", timerBoxX + (timerBoxW - fmTL.stringWidth("TEMPO")) / 2,
                timerBoxY + timerLabelFs + 3);
        g2.setFont(cf.apply((float) timerValFs));
        FontMetrics fmTV = g2.getFontMetrics();
        g2.setColor(urg ? Color.RED : Color.WHITE);
        g2.drawString(timerStr, timerBoxX + (timerBoxW - fmTV.stringWidth(timerStr)) / 2,
                timerBoxY + timerBoxH - 5);

        // ── Celle griglia ─────────────────────────────────────────────────────
        for (int r = 0; r < TetrisGame.ROWS; r++) {
            for (int c = 0; c < TetrisGame.COLS; c++) {
                int val = t.grid[r][c];
                if (val == 0) {
                    g2.setColor(new Color(18, 26, 20));
                    g2.fillRect(gridX + c*cellW, gridY + r*cellH, cellW - 1, cellH - 1);
                } else {
                    disegnaCellaTetris(g2, gridX + c*cellW, gridY + r*cellH,
                            cellW - 1, cellH - 1, TetrisGame.COLORI[val - 1]);
                }
            }
        }

        // ── Pezzo corrente + ghost ────────────────────────────────────────────
        if (!t.gameOver && !t.completato) {
            int col = TetrisGame.COLORI[t.tipoPezzo];
            // Ghost
            int ghostY2 = t.pezzoY;
            outer:
            while (true) {
                int ny = ghostY2 + 1;
                for (int[] cell : t.getCelleCorrente()) {
                    int gx = cell[0], gy = ny + (cell[1] - t.pezzoY);
                    if (gx < 0 || gx >= TetrisGame.COLS || gy >= TetrisGame.ROWS) break outer;
                    if (gy >= 0 && t.grid[gy][gx] != 0) break outer;
                }
                ghostY2 = ny;
            }
            int ghostDy = ghostY2 - t.pezzoY;
            if (ghostDy > 0) {
                for (int[] cell : t.getCelleCorrente()) {
                    int cx = gridX + cell[0] * cellW;
                    int cy = gridY + (cell[1] + ghostDy) * cellH;
                    if (cell[1] + ghostDy >= 0) {
                        int r2 = (col >> 16) & 0xFF, gv = (col >> 8) & 0xFF, b2 = col & 0xFF;
                        g2.setColor(new Color(r2, gv, b2, 55));
                        g2.fillRect(cx, cy, cellW - 1, cellH - 1);
                    }
                }
            }
            // Pezzo vero
            for (int[] cell : t.getCelleCorrente()) {
                int cx = gridX + cell[0] * cellW;
                int cy = gridY + cell[1] * cellH;
                if (cell[1] >= 0)
                    disegnaCellaTetris(g2, cx, cy, cellW - 1, cellH - 1, col);
            }
        }

        // Linee griglia leggere sul LCD
        g2.setColor(new Color(25, 38, 28, 80));
        for (int c = 1; c < TetrisGame.COLS; c++)
            g2.drawLine(gridX + c*cellW - 1, gridY, gridX + c*cellW - 1, gridY + gridH);
        for (int r = 1; r < TetrisGame.ROWS; r++)
            g2.drawLine(gridX, gridY + r*cellH - 1, gridX + gridW, gridY + r*cellH - 1);

        // ── Istruzioni centrate in basso ──────────────────────────────────────
        if (!t.completato && !t.gameOver) {
            int iFs = Math.max(9, (int)(cellH * 0.55f));
            g2.setFont(cfl.apply((float) iFs));
            g2.setColor(new Color(80, 80, 110));
            String istr = "A/D muovi   W ruota   S scendi   SPAZIO caduta   ESC salta";
            FontMetrics fmI = g2.getFontMetrics();
            g2.drawString(istr, W / 2 - fmI.stringWidth(istr) / 2,
                    shellY + shellH + (int)(cellH * 0.9f));
        }

        // ── Overlay fine partita ──────────────────────────────────────────────
        if (t.completato || t.gameOver) {
            g2.setColor(new Color(0, 0, 0, 175));
            g2.fillRect(0, 0, W, H);
            int oFs1 = (int)(cellH * 1.6f);
            int oFs2 = (int)(cellH * 1.05f);
            int oFs3 = (int)(cellH * 0.80f);
            String msg = t.gameOver ? "GAME OVER" : "TEMPO SCADUTO!";
            g2.setFont(cf.apply((float) oFs1));
            FontMetrics fm1 = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(msg, W/2 - fm1.stringWidth(msg)/2, H/2 - cellH);
            g2.setFont(cfl.apply((float) oFs2));
            FontMetrics fm2 = g2.getFontMetrics();
            String ptsTxt = "Punteggio: " + t.punteggio;
            g2.setColor(new Color(200, 200, 255));
            g2.drawString(ptsTxt, W/2 - fm2.stringWidth(ptsTxt)/2, H/2 + (int)(cellH * 0.7f));
            String premioTxt = t.getPowerUp().equals("NESSUNO")
                    ? "Nessun power-up" : "Power-up: " + t.getPowerUp();
            g2.setFont(cf.apply((float) oFs2));
            FontMetrics fm3 = g2.getFontMetrics();
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(premioTxt, W/2 - fm3.stringWidth(premioTxt)/2, H/2 + (int)(cellH * 2.0f));
            g2.setFont(cfl.apply((float) oFs3));
            FontMetrics fm4 = g2.getFontMetrics();
            g2.setColor(new Color(160, 160, 200));
            String hint = "INVIO per continuare";
            g2.drawString(hint, W/2 - fm4.stringWidth(hint)/2, H/2 + (int)(cellH * 3.8f));
        }
    }

    /**
     * Disegna un pannello laterale del Gameboy (sx o dx).
     * sx=false → pannello sinistra: PUNTEGGIO, LIVELLO, RIGHE
     * sx=true  → pannello destra: NEXT piece, PREMIO
     */
    private void disegnaPannelloLaterale(Graphics2D g2,
                                         int px, int py, int pw, int ph, int cellH,
                                         java.util.function.Function<Float, java.awt.Font> cf,
                                         java.util.function.Function<Float, java.awt.Font> cfl,
                                         TetrisGame t, boolean isDestro) {

        // Sfondo pannello
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(px + 3, py + 4, pw, ph, 14, 14);
        GradientPaint panGrad = new GradientPaint(
                px, py, new Color(38, 36, 54),
                px, py + ph, new Color(22, 20, 34));
        g2.setPaint(panGrad);
        g2.fillRoundRect(px, py, pw, ph, 14, 14);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(80, 75, 110));
        g2.drawRoundRect(px, py, pw, ph, 14, 14);
        g2.setStroke(new BasicStroke(1f));

        if (t == null) return;

        int pad     = (int)(cellH * 0.4f);
        // Scala i font anche in base alla larghezza del pannello per evitare overflow
        int labelFs = Math.max(9,  Math.min((int)(cellH * 0.72f), (int)(pw * 0.18f)));
        int valFs   = Math.max(11, Math.min((int)(cellH * 0.92f), (int)(pw * 0.22f)));
        int cy      = py + pad + labelFs;

        if (!isDestro) {
            // ── Pannello SINISTRO: PUNTEGGIO · LIVELLO · RIGHE ───────────────
            String[][] voci = {
                    {"PUNTEGGIO", "" + t.punteggio},
                    {"LIVELLO",   "" + t.livello},
                    {"RIGHE",     "" + t.righe},
            };
            Color[] coloriVal = {
                    Color.WHITE,
                    new Color(255, 220, 80),
                    new Color(100, 220, 100),
            };
            for (int i = 0; i < voci.length; i++) {
                // Separatore
                if (i > 0) {
                    g2.setColor(new Color(70, 65, 100, 120));
                    g2.drawLine(px + pad, cy - labelFs, px + pw - pad, cy - labelFs);
                }
                g2.setFont(cfl.apply((float) labelFs));
                FontMetrics fmL = g2.getFontMetrics();
                g2.setColor(new Color(140, 135, 180));
                g2.drawString(voci[i][0], px + (pw - fmL.stringWidth(voci[i][0])) / 2, cy);
                cy += (int)(labelFs * 1.1f);
                g2.setFont(cf.apply((float) valFs));
                FontMetrics fmV = g2.getFontMetrics();
                g2.setColor(coloriVal[i]);
                g2.drawString(voci[i][1], px + (pw - fmV.stringWidth(voci[i][1])) / 2, cy);
                cy += (int)(valFs * 1.5f);
            }
        } else {
            // ── Pannello DESTRO: NEXT + PREMIO ───────────────────────────────
            // NEXT label
            g2.setFont(cfl.apply((float) labelFs));
            FontMetrics fmL = g2.getFontMetrics();
            g2.setColor(new Color(140, 135, 180));
            g2.drawString("PROSSIMO", px + (pw - fmL.stringWidth("PROSSIMO")) / 2, cy);
            cy += (int)(labelFs * 0.5f);

            // Next piece centrata
            int cellN  = Math.max(8, (int)(cellH * 0.75f));
            int nextW  = 4 * cellN;
            int nextX  = px + (pw - nextW) / 2;
            int nextYs = cy + 4;
            int nc     = TetrisGame.COLORI[t.tipoNext];
            for (int[] cell : t.getCelleNext())
                disegnaCellaTetris(g2, nextX + cell[0]*cellN, nextYs + cell[1]*cellN,
                        cellN - 1, cellN - 1, nc);
            cy += 4 * cellN + (int)(cellH * 0.6f);

            // Separatore
            g2.setColor(new Color(70, 65, 100, 120));
            g2.drawLine(px + pad, cy, px + pw - pad, cy);
            cy += (int)(labelFs * 0.9f);

            // PREMIO label
            g2.setFont(cfl.apply((float) labelFs));
            FontMetrics fmP = g2.getFontMetrics();
            g2.setColor(new Color(140, 135, 180));
            g2.drawString("PREMIO", px + (pw - fmP.stringWidth("PREMIO")) / 2, cy);
            cy += (int)(labelFs * 1.1f);

            String pu = t.getPowerUp();
            String puLabel = switch (pu) {
                case "CURA"     -> "+VITA";
                case "VELOCITA" -> "+VEL";
                case "DANNO"    -> "+DANNO";
                case "TUTTO"    -> "TUTTO!";
                default         -> "NESSUNO";
            };
            Color puColor = switch (pu) {
                case "CURA"     -> new Color(100, 220, 100);
                case "VELOCITA" -> new Color(100, 180, 255);
                case "DANNO"    -> new Color(255, 100, 100);
                case "TUTTO"    -> new Color(255, 215, 0);
                default         -> new Color(140, 140, 160);
            };
            g2.setFont(cf.apply((float) valFs));
            FontMetrics fmPV = g2.getFontMetrics();
            g2.setColor(puColor);
            g2.drawString(puLabel, px + (pw - fmPV.stringWidth(puLabel)) / 2, cy);
            cy += (int)(valFs * 1.5f);

            // Soglia prossimo premio
            String soglia = switch (pu) {
                case "TUTTO"    -> "";
                case "DANNO"    -> "6000: TUTTO";
                case "VELOCITA" -> "3000: DANNO";
                case "CURA"     -> "1500: VEL";
                default         -> "500: CURA";
            };
            if (!soglia.isEmpty()) {
                int sFs = Math.max(8, (int)(labelFs * 0.80f));
                g2.setFont(cfl.apply((float) sFs));
                FontMetrics fmS = g2.getFontMetrics();
                g2.setColor(new Color(110, 105, 145));
                g2.drawString(soglia, px + (pw - fmS.stringWidth(soglia)) / 2, cy);
            }
        }
    }

    private void disegnaCellaTetris(Graphics2D g2, int x, int y, int w, int h, int rgb) {
        int r = (rgb >> 16) & 0xFF, gv = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        g2.setColor(new Color(r, gv, b));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(Math.min(255, r + 60), Math.min(255, gv + 60), Math.min(255, b + 60)));
        g2.drawLine(x, y, x + w, y);
        g2.drawLine(x, y, x, y + h);
        g2.setColor(new Color(Math.max(0, r - 60), Math.max(0, gv - 60), Math.max(0, b - 60)));
        g2.drawLine(x + w, y, x + w, y + h);
        g2.drawLine(x, y + h, x + w, y + h);
    }

    // ── Gioco ─────────────────────────────────────────────────────────────────

    private void disegnaGioco(Graphics2D g2, int W, int H) {
        // Applica scala + offset per adattare il gameplay alla finestra reale
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);

        // Sfondo: riempie tutta la finestra col colore muro del tema
        TileSet tsBg = tileSetCorrente();
        g2.setColor(tsBg.coloreTemaMuro);
        g2.fillRect(0, 0, W, H);

        // Salva transform, applica scala gameplay
        java.awt.geom.AffineTransform baseTransform = g2.getTransform();
        g2.translate(gox, goy);
        g2.scale(gs, gs);

        if (roomMgr.inStanzaShop) {
            disegnaStanzaShop(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            for (Shopkeeper sk : roomMgr.getShopkeeperShop()) sk.draw(g2);
            for (ShopItem   si : roomMgr.getItemsShop())      si.draw(g2);
            if (pugniAttivi != null) for (Pugno p : pugniAttivi) p.draw(g2);
            for (Nemico n : roomMgr.getShopNemici()) { n.draw(g2, null); n.disegnaBarraVita(g2); }
            disegnaGiocatore(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            g2.setTransform(baseTransform);
            disegnaHUD(g2, W, H);
            state.dialogoShopkeeper.disegna(g2);
            if (state.dialogoShopkeeper.isVisibile())
                disegnaSceltaShopkeeper(g2, W, H);
            // Dialogo narrazione shopkeeper (JRPG) — sopra il vecchio dialogo
            if (state.dialogoNarrazione.isAttivo())
                disegnaDialogoNarrazione(g2, W, H);
            return;
        }

        disegnaAmbiente(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        disegnaOggetti(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        if (pugniAttivi != null) for (Pugno p : pugniAttivi) p.draw(g2);
        disegnaNemici(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        disegnaGiocatore(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        // Ripristina transform e disegna HUD sopra (non scalato)
        g2.setTransform(baseTransform);
        disegnaHUD(g2, W, H);

        // Banner stanza Casa (prima stanza mondo 1)
        if (state.mondoAttuale == 1 && state.stanzaNelMondo == 1
                && state.stanzaCasaVisitata) {
            disegnaBannerCasa(g2, W, H);
        }

        // Dialogo narrazione (boss intro + shopkeeper JRPG)
        if (state.dialogoNarrazione.isAttivo()) {
            disegnaDialogoNarrazione(g2, W, H);
        }

        // Dialogo Casa — "WHAT? I'VE PLAYED TOO MUCH..."
        if (state.mostraDialogoCasa) {
            disegnaDialogoCasa(g2, W, H);
        }
    }

    // ── Stanza Shop ───────────────────────────────────────────────────────────

    private void disegnaStanzaShop(Graphics2D g2, int W, int H) {
        TileSet ts = tileSetCorrente();

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                if (TileSet.isMuro(i, j)) {
                    if (ts.imgMuro != null)
                        g2.drawImage(ts.imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(40, 20, 10)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (ts.imgPavimento != null)
                        g2.drawImage(ts.imgPavimento, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(80, 60, 40)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        // Targa "NEGOZIO" in cima con font custom
        String negozioTxt = "NEGOZIO";
        Font fontNegozio = res.fontCustom != null
                ? res.fontCustom.deriveFont(Font.BOLD, 30f)
                : new Font("Consolas", Font.BOLD, 28);
        g2.setFont(fontNegozio);
        FontMetrics fmN = g2.getFontMetrics();
        int negX = W / 2 - fmN.stringWidth(negozioTxt) / 2;
        int negY = GameState.TILE_SIZE - 10;
        // Ombra
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(negozioTxt, negX + 2, negY + 2);
        // Testo dorato
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(negozioTxt, negX, negY);

        // Porta sud (per uscire) al centro in basso
        int portaSudX = (GameState.COL_TOTALI / 2) * GameState.TILE_SIZE;
        int portaSudY = (GameState.RIG_TOTALI - 1) * GameState.TILE_SIZE;
        if (res.imgPorta != null) {
            g2.drawImage(res.imgPorta, portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
        } else {
            g2.setColor(new Color(0, 200, 100, 180));
            g2.fillRect(portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE);
        }


        // Oggetti shop
        for (Shopkeeper sk : roomMgr.getShopkeeperShop()) sk.draw(g2);
        for (ShopItem   si : roomMgr.getItemsShop())      si.draw(g2);
    }


    /** Scala del gameplay: adatta le coordinate tile/giocatore alla finestra reale. */
    private double gameScale(int W, int H) {
        return Math.min(
                (double) W / GameState.LARGHEZZA_GIOCO,
                (double) H / GameState.ALTEZZA_GIOCO
        );
    }
    /** Offset X per centrare il gioco nella finestra reale. */
    private int gameOffX(int W, int H) {
        return (W - (int)(GameState.LARGHEZZA_GIOCO * gameScale(W, H))) / 2;
    }
    /** Offset Y per centrare il gioco nella finestra reale. */
    private int gameOffY(int W, int H) {
        return (H - (int)(GameState.ALTEZZA_GIOCO * gameScale(W, H))) / 2;
    }

    // ── Font helpers ──────────────────────────────────────────────────────────



    /**
     * Disegna testo usando (in ordine di priorità):
     *  1. fontCustom (PHONIXEA.ttf) se caricato
     *  2. BitmapFont (font.png) se disponibile
     *  3. Font Java corrente come fallback
     */
    private void drawText(Graphics2D g2, String testo, int x, int y, int altPx) {
        if (res.fontCustom != null) {
            java.awt.Font f = res.fontCustom.deriveFont((float) altPx);
            g2.setFont(f);
            g2.drawString(testo, x, y);
        } else if (res.bitmapFont != null && res.bitmapFont.isDisponibile()) {
            float sc = res.bitmapFont.scalaPer(altPx);
            int topY = y - (int)(res.bitmapFont.getCharH() * sc);
            res.bitmapFont.disegna(g2, testo.toUpperCase(), x, topY, sc);
        } else {
            g2.drawString(testo, x, y);
        }
    }

    private void drawTextCentered(Graphics2D g2, String testo, int centroX, int y, int altPx) {
        if (res.fontCustom != null) {
            java.awt.Font f = res.fontCustom.deriveFont((float) altPx);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, centroX - fm.stringWidth(testo)/2, y);
        } else if (res.bitmapFont != null && res.bitmapFont.isDisponibile()) {
            float sc = res.bitmapFont.scalaPer(altPx);
            int topY = y - (int)(res.bitmapFont.getCharH() * sc);
            res.bitmapFont.disegnaCentrato(g2, testo.toUpperCase(), centroX, topY, sc);
        } else {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, centroX - fm.stringWidth(testo)/2, y);
        }
    }


    private void disegnaAmbiente(Graphics2D g2, int W, int H) {
        TileSet ts = tileSetCorrente();

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                if (TileSet.isMuro(i, j)) {
                    if (ts.imgMuro != null)
                        g2.drawImage(ts.imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(ts.coloreTemaMuro); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (ts.imgPavimento != null)
                        g2.drawImage(ts.imgPavimento, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(ts.coloreTemaFondo); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        // ── Ostacoli inagibili sovrapposti alle tile ─────────────────────────────
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        if (ostacoli != null && state.statoGioco == GameState.StatoGioco.GIOCO) {
            int mondoIdx = Math.min(3, Math.max(0, state.mondoAttuale - 1));
            BufferedImage imgOst = res.imgOstacoloPerMondo[mondoIdx];
            if (imgOst == null) imgOst = res.imgOstacolo;
            for (int[] o : ostacoli) {
                int ox = o[0] * GameState.TILE_SIZE;
                int oy = o[1] * GameState.TILE_SIZE;
                if (imgOst != null) {
                    g2.drawImage(imgOst, ox, oy, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                } else {
                    g2.setColor(new Color(40, 20, 10, 200));
                    g2.fillRect(ox + 4, oy + 4, GameState.TILE_SIZE - 8, GameState.TILE_SIZE - 8);
                    g2.setColor(new Color(100, 50, 20));
                    g2.setStroke(new java.awt.BasicStroke(3));
                    g2.drawLine(ox + 8, oy + 8, ox + GameState.TILE_SIZE - 8, oy + GameState.TILE_SIZE - 8);
                    g2.drawLine(ox + GameState.TILE_SIZE - 8, oy + 8, ox + 8, oy + GameState.TILE_SIZE - 8);
                    g2.setStroke(new java.awt.BasicStroke(1));
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

        }
    }

    private void disegnaOggetti(Graphics2D g2, int W, int H) {
        for (Cura       c  : roomMgr.getCureCorrenti())         c.draw(g2);
        for (Moneta     m  : roomMgr.getMoneteCorrenti())       m.draw(g2);
        for (Shopkeeper sk : roomMgr.getShopkeepersCorrenti())  sk.draw(g2);
        for (ShopItem   si : roomMgr.getShopItemsCorrenti())    si.draw(g2);
    }

    private void disegnaNemici(Graphics2D g2, int W, int H) {
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if (n instanceof Boss) {
                n.draw(g2, res.getBossSprite(state.mondoAttuale));
                // Boss: solo barra HUD grande, NON quella piccola
            } else if (n instanceof NemicoForte) {
                n.draw(g2, res.getNemicoForteSprite(state.mondoAttuale));
                n.disegnaBarraVita(g2);
            } else {
                n.draw(g2, res.getNemicoSprite(state.mondoAttuale));
                n.disegnaBarraVita(g2);
            }
        }
    }

    private void disegnaGiocatore(Graphics2D g2, int W, int H) {
        // Lampeggio durante invulnerabilità
        if (state.invulnerabile && state.timerInvulnerabilita % 10 >= 5) return;

        int px = (int) state.x, py = (int) state.y;
        int ps = GameState.PG_SIZE;

        // ── Effetto burn: alone arancione pulsante ────────────────────────────
        if (state.burnAttivo) {
            float burnPulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() * 0.012f);
            int   glowAlpha = (int)(120 * burnPulse);
            int   glowSize  = (int)(ps * 1.35f);
            int   glowOff   = (glowSize - ps) / 2;
            // alone esterno
            g2.setColor(new Color(255, 80, 0, glowAlpha / 2));
            g2.fillOval(px - glowOff - 4, py - glowOff - 4, glowSize + 8, glowSize + 8);
            // alone interno più intenso
            g2.setColor(new Color(255, 140, 0, glowAlpha));
            g2.fillOval(px - glowOff, py - glowOff, glowSize, glowSize);
        }

        // ── Sprite giocatore ──────────────────────────────────────────────────
        BufferedImage imgPG = ui.listaPersonaggi
                .get(state.indicePersonaggioSelezionato).imgGioco;
        if (imgPG != null) {
            g2.drawImage(imgPG, px, py, ps, ps, null);
        } else {
            g2.setColor(Color.CYAN);
            g2.fillOval(px, py, ps, ps);
        }

        // ── Overlay burn PNG sopra il personaggio ─────────────────────────────
        if (state.burnAttivo && res.imgBurn != null) {
            float alpha = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.015f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(res.imgBurn, px, py, ps, ps, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else if (state.burnAttivo) {
            // Fallback: tinta arancione semitrasparente
            g2.setColor(new Color(255, 80, 0, 110));
            g2.fillRect(px, py, ps, ps);
        }
    }

    // ── Dialogo Narrazione JRPG (condiviso boss + shopkeeper) ────────────────

    // ── Scelta attacco shopkeeper — stesso stile del dialogo narrazione ───────

    private void disegnaSceltaShopkeeper(Graphics2D g2, int W, int H) {
        if (!state.dialogoShopkeeper.isVisibile()) return;

        // ── Dim overlay ───────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, W, H);

        // ── Layout box (identico al dialogo narrazione) ───────────────────────
        int boxH  = (int)(H * 0.30f);
        int boxY  = H - boxH - (int)(H * 0.03f);
        int boxX  = (int)(W * 0.04f);
        int boxW  = W - boxX * 2;
        int pad   = (int)(W * 0.018f);
        int sprSz = boxH - pad * 2;
        int sprX  = boxX + pad;
        int sprY  = boxY + pad;
        int txtX  = sprX + sprSz + pad;
        int txtW  = boxW - sprSz - pad * 3;

        // Colore bordo rosso-arancio (come nemico/boss)
        Color borderColor = new Color(220, 80, 60);
        Color bgColor     = new Color(22, 8, 8, 248);

        // Ombra
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(boxX + 5, boxY + 5, boxW, boxH, 18, 18);

        // Sfondo
        g2.setColor(bgColor);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        // Bordo esterno
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(borderColor);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        // Bordo interno
        g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                borderColor.getBlue(), 70));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 15, 15);
        g2.setStroke(new BasicStroke(1f));

        // ── Sprite shopkeeper ─────────────────────────────────────────────────
        BufferedImage sprSk = res.imgShopkeeper;
        if (sprSk != null) {
            g2.setColor(new Color(20, 15, 35));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(sprSk, sprX, sprY, sprSz, sprSz, null);
        }

        // ── Nome (font custom) ────────────────────────────────────────────────
        int nomeFs = Math.max(13, (int)(H * 0.028f));
        g2.setFont(res.fontCustom != null
                ? res.fontCustom.deriveFont(Font.BOLD, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));
        FontMetrics fmN = g2.getFontMetrics();
        String nomeShop = "NEGOZIANTE";
        int nomeBgW = fmN.stringWidth(nomeShop) + 18;
        Color nomeColor = new Color(255, 120, 80);
        g2.setColor(new Color(nomeColor.getRed() / 4, nomeColor.getGreen() / 4,
                nomeColor.getBlue() / 4, 210));
        g2.fillRoundRect(txtX - 2, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);
        g2.setColor(nomeColor);
        g2.drawString(nomeShop, txtX + 6, sprY + fmN.getAscent());

        // ── Testo domanda ─────────────────────────────────────────────────────
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();
        int lineY = sprY + fmN.getHeight() + (int)(H * 0.013f);

        // Riga 1: domanda
        String domanda = "Vuoi attaccare il negoziante?";
        disegnaRigaDialogo(g2, domanda, txtX, lineY);
        lineY += fmT.getHeight() + 3;

        // Riga 2: conseguenza in grigio
        int avvFs = Math.max(9, (int)(H * 0.017f));
        g2.setFont(new Font("Consolas", Font.PLAIN, avvFs));
        g2.setColor(new Color(180, 130, 120));
        g2.drawString("Otterrai 20 monete, ma perderai lo shop.", txtX, lineY);

        // ── Bottoni SI / NO nello stile del box ──────────────────────────────
        int btnW   = (int)(txtW * 0.28f);
        int btnH   = Math.max(30, (int)(H * 0.055f));
        int btnGap = (int)(txtW * 0.06f);
        int btnY   = boxY + boxH - btnH - pad;
        int btnX0  = txtX;

        int scelta = state.dialogoShopkeeper.getScelta(); // 0=No, 1=Si

        String[] etichette = { "NO", "SI" };
        Color[] coloriSfondo = {
                new Color(20, 50, 20),
                new Color(60, 15, 15)
        };
        Color[] coloriSel = {
                new Color(40, 120, 40),
                new Color(150, 30, 30)
        };
        Color[] coloriBordo = {
                new Color(60, 180, 60),
                new Color(220, 60, 60)
        };

        for (int i = 0; i < 2; i++) {
            boolean sel = (i == scelta);
            int bx = btnX0 + i * (btnW + btnGap);

            // Ombra
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(bx + 3, btnY + 3, btnW, btnH, 10, 10);

            // Sfondo
            g2.setColor(sel ? coloriSel[i] : coloriSfondo[i]);
            g2.fillRoundRect(bx, btnY, btnW, btnH, 10, 10);

            // Bordo
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
            g2.setColor(sel ? coloriBordo[i] : new Color(80, 80, 80));
            g2.drawRoundRect(bx, btnY, btnW, btnH, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Glow se selezionato
            if (sel) {
                g2.setColor(new Color(coloriBordo[i].getRed(),
                        coloriBordo[i].getGreen(), coloriBordo[i].getBlue(), 40));
                g2.fillRoundRect(bx - 3, btnY - 3, btnW + 6, btnH + 6, 13, 13);
            }

            // Testo bottone (font custom)
            int btnFs = Math.max(12, (int)(H * 0.025f));
            g2.setFont(res.fontCustom != null
                    ? res.fontCustom.deriveFont(Font.BOLD, (float) btnFs)
                    : new Font("Consolas", Font.BOLD, btnFs));
            FontMetrics fmB = g2.getFontMetrics();
            g2.setColor(sel ? Color.WHITE : new Color(160, 160, 160));
            int lx = bx + (btnW - fmB.stringWidth(etichette[i])) / 2;
            int ly = btnY + (btnH + fmB.getAscent() - fmB.getDescent()) / 2;
            // Ombra testo
            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString(etichette[i], lx + 1, ly + 1);
            g2.setColor(sel ? Color.WHITE : new Color(160, 160, 160));
            g2.drawString(etichette[i], lx, ly);
        }

        // ── Hint tasti ────────────────────────────────────────────────────────
        int hintFs = Math.max(9, (int)(H * 0.015f));
        g2.setFont(new Font("Consolas", Font.ITALIC, hintFs));
        g2.setColor(new Color(120, 100, 100));
        String hint = "A/D per scegliere   INVIO per confermare   ESC per annullare";
        g2.drawString(hint, boxX + pad, boxY + boxH - 6);
    }

    private void disegnaDialogoNarrazione(Graphics2D g2, int W, int H) {
        DialogoNarrazione.Pagina pag = state.dialogoNarrazione.getPagina();
        if (pag == null) return;

        // ── Layout ────────────────────────────────────────────────────────────
        int boxH  = (int)(H * 0.30f);
        int boxY  = H - boxH - (int)(H * 0.03f);
        int boxX  = (int)(W * 0.04f);
        int boxW  = W - boxX * 2;
        int pad   = (int)(W * 0.018f);
        int sprSz = boxH - pad * 2;

        boolean isLeft = pag.isLeft;
        int sprX = isLeft ? boxX + pad : boxX + boxW - pad - sprSz;
        int sprY = boxY + pad;
        int txtX = isLeft ? sprX + sprSz + pad : boxX + pad;
        int txtW = boxW - sprSz - pad * 3;

        // ── Dim sfondo ────────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, W, H);

        // ── Box principale ────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(boxX + 5, boxY + 5, boxW, boxH, 18, 18);

        // Colore bordo: viola per protagonista, rosso-arancio per boss/nemico
        Color borderColor = isLeft ? new Color(160, 100, 255) : new Color(220, 80, 60);
        Color bgColor     = isLeft ? new Color(12, 8, 22, 248) : new Color(22, 8, 8, 248);
        g2.setColor(bgColor);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(borderColor);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2.setStroke(new BasicStroke(1f));
        // Bordo interno
        g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                borderColor.getBlue(), 70));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 15, 15);

        // ── Sprite ────────────────────────────────────────────────────────────
        if (pag.sprite != null) {
            g2.setColor(new Color(20, 15, 35));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(pag.sprite, sprX, sprY, sprSz, sprSz, null);
        }

        // ── Nome (font custom) ────────────────────────────────────────────────
        int nomeFs = Math.max(13, (int)(H * 0.028f));
        g2.setFont(res.fontCustom != null
                ? res.fontCustom.deriveFont(Font.BOLD, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));
        FontMetrics fmN = g2.getFontMetrics();
        int nomeBgW = fmN.stringWidth(pag.nome) + 18;
        // Sfondo nome
        Color nomeColor = isLeft ? new Color(255, 215, 0) : new Color(255, 120, 80);
        g2.setColor(new Color(nomeColor.getRed() / 4, nomeColor.getGreen() / 4,
                nomeColor.getBlue() / 4, 210));
        int nomeBgX = isLeft ? txtX - 2 : txtX - 2;
        g2.fillRoundRect(nomeBgX, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);
        g2.setColor(nomeColor);
        g2.drawString(pag.nome, txtX + 6, sprY + fmN.getAscent());

        // ── Testo con wrap ────────────────────────────────────────────────────
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();
        int lineY  = sprY + fmN.getHeight() + (int)(H * 0.013f);
        int lineH  = fmT.getHeight() + 3;

        String[] parole = pag.testo.split(" ");
        StringBuilder riga = new StringBuilder();
        for (String p : parole) {
            String prova = riga.isEmpty() ? p : riga + " " + p;
            if (fmT.stringWidth(prova) > txtW) {
                disegnaRigaDialogo(g2, riga.toString(), txtX, lineY);
                lineY += lineH;
                riga = new StringBuilder(p);
            } else {
                riga = new StringBuilder(prova);
            }
        }
        if (!riga.isEmpty()) disegnaRigaDialogo(g2, riga.toString(), txtX, lineY);

        // ── Indicatore pagina ─────────────────────────────────────────────────
        int tot = state.dialogoNarrazione.getTotale();
        int cur = state.dialogoNarrazione.getIndice() + 1;
        // Pallini paginazione
        int dotR = Math.max(4, (int)(H * 0.007f));
        int dotY = boxY + boxH - dotR * 2 - (int)(H * 0.012f);
        int dotSpacing = dotR * 3;
        int dotsW = tot * dotSpacing - dotR;
        int dotStartX = boxX + (boxW - dotsW) / 2;
        for (int i = 0; i < tot; i++) {
            g2.setColor(i + 1 == cur ? borderColor : new Color(80, 75, 100));
            g2.fillOval(dotStartX + i * dotSpacing, dotY, dotR * 2, dotR * 2);
        }

        // ▼ lampeggiante
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (blink) {
            int hFs = Math.max(9, (int)(H * 0.016f));
            g2.setFont(new Font("Arial", Font.ITALIC, hFs));
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 200));
            String hint = cur < tot ? "[ INVIO per continuare ]" : "[ INVIO per iniziare ]";
            FontMetrics fmH = g2.getFontMetrics();
            g2.drawString(hint, boxX + boxW - fmH.stringWidth(hint) - pad,
                    boxY + boxH - pad / 2);
        }
    }

    private void disegnaRigaDialogo(Graphics2D g2, String testo, int x, int y) {
        // Glow leggero
        g2.setColor(new Color(200, 170, 255, 45));
        g2.drawString(testo, x + 1, y + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(testo, x, y);
    }

    private void disegnaDialogoCasa(Graphics2D g2, int W, int H) {
        // ── Layout ────────────────────────────────────────────────────────────
        int boxH   = (int)(H * 0.28);
        int boxY   = H - boxH - (int)(H * 0.03);
        int boxX   = (int)(W * 0.04);
        int boxW   = W - boxX * 2;
        int pad    = (int)(W * 0.018);

        // Sprite size
        int sprW   = boxH - pad * 2;
        int sprH   = sprW;
        int sprX   = boxX + pad;
        int sprY   = boxY + pad;

        // Testo area
        int txtX   = sprX + sprW + pad;
        int txtW   = boxW - sprW - pad * 3;

        // ── Sfondo scuro semitrasparente su tutta la scena ────────────────────
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, W, H);

        // ── Box dialogo ───────────────────────────────────────────────────────
        // Ombra
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(boxX + 4, boxY + 4, boxW, boxH, 16, 16);
        // Sfondo
        g2.setColor(new Color(12, 8, 20, 245));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        // Bordo luminoso
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(180, 120, 255));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        // Linea interna di accento
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(100, 60, 160, 120));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 13, 13);

        // ── Sprite personaggio ────────────────────────────────────────────────
        java.awt.image.BufferedImage sprPg =
                res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
        if (sprPg != null) {
            // Riquadro sprite con bordo
            g2.setColor(new Color(30, 20, 50));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprW + 6, sprH + 6, 8, 8);
            g2.setColor(new Color(180, 120, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprW + 6, sprH + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(sprPg, sprX, sprY, sprW, sprH, null);
        }

        // ── Nome personaggio (font custom) ────────────────────────────────────
        DatiPersonaggio pg = ui.listaPersonaggi.get(
                Math.min(state.indicePersonaggioSelezionato, ui.listaPersonaggi.size() - 1));
        String nome = pg.nome.toUpperCase();

        int nomeFs  = Math.max(13, (int)(H * 0.028));
        g2.setFont(res.fontCustom != null
                ? res.fontCustom.deriveFont(Font.BOLD, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));

        FontMetrics fmN = g2.getFontMetrics();
        int nomeY = sprY + fmN.getAscent();

        // Piccolo sfondo dietro il nome
        int nomeBgW = fmN.stringWidth(nome) + 16;
        g2.setColor(new Color(120, 60, 200, 200));
        g2.fillRoundRect(txtX - 2, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);

        g2.setColor(new Color(255, 220, 80));
        g2.drawString(nome, txtX + 6, nomeY + 1);

        // ── Testo dialogo (wrapping manuale) ─────────────────────────────────
        String testo = "WHAT? I'VE PLAYED TOO MUCH, IM GONNA BE LATE!!!";
        int testoFs  = Math.max(11, (int)(H * 0.022));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();

        // Wrap su più righe
        int lineY    = nomeY + fmN.getHeight() + (int)(H * 0.012);
        int lineH    = fmT.getHeight() + 3;
        String[] parole = testo.split(" ");
        StringBuilder riga = new StringBuilder();
        for (String parola : parole) {
            String prova = riga.isEmpty() ? parola : riga + " " + parola;
            if (fmT.stringWidth(prova) > txtW) {
                // disegna riga corrente con leggero effetto glow
                g2.setColor(new Color(200, 160, 255, 60));
                g2.drawString(riga.toString(), txtX + 1, lineY + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(riga.toString(), txtX, lineY);
                lineY += lineH;
                riga = new StringBuilder(parola);
            } else {
                riga = new StringBuilder(prova);
            }
        }
        if (!riga.isEmpty()) {
            g2.setColor(new Color(200, 160, 255, 60));
            g2.drawString(riga.toString(), txtX + 1, lineY + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(riga.toString(), txtX, lineY);
        }

        // ── Indicatore "premi INVIO" lampeggiante ────────────────────────────
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (blink) {
            int hintFs = Math.max(9, (int)(H * 0.016));
            g2.setFont(new Font("Arial", Font.ITALIC, hintFs));
            g2.setColor(new Color(160, 120, 220));
            String hint = "[ INVIO per continuare ]";
            g2.drawString(hint,
                    boxX + boxW - g2.getFontMetrics().stringWidth(hint) - pad,
                    boxY + boxH - pad / 2);
        }
    }

    private void disegnaBannerCasa(Graphics2D g2, int W, int H) {
        // Calcola posizione banda superiore
        double gs = gameScale(W, H);
        int gH = (int)(GameState.ALTEZZA_GIOCO * gs);
        int goy = (H - gH) / 2;
        int banH = Math.max(28, goy > 10 ? goy - 4 : 28);
        int banY = goy > banH ? (goy - banH) / 2 : 2;
        int fs = Math.max(11, banH - 8);

        g2.setColor(new Color(30, 20, 50, 200));
        g2.fillRoundRect(W/2 - (int)(W*0.15), banY, (int)(W*0.30), banH, 8, 8);

        g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)fs)
                : new Font("Consolas", Font.BOLD, fs));
        g2.setColor(new Color(255, 200, 80));
        String txt = "CASA";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, W/2 - fm.stringWidth(txt)/2, banY + banH/2 + fm.getAscent()/2 - 2);
    }

    private void disegnaHUD(Graphics2D g2, int W, int H) {
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);
        int    gW  = (int)(GameState.LARGHEZZA_GIOCO * gs);
        int    gH  = (int)(GameState.ALTEZZA_GIOCO   * gs);

        int bandaB = H - goy - gH;
        int bandaR = W - gox - gW;
        int bandaL = gox;

        boolean hudBasso    = bandaB >= 36;
        boolean hudDestra   = !hudBasso && bandaR >= 110;
        boolean hudSinistra = !hudBasso && !hudDestra && bandaL >= 110;

        int fs  = Math.max(11, (int)(gs * 14));
        int ico = Math.max(14, (int)(gs * 20));

        if (hudBasso) {
            int hx = gox, hy = goy + gH;
            int hw = gW,  hh = bandaB;
            g2.setColor(new Color(10, 10, 25, 230));
            g2.fillRect(hx, hy, hw, hh);
            g2.setColor(new Color(60, 60, 100));
            g2.drawLine(hx, hy, hx + hw, hy);

            int cy = hy + hh / 2;
            int cx = hx + 10;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, cx + i * (ico + 3), cy - ico/2, ico, ico, null);
            cx += state.vite * (ico + 3) + 12;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, cx, cy - ico/2, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, cx + ico + 4, cy + fs/3);
            cx += ico + 4 + g2.getFontMetrics().stringWidth("" + state.monete) + 18;
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, fs));
            g2.setColor(ts.coloreTemaUI);
            String mondoStr = state.modalitaScelta == GameState.Modalita.STORIA
                    ? "M" + state.mondoAttuale + ": " + ts.nomeMondo
                    : "Inf M" + state.mondoAttuale + ": " + ts.nomeMondo;
            drawText(g2, mondoStr, cx, cy + fs/3, fs);
            cx += g2.getFontMetrics().stringWidth(mondoStr) + 18;
            g2.setColor(Color.WHITE);
            g2.drawString("Stanza " + state.stanzaNelMondo + "/" + GameState.STANZA_BOSS, cx, cy + fs/3);

            // Indicatore burn
            if (state.burnAttivo) {
                boolean blinkBurn = (System.currentTimeMillis() / 300) % 2 == 0;
                if (blinkBurn) {
                    cx += g2.getFontMetrics().stringWidth("Stanza " + state.stanzaNelMondo + "/" + GameState.STANZA_BOSS) + 14;
                    if (res.imgBurn != null)
                        g2.drawImage(res.imgBurn, cx, cy - ico/2, ico, ico, null);
                    else {
                        g2.setColor(new Color(255, 80, 0));
                        g2.fillOval(cx, cy - ico/2, ico, ico);
                    }
                    g2.setFont(new Font("Consolas", Font.BOLD, fs));
                    g2.setColor(new Color(255, 140, 0));
                    g2.drawString("BURN!", cx + ico + 4, cy + fs/3);
                }
            }

        } else if (hudDestra || hudSinistra) {
            int px = hudDestra ? gox + gW : 0;
            int pw = hudDestra ? bandaR   : bandaL;
            g2.setColor(new Color(10, 10, 25, 230));
            g2.fillRect(px, 0, pw, H);
            g2.setColor(new Color(60, 60, 100));
            int lineX = hudDestra ? px : px + pw - 1;
            g2.drawLine(lineX, 0, lineX, H);

            int lx = px + pw / 2;
            int ly = 20;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, lx - ico/2, ly + i * (ico + 3), ico, ico, null);
            ly += state.vite * (ico + 3) + 14;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, lx - ico/2, ly, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, lx - ico/2, ly + ico + fs + 2);
            ly += ico + fs + 16;
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, Math.max(9, fs - 2)));
            g2.setColor(ts.coloreTemaUI);
            String mn = "M" + state.mondoAttuale;
            g2.drawString(mn, lx - g2.getFontMetrics().stringWidth(mn)/2, ly);
            ly += fs + 6;
            g2.setColor(Color.WHITE);
            String st = state.stanzaNelMondo + "/" + GameState.STANZA_BOSS;
            g2.drawString(st, lx - g2.getFontMetrics().stringWidth(st)/2, ly);

        } else {
            // Overlay compatto angolo top-left del gioco
            int pad = 6;
            g2.setColor(new Color(10, 10, 25, 190));
            g2.fillRoundRect(gox + 4, goy + 4, (int)(gW * 0.38), ico + fs + pad * 3, 6, 6);
            int cx = gox + 10;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, cx + i * (ico + 2), goy + pad, ico, ico, null);
            cx += state.vite * (ico + 2) + 8;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, cx, goy + pad, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, cx + ico + 3, goy + pad + ico - 2);
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, fs));
            g2.setColor(ts.coloreTemaUI);
            g2.drawString("M" + state.mondoAttuale + " St." + state.stanzaNelMondo, gox + 10, goy + pad + ico + fs + 4);
        }

        disegnaUIBoss(g2, W, H, gox, goy, gW, gH);
    }
    private void disegnaUIBoss(Graphics2D g2, int W, int H, int gox, int goy, int gW, int gH) {
        if (state.stanzaNelMondo != GameState.STANZA_BOSS
                || !state.bossSpawnato || state.bossSconfitto) return;

        Boss bossCorrente = null;
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if (n instanceof Boss b) { bossCorrente = b; break; }
        }
        if (bossCorrente == null) return;

        String[] nomiUI = {
                "MANNIE", "PRESAGIO", "RE FORNO", "YABBADUHLON"
        };
        String nomeBoss = nomiUI[((state.mondoAttuale - 1) % 4)];

        // Barra boss: larga 50% della stanza, centrata, nella banda superiore
        int barW    = (int)(gW * 0.5);
        int barH    = Math.max(18, (int)(goy * 0.35));  // usa la banda superiore
        int fontSize = Math.max(12, (int)(goy * 0.30));
        int hpFs    = Math.max(10, (int)(goy * 0.22));
        int padX    = Math.max(12, barW / 20);
        int padY    = Math.max(6,  (int)(goy * 0.08));
        int panW    = barW + padX * 2;
        int panH    = padY + fontSize + padY / 2 + barH + padY / 2 + hpFs + padY;
        int uiX     = gox + gW / 2 - barW / 2;
        // Centra il pannello verticalmente nella banda superiore
        int panY    = (goy - panH) / 2;
        if (panY < 2) panY = 2;

        // coordinate interne
        int nomeY = panY + padY + fontSize;
        int barY  = nomeY + padY / 2;
        int hpY   = barY + barH + hpFs;

        // Pannello sfondo
        g2.setColor(new Color(15, 5, 5, 220));
        g2.fillRoundRect(uiX - padX, panY, panW, panH, 12, 12);
        g2.setColor(new Color(100, 20, 20));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(uiX - padX, panY, panW, panH, 12, 12);
        g2.setStroke(new BasicStroke(1f));

        // Nome boss
        g2.setFont(res.fontCustom != null ? res.fontCustom.deriveFont(Font.BOLD, (float)fontSize) : new Font("Consolas", Font.BOLD, fontSize));
        g2.setColor(new Color(255, 200, 80));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(nomeBoss, uiX + (barW - fm.stringWidth(nomeBoss)) / 2, nomeY);

        // Barra vita
        g2.setColor(new Color(40, 10, 10));
        g2.fillRect(uiX, barY, barW, barH);
        float perc = (float) bossCorrente.getVita() / bossCorrente.getVitaMax();
        Color colVita = perc > 0.5f ? new Color(200, 40, 40)
                : perc > 0.25f ? new Color(220, 120, 0)
                : new Color(255, 40, 40);
        g2.setColor(colVita);
        g2.fillRect(uiX, barY, (int)(barW * perc), barH);
        g2.setColor(new Color(80, 20, 20));
        g2.drawRect(uiX, barY, barW, barH);

        // HP text
        g2.setFont(new Font("Consolas", Font.BOLD, hpFs));
        g2.setColor(Color.WHITE);
        String hp = bossCorrente.getVita() + " / " + bossCorrente.getVitaMax();
        g2.drawString(hp, uiX + (barW - g2.getFontMetrics().stringWidth(hp)) / 2, hpY);

        // Timer — in alto a destra
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        g2.setFont(new Font("Consolas", Font.BOLD, Math.max(12, H / 28)));
        g2.setColor(state.tempoRimanenteBoss < 600
                ? (blink ? Color.RED : new Color(200, 80, 80))
                : new Color(200, 200, 200));
        g2.drawString("⏱ " + (state.tempoRimanenteBoss / 60) + "s",
                W - (int)(W * 0.1), goy + (int)(H * 0.04));
    }
    // ── Utilità UI ────────────────────────────────────────────────────────────

    /** Sfondo scuro pieno con leggero alone centrale */
    /** Avvia la musica corretta in base allo stato del gioco. */
    private void aggiornaMusica() {
        switch (state.statoGioco) {
            case MENU, IMPOSTAZIONI, CONTROLLI,
                 SELEZIONE_PERSONAGGIO, SELEZIONE_MODALITA ->
                    state.audio.suonaMusica(AudioManager.MENU);

            case GIOCO -> {
                if (state.stanzaNelMondo == GameState.STANZA_BOSS
                        && state.bossSpawnato && !state.bossSconfitto) {
                    state.audio.suonaMusica(AudioManager.BOSS);
                } else {
                    state.audio.suonaMusicaMondo(state.mondoAttuale);
                }
            }

            case GAME_OVER ->
                    state.audio.suonaMusica(AudioManager.SCONFITTA);

            case VITTORIA_STORIA ->
                    state.audio.suonaMusica(AudioManager.VITTORIA);

            case TETRIS ->
                    state.audio.suonaMusica(AudioManager.TETRIS);

            case PAUSA -> { /* non cambia la musica durante la pausa */ }
        }
    }

    /** Restituisce il TileSet corretto: Casa (mondo 0) nella stanza 1 del mondo 1. */
    private TileSet tileSetCorrente() {
        // La stanza Casa è: mondo 1, stanza 1, dopo il Tetris (stanzaCasaVisitata viene
        // impostato da RoomManager appena genera la stanza, prima del rendering)
        boolean isCasa = state.mondoAttuale == 1
                && state.stanzaNelMondo == 1
                && state.stanzaCasaVisitata;
        return TileSet.perMondo(isCasa ? 0 : state.mondoAttuale, res);
    }

    private void sfondoOverlay(Graphics2D g2, int W, int H, Color base) {
        g2.setColor(base);
        g2.fillRect(0, 0, W, H);
        for (int r = 250; r > 0; r -= 12) {
            int alpha = (int)(15 * (1.0 - r / 250.0));
            g2.setColor(new Color(60, 60, 140, alpha));
            g2.fillOval(W/2 - r, H/2 - r, r*2, r*2);
        }
    }
}