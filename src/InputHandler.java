import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;

/**
 * InputHandler.java
 * Gestisce tutti gli input da tastiera e da mouse.
 * Modifica GameState in risposta agli input ricevuti.
 * Espone un KeyAdapter e un MouseAdapter pronti per essere
 * agganciati al JPanel principale con addKeyListener/addMouseListener.
 */
public class InputHandler {

    private final GameState   state;
    private final UIManager   ui;
    private final RoomManager roomMgr;

    // Riferimento al pannello per fullscreen toggle
    private final FullscreenManager fullscreenMgr;

    // ─────────────────────────────────────────────────────────────────────────
    public InputHandler(GameState state, UIManager ui, RoomManager roomMgr,
                        FullscreenManager fullscreenMgr) {
        this.state          = state;
        this.ui             = ui;
        this.roomMgr        = roomMgr;
        this.fullscreenMgr  = fullscreenMgr;
    }

    // ── KeyAdapter ────────────────────────────────────────────────────────────

    public KeyAdapter getKeyAdapter() {
        return new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();

                // F11 fullscreen globale
                if (k == KeyEvent.VK_F11) {
                    fullscreenMgr.toggle();
                    return;
                }

                switch (state.statoGioco) {
                    case MENU                  -> gestisciMenu(k);
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

    // ── MouseAdapter ──────────────────────────────────────────────────────────

    /**
     * Restituisce un MouseAdapter che usa le coordinate logiche scalate.
     * Lo scaling deve essere calcolato dal chiamante e passato via
     * FullscreenManager.
     */
    public MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pLogico = fullscreenMgr.scalaCoordinate(e.getX(), e.getY());
                gestisciClick(pLogico);
            }
        };
    }

    // ── Gestori per stato ─────────────────────────────────────────────────────

    private void gestisciMenu(int k) {
        if (k == KeyEvent.VK_ENTER) {
            state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
        }
    }

    private void gestisciSelezionePG(int k) {
        int n = ui.getNumPersonaggi();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
            state.indicePersonaggioSelezionato =
                    (state.indicePersonaggioSelezionato - 1 + n) % n;
        } else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            state.indicePersonaggioSelezionato =
                    (state.indicePersonaggioSelezionato + 1) % n;
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            state.statoGioco = GameState.StatoGioco.SELEZIONE_MODALITA;
        }
    }

    private void gestisciSelezioneModalita(int k) {
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A
                || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            state.indiceModalitaSelezionata = (state.indiceModalitaSelezionata == 0) ? 1 : 0;
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            confermaSelezioneModalita();
        } else if (k == KeyEvent.VK_ESCAPE) {
            state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
        }
    }

    private void gestisciPausa(int k) {
        if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER) {
            state.statoGioco = GameState.StatoGioco.GIOCO;
        } else if (k == KeyEvent.VK_Q) {
            System.exit(0);
        }
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

    // ── Gestione Click Mouse ──────────────────────────────────────────────────

    private void gestisciClick(Point p) {
        switch (state.statoGioco) {

            case MENU -> {
                Rectangle areaStart = new Rectangle(
                        GameState.LARGHEZZA_GIOCO / 2 - 160,
                        GameState.ALTEZZA_GIOCO   / 2,
                        320, 50);
                if (areaStart.contains(p)) {
                    state.statoGioco = GameState.StatoGioco.SELEZIONE_PERSONAGGIO;
                }
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

            case GAME_OVER -> {
                if (ui.btnRiprova.contains(p)) {
                    DatiPersonaggio pg = ui.getPersonaggioSelezionato(state.indicePersonaggioSelezionato);
                    state.resetTotale(pg);
                    roomMgr.resetCompleto();
                    state.statoGioco = GameState.StatoGioco.GIOCO;
                } else if (ui.btnEsci.contains(p)) {
                    System.exit(0);
                }
            }

            case VITTORIA_STORIA -> {
                if (ui.btnMenuPrincipale.contains(p)) {
                    state.tornaAlMenu();
                    roomMgr.resetCompleto();
                }
            }
        }
    }

    // ── Helpers input movimento/sparo ─────────────────────────────────────────

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

    // ── Conferma modalità e avvio partita ─────────────────────────────────────

    private void confermaSelezioneModalita() {
        state.modalitaScelta = (state.indiceModalitaSelezionata == 0)
                ? GameState.Modalita.STORIA
                : GameState.Modalita.INFINITA;

        DatiPersonaggio pg = ui.getPersonaggioSelezionato(state.indicePersonaggioSelezionato);
        state.resetTotale(pg);
        roomMgr.resetCompleto();

        state.statoGioco = GameState.StatoGioco.GIOCO;
        System.out.println("[InputHandler] Partita avviata: PG=" + pg.nome
                + ", Modalità=" + state.modalitaScelta);
    }
}