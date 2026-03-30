package game;

import java.util.Random;

/**
 * TetrisGame.java — logica Tetris autocontenuta.
 *
 * Griglia 10×20. Pezzi classici I,O,T,S,Z,L,J.
 * Il punteggio finale determina il power-up nella stanza Casa:
 *   < 500  → NESSUNO (solo una cura)
 *   500+   → CURA    (vita extra)
 *   1500+  → VELOCITA (velocità +2)
 *   3000+  → DANNO   (danno +2)
 *   6000+  → TUTTO   (cura + velocità + danno)
 */
public class TetrisGame {

    // ── Costanti griglia ──────────────────────────────────────────────────────
    public static final int COLS = 10;
    public static final int ROWS = 20;

    // ── Pezzi (coordinate relative al pivot, 4 rotazioni) ────────────────────
    private static final int[][][][] PEZZI = {
        // I
        { {{0,1},{1,1},{2,1},{3,1}}, {{2,0},{2,1},{2,2},{2,3}},
          {{0,2},{1,2},{2,2},{3,2}}, {{1,0},{1,1},{1,2},{1,3}} },
        // O
        { {{0,0},{1,0},{0,1},{1,1}}, {{0,0},{1,0},{0,1},{1,1}},
          {{0,0},{1,0},{0,1},{1,1}}, {{0,0},{1,0},{0,1},{1,1}} },
        // T
        { {{1,0},{0,1},{1,1},{2,1}}, {{1,0},{1,1},{2,1},{1,2}},
          {{0,1},{1,1},{2,1},{1,2}}, {{1,0},{0,1},{1,1},{1,2}} },
        // S
        { {{1,0},{2,0},{0,1},{1,1}}, {{1,0},{1,1},{2,1},{2,2}},
          {{1,1},{2,1},{0,2},{1,2}}, {{0,0},{0,1},{1,1},{1,2}} },
        // Z
        { {{0,0},{1,0},{1,1},{2,1}}, {{2,0},{1,1},{2,1},{1,2}},
          {{0,1},{1,1},{1,2},{2,2}}, {{1,0},{0,1},{1,1},{0,2}} },
        // L
        { {{2,0},{0,1},{1,1},{2,1}}, {{1,0},{1,1},{1,2},{2,2}},
          {{0,1},{1,1},{2,1},{0,2}}, {{0,0},{1,0},{1,1},{1,2}} },
        // J
        { {{0,0},{0,1},{1,1},{2,1}}, {{1,0},{2,0},{1,1},{1,2}},
          {{0,1},{1,1},{2,1},{2,2}}, {{1,0},{1,1},{0,2},{1,2}} }
    };

    // Colori per ogni pezzo (indice RGB packed)
    public static final int[] COLORI = {
        0x00CFFF, // I — azzurro
        0xFFD700, // O — giallo
        0xAA00FF, // T — viola
        0x00DD44, // S — verde
        0xFF2222, // Z — rosso
        0xFF8800, // L — arancio
        0x4488FF  // J — blu
    };

    // ── Stato griglia ─────────────────────────────────────────────────────────
    /** grid[rig][col] = 0 se vuota, altrimenti indice pezzo +1 */
    public final int[][] grid = new int[ROWS][COLS];

    // ── Pezzo corrente ────────────────────────────────────────────────────────
    public int tipoPezzo, tipoNext;
    public int rotazione = 0;
    public int pezzoX, pezzoY;

    // ── Stato partita ─────────────────────────────────────────────────────────
    public int  punteggio = 0;
    public int  livello   = 1;
    public int  righe     = 0;
    public boolean gameOver  = false;
    public boolean completato = false; // true dopo 2 minuti o 150 righe

    // ── Timing ────────────────────────────────────────────────────────────────
    private int  tickContatore = 0;
    private int  tickInterval  = 40; // frame per caduta automatica
    private long tempoInizio;
    public static final long DURATA_MS = 120_000; // 120 secondi
    public boolean dialogoCasaMostrato = false;  // true dopo aver mostrato il dialogo fine-tetris

    private final Random random = new Random();

    // ── Init ─────────────────────────────────────────────────────────────────

    public TetrisGame() {
        tempoInizio = System.currentTimeMillis();
        tipoNext    = random.nextInt(PEZZI.length);
        spawnProssimo();
    }

    private void spawnProssimo() {
        tipoPezzo = tipoNext;
        tipoNext  = random.nextInt(PEZZI.length);
        rotazione = 0;
        pezzoX    = COLS / 2 - 2;
        pezzoY    = 0;
        if (!puoStare(pezzoX, pezzoY, rotazione)) {
            gameOver   = true;
            completato = true;
        }
    }

    // ── Update (chiamato ogni frame dal GameLoop quando in stato TETRIS) ──────

    public void update() {
        if (gameOver || completato) return;

        // Controlla tempo
        if (System.currentTimeMillis() - tempoInizio >= DURATA_MS) {
            completato = true;
            return;
        }

        // Accelera col livello
        tickInterval = Math.max(6, 40 - (livello - 1) * 4);

        tickContatore++;
        if (tickContatore >= tickInterval) {
            tickContatore = 0;
            scendi();
        }
    }

    // ── Controlli ─────────────────────────────────────────────────────────────

    public void muoviSinistra() {
        if (!gameOver && puoStare(pezzoX - 1, pezzoY, rotazione)) pezzoX--;
    }

    public void muoviDestra() {
        if (!gameOver && puoStare(pezzoX + 1, pezzoY, rotazione)) pezzoX++;
    }

    public void ruota() {
        if (gameOver) return;
        int nuova = (rotazione + 1) % 4;
        if (puoStare(pezzoX, pezzoY, nuova)) {
            rotazione = nuova;
        } else if (puoStare(pezzoX + 1, pezzoY, nuova)) {
            pezzoX++; rotazione = nuova;
        } else if (puoStare(pezzoX - 1, pezzoY, nuova)) {
            pezzoX--; rotazione = nuova;
        }
    }

    public void scendiForzato() {
        if (!gameOver) scendi();
    }

    public void cadutaIstantanea() {
        if (gameOver) return;
        while (puoStare(pezzoX, pezzoY + 1, rotazione)) pezzoY++;
        blocca();
    }

    // ── Logica interna ────────────────────────────────────────────────────────

    private void scendi() {
        if (puoStare(pezzoX, pezzoY + 1, rotazione)) {
            pezzoY++;
        } else {
            blocca();
        }
    }

    private void blocca() {
        int[][] forme = PEZZI[tipoPezzo][rotazione];
        for (int[] c : forme) {
            int gx = pezzoX + c[0];
            int gy = pezzoY + c[1];
            if (gy >= 0 && gy < ROWS && gx >= 0 && gx < COLS)
                grid[gy][gx] = tipoPezzo + 1;
        }
        int cancellate = cancellaRighe();
        aggiungiPunteggio(cancellate);
        spawnProssimo();

        // 150 righe = fine anticipata
        if (righe >= 150) completato = true;
    }

    private int cancellaRighe() {
        int count = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean piena = true;
            for (int c = 0; c < COLS; c++) if (grid[r][c] == 0) { piena = false; break; }
            if (piena) {
                // Sposta tutto giù
                for (int rr = r; rr > 0; rr--)
                    System.arraycopy(grid[rr - 1], 0, grid[rr], 0, COLS);
                grid[0] = new int[COLS];
                r++; count++;
            }
        }
        righe += count;
        livello = 1 + righe / 10;
        return count;
    }

    private void aggiungiPunteggio(int cancellate) {
        int[] bonus = {0, 100, 300, 500, 800};
        if (cancellate > 0 && cancellate <= 4)
            punteggio += bonus[cancellate] * livello;
    }

    private boolean puoStare(int nx, int ny, int rot) {
        for (int[] c : PEZZI[tipoPezzo][rot]) {
            int gx = nx + c[0];
            int gy = ny + c[1];
            if (gx < 0 || gx >= COLS || gy >= ROWS) return false;
            if (gy >= 0 && grid[gy][gx] != 0) return false;
        }
        return true;
    }

    // ── Accessori ─────────────────────────────────────────────────────────────

    /** Celle del pezzo corrente in coordinate griglia assolute. */
    public int[][] getCelleCorrente() {
        int[][] forme = PEZZI[tipoPezzo][rotazione];
        int[][] abs   = new int[4][2];
        for (int i = 0; i < 4; i++) {
            abs[i][0] = pezzoX + forme[i][0];
            abs[i][1] = pezzoY + forme[i][1];
        }
        return abs;
    }

    /** Celle del pezzo successivo. */
    public int[][] getCelleNext() {
        int[][] forme = PEZZI[tipoNext][0];
        int[][] abs   = new int[4][2];
        for (int i = 0; i < 4; i++) {
            abs[i][0] = forme[i][0];
            abs[i][1] = forme[i][1];
        }
        return abs;
    }

    /** Secondi rimanenti (0 se finito). */
    public int secondiRimanenti() {
        long elapsed = System.currentTimeMillis() - tempoInizio;
        return (int) Math.max(0, (DURATA_MS - elapsed) / 1000);
    }

    /**
     * Power-up ottenuto in base al punteggio finale.
     * I premi sono cumulativi: raggiungendo una soglia alta si ottengono
     * anche tutti i premi delle soglie inferiori.
     *   500+  → CURA
     *   1500+ → VELOCITA  (+ CURA)
     *   3000+ → DANNO     (+ VELOCITA + CURA)
     *   5000+ → MELEE     (+ DANNO + VELOCITA + CURA)
     *   6000+ → TUTTO     (tutto quanto sopra)
     */
    public String getPowerUp() {
        if (punteggio >= 6000) return "TUTTO";
        if (punteggio >= 5000) return "MELEE";
        if (punteggio >= 3000) return "DANNO";
        if (punteggio >= 1500) return "VELOCITA";
        if (punteggio >= 500)  return "CURA";
        return "NESSUNO";
    }
}
