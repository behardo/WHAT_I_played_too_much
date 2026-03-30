package core;

import render.RenderEngine;
import resource.ResourceLoader;
import room.RoomManager;
import ui.FullscreenManager;
import ui.InputHandler;
import ui.UIManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * WhatIvePlayedTooMuch.java  ←  Entry point + collante
 *
 * Questo file ora fa SOLO tre cose:
 *   1. Costruisce e collega tutte le sottoclassi
 *   2. Implementa ActionListener per il game loop (Timer a 16ms)
 *   3. Implementa paintComponent() delegando a RenderEngine
 *
 * Tutta la logica è nelle classi dedicate:
 *   - GameState       → dati e stato
 *   - ResourceLoader  → caricamento immagini
 *   - UIManager       → layout menu e pulsanti
 *   - RoomManager     → generazione e memoria stanze
 *   - InputHandler    → tastiera e mouse
 *   - FullscreenManager → toggle fullscreen e scaling
 *   - GameLoop        → aggiornamento frame (movimento, collisioni, ecc.)
 *   - RenderEngine    → tutto il disegno
 */
public class WhatIvePlayedTooMuch extends JPanel implements ActionListener {

    // ── Dipendenze ────────────────────────────────────────────────────────────
    private final GameState         state;
    private final ResourceLoader    res;
    UIManager ui;
    private final RoomManager       roomMgr;
    private final FullscreenManager fullscreen;
    private final InputHandler      input;
    private final GameLoop          gameLoop;
    private final RenderEngine      renderer;

    // ── Finestra statica (per fullscreen) ─────────────────────────────────────
    private static JFrame         finestra;
    private static GraphicsDevice device;

    // ─────────────────────────────────────────────────────────────────────────
    public WhatIvePlayedTooMuch() {

        // 1. Dati fondamentali
        state      = new GameState();
        res        = new ResourceLoader(this);
        state.audio.stampaStatoCaricamento(); // debug: mostra quali clip sono caricate

        // 2. UI e layout
        ui         = new UIManager(res);

        // 3. Logica stanze
        roomMgr    = new RoomManager(state, res);

        // 4. Fullscreen — finestra è già impostata nel main() prima di new WhatIvePlayedTooMuch()
        fullscreen = new FullscreenManager(finestra, this, device);

        // 5. Input (dipende da state, ui, roomMgr, fullscreen)
        input      = new InputHandler(state, ui, roomMgr, fullscreen, res);

        // 6. Game loop (dipende da state, roomMgr)
        gameLoop   = new GameLoop(state, roomMgr, ui);
        gameLoop.setResourceLoader(res);
        gameLoop.setPungoImage(res.imgPugno);
        gameLoop.setShopkeeperImage(res.imgShopkeeper);
        gameLoop.setShopkeeperNemicoImage(res.imgShopkeeperNemico);
        gameLoop.setDropImages(res.imgCura, res.imgMoneta);

        // 7. Renderer (dipende da tutti i precedenti)
        renderer   = new RenderEngine(state, res, roomMgr, ui, fullscreen);
        renderer.setPugniAttivi(gameLoop.pugniAttivi);
        renderer.setProiettiliCannone(gameLoop.proiettiliCannone);

        // 8. Callback eventi: GameLoop → RoomManager → GameState già collegati
        //    Aggiunge listener per vittoria storia e game over se necessario
        state.setEventListener(new GameState.GameEventListener() {
            @Override public void onGameOver() {
                System.out.println("[Game] GAME OVER al mondo " + state.mondoAttuale
                        + ", stanza " + state.stanzaNelMondo);
            }
            @Override public void onVittoria() {
                System.out.println("[Game] VITTORIA!");
            }
        });

        roomMgr.setEventListener(new RoomManager.RoomEventListener() {
            @Override public void onCambioMondo(int nuovoMondo) {
                System.out.println("[Game] Cambio mondo → " + nuovoMondo);
            }
            @Override public void onVittoriaStoria() {
                System.out.println("[Game] Storia completata!");
            }
        });

        // 9. Setup pannello
        // NON impostiamo una preferredSize fissa sul pannello: la finestra è resizable
        // e il rendering stretch si adatta a qualsiasi dimensione.
        // La dimensione preferita iniziale è gestita da main() sulla JFrame.
        setFocusable(true);
        addKeyListener(input.getKeyAdapter());
        addMouseListener(input.getMouseAdapter());
        addMouseMotionListener(input.getMouseAdapter());
        addMouseWheelListener(input.getMouseAdapter());

        // 10. Avvia game loop a 60 fps
        new Timer(16, this).start();
    }

    /** Accesso al UIManager per aggiornamenti layout esterni (già dichiarato sopra). */

    /** Forza ricalcolo layout bottoni con dimensioni reali. */
    public void forceLayoutUpdate(int w, int h) {
        if (ui != null) ui.ricalcolaBottoni(w, h);
    }

    // ── Game loop ─────────────────────────────────────────────────────────────

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state.statoGioco == GameState.StatoGioco.GIOCO
                || state.statoGioco == GameState.StatoGioco.BOSS_RUSH) {
            gameLoop.tick();
        } else if (state.statoGioco == GameState.StatoGioco.TETRIS
                && state.tetris != null) {
            state.tetris.update();
            // Auto-avanza se il tetris è finito (tempo scaduto)
            if (state.tetris.completato && !state.tetris.gameOver) {
                // aspetta input giocatore per ENTER — non avanza automaticamente
            }
        }
        repaint();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render((Graphics2D) g, getWidth(), getHeight());
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Usa invokeLater per sicurezza thread Swing
        SwingUtilities.invokeLater(() -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            device   = ge.getDefaultScreenDevice();

            finestra = new JFrame("WHAT: I'VE PLAYED TOO MUCH");
            finestra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Ridimensionabile liberamente — il rendering si adatta in stretch
            finestra.setResizable(true);

            WhatIvePlayedTooMuch gioco = new WhatIvePlayedTooMuch();
            finestra.add(gioco);

            // Dimensione iniziale = dimensione logica di gioco
            finestra.setPreferredSize(
                    new Dimension(GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO));

            // Dimensione minima: metà della logica (evita finestre troppo piccole)
            finestra.setMinimumSize(
                    new Dimension(GameState.LARGHEZZA_GIOCO / 2, GameState.ALTEZZA_GIOCO / 2));

            finestra.pack();
            finestra.setLocationRelativeTo(null);
            finestra.setVisible(true);
            gioco.requestFocusInWindow();
            // Ricalcola layout dopo che la finestra ha dimensioni reali
            javax.swing.SwingUtilities.invokeLater(() -> {
                int pw = gioco.getWidth();
                int ph = gioco.getHeight();
                if (pw > 0 && ph > 0) gioco.forceLayoutUpdate(pw, ph);
            });
        });
    }
}
