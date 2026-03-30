package entity;

import core.GameState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * ShopkeeperNemico.java
 * Lo shopkeeper arrabbiato: appare quando il giocatore lo attacca.
 */
public class ShopkeeperNemico extends Nemico {

    private static final int VITA      = 30;
    private static final int TAGLIA    = 72;
    private static final float VEL     = 3.2f;
    private final BufferedImage imgSk;

    public ShopkeeperNemico(float pixelX, float pixelY, BufferedImage imgShopkeeper) {
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
        float dx   = pgX - x, dy = pgY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 1f) { x += (dx / dist) * velocita; y += (dy / dist) * velocita; }
        clampBordi();
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage ignoredSprite) {
        if (morto) return;
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(Color.RED);
        g2.drawString("NON SCAPPARE!!!", (int) x - 5, (int) y - 4);
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, size, size); }
}
