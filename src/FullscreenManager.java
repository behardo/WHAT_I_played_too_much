import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * FullscreenManager.java
 *
 * Gestisce fullscreen (F11) e ridimensionamento.
 * Le coordinate mouse sono sempre relative al pannello (componente del listener),
 * quindi scalaCoordinate restituisce le coordinate invariate.
 */
public class FullscreenManager {

    private final JFrame         finestra;
    private final JPanel         pannello;
    private final GraphicsDevice device;
    private boolean isF11Fullscreen = false;

    public FullscreenManager(JFrame finestra, JPanel pannello, GraphicsDevice device) {
        this.finestra = finestra;
        this.pannello = pannello;
        this.device   = device;
        collegaListenerFinestra();
    }

    // UIManager non serve più qui — ricalcolaBottoni viene chiamato ogni frame da render()
    public void setUIManager(UIManager ui) { /* no-op: render() aggiorna ogni frame */ }

    private void collegaListenerFinestra() {
        if (finestra == null) return;

        finestra.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
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
                pannello.revalidate();
                pannello.repaint();
                pannello.requestFocusInWindow();
            }
        });
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

        // Due invokeLater annidati: il primo aspetta che la finestra sia visibile,
        // il secondo aspetta che il layout Swing sia completamente definito.
        // Questo garantisce che requestFocus e repaint avvengano con dimensioni corrette.
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            pannello.requestFocus();
            pannello.repaint();
        }));
    }

    public boolean isF11Fullscreen() { return isF11Fullscreen; }

    /**
     * Le coordinate mouse da MouseEvent.getX()/getY() sono già relative
     * al componente su cui è registrato il listener (il JPanel).
     * Non serve nessuna conversione.
     */
    public Point scalaCoordinate(int mouseX, int mouseY) {
        return new Point(mouseX, mouseY);
    }

    /** Non usato — mantenuto per compatibilità. */
    public void aggiornaParametri(double scale, int offX, int offY) { }
}