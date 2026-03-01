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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sfondo nero (letterbox)
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, panelWidth, panelHeight);

        // Scaling fullscreen con letterbox
        double[] params  = fullscreen.getScaleParams(panelWidth, panelHeight);
        double   scale   = params[2];
        double   offsetX = params[3];
        double   offsetY = params[4];

        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        // Dispatch per stato
        switch (state.statoGioco) {
            case MENU                  -> disegnaMenu(g2);
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
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("TimesRoman", Font.BOLD, 40));
        g2.drawString("WHAT: I'VE PLAYED TOO MUCH",
                GameState.LARGHEZZA_GIOCO / 2 - 300, GameState.ALTEZZA_GIOCO / 2 - 50);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Premi [INVIO] o [Click] per iniziare",
                GameState.LARGHEZZA_GIOCO / 2 - 160, GameState.ALTEZZA_GIOCO / 2 + 20);

        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.drawString("Premi [F11] per Fullscreen",
                GameState.LARGHEZZA_GIOCO / 2 - 100, GameState.ALTEZZA_GIOCO / 2 + 50);
    }

    // ── Selezione Personaggio ─────────────────────────────────────────────────

    private void disegnaSelezionePersonaggio(Graphics2D g2) {
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 36));
        g2.drawString("SCEGLI IL TUO MURATORE", GameState.LARGHEZZA_GIOCO / 2 - 220, 80);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.drawString("[Frecce/Mouse] per navigare, [INVIO/Click] per confermare",
                GameState.LARGHEZZA_GIOCO / 2 - 200, 110);

        for (int i = 0; i < ui.listaPersonaggi.size(); i++) {
            DatiPersonaggio pg   = ui.listaPersonaggi.get(i);
            Rectangle       rect = ui.rectsSelezionePG[i];
            boolean         sel  = (i == state.indicePersonaggioSelezionato);

            // Sfondo riquadro
            g2.setColor(sel ? new Color(255, 215, 0, 100) : new Color(255, 255, 255, 30));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(sel ? Color.YELLOW : Color.GRAY);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);

            // Icona personaggio
            int imgSize = 80;
            if (pg.imgIcona != null) {
                g2.drawImage(pg.imgIcona, rect.x + (rect.width / 2 - imgSize / 2),
                        rect.y + 20, imgSize, imgSize, null);
            }

            // Testo stats
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.drawString(pg.nome, rect.x + 10, rect.y + imgSize + 50);

            g2.setFont(new Font("Arial", Font.ITALIC, 14));
            g2.drawString(pg.descrizione, rect.x + 10, rect.y + imgSize + 75);

            if (res.imgCuore != null) {
                g2.drawImage(res.imgCuore, rect.x + 10, rect.y + 160, 20, 20, null);
            }
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(Color.RED);
            g2.drawString("" + pg.vitaMax, rect.x + 35, rect.y + 175);
            g2.setColor(Color.CYAN);
            g2.drawString("VEL: " + pg.velocitaBase, rect.x + 60, rect.y + 175);
            g2.setColor(Color.WHITE);
            g2.drawString("DMG: " + pg.dannoBase, rect.x + 60, rect.y + 195);
        }
    }

    // ── Selezione Modalità ────────────────────────────────────────────────────

    private void disegnaSelezioneModalita(Graphics2D g2) {
        g2.setColor(new Color(40, 20, 50));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 40));
        g2.drawString("SELEZIONA LA SFIDA", GameState.LARGHEZZA_GIOCO / 2 - 180, 80);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString("[Frecce/Mouse] per navigare, [INVIO/Click] per confermare, [ESC] per PG",
                GameState.LARGHEZZA_GIOCO / 2 - 270, 115);

        String[] nomi   = { "STORIA CLASSICA",    "MODALITA INFINITA" };
        String[] desc1  = { "Sconfiggi 4 Boss",   "Sopravvivi a mondi infiniti!" };
        String[] desc2  = { "per vincere!",        "Nemici sempre più forti." };
        BufferedImage[] icone = { res.imgIconaStoria, res.imgIconaInfinita };
        String[] fallback = { "📖", "∞" };

        for (int i = 0; i < 2; i++) {
            Rectangle rect = ui.rectsSelezioneModalita[i];
            boolean   sel  = (i == state.indiceModalitaSelezionata);

            g2.setColor(sel ? new Color(173, 216, 230, 100) : new Color(255, 255, 255, 20));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(sel ? Color.CYAN : Color.GRAY);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);

            int imgSize = 120;
            if (icone[i] != null) {
                g2.drawImage(icone[i], rect.x + (rect.width / 2 - imgSize / 2),
                        rect.y + 20, imgSize, imgSize, null);
            } else {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 100));
                g2.drawString(fallback[i], rect.x + (rect.width / 2 - 50), rect.y + 110);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 22));
            g2.drawString(nomi[i],  rect.x + 10, rect.y + 170);
            g2.setFont(new Font("Arial", Font.ITALIC, 15));
            g2.drawString(desc1[i], rect.x + 10, rect.y + 195);
            g2.drawString(desc2[i], rect.x + 10, rect.y + 215);
        }
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void disegnaPausa(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 50));
        g2.drawString("P A U S A",
                GameState.LARGHEZZA_GIOCO / 2 - 120, GameState.ALTEZZA_GIOCO / 2 - 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Premi [ESC] o [INVIO] per riprendere",
                GameState.LARGHEZZA_GIOCO / 2 - 160, GameState.ALTEZZA_GIOCO / 2 + 30);
        g2.drawString("Premi [Q] per uscire",
                GameState.LARGHEZZA_GIOCO / 2 - 80,  GameState.ALTEZZA_GIOCO / 2 + 60);
    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void disegnaGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.RED);
        g2.setFont(new Font("Consolas", Font.BOLD, 60));
        g2.drawString("G A M E   O V E R",
                GameState.LARGHEZZA_GIOCO / 2 - 250, GameState.ALTEZZA_GIOCO / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.drawString("Mondo " + state.mondoAttuale
                        + ", Stanza " + state.stanzaNelMondo
                        + ", Monete: " + state.monete,
                GameState.LARGHEZZA_GIOCO / 2 - 210, GameState.ALTEZZA_GIOCO / 2 + 20);

        // Stats extra per Infinita
        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            int totale = (state.mondoAttuale - 1) * GameState.STANZA_BOSS + state.stanzaNelMondo;
            g2.drawString("Stanze totali superate: " + totale,
                    GameState.LARGHEZZA_GIOCO / 2 - 150, GameState.ALTEZZA_GIOCO / 2 + 50);
        }

        disegnaPulsante(g2, ui.btnRiprova,        "RIPROVA");
        disegnaPulsante(g2, ui.btnEsci,           "ESCI");
    }

    // ── Vittoria Storia ───────────────────────────────────────────────────────

    private void disegnaVittoriaStoria(Graphics2D g2) {
        g2.setColor(new Color(20, 100, 20));
        g2.fillRect(0, 0, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Consolas", Font.BOLD, 55));
        g2.drawString("🎊  STORIA COMPLETATA!  🎊",
                GameState.LARGHEZZA_GIOCO / 2 - 370, GameState.ALTEZZA_GIOCO / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        g2.drawString("Il muratore ha superato tutti i 4 mondi e i loro Boss!",
                GameState.LARGHEZZA_GIOCO / 2 - 300, GameState.ALTEZZA_GIOCO / 2 + 20);
        g2.drawString("Monete raccolte: " + state.monete,
                GameState.LARGHEZZA_GIOCO / 2 - 120, GameState.ALTEZZA_GIOCO / 2 + 60);

        disegnaPulsante(g2, ui.btnMenuPrincipale, "MENU PRINCIPALE");
    }

    // ── Gioco ─────────────────────────────────────────────────────────────────

    private void disegnaGioco(Graphics2D g2) {
        disegnaAmbiente(g2);
        disegnaOggetti(g2);
        if (pugniAttivi != null) {
            for (Pugno p : pugniAttivi) p.draw(g2);
        }
        disegnaNemici(g2);
        disegnaGiocatore(g2);
        disegnaHUD(g2);
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

        // Porte
        int portaY  = (GameState.RIG_TOTALI / 2) * GameState.TILE_SIZE;
        int portaDX = (GameState.COL_TOTALI - 1) * GameState.TILE_SIZE;

        if (state.indiceStanzaMemoria > 0 && res.imgPorta != null) {
            g2.drawImage(res.imgPorta, 0, portaY, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
        }

        boolean bossUscita = state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.bossSpawnato && state.bossSconfitto;

        if (bossUscita) {
            g2.setColor(new Color(0, 255, 255, 150));
            g2.fillRect(portaDX, portaY, GameState.TILE_SIZE, GameState.TILE_SIZE);
        } else if (res.imgPorta != null) {
            g2.drawImage(res.imgPorta, portaDX, portaY, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
        }

        // Porta Shop (stanza 4, mondi dispari)
        if (state.stanzaNelMondo == 4 && state.mondoAttuale % 2 != 0 && res.imgShopDoor != null) {
            g2.drawImage(res.imgShopDoor, 7 * GameState.TILE_SIZE, 0, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
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

    private void disegnaPulsante(Graphics2D g2, Rectangle rect, String label) {
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        // Centra il testo
        FontMetrics fm     = g2.getFontMetrics();
        int         tW     = fm.stringWidth(label);
        int         tH     = fm.getAscent();
        int         textX  = rect.x + (rect.width  - tW) / 2;
        int         textY  = rect.y + (rect.height + tH) / 2 - 4;
        g2.drawString(label, textX, textY);
    }
}