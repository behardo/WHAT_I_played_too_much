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
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = fullscreenMgr.scalaCoordinate(e.getX(), e.getY());
                state.mouseX = p.x;
                state.mouseY = p.y;
                ui.aggiornaMouse(p.x, p.y);
            }
        };
    }

    // ── Gestori tastiera ──────────────────────────────────────────────────────

    private void gestisciMenu(int k) {
        if (k == KeyEvent.VK_ENTER) state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
    }

    private void gestisciImpostazioni(int k) {
        if (k == KeyEvent.VK_ESCAPE) tornaDaImpostazioni();
        if (k == KeyEvent.VK_LEFT)  state.impostazioni.cambiaVolumeMusica(-10);
        if (k == KeyEvent.VK_RIGHT) state.impostazioni.cambiaVolumeMusica(+10);
    }

    private void gestisciSelezionePG(int k) {
        int n = ui.getNumPersonaggi();
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A)
            state.indicePersonaggioSelezionato = (state.indicePersonaggioSelezionato - 1 + n) % n;
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
            state.indicePersonaggioSelezionato = (state.indicePersonaggioSelezionato + 1) % n;
        else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE)
            state.statoGioco = GameState.StatoGioco.SELEZIONE_MODALITA;
        else if (k == KeyEvent.VK_ESCAPE)
            state.statoGioco = GameState.StatoGioco.MENU;
    }

    private void gestisciSelezioneModalita(int k) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
            state.indiceModalitaSelezionata = (state.indiceModalitaSelezionata == 0) ? 1 : 0;
        else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) confermaSelezioneModalita();
        else if (k == KeyEvent.VK_ESCAPE) state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
    }

    private void gestisciPausa(int k) {
        if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER)
            state.statoGioco = GameState.StatoGioco.GIOCO;
        else if (k == KeyEvent.VK_Q)
            System.exit(0);
    }

    private void gestisciGioco(int k, boolean pressed) {
        if (k == KeyEvent.VK_ESCAPE) {
            state.statoPrecedente = GameState.StatoGioco.GIOCO;
            state.statoGioco      = GameState.StatoGioco.PAUSA;
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
                if (ui.btnDifficolta.contains(p)) state.impostazioni.cicladifficolta();
                if (ui.btnChiudiImpostazioni.contains(p)) tornaDaImpostazioni();
            }

            case CONTROLLI -> {
                if (ui.btnChiudiControlli.contains(p))
                    state.statoGioco = state.statoPrecedente != null
                            ? state.statoPrecedente : GameState.StatoGioco.MENU;
            }

            case SELEZIONE_PERSONAGGIO -> {
                for (int i = 0; i < 4; i++) {
                    if (ui.rectsSelezionePG[i].contains(p)) {
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