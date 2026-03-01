import java.awt.Color;
import java.util.ArrayList;

/**
 * GameState.java
 * Contiene tutti i dati di stato del gioco: statistiche giocatore,
 * progressione, flags di stato, e metodi di reset/danno.
 * Viene passato per riferimento a tutte le altre classi.
 */
public class GameState {

    // ── Enumerazioni ──────────────────────────────────────────────────────────
    public enum StatoGioco {
        MENU, SELEZIONE_PERSONAGGIO, SELEZIONE_MODALITA,
        GIOCO, VITTORIA_STORIA, GAME_OVER, PAUSA
    }

    public enum Modalita { STORIA, INFINITA }

    // ── Stato corrente ────────────────────────────────────────────────────────
    public StatoGioco statoGioco   = StatoGioco.MENU;
    public StatoGioco statoPrecedente;
    public Modalita   modalitaScelta = Modalita.STORIA;

    // ── Posizione giocatore ───────────────────────────────────────────────────
    public float x, y;

    // ── Statistiche giocatore (impostate da DatiPersonaggio + shop) ───────────
    public float velocita;
    public int   dannoPugno;
    public int   viteMaxGiocatore;
    public int   vite;

    // ── Progressione mondo/stanza ─────────────────────────────────────────────
    public int     mondoAttuale       = 1;
    public int     stanzaNelMondo     = 1;
    public int     indiceStanzaMemoria = 0;
    public boolean bossSpawnato       = false;
    public boolean bossSconfitto      = false;

    // ── Risorse ───────────────────────────────────────────────────────────────
    public int monete = 0;

    // ── Invulnerabilità ───────────────────────────────────────────────────────
    public boolean invulnerabile       = false;
    public int     timerInvulnerabilita = 0;

    // ── Timer Boss ────────────────────────────────────────────────────────────
    public int tempoRimanenteBoss;
    public static final int TEMPO_BOSS_DEFAULT = 120 * 60; // 120 secondi

    // ── Selezione personaggio / modalità ──────────────────────────────────────
    public int indicePersonaggioSelezionato = 0;
    public int indiceModalitaSelezionata    = 0;

    // ── Costanti layout ───────────────────────────────────────────────────────
    public static final int TILE_SIZE      = 64;
    public static final int COL_GIOCO      = 15;
    public static final int RIG_GIOCO      = 5;
    public static final int OFFSET         = 1;
    public static final int COL_TOTALI     = COL_GIOCO + (OFFSET * 2);
    public static final int RIG_TOTALI     = RIG_GIOCO + (OFFSET * 2);
    public static final int LARGHEZZA_GIOCO = COL_TOTALI * TILE_SIZE;
    public static final int ALTEZZA_GIOCO   = RIG_TOTALI * TILE_SIZE;
    public static final int PG_SIZE         = 50;
    public static final int STANZA_BOSS     = 8;
    public static final int MONDI_STORIA_MAX = 4;

    // ── Input movimento ───────────────────────────────────────────────────────
    public boolean up, down, left, right;
    public boolean shootUp, shootDown, shootLeft, shootRight;
    public int  cooldownSparo = 0;
    public static final int SPARO_DELAY = 12;

    // ── Callback danno/morte (impostata dal game loop principale) ─────────────
    private GameEventListener eventListener;

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    // ── Metodi di stato ───────────────────────────────────────────────────────

    /**
     * Applica i dati base del personaggio scelto e resetta la posizione.
     */
    public void applicaDatiPersonaggio(DatiPersonaggio pg) {
        this.velocita         = pg.velocitaBase;
        this.dannoPugno       = pg.dannoBase;
        this.viteMaxGiocatore = pg.vitaMax;
        this.vite             = viteMaxGiocatore;
    }

    /**
     * Centra il giocatore nello schermo e pulisce gli input.
     */
    public void resetGiocatore() {
        this.x = LARGHEZZA_GIOCO / 2f;
        this.y = ALTEZZA_GIOCO  / 2f;
        invulnerabile       = false;
        timerInvulnerabilita = 0;
        up = down = left = right = false;
        shootUp = shootDown = shootLeft = shootRight = false;
        cooldownSparo = 0;
    }

    /**
     * Reset completo ad inizio nuova partita.
     * La logica del mondo/stanza è in RoomManager; qui si azzerano solo
     * i contatori di stato e le statistiche base.
     */
    public void resetTotale(DatiPersonaggio pg) {
        applicaDatiPersonaggio(pg);
        mondoAttuale         = 1;
        stanzaNelMondo       = 1;
        indiceStanzaMemoria  = 0;
        monete               = 0;
        bossSpawnato         = false;
        bossSconfitto        = false;
        resetGiocatore();
    }

    /**
     * Ricevi danno: scala le vite e attiva invulnerabilità temporanea.
     * Notifica l'event listener se le vite azzerano.
     */
    public void riceviDanno() {
        if (invulnerabile) return;
        vite--;
        invulnerabile        = true;
        timerInvulnerabilita = 0;
        if (vite <= 0) {
            statoGioco = StatoGioco.GAME_OVER;
            if (eventListener != null) eventListener.onGameOver();
        }
    }

    /**
     * Tick dell'invulnerabilità: chiamato ogni frame durante GIOCO.
     */
    public void tickInvulnerabilita() {
        if (!invulnerabile) return;
        timerInvulnerabilita++;
        if (timerInvulnerabilita > 60) {
            invulnerabile        = false;
            timerInvulnerabilita = 0;
        }
    }

    /**
     * Tick timer boss: decrementa e attiva GAME_OVER se scade.
     */
    public void tickTimerBoss() {
        if (stanzaNelMondo != STANZA_BOSS || !bossSpawnato || bossSconfitto) return;
        tempoRimanenteBoss--;
        if (tempoRimanenteBoss <= 0) {
            statoGioco = StatoGioco.GAME_OVER;
            if (eventListener != null) eventListener.onGameOver();
        }
    }

    /**
     * Resetta tutto e torna al menu (usato dopo vittoria/sconfitta).
     */
    public void tornaAlMenu() {
        indicePersonaggioSelezionato = 0;
        indiceModalitaSelezionata    = 0;
        mondoAttuale                 = 1;
        stanzaNelMondo               = 1;
        indiceStanzaMemoria          = 0;
        monete                       = 0;
        bossSpawnato                 = false;
        bossSconfitto                = false;
        statoGioco                   = StatoGioco.MENU;
        resetGiocatore();
    }

    // ── Interfaccia callback ──────────────────────────────────────────────────
    public interface GameEventListener {
        void onGameOver();
        void onVittoria();
    }
}
