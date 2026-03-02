import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * FullscreenManager.java
 *
 * Gestisce fullscreen e ridimensionamento.
 * Il rendering usa SEMPRE le dimensioni reali del pannello —
 * niente stretch, niente bande nere, niente deformazione.
 *
 * I menu e il gioco si ridisegnano in coordinate proporzionali
 * alla dimensione corrente del pannello.
 */
public class FullscreenManager {

    private final JFrame         finestra;
    private final JPanel         pannello;
    private final GraphicsDevice device;
    private boolean isF11Fullscreen = false;

    // UIManager da aggiornare quando cambia la dimensione
    private UIManager uiManager;

    public FullscreenManager(JFrame finestra, JPanel pannello, GraphicsDevice device) {
        this.finestra = finestra;
        this.pannello = pannello;
        this.device   = device;
        collegaListenerFinestra();
    }

    public void setUIManager(UIManager ui) {
        this.uiManager = ui;
    }

    private void collegaListenerFinestra() {
        if (finestra == null) return;

        finestra.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                aggiornaUI();
                pannello.repaint();
            }
        });

        finestra.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                boolean mass = (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
                if (!mass && !isF11Fullscreen) {
                    finestra.setSize(
                            GameState.LARGHEZZA_GIOCO + finestra.getInsets().left + finestra.getInsets().right,
                            GameState.ALTEZZA_GIOCO   + finestra.getInsets().top  + finestra.getInsets().bottom
                    );
                    finestra.setLocationRelativeTo(null);
                }
                aggiornaUI();
                pannello.revalidate();
                pannello.repaint();
                pannello.requestFocusInWindow();
            }
        });
    }

    private void aggiornaUI() {
        if (uiManager != null) {
            int w = pannello.getWidth()  > 0 ? pannello.getWidth()  : GameState.LARGHEZZA_GIOCO;
            int h = pannello.getHeight() > 0 ? pannello.getHeight() : GameState.ALTEZZA_GIOCO;
            uiManager.ricalcolaBottoni(w, h);
        }
    }

    public void toggle() {
        isF11Fullscreen = !isF11Fullscreen;
        finestra.dispose();
        if (isF11Fullscreen) {
            finestra.setUndecorated(true);
            device.setFullScreenWindow(finestra);
        } else {
            finestra.setUndecorated(false);
            device.setFullScreenWindow(null);
            finestra.setPreferredSize(new Dimension(GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO));
            finestra.pack();
            finestra.setLocationRelativeTo(null);
        }
        finestra.setVisible(true);
        pannello.requestFocusInWindow();
        aggiornaUI();
    }

    public boolean isF11Fullscreen() { return isF11Fullscreen; }

    /**
     * Converte coordinate mouse reali in coordinate logiche proporzionali.
     * Con questo approccio non c'è scala fissa: le coordinate reali
     * corrispondono già alle coordinate di disegno.
     */
    public Point scalaCoordinate(int mouseX, int mouseY) {
        // Il rendering usa direttamente le dimensioni del pannello,
        // quindi le coordinate mouse sono già corrette
        return new Point(mouseX, mouseY);
    }

    /** Nessun parametro di scala: rendering diretto. */
    public void aggiornaParametri(double scale, int offX, int offY) { /* non usato */ }
}