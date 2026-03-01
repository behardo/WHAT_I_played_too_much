import javax.swing.*;
import java.awt.*;

/**
 * FullscreenManager.java
 * Incapsula la logica di toggle fullscreen e la conversione
 * delle coordinate mouse dallo spazio schermo allo spazio logico di gioco.
 *
 * Disaccoppia InputHandler dalla finestra concreta.
 */
public class FullscreenManager {

    private final JFrame          finestra;
    private final JPanel          pannello;
    private final GraphicsDevice  device;
    private boolean               isFullscreen = false;

    public FullscreenManager(JFrame finestra, JPanel pannello, GraphicsDevice device) {
        this.finestra  = finestra;
        this.pannello  = pannello;
        this.device    = device;
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    public void toggle() {
        isFullscreen = !isFullscreen;
        finestra.dispose();

        if (isFullscreen) {
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

    public boolean isFullscreen() { return isFullscreen; }

    // ── Scaling coordinate ────────────────────────────────────────────────────

    /**
     * Converte le coordinate reali del mouse nelle coordinate logiche di gioco,
     * tenendo conto del letterboxing fullscreen.
     */
    public Point scalaCoordinate(int mouseX, int mouseY) {
        double scaleX  = (double) pannello.getWidth()  / GameState.LARGHEZZA_GIOCO;
        double scaleY  = (double) pannello.getHeight() / GameState.ALTEZZA_GIOCO;
        double scale   = Math.min(scaleX, scaleY);
        int    offsetX = (int) ((pannello.getWidth()  - GameState.LARGHEZZA_GIOCO * scale) / 2);
        int    offsetY = (int) ((pannello.getHeight() - GameState.ALTEZZA_GIOCO   * scale) / 2);

        int logX = (int) ((mouseX - offsetX) / scale);
        int logY = (int) ((mouseY - offsetY) / scale);
        return new Point(logX, logY);
    }

    /**
     * Calcola e restituisce i parametri di scala e offset attuali.
     * Usato da RenderEngine per applicare la stessa trasformazione al disegno.
     * @return array [scaleX, scaleY, scale, offsetX, offsetY]  (double per i primi 3, int per gli ultimi 2)
     */
    public double[] getScaleParams(int panelWidth, int panelHeight) {
        double scaleX  = (double) panelWidth  / GameState.LARGHEZZA_GIOCO;
        double scaleY  = (double) panelHeight / GameState.ALTEZZA_GIOCO;
        double scale   = Math.min(scaleX, scaleY);
        double offsetX = (panelWidth  - GameState.LARGHEZZA_GIOCO * scale) / 2.0;
        double offsetY = (panelHeight - GameState.ALTEZZA_GIOCO   * scale) / 2.0;
        return new double[]{scaleX, scaleY, scale, offsetX, offsetY};
    }
}