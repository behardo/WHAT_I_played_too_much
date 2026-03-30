package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Shopkeeper {
    public float x, y;
    private int size = 80;
    private BufferedImage imgShopkeeper;

    public Shopkeeper(int grigliaX, int grigliaY, int tileSize, BufferedImage img) {
        this.x = grigliaX * tileSize + (tileSize / 2 - size / 2);
        this.y = grigliaY * tileSize + (tileSize / 2 - size / 2);
        this.imgShopkeeper = img;
    }

    public void draw(Graphics2D g2) {
        if (imgShopkeeper != null) {
            g2.drawImage(imgShopkeeper, (int) x, (int) y, size, size, null);
        } else {
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect((int) x, (int) y, size, size);
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle((int) x - 20, (int) y - 20, size + 40, size + 40);
    }

    public void attivaBattuta() { /* nessuna battuta */ }
    public float getX() { return x; }
    public float getY() { return y; }
}
