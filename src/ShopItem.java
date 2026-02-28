import java.awt.*;
import java.awt.image.BufferedImage;

// --- SHOP: Classe per gli oggetti in vendita ---
public class ShopItem {
    public float x, y;
    private int size = 45;
    private int costo;
    private String tipo; // "CURA", "VELOCITA", "DANNO"
    private BufferedImage imgItem;
    public boolean acquistato = false;

    public ShopItem(int grigliaX, int grigliaY, int tileSize, String tipo, int costo, BufferedImage img) {
        // Centriamo l'oggetto nella tile
        this.x = grigliaX * tileSize + (tileSize/2 - size/2);
        this.y = grigliaY * tileSize + (tileSize/2 - size/2);
        this.tipo = tipo;
        this.costo = costo;
        this.imgItem = img;
    }

    public void draw(Graphics2D g2) {
        if (acquistato) return; // Non disegnare se acquistato

        if (imgItem != null) {
            g2.drawImage(imgItem, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato
            g2.setColor(tipo.equals("CURA") ? Color.RED : Color.CYAN);
            g2.fillOval((int)x, (int)y, size, size);
        }

        // --- SHOP: Disegno del prezzo ---
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(costo + " MONETE", (int)x, (int)y - 10);
    }

    // Hitbox circolare per l'acquisto
    public boolean controllaAcquisto(float px, float py, int pSize, int moneteGiocatore) {
        if (acquistato || moneteGiocatore < costo) return false;

        float dx = (x + size/2) - (px + pSize/2);
        float dy = (y + size/2) - (py + pSize/2);
        float distanzaSq = dx*dx + dy*dy;
        return distanzaSq < (size/2 + pSize/2)*(size/2 + pSize/2);
    }

    public int getCosto() { return costo; }
    public String getTipo() { return tipo; }
    public void setAcquistato() { acquistato = true; }
}