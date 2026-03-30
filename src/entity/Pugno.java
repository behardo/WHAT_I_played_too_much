package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Pugno {
    public float x, y;
    private float velocitaX, velocitaY;
    private float velocitaMax = 10.0f;
    private int size = 30;
    private int danno;
    private int durataFrame = 15;
    private int frameVissuti = 0;
    public boolean daRimuovere = false;
    private BufferedImage imgPugno;

    public Pugno(float startX, float startY, int direzioneX, int direzioneY, BufferedImage img, int dannoPassato) {
        this.x = startX + 10;
        this.y = startY + 10;
        this.imgPugno = img;
        this.danno = dannoPassato;
        this.velocitaX = direzioneX * velocitaMax;
        this.velocitaY = direzioneY * velocitaMax;
        if (direzioneX != 0 && direzioneY != 0) {
            this.velocitaX /= 1.414f;
            this.velocitaY /= 1.414f;
        }
    }

    public void update() {
        x += velocitaX;
        y += velocitaY;
        frameVissuti++;
        if (frameVissuti >= durataFrame) {
            daRimuovere = true;
        }
    }

    public void draw(Graphics2D g2) {
        if (imgPugno != null) {
            g2.drawImage(imgPugno, (int)x, (int)y, size, size, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, size, size);
    }

    public int getDanno() {
        return danno;
    }
}
