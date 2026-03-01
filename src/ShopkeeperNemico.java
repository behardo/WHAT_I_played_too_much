import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * ShopkeeperNemico.java
 * Lo shopkeeper arrabbiato: appare quando il giocatore lo attacca.
 *
 * - Vita: 30
 * - Velocità: molto alta (si vendica)
 * - Drop: 20 monete (già dati al momento dell'attacco)
 * - Visivamente: lo sprite shopkeeper con tinta rossa e occhi arrabbiati
 * - Crea direttamente dalla posizione pixel dello shopkeeper (non da tile)
 */
public class ShopkeeperNemico extends Nemico {

    private static final int VITA      = 30;
    private static final int TAGLIA    = 72;
    private static final float VEL     = 3.2f;
    private final BufferedImage imgSk;

    // Costruttore da coordinate pixel (non tile)
    public ShopkeeperNemico(float pixelX, float pixelY, BufferedImage imgShopkeeper) {
        // Usiamo il costruttore tile con 0,0 poi sovrascriviamo x,y
        super(0, 0, GameState.TILE_SIZE, VITA);
        this.x          = pixelX;
        this.y          = pixelY;
        this.size       = TAGLIA;
        this.dimensione = TAGLIA;
        this.velocita   = VEL;
        this.imgSk      = imgShopkeeper;
    }

    @Override
    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;
        // Insegue diretto senza separazione: è furioso
        float dx   = pgX - x, dy = pgY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 1f) { x += (dx / dist) * velocita; y += (dy / dist) * velocita; }
        clampBordi();
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage ignoredSprite) {
        if (morto) return;

        // Sprite con tinta rossa
        if (imgSk != null) {
            g2.drawImage(imgSk, (int) x, (int) y, size, size, null);
            g2.setColor(new Color(220, 0, 0, 100));
            g2.fillRect((int) x, (int) y, size, size);
        } else {
            g2.setColor(new Color(180, 0, 0));
            g2.fillRect((int) x, (int) y, size, size);
        }

        // Etichetta "TRADITORE!" sopra
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(Color.RED);
        g2.drawString("TRADITORE!", (int) x - 5, (int) y - 4);
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, size, size); }
}