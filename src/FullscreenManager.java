import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * FullscreenManager.java
 *
 * Gestisce il ridimensionamento e il fullscreen mantenendo il rapporto
 * d'aspetto originale (1088×448) senza deformare il contenuto.
 *
 * Modalità di scaling: LETTERBOX
 *  - Si calcola il fattore di scala uniforme (il minore tra scaleX e scaleY)
 *  - Il contenuto logico viene centrato nel pannello
 *  - Le aree non coperte vengono riempite di nero
 *
 * F11 → fullscreen hardware (barra titolo rimossa)
 * Bordi/massimizza → finestra si ridimensiona, contenuto sempre proporzionato
 */
public class FullscreenManager {

    private final JFrame         finestra;
    private final JPanel         pannello;
    private final GraphicsDevice device;

    private boolean isF11Fullscreen = false;

    // Parametri letterbox calcolati all'ultimo render (letti da scalaCoordinate)
    private double lastScale   = 1.0;
    private int    lastOffsetX = 0;
    private int    lastOffsetY = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public FullscreenManager(JFrame finestra, JPanel pannello, GraphicsDevice device) {
        this.finestra = finestra;
        this.pannello = pannello;
        this.device   = device;
        collegaListenerFinestra();
    }

    // ── Listener finestra ─────────────────────────────────────────────────────

    private void collegaListenerFinestra() {
        if (finestra == null) return;

        finestra.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pannello.revalidate();
                pannello.repaint();
            }
        });

        finestra.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                boolean massimizzata = (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
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

    public void toggle() {
        isF11Fullscreen = !isF11Fullscreen;
        finestra.dispose();

        if (isF11Fullscreen) {
            finestra.setUndecorated(true);
            device.setFullScreenWindow(finestra);
        } else {
            finestra.setUndecorated(false);
            device.setFullScreenWindow(null);
            finestra.setPreferredSize(
                    new Dimension(GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO));
            finestra.pack();
            finestra.setLocationRelativeTo(null);
        }

        finestra.setVisible(true);
        pannello.requestFocusInWindow();
    }

    public boolean isF11Fullscreen() { return isF11Fullscreen; }

    // ── Calcolo letterbox ─────────────────────────────────────────────────────

    /**
     * Calcola i parametri letterbox per il pannello dato.
     * Scala uniforme: il contenuto logico viene centrato, bande nere ai bordi.
     *
     * @return double[] { scale, offsetX, offsetY }
     */
    public double[] getScaleParams(int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            lastScale = 1.0; lastOffsetX = 0; lastOffsetY = 0;
            return new double[]{ 1.0, 0.0, 0.0 };
        }

        double scaleX = (double) panelWidth  / GameState.LARGHEZZA_GIOCO;
        double scaleY = (double) panelHeight / GameState.ALTEZZA_GIOCO;

        // Scala uniforme = il minore dei due (no deformazione)
        double scale = Math.min(scaleX, scaleY);

        // Contenuto centrato: calcola offset delle bande nere
        int contentW = (int)(GameState.LARGHEZZA_GIOCO * scale);
        int contentH = (int)(GameState.ALTEZZA_GIOCO   * scale);
        int offsetX  = (panelWidth  - contentW) / 2;
        int offsetY  = (panelHeight - contentH) / 2;

        lastScale   = scale;
        lastOffsetX = offsetX;
        lastOffsetY = offsetY;

        return new double[]{ scale, offsetX, offsetY };
    }

    // ── Scaling coordinate mouse ──────────────────────────────────────────────

    /**
     * Converte le coordinate reali del mouse in coordinate logiche di gioco,
     * tenendo conto delle bande nere laterbox.
     *
     *   logX = (mouseX - offsetX) / scale
     *   logY = (mouseY - offsetY) / scale
     *
     * Clampato a [0, dimensione logica] per gestire click nelle bande nere.
     */
    public Point scalaCoordinate(int mouseX, int mouseY) {
        double scale = lastScale > 0 ? lastScale : 1.0;

        int logX = (int)((mouseX - lastOffsetX) / scale);
        int logY = (int)((mouseY - lastOffsetY) / scale);

        // Clamp: i click nelle bande nere restano ai bordi del gioco
        logX = Math.max(0, Math.min(logX, GameState.LARGHEZZA_GIOCO - 1));
        logY = Math.max(0, Math.min(logY, GameState.ALTEZZA_GIOCO   - 1));

        return new Point(logX, logY);
    }
}