import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;

/**
 * InputHandler.java
 * Gestisce tutti gli input da tastiera e da mouse.
 * Espone un KeyAdapter e un MouseAdapter (con MouseMotionListener per hover).
 */
public class InputHandler {

    private final GameState          state;
    private final UIManager          ui;
    private final RoomManager        roomMgr;
    private final FullscreenManager  fullscreenMgr;

    public InputHandler(GameState state, UIManager ui, RoomManager roomMgr,
                        FullscreenManager fullscreenMgr) {
        this.state         = state;
        this.ui            = ui;
        this.roomMgr       = roomMgr;
        this.fullscreenMgr = fullscreenMgr;
    }

    // ── KeyAdapter ────────────────────────────────────────────────────────────

    public KeyAdapter getKeyAdapter() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_F11) { fullscreenMgr.toggle(); return; }

                switch (state.statoGioco) {
                    case MENU                  -> gestisciMenu(k);
                    case IMPOSTAZIONI          -> gestisciImpostazioni(k);
                    case CONTROLLI             -> { if (k == KeyEvent.VK_ESCAPE) state.statoGioco = GameState.StatoGioco.MENU; }
                    case SELEZIONE_PERSONAGGIO -> gestisciSelezionePG(k);
                    case SELEZIONE_MODALITA    -> gestisciSelezioneModalita(k);
                    case PAUSA                 -> gestisciPausa(k);
                    case GIOCO                 -> gestisciGioco(k, true);
                    case VITTORIA_STORIA,
                         GAME_OVER             -> gestisciFinale(k);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (state.statoGioco == GameState.StatoGioco.GIOCO) {
                    toggleMovimento(e.getKeyCode(), false);
                    toggleSparo(e.getKeyCode(), false);
                }
            }
        };
    }

    // ── MouseAdapter (click + movimento per hover) ────────────────────────────

    public MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = fullscreenMgr.scalaCoordinate(e.getX(), e.getY());
                gestisciClick(p);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = fullscreenMgr.scalaCoordinate(e.getX(), e.getY());
                state.mouseX = p.x;
                state.mouseY = p.y;
                ui.aggiornaMouse(p.x, p.y);
                sincronizzaIndicePausa(p);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = fullscreenMgr.scalaCoordinate(e.getX(), e.getY());
                state.mouseX = p.x;
                state.mouseY = p.y;
                ui.aggiornaMouse(p.x, p.y);
                sincronizzaIndicePausa(p);
            }
        };
    }

    // ── Gestori tastiera ──────────────────────────────────────────────────────

    private void sincronizzaIndicePausa(Point p) {
        if (state.statoGioco != GameState.StatoGioco.PAUSA) return;
        MenuButton[] btns = { ui.btnRiprendi, ui.btnImpostazioniPausa,
                ui.btnMenuPrincipalePausa, ui.btnEsciPausa };
        for (int i = 0; i < btns.length; i++)
            if (btns[i].contains(p)) { state.indiceBtnPausa = i; return; }
    }

    private void gestisciMenu(int k) {
        if (k == KeyEvent.VK_ENTER) state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
    }

    private void gestisciImpostazioni(int k) {
        if (k == KeyEvent.VK_ESCAPE) tornaDaImpostazioni();
        if (k == KeyEvent.VK_LEFT)  state.impostazioni.cambiaVolumeMusica(-10);
        if (k == KeyEvent.VK_RIGHT) state.impostazioni.cambiaVolumeMusica(+10);
    }

    private void gestisciSelezionePG(int k) {
        // Controlla combo B×5
        boolean isB = (k == KeyEvent.VK_B);
        boolean comboCompletata = state.sistemaPersonaggi.registraPressione(isB);
        if (comboCompletata) System.out.println("[Segreto] G.O.D. sbloccato!");

        // Mostra sempre 4 base, segreto solo se attivo
        int disponibili = state.sistemaPersonaggi.isSegretoAttivo() ? 5 : 4;

        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
            // Vai a sinistra saltando i bloccati
            int next = state.indicePersonaggioSelezionato;
            for (int t = 0; t < disponibili; t++) {
                next = (next - 1 + disponibili) % disponibili;
                if (state.sistemaPersonaggi.isSbloccato(next)) break;
            }
            state.indicePersonaggioSelezionato = next;
        } else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            int next = state.indicePersonaggioSelezionato;
            for (int t = 0; t < disponibili; t++) {
                next = (next + 1) % disponibili;
                if (state.sistemaPersonaggi.isSbloccato(next)) break;
            }
            state.indicePersonaggioSelezionato = next;
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            if (state.sistemaPersonaggi.isSbloccato(state.indicePersonaggioSelezionato))
                state.statoGioco = GameState.StatoGioco.SELEZIONE_MODALITA;
        } else if (k == KeyEvent.VK_ESCAPE) {
            state.statoGioco = GameState.StatoGioco.MENU;
        }
    }

    private void gestisciSelezioneModalita(int k) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
            state.indiceModalitaSelezionata = (state.indiceModalitaSelezionata == 0) ? 1 : 0;
        else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) confermaSelezioneModalita();
        else if (k == KeyEvent.VK_ESCAPE) state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
    }

    private void gestisciPausa(int k) {
        // Navigazione con frecce
        if (k == KeyEvent.VK_UP   || k == KeyEvent.VK_W) {
            state.indiceBtnPausa = (state.indiceBtnPausa - 1 + 4) % 4; return;
        }
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            state.indiceBtnPausa = (state.indiceBtnPausa + 1) % 4; return;
        }
        if (k == KeyEvent.VK_ESCAPE) {
            state.statoGioco = GameState.StatoGioco.GIOCO; return;
        }
        if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            switch (state.indiceBtnPausa) {
                case 0 -> state.statoGioco = GameState.StatoGioco.GIOCO;
                case 1 -> { state.statoPrecedente = GameState.StatoGioco.PAUSA;
                    state.statoGioco = GameState.StatoGioco.IMPOSTAZIONI; }
                case 2 -> { state.tornaAlMenu(); roomMgr.resetCompleto(); }
                case 3 -> System.exit(0);
            }
        }
        if (k == KeyEvent.VK_Q) System.exit(0);
    }

    private void gestisciGioco(int k, boolean pressed) {
        // Dialogo shopkeeper ha priorità sugli altri input
        if (state.dialogoShopkeeper.isVisibile()) {
            if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A)     state.dialogoShopkeeper.spostaScelta(-1);
            if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)     state.dialogoShopkeeper.spostaScelta(1);
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) state.dialogoShopkeeper.conferma();
            if (k == KeyEvent.VK_ESCAPE)                           state.dialogoShopkeeper.annulla();
            return; // blocca movimento durante il dialogo
        }

        if (k == KeyEvent.VK_ESCAPE) {
            state.statoPrecedente  = GameState.StatoGioco.GIOCO;
            state.statoGioco       = GameState.StatoGioco.PAUSA;
            state.indiceBtnPausa   = 0;  // seleziona "RIPRENDI" di default
            return;
        }
        toggleMovimento(k, pressed);
        toggleSparo(k, pressed);
    }

    private void gestisciFinale(int k) {
        if (k == KeyEvent.VK_ENTER) {
            state.tornaAlMenu();
            roomMgr.resetCompleto();
        }
    }

    // ── Gestione click mouse ──────────────────────────────────────────────────

    private void gestisciClick(Point p) {
        switch (state.statoGioco) {

            case MENU -> {
                if (ui.btnGioca.contains(p))
                    state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
                else if (ui.btnImpostazioni.contains(p)) {
                    state.statoPrecedente = GameState.StatoGioco.MENU;
                    state.statoGioco      = GameState.StatoGioco.IMPOSTAZIONI;
                } else if (ui.btnControlli.contains(p)) {
                    state.statoPrecedente = GameState.StatoGioco.MENU;
                    state.statoGioco      = GameState.StatoGioco.CONTROLLI;
                } else if (ui.btnEsciMenu.contains(p))
                    System.exit(0);
            }

            case IMPOSTAZIONI -> {
                if (ui.btnMusMeno.contains(p))  state.impostazioni.cambiaVolumeMusica(-10);
                if (ui.btnMusPiu.contains(p))   state.impostazioni.cambiaVolumeMusica(+10);
                if (ui.btnEffMeno.contains(p))  state.impostazioni.cambiaVolumeEffetti(-10);
                if (ui.btnEffPiu.contains(p))   state.impostazioni.cambiaVolumeEffetti(+10);
                if (ui.btnChiudiImpostazioni.contains(p)) tornaDaImpostazioni();
            }

            case CONTROLLI -> {
                if (ui.btnChiudiControlli.contains(p))
                    state.statoGioco = state.statoPrecedente != null
                            ? state.statoPrecedente : GameState.StatoGioco.MENU;
            }

            case SELEZIONE_PERSONAGGIO -> {
                for (int i = 0; i < 4; i++) {
                    if (ui.rectsSelezionePG[i].contains(p)
                            && state.sistemaPersonaggi.isSbloccato(i)) {
                        state.indicePersonaggioSelezionato = i;
                        state.statoGioco = GameState.StatoGioco.SELEZIONE_MODALITA;
                        break;
                    }
                }
            }

            case SELEZIONE_MODALITA -> {
                for (int i = 0; i < 2; i++) {
                    if (ui.rectsSelezioneModalita[i].contains(p)) {
                        state.indiceModalitaSelezionata = i;
                        confermaSelezioneModalita();
                        break;
                    }
                }
            }

            case PAUSA -> {
                if (ui.btnRiprendi.contains(p))
                    state.statoGioco = GameState.StatoGioco.GIOCO;
                else if (ui.btnImpostazioniPausa.contains(p)) {
                    state.statoPrecedente = GameState.StatoGioco.PAUSA;
                    state.statoGioco      = GameState.StatoGioco.IMPOSTAZIONI;
                } else if (ui.btnMenuPrincipalePausa.contains(p)) {
                    state.tornaAlMenu();
                    roomMgr.resetCompleto();
                } else if (ui.btnEsciPausa.contains(p))
                    System.exit(0);
            }

            case GAME_OVER -> {
                if (ui.btnRiprova.contains(p)) {
                    DatiPersonaggio pg = ui.getPersonaggioSelezionato(state.indicePersonaggioSelezionato);
                    state.resetTotale(pg);
                    roomMgr.resetCompleto();
                    state.statoGioco = GameState.StatoGioco.GIOCO;
                } else if (ui.btnMenuPrincipaleGO.contains(p)) {
                    state.tornaAlMenu();
                    roomMgr.resetCompleto();
                } else if (ui.btnEsciGO.contains(p))
                    System.exit(0);
            }

            case VITTORIA_STORIA -> {
                if (ui.btnMenuPrincipaleVittoria.contains(p)) {
                    state.tornaAlMenu();
                    roomMgr.resetCompleto();
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void tornaDaImpostazioni() {
        state.statoGioco = state.statoPrecedente != null
                ? state.statoPrecedente : GameState.StatoGioco.MENU;
    }

    private void confermaSelezioneModalita() {
        state.modalitaScelta = (state.indiceModalitaSelezionata == 0)
                ? GameState.Modalita.STORIA : GameState.Modalita.INFINITA;
        DatiPersonaggio pg = ui.getPersonaggioSelezionato(state.indicePersonaggioSelezionato);
        state.resetTotale(pg);
        roomMgr.resetCompleto();
        state.statoGioco = GameState.StatoGioco.GIOCO;
    }

    public void toggleMovimento(int k, boolean pressed) {
        if (k == KeyEvent.VK_W) state.up    = pressed;
        if (k == KeyEvent.VK_S) state.down  = pressed;
        if (k == KeyEvent.VK_A) state.left  = pressed;
        if (k == KeyEvent.VK_D) state.right = pressed;
    }

    public void toggleSparo(int k, boolean pressed) {
        if (k == KeyEvent.VK_UP)    state.shootUp    = pressed;
        if (k == KeyEvent.VK_DOWN)  state.shootDown  = pressed;
        if (k == KeyEvent.VK_LEFT)  state.shootLeft  = pressed;
        if (k == KeyEvent.VK_RIGHT) state.shootRight = pressed;
    }
}