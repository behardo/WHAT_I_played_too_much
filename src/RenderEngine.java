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
                g2.setFont(new Font("Consolas", Font.BOLD, cardFs));
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
            g2.setFont(new Font("Consolas", Font.BOLD, cardFs));
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
            g2.setFont(new Font("Consolas", Font.BOLD, nomeFs));
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
        String stats = "Mondo " + state.mondoAttuale + "  •  Stanza " + state.stanzaNelMondo
                + "  •  Monete " + state.monete;
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

    // ── Gioco ─────────────────────────────────────────────────────────────────

    private void disegnaGioco(Graphics2D g2, int W, int H) {
        // Applica scala + offset per adattare il gameplay alla finestra reale
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);

        // Sfondo: riempie tutta la finestra col colore muro del tema
        TileSet tsBg = TileSet.perMondo(state.mondoAttuale, res);
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
    }

    // ── Stanza Shop ───────────────────────────────────────────────────────────

    private void disegnaStanzaShop(Graphics2D g2, int W, int H) {
        TileSet ts = TileSet.perMondo(state.mondoAttuale, res);

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

        // Targa "SHOP" in cima
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Consolas", Font.BOLD, 28));
        g2.drawString("✦  NEGOZIO  ✦", W / 2 - 110, GameState.TILE_SIZE - 10);

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
        TileSet ts = TileSet.perMondo(state.mondoAttuale, res);

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
            for (int[] o : ostacoli) {
                int ox = o[0] * GameState.TILE_SIZE;
                int oy = o[1] * GameState.TILE_SIZE;
                if (res.imgOstacolo != null) {
                    g2.drawImage(res.imgOstacolo, ox, oy, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                } else {
                    // Fallback: rettangolo scuro con X
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
            TileSet ts = TileSet.perMondo(state.mondoAttuale, res);
            g2.setFont(new Font("Consolas", Font.BOLD, fs));
            g2.setColor(ts.coloreTemaUI);
            String mondoStr = state.modalitaScelta == GameState.Modalita.STORIA
                    ? "M" + state.mondoAttuale + ": " + ts.nomeMondo
                    : "Inf M" + state.mondoAttuale + ": " + ts.nomeMondo;
            drawText(g2, mondoStr, cx, cy + fs/3, fs);
            cx += g2.getFontMetrics().stringWidth(mondoStr) + 18;
            g2.setColor(Color.WHITE);
            g2.drawString("Stanza " + state.stanzaNelMondo + "/" + GameState.STANZA_BOSS, cx, cy + fs/3);

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
            TileSet ts = TileSet.perMondo(state.mondoAttuale, res);
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
            TileSet ts = TileSet.perMondo(state.mondoAttuale, res);
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
                "⚒ BRUTALE", "🌑 OMBRA", "🔥 CARICA", "💀 FINALE"
        };
        String nomeBoss = nomiUI[((state.mondoAttuale - 1) % 4)];

        // Posiziona la barra boss nella banda superiore (sopra l'area di gioco)
        int bandaT  = goy;
        int barW    = (int)(gW * 0.6);
        int barH    = Math.max(16, (int)(gH * 0.05));
        int fontSize = Math.max(11, H / 35);
        int hpFs    = Math.max(10, H / 40);
        int pad     = Math.max(8, barH / 2);          // padding interno proporzionale
        int panH    = fontSize + pad + barH + hpFs + pad * 2;
        int uiX     = gox + gW / 2 - barW / 2;

        // Y del pannello: centrato nella banda superiore se c'è spazio, altrimenti a 4px dal bordo
        int panY = bandaT >= panH + 8
                ? goy - panH - (bandaT - panH) / 2
                : goy + 4;

        // coordinate interne
        int nomeY = panY + pad + fontSize;
        int barY  = nomeY + pad / 2;
        int hpY   = barY + barH + hpFs;

        // Pannello sfondo
        g2.setColor(new Color(15, 5, 5, 220));
        g2.fillRoundRect(uiX - pad, panY, barW + pad * 2, panH, 12, 12);
        g2.setColor(new Color(100, 20, 20));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(uiX - pad, panY, barW + pad * 2, panH, 12, 12);
        g2.setStroke(new BasicStroke(1f));

        // Nome boss
        g2.setFont(new Font("Consolas", Font.BOLD, fontSize));
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