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
    private final UIManager         ui;
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

        // 2. UI e layout
        ui         = new UIManager(res);

        // 3. Logica stanze
        roomMgr    = new RoomManager(state, res);

        // 4. Fullscreen
        fullscreen = new FullscreenManager(finestra, this, device);

        // 5. Input (dipende da state, ui, roomMgr, fullscreen)
        input      = new InputHandler(state, ui, roomMgr, fullscreen);

        // 6. Game loop (dipende da state, roomMgr)
        gameLoop   = new GameLoop(state, roomMgr);
        gameLoop.setPungoImage(res.imgPugno);
        gameLoop.setDropImages(res.imgCura, res.imgMoneta);

        // 7. Renderer (dipende da tutti i precedenti)
        renderer   = new RenderEngine(state, res, roomMgr, ui, fullscreen);
        renderer.setPugniAttivi(gameLoop.pugniAttivi);

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
        setPreferredSize(new Dimension(GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO));
        setFocusable(true);
        addKeyListener(input.getKeyAdapter());
        addMouseListener(input.getMouseAdapter());
        addMouseMotionListener(input.getMouseAdapter());

        // 10. Avvia game loop a 60 fps
        new Timer(16, this).start();
    }

    // ── Game loop ─────────────────────────────────────────────────────────────

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state.statoGioco == GameState.StatoGioco.GIOCO) {
            gameLoop.tick();
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
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        device   = ge.getDefaultScreenDevice();
        finestra = new JFrame("WHAT: I'VE PLAYED TOO MUCH");

        WhatIvePlayedTooMuch gioco = new WhatIvePlayedTooMuch();
        finestra.add(gioco);
        finestra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        finestra.setResizable(false);
        finestra.pack();
        finestra.setLocationRelativeTo(null);
        finestra.setVisible(true);
        gioco.requestFocusInWindow();
    }
}