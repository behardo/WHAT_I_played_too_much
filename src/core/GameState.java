package core;

import audio.AudioManager;
import data.DatiPersonaggio;
import data.Impostazioni;
import data.Lang;
import data.SistemaPersonaggi;
import game.TetrisGame;
import ui.DialogoNarrazione;
import ui.DialogoShopkeeper;

/**
 * GameState.java
 * Contiene tutti i dati di stato del gioco: statistiche giocatore,
 * progressione, flags di stato, e metodi di reset/danno.
 * Viene passato per riferimento a tutte le altre classi.
 */
public class GameState {

    // ── Enumerazioni ──────────────────────────────────────────────────────────
    public enum StatoGioco {
        MENU, IMPOSTAZIONI, CONTROLLI,
        SELEZIONE_PERSONAGGIO, SELEZIONE_MODALITA,
        TETRIS, GIOCO, BOSS_RUSH, UFFICIO, VITTORIA_STORIA, GAME_OVER, PAUSA
    }

    // ── Nota in Casa ──────────────────────────────────────────────────────────
    public boolean notaRaccolta    = false;   // true se il giocatore l'ha presa
    public boolean mostraNota      = false;   // true mentre il popup è visibile
    public static final String CODICE_DEBUG = "WIPT-4269";  // codice mostrato nella nota

    // ── Ufficio ───────────────────────────────────────────────────────────────
    public boolean ufficioDialogoAvviato = false;

    // ── Boss Rush (tombino dopo Mannie) ───────────────────────────────────────

    /** True se il tombino è visibile (boss m1 sconfitto, modalità storia) */
    public boolean tombinoVisibile      = false;
    /** True se il giocatore è dentro la boss rush */
    public boolean inBossRush           = false;
    /** Quale boss si sta affrontando nella rush: 2=Presagio, 3=ReForni, 4=Gelo */
    public int     bossRushIndice       = 2;
    /** Boss rush completata (tutti e 3 sconfitti) */
    public boolean bossRushCompletata   = false;
    /** Quanti boss rush boss sconfitti finora */
    public int     bossRushSconfitti    = 0;
    /** Power-up guadagnati nella boss rush (uno per boss, max 3) */
    public int     bossRushPowerUp1     = 0; // 0=nessuno, 1=cura, 2=vel, 3=danno, 4=melee
    public int     bossRushPowerUp2     = 0;
    public int     bossRushPowerUp3     = 0;
    /** Schermata scelta power-up attiva */
    public boolean bossRushSceltaPowerUp = false;
    /** Opzioni presentate per la scelta corrente (3 indici) */
    public int[]   bossRushOpzioni      = {1, 2, 3};
    /** Opzione selezionata (0-2) */
    public int     bossRushOpzioneScelta = 0;
    /** Rettangoli delle card opzioni power-up (settati dal renderer) */
    public java.awt.Rectangle[] bossRushRectsOpzioni = new java.awt.Rectangle[3];

    public enum Modalita { STORIA, INFINITA }

    // ── Stato corrente ────────────────────────────────────────────────────────
    public StatoGioco statoGioco     = StatoGioco.MENU;
    public StatoGioco statoPrecedente;
    public Modalita   modalitaScelta = Modalita.STORIA;

    /** Lingua UI — sincronizzata con Lang.lingua */
    public Lang.Lingua lingua = Lang.Lingua.IT;

    // ── Impostazioni (dati persistenti tra le schermate) ──────────────────────
    public final Impostazioni impostazioni = new Impostazioni();

    // ── Audio ─────────────────────────────────────────────────────────────────
    public final AudioManager audio = new AudioManager(WhatIvePlayedTooMuch.class);

    // ── Sistema personaggi (sblocchi + combo segreto) ─────────────────────────
    public final SistemaPersonaggi sistemaPersonaggi = new SistemaPersonaggi();

    // ── Dialogo shopkeeper (attacco sì/no) ────────────────────────────────────
    public final DialogoShopkeeper dialogoShopkeeper = new DialogoShopkeeper();

    // ── Posizione mouse (coordinate logiche, aggiornata ogni frame) ───────────
    public int mouseX = 0;
    public int mouseY = 0;
    public int indiceBtnPausa = 0;  // 0=Riprendi 1=Impostazioni 2=Menu 3=Esci

    // ── Posizione giocatore ───────────────────────────────────────────────────
    public float x, y;

    // ── Statistiche giocatore (impostate da DatiPersonaggio + shop) ───────────
    public float velocita;
    public int   dannoPugno;
    public int   viteMaxGiocatore;
    public int   vite;

    // ── Progressione mondo/stanza ─────────────────────────────────────────────
    public int     mondoAttuale        = 1;

    // ── Tetris pre-run ────────────────────────────────────────────────────────
    public TetrisGame tetris           = null;   // istanza attiva solo durante TETRIS
    public String     powerUpCasa      = "NESSUNO"; // risultato tetris
    public boolean    stanzaCasaVisitata = false;
    public boolean    mostraDialogoCasa  = false; // dialogo "WHAT? I'VE PLAYED TOO MUCH"

    // ── Dialogo narrazione (JRPG multi-pagina) ────────────────────────────────
    public final DialogoNarrazione dialogoNarrazione = new DialogoNarrazione();
    public boolean dialogoShopkeeperNarrazioneAvviata = false;

    // ── Effetto bruciatura (boss 3) ───────────────────────────────────────────
    public boolean burnAttivo  = false;
    public int     burnTimer   = 0;
    public int     burnTick    = 0;
    // Freeze (ghiaccio) — immobilizza il giocatore temporaneamente
    public boolean freezeAttivo = false;
    public int     freezeTimer  = 0;
    public static final int FREEZE_DURATA = 180; // 3 secondi
    // Slow (tile ghiaccio) — riduce velocità temporaneamente
    public boolean slowAttivo  = false;
    public int     slowTimer   = 0;
    public static final int SLOW_DURATA   = 180; // 3 secondi
    public static final float SLOW_MULT   = 0.45f;
    public static final int BURN_DURATA     = 90;  // ~1.5s = esattamente 2 tick di danno
    public static final int BURN_INTERVALLO = 40;  // danno ogni ~0.66s
    public static final int BURN_DANNO      = 1;
    public int     stanzaNelMondo      = 1;
    public int     indiceStanzaMemoria = 0;
    public boolean bossSpawnato        = false;
    public boolean bossSconfitto       = false;

    /**
     * True quando il boss del mondo PRECEDENTE è stato sconfitto.
     * Determina se la porta dello shop (nord) è visibile nella stanza 1 del nuovo mondo.
     * Viene impostato a true in avanzaAlMondoSuccessivo() e resettato a false
     * solo all'inizio di una nuova partita completa.
     */
    public boolean shopSbloccato = false;

    // ── UI Controlli scroll ───────────────────────────────────────────────────
    public int controlliScrollY = 0; // offset pixel scroll tendina controlli

    // ── Risorse ───────────────────────────────────────────────────────────────
    public int monete = 0;

    // ── Invulnerabilità ───────────────────────────────────────────────────────
    public boolean invulnerabile       = false;
    public int     timerInvulnerabilita = 0;

    // ── Timer Boss ────────────────────────────────────────────────────────────
    public int tempoRimanenteBoss;
    public static final int TEMPO_BOSS_DEFAULT = 120 * 60; // 120 secondi

    // ── Selezione personaggio / modalità ──────────────────────────────────────
    public int    indicePersonaggioSelezionato = 0;
    public int    indiceModalitaSelezionata    = 0;
    public String nomePGCorrente               = "BELLGERD";

    /** Nome del personaggio corrente per i dialoghi. */
    public String nomePersonaggioCorrente() { return nomePGCorrente; }

    // ── Costanti layout ───────────────────────────────────────────────────────
    public static final int TILE_SIZE      = 64;
    public static final int COL_GIOCO      = 15;
    public static final int RIG_GIOCO      = 5;
    public static final int OFFSET         = 1;
    public static final int COL_TOTALI     = COL_GIOCO + (OFFSET * 2);
    public static final int RIG_TOTALI     = RIG_GIOCO + (OFFSET * 2);
    public static final int LARGHEZZA_GIOCO = COL_TOTALI * TILE_SIZE;
    /** Posizione tile tombino nella stanza boss m1 (centro pavimento) */
    public static final int TOMBINO_COL = COL_TOTALI / 2;
    public static final int TOMBINO_RIG = RIG_TOTALI / 2 + 1;
    public static final int ALTEZZA_GIOCO   = RIG_TOTALI * TILE_SIZE;
    public static final int PG_SIZE         = 50;
    public static final int STANZA_BOSS     = 8;
    public static final int MONDI_STORIA_MAX = 5;

    // ── Input movimento ───────────────────────────────────────────────────────
    public boolean up, down, left, right;
    public boolean shootUp, shootDown, shootLeft, shootRight;
    public boolean meleeAttivo   = false;   // il giocatore ha premuto Z
    public boolean meleeUnlocked = false;   // sbloccato dopo aver battuto lo shopkeeper
    public int     meleeDannoBonus = 0;     // bonus danno melee accumulato
    public boolean meleeUnlockedDaArdua = false; // melee dato dalla stanza ardua (non shop)

    // Ricompensa stanza ardua
    public boolean ardua_completed = false; // completata in questo mondo
    public int     stanzaConPortaArdua = 4; // stanza in cui appare la porta sud (random 3-6 per mondo)
    public String  arduaRicompensaMsg = ""; // messaggio ricompensa da mostrare
    public int     arduaRicompensaTimer = 0;
    // Malus attivi durante la stanza ardua (ripristinati all'uscita)
    public int   arduaMalusDanno    = 0;   // riduzione danno temporanea
    public float arduaMalusVelocita = 0f;  // riduzione velocita temporanea
    public int   arduaMalusFireRate = 0;   // aumento cooldown sparo temporaneo
    public static final int STANZA_ARDUA = 7;

    // Direzione attuale del giocatore (aggiornata dal GameLoop ogni frame)
    public int facingX = 0, facingY = 1;    // default: giu

    // Stato animazione attacco melee
    public int  meleeCooldown  = 0;
    public int  meleeTimer     = 0;         // frame rimasti di animazione attiva
    public int  meleeHitX, meleeHitY;       // centro della hitbox melee (pixel)
    public int  meleeNomeTimer = 0;         // frame rimasti per mostrare il nome dell'attacco
    public static final int MELEE_NOME_DURATA = 55; // frame di visibilità del nome
    public static final int MELEE_DELAY     = 45;  // frame tra un attacco e l'altro
    public static final int MELEE_DURATION  = 12;  // frame di hitbox attiva

    /** Nome dell'attacco melee in base al personaggio selezionato. */
    public String getMeleeNome() {
        return switch (indicePersonaggioSelezionato) {
            case 0 -> Lang.t("melee.0");
            case 1 -> Lang.t("melee.1");
            case 2 -> Lang.t("melee.2");
            case 3 -> Lang.t("melee.3");
            case 4 -> Lang.t("melee.4");
            default -> Lang.t("melee.default");
        };
    }
    public int  cooldownSparo = 0;
    public static final int SPARO_DELAY = 12;
    public int sparoDelayRiduzione = 0; // riduzione al cooldown sparo (fire rate up)

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
        notaRaccolta   = false;
        mostraNota     = false;
        ufficioDialogoAvviato = false;
        meleeTimer     = 0;
        meleeNomeTimer = 0;
        meleeCooldown  = 0;
        meleeAttivo    = false;
        ardua_completed      = false;
        stanzaConPortaArdua  = 4; // verrà randomizzata da RoomManager
        arduaRicompensaMsg   = "";
        arduaRicompensaTimer = 0;
        arduaMalusDanno      = 0;
        arduaMalusVelocita   = 0f;
        arduaMalusFireRate   = 0;
        facingX = 0; facingY = 1;
        cooldownSparo = 0;
        sparoDelayRiduzione = 0;
    }

    /**
     * Reset completo ad inizio nuova partita.
     * La logica del mondo/stanza è in RoomManager; qui si azzerano solo
     * i contatori di stato e le statistiche base.
     */
    public void resetTotale(DatiPersonaggio pg) {
        applicaDatiPersonaggio(pg);
        nomePGCorrente       = pg != null ? pg.nome.toUpperCase() : "BELLGERD";
        mondoAttuale         = 1;
        stanzaNelMondo       = 1;
        indiceStanzaMemoria  = 0;
        monete               = 0;
        bossSpawnato         = false;
        bossSconfitto        = false;
        shopSbloccato        = false;
        sistemaPersonaggi.resetCompleto(); // nuova partita: azzera anche sblocchi
        tetris             = null;
        powerUpCasa        = "NESSUNO";
        stanzaCasaVisitata = false;
        mostraDialogoCasa  = false;
        dialogoNarrazione.pulisci();
        dialogoShopkeeperNarrazioneAvviata = false;
        burnAttivo = false; burnTimer = 0; burnTick = 0;
        freezeAttivo = false; freezeTimer = 0;
        slowAttivo = false; slowTimer = 0;
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
        shopSbloccato                = false;
        sistemaPersonaggi.resetSoloCombo(); // mantiene i mondi sconfitti (sblocchi)
        dialogoShopkeeper.reset();
        statoGioco                   = StatoGioco.MENU;
        tombinoVisibile              = false;
        inBossRush                   = false;
        bossRushIndice               = 2;
        bossRushCompletata           = false;
        bossRushSconfitti            = 0;
        bossRushPowerUp1             = 0;
        bossRushPowerUp2             = 0;
        bossRushPowerUp3             = 0;
        bossRushSceltaPowerUp        = false;
        bossRushOpzioneScelta        = 0;
        resetGiocatore();
    }

    // ── Interfaccia callback ──────────────────────────────────────────────────
    public interface GameEventListener {
        void onGameOver();
        void onVittoria();
    }
}
