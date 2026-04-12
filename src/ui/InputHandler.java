package ui;

import core.GameState;
import data.DatiPersonaggio;
import data.Lang;
import game.TetrisGame;
import resource.ResourceLoader;
import room.RoomManager;

import java.awt.Point;
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
    private final ResourceLoader     res;

    public InputHandler(GameState state, UIManager ui, RoomManager roomMgr,
                        FullscreenManager fullscreenMgr, ResourceLoader res) {
        this.state         = state;
        this.ui            = ui;
        this.roomMgr       = roomMgr;
        this.fullscreenMgr = fullscreenMgr;
        this.res           = res;
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
                    case CONTROLLI             -> {
                        if (k == KeyEvent.VK_ESCAPE) {
                            state.statoGioco = state.statoPrecedente != null
                                    ? state.statoPrecedente : GameState.StatoGioco.MENU;
                            state.controlliScrollY = 0;
                        }
                        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) state.controlliScrollY += 28;
                        if (k == KeyEvent.VK_UP   || k == KeyEvent.VK_W) state.controlliScrollY = Math.max(0, state.controlliScrollY - 28);
                    }
                    case SELEZIONE_PERSONAGGIO -> gestisciSelezionePG(k);
                    case SELEZIONE_MODALITA    -> gestisciSelezioneModalita(k);
                    case TETRIS                -> gestisciTetris(k);
                    case PAUSA                 -> gestisciPausa(k);
                    case GIOCO                 -> gestisciGioco(k, true);
                    case BOSS_RUSH             -> gestisciBossRush(k);
                    case VITTORIA_STORIA,
                         GAME_OVER             -> gestisciFinale(k);
                    case UFFICIO               -> gestisciUfficio(k);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (state.statoGioco == GameState.StatoGioco.GIOCO
                        || state.statoGioco == GameState.StatoGioco.BOSS_RUSH) {
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
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                if (state.statoGioco == GameState.StatoGioco.CONTROLLI) {
                    state.controlliScrollY = Math.max(0, state.controlliScrollY + (int)(e.getWheelRotation() * 28));
                }
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
        // 0. Popup nota codice debug
        if (state.mostraNota) {
            if (state.notaFase == 0) {
                // Fase 0: nota classica
                if (k == KeyEvent.VK_ESCAPE) {
                    state.mostraNota = false;
                    state.notaFase   = 0;
                    return;
                }
                if (k == KeyEvent.VK_T || k == KeyEvent.VK_ENTER) {
                    // Passa al terminale
                    state.notaFase           = 1;
                    state.notaTerminaleInput = "";
                    return;
                }
            } else {
                // Fase 1: terminale anni '80
                if (k == KeyEvent.VK_ESCAPE) {
                    state.notaFase           = 0;
                    state.notaTerminaleInput = "";
                    return;
                }
                if (k == KeyEvent.VK_BACK_SPACE && !state.notaTerminaleInput.isEmpty()) {
                    state.notaTerminaleInput = state.notaTerminaleInput
                            .substring(0, state.notaTerminaleInput.length() - 1);
                    return;
                }
                if (k == KeyEvent.VK_ENTER) {
                    // Verifica codice
                    if (state.notaTerminaleInput.equalsIgnoreCase(core.GameState.CODICE_DEBUG)) {
                        state.notaTerminaleRisolta = true;
                        state.sistemaPersonaggi.sblocaDitto();
                        state.notaFase = 2; // fase successo
                    } else {
                        state.notaTerminaleInput = ""; // reset — riprova
                    }
                    return;
                }
                if (k == KeyEvent.VK_SPACE && state.notaFase == 2) {
                    state.mostraNota = false;
                    state.notaFase   = 0;
                    return;
                }
                // Tasti alfanumerici e trattino
                char c = (char) k;
                boolean isAlpha = (k >= KeyEvent.VK_A && k <= KeyEvent.VK_Z);
                boolean isDigit = (k >= KeyEvent.VK_0 && k <= KeyEvent.VK_9);
                boolean isMinus = (k == KeyEvent.VK_MINUS || k == 109);
                if ((isAlpha || isDigit || isMinus) && state.notaTerminaleInput.length() < 12) {
                    if (isAlpha) c = java.awt.event.KeyEvent.getKeyText(k).toUpperCase().charAt(0);
                    else if (isMinus) c = '-';
                    state.notaTerminaleInput += c;
                    return;
                }
            }
            return;
        }

        // 1. Dialogo Casa (singola pagina)
        if (state.mostraDialogoCasa
                && (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE
                || k == KeyEvent.VK_ESCAPE)) {
            state.mostraDialogoCasa = false;
            return;
        }

        // 2. DialogoNarrazione (JRPG multi-pagina: boss + shopkeeper introduzione)
        if (state.dialogoNarrazione.isAttivo()) {
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_Z) {
                boolean finito = state.dialogoNarrazione.avanza();
                // Se finito e siamo in stanza shop, mostra la scelta Sì/No
                if (finito && state.dialogoShopkeeperNarrazioneAvviata) {
                    // Forza lo stato MOSTRA_DIALOGO del vecchio sistema
                    // (già in MOSTRA_DIALOGO, non serve fare nulla —
                    //  gestisciShopkeeperScelta() se ne occupa al prossimo input)
                }
            }
            return; // blocca tutto il resto durante la narrazione
        }

        // 3. Scelta Sì/No shopkeeper (appare dopo che la narrazione è finita)
        if (state.dialogoShopkeeperNarrazioneAvviata
                && state.dialogoShopkeeper.isVisibile()) {
            if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A)     state.dialogoShopkeeper.spostaScelta(-1);
            if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)     state.dialogoShopkeeper.spostaScelta(1);
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) state.dialogoShopkeeper.conferma();
            if (k == KeyEvent.VK_ESCAPE)                           state.dialogoShopkeeper.annulla();
            return;
        }

        // 4. Pausa
        if (k == KeyEvent.VK_ESCAPE) {
            state.statoPrecedente = GameState.StatoGioco.GIOCO;
            state.statoGioco      = GameState.StatoGioco.PAUSA;
            state.indiceBtnPausa  = 0;
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

    private void gestisciUfficio(int k) {
        if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_Z) {
            if (state.dialogoNarrazione.isAttivo()) {
                boolean finito = state.dialogoNarrazione.avanza();
                if (finito) {
                    // Dialogo col capo finito → schermata finale
                    state.statoGioco = GameState.StatoGioco.VITTORIA_STORIA;
                }
            }
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
                    state.statoPrecedente  = GameState.StatoGioco.MENU;
                    state.statoGioco       = GameState.StatoGioco.CONTROLLI;
                    state.controlliScrollY = 0;
                } else if (ui.btnEsciMenu.contains(p))
                    System.exit(0);
                else if (ui.btnLingua.contains(p))
                    cambiLingua();
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

            case BOSS_RUSH -> {
                if (state.bossRushSceltaPowerUp) {
                    // Click sulle 3 card opzioni
                    for (int i = 0; i < 3; i++) {
                        if (state.bossRushRectsOpzioni != null
                                && i < state.bossRushRectsOpzioni.length
                                && state.bossRushRectsOpzioni[i] != null
                                && state.bossRushRectsOpzioni[i].contains(p)) {
                            roomMgr.applicaPowerUpBossRush(i);
                            break;
                        }
                    }
                }
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

    /** Alterna IT ↔ EN e aggiorna le label di tutti i bottoni. */
    private void cambiLingua() {
        Lang.lingua = (Lang.lingua == Lang.Lingua.IT) ? Lang.Lingua.EN : Lang.Lingua.IT;
        state.lingua = Lang.lingua;
        // Aggiorna label di tutti i bottoni che dipendono dalla lingua
        ui.btnGioca.label              = Lang.t("btn.gioca");
        ui.btnImpostazioni.label       = Lang.t("btn.impostazioni");
        ui.btnControlli.label          = Lang.t("btn.controlli");
        ui.btnEsciMenu.label           = Lang.t("btn.esci");
        ui.btnLingua.label             = Lang.t("btn.lingua");
        ui.btnRiprendi.label           = Lang.t("btn.riprendi");
        ui.btnImpostazioniPausa.label  = Lang.t("btn.impostazioni");
        ui.btnMenuPrincipalePausa.label= Lang.t("btn.menu");
        ui.btnEsciPausa.label          = Lang.t("btn.esci");
        ui.btnRiprova.label            = Lang.t("btn.riprova");
        ui.btnMenuPrincipaleGO.label   = Lang.t("btn.menu");
        ui.btnEsciGO.label             = Lang.t("btn.esci");
        ui.btnMenuPrincipaleVittoria.label = Lang.t("btn.menu.principale");
        ui.btnChiudiImpostazioni.label = Lang.t("btn.indietro");
        ui.btnChiudiControlli.label    = Lang.t("btn.indietro");
        // Aggiorna bandiera sul bottone
        if (res != null) {
            ui.btnLingua.setIcona(
                    Lang.lingua == Lang.Lingua.IT ? res.imgBandieraIT : res.imgBandieraEN
            );
        }
    }

    private void gestisciBossRush(int k) {
        // Se è in corso la scelta power-up
        if (state.bossRushSceltaPowerUp) {
            if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A)
                state.bossRushOpzioneScelta = Math.max(0, state.bossRushOpzioneScelta - 1);
            if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
                state.bossRushOpzioneScelta = Math.min(2, state.bossRushOpzioneScelta + 1);
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE)
                roomMgr.applicaPowerUpBossRush(state.bossRushOpzioneScelta);
            return;
        }
        // Altrimenti gestisci come gioco normale
        gestisciGioco(k, true);
    }

    private void confermaSelezioneModalita() {
        state.modalitaScelta = (state.indiceModalitaSelezionata == 0)
                ? GameState.Modalita.STORIA : GameState.Modalita.INFINITA;
        DatiPersonaggio pg = ui.getPersonaggioSelezionato(state.indicePersonaggioSelezionato);
        state.resetTotale(pg);
        roomMgr.resetCompleto();
        // Avvia il mini-Tetris prima di entrare nel gioco
        state.tetris     = new TetrisGame();
        state.statoGioco = GameState.StatoGioco.TETRIS;
    }

    /** Avvia il gioco vero dopo il Tetris, applicando il power-up Casa. */
    private void avviaGiocoDopTetris() {
        state.powerUpCasa       = state.tetris != null ? state.tetris.getPowerUp() : state.powerUpCasa;
        state.tetris            = null;
        state.mostraDialogoCasa = true;
        state.statoGioco        = GameState.StatoGioco.GIOCO;
        // Genera la stanza 1 (Casa) — mondoAttuale=1, stanzaNelMondo=1, stanzaCasaVisitata=false
        // RoomManager la riconosce e genera la stanza Casa con gli items del Tetris
        roomMgr.resetCompleto();
        roomMgr.generaNuovaStanza();
    }

    private void gestisciTetris(int k) {
        if (state.tetris == null) { avviaGiocoDopTetris(); return; }
        TetrisGame t = state.tetris;
        if (t.completato || t.gameOver) {
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE
                    || k == KeyEvent.VK_ESCAPE) avviaGiocoDopTetris();
            return;
        }
        switch (k) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> t.muoviSinistra();
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> t.muoviDestra();
            case KeyEvent.VK_W, KeyEvent.VK_UP    -> t.ruota();
            case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> t.scendiForzato();
            case KeyEvent.VK_SPACE                -> t.cadutaIstantanea();
            case KeyEvent.VK_ESCAPE               -> avviaGiocoDopTetris(); // salta
        }
    }

    public void toggleMovimento(int k, boolean pressed) {
        if (k == KeyEvent.VK_W) state.up    = pressed;
        if (k == KeyEvent.VK_S) state.down  = pressed;
        if (k == KeyEvent.VK_A) state.left  = pressed;
        if (k == KeyEvent.VK_D) state.right = pressed;
        // Melee — solo al keyPressed (non held)
        if (k == KeyEvent.VK_Z && pressed && state.meleeUnlocked) {
            state.meleeAttivo = true;
        }
    }

    public void toggleSparo(int k, boolean pressed) {
        if (k == KeyEvent.VK_UP)    state.shootUp    = pressed;
        if (k == KeyEvent.VK_DOWN)  state.shootDown  = pressed;
        if (k == KeyEvent.VK_LEFT)  state.shootLeft  = pressed;
        if (k == KeyEvent.VK_RIGHT) state.shootRight = pressed;
    }
}