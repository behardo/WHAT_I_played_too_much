import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * FullscreenManager.java
 *
 * Gestisce due modalità di fullscreen:
 *
 *  1. F11  → fullscreen hardware vero (device.setFullScreenWindow).
 *             Usa stretch: scaleX e scaleY indipendenti per riempire lo schermo.
 *
 *  2. Bottone massimizza della finestra (o trascina i bordi)  → si comporta
 *             esattamente come F11 in termini di rendering (stretch),
 *             ma senza rimuovere la barra del titolo.
 *
 * Il rendering stretch significa che non ci sono bande nere: il contenuto
 * logico (LARGHEZZA_GIOCO × ALTEZZA_GIOCO) viene deformato per riempire
 * qualsiasi dimensione del pannello.
 *
 * Le coordinate mouse vengono scalate con gli stessi fattori X/Y indipendenti
 * così i click sono sempre precisi indipendentemente dalla dimensione finestra.
 */
public class FullscreenManager {

    private final JFrame         finestra;
    private final JPanel         pannello;
    private final GraphicsDevice device;

    // True solo quando attivo via F11 (fullscreen hardware senza barra titolo)
    private boolean isF11Fullscreen = false;

    // ─────────────────────────────────────────────────────────────────────────
    public FullscreenManager(JFrame finestra, JPanel pannello, GraphicsDevice device) {
        this.finestra = finestra;
        this.pannello = pannello;
        this.device   = device;
        collegaListenerFinestra();
    }

    // ── Collegamento listener finestra ────────────────────────────────────────

    /**
     * Collega i listener alla JFrame per reagire a:
     *  - Ridimensionamento (trascina bordi)
     *  - Massimizzazione/ripristino (bottone quadratino)
     *
     * Non fa nulla di speciale: il rendering stretch si adatta automaticamente
     * perché usa sempre le dimensioni correnti del pannello.
     * Il listener serve solo per forzare un repaint immediato.
     */
    private void collegaListenerFinestra() {
        if (finestra == null) return;

        // Ridimensionamento continuo mentre si trascina il bordo
        finestra.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pannello.revalidate();
                pannello.repaint();
            }
        });

        // Massimizzazione / ripristino dal bottone della finestra
        finestra.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                int newState = e.getNewState();
                boolean massimizzata = (newState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;

                // Quando si ripristina dalla massimizzazione, repack alla
                // dimensione preferita originale
                if (!massimizzata && !isF11Fullscreen) {
                    finestra.setSize(
                            GameState.LARGHEZZA_GIOCO + finestra.getInsets().left + finestra.getInsets().right,
                            GameState.ALTEZZA_GIOCO   + finestra.getInsets().top  + finestra.getInsets().bottom
                    );
                    finestra.setLocationRelativeTo(null);
                }

                pannello.revalidate();
                pannello.repaint();
                pannello.requestFocusInWindow();
            }
        });
    }

    // ── Toggle F11 ────────────────────────────────────────────────────────────

    /**
     * Attiva/disattiva il fullscreen hardware via F11.
     * In fullscreen hardware la barra del titolo viene rimossa.
     */
    public void toggle() {
        isF11Fullscreen = !isF11Fullscreen;
        finestra.dispose();

        if (isF11Fullscreen) {
            finestra.setUndecorated(true);
            device.setFullScreenWindow(finestra);
        } else {
            finestra.setUndecorated(false);
            device.setFullScreenWindow(null);
            // Ripristina dimensione base
            finestra.setPreferredSize(
                    new Dimension(GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO));
            finestra.pack();
            finestra.setLocationRelativeTo(null);
        }

        finestra.setVisible(true);
        pannello.requestFocusInWindow();
    }

    public boolean isF11Fullscreen() { return isF11Fullscreen; }

    // ── Scaling coordinate mouse ──────────────────────────────────────────────

    /**
     * Converte le coordinate reali del mouse in coordinate logiche di gioco.
     *
     * Con lo stretch, scaleX e scaleY sono indipendenti:
     *   logX = mouseX / scaleX
     *   logY = mouseY / scaleY
     *
     * Non c'è offset perché il contenuto riempie sempre tutto il pannello.
     */
    public Point scalaCoordinate(int mouseX, int mouseY) {
        double scaleX = (double) pannello.getWidth()  / GameState.LARGHEZZA_GIOCO;
        double scaleY = (double) pannello.getHeight() / GameState.ALTEZZA_GIOCO;

        // Protezione divisione per zero (può accadere all'avvio)
        if (scaleX == 0) scaleX = 1;
        if (scaleY == 0) scaleY = 1;

        int logX = (int) (mouseX / scaleX);
        int logY = (int) (mouseY / scaleY);
        return new Point(logX, logY);
    }

    /**
     * Restituisce i parametri di scala per il rendering.
     *
     * Con lo stretch:
     *  - scaleX = pannelloW / LARGHEZZA_GIOCO
     *  - scaleY = pannelloH / ALTEZZA_GIOCO
     *  - offsetX = 0, offsetY = 0  (nessuna banda nera)
     *
     * @return double[] { scaleX, scaleY, 0.0, 0.0, 0.0 }
     *         (i campi scale/offsetX/offsetY non sono più usati, mantenuti
     *          per compatibilità con la firma originale)
     */
    public double[] getScaleParams(int panelWidth, int panelHeight) {
        double scaleX = (panelWidth  > 0) ? (double) panelWidth  / GameState.LARGHEZZA_GIOCO : 1.0;
        double scaleY = (panelHeight > 0) ? (double) panelHeight / GameState.ALTEZZA_GIOCO   : 1.0;
        // [scaleX, scaleY, scaleUniforme(non usata), offsetX, offsetY]
        return new double[]{ scaleX, scaleY, Math.min(scaleX, scaleY), 0.0, 0.0 };
    }
}