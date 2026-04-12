package entity;

import core.GameState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * ShopkeeperNemico — shopkeeper che attacca.
 * Si comporta come un mini-boss: barra vita HUD, attacchi melee con cooldown.
 */
public class ShopkeeperNemico extends Nemico {

    public  static final int   VITA_MAX  = 80;
    private static final int   TAGLIA    = 72;
    private static final float VEL       = 3.5f;

    // Melee
    private static final int   MELEE_RANGE    = 65;   // px
    private static final int   MELEE_DANNO    = 1;
    private static final int   MELEE_COOLDOWN = 60;   // frame — attacca ogni ~1s
    private int  meleeCooldown   = 0;
    private int  meleeFlashTimer = 0;   // frame di animazione attacco visibile
    private static final int MELEE_FLASH_DURATA = 18;

    // Sprite
    private final BufferedImage imgSk;      // sprite arrabbiato (passato dal RoomManager)

    public ShopkeeperNemico(float pixelX, float pixelY, BufferedImage imgShopkeeperNemico) {
        super(0, 0, GameState.TILE_SIZE, VITA_MAX);
        this.x          = pixelX;
        this.y          = pixelY;
        this.size       = TAGLIA;
        this.dimensione = TAGLIA;
        this.velocita   = VEL;
        this.vitaMax    = VITA_MAX;
        this.vitaAttuale = VITA_MAX;
        this.imgSk      = imgShopkeeperNemico;
    }

    @Override
    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;
        if (meleeCooldown > 0) meleeCooldown--;
        if (meleeFlashTimer > 0) meleeFlashTimer--;
        float dx   = pgX - x, dy = pgY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 1f) {
            x += (dx / dist) * velocita;
            y += (dy / dist) * velocita;
        }
        clampBordi();
    }

    @Override
    public void update(float pgX, float pgY, List<Nemico> altri, int[][] ostacoli) {
        update(pgX, pgY, altri);
    }

    /**
     * Controlla se lo shopkeeper è abbastanza vicino da fare un attacco melee.
     * Chiamato da GameLoop ogni tick.
     * @return true se il danno va applicato
     */
    public boolean tentaAttaccoMelee(float pgX, float pgY) {
        if (morto || meleeCooldown > 0) return false;
        float dx   = pgX - x + size / 2f;
        float dy   = pgY - y + size / 2f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= MELEE_RANGE + GameState.PG_SIZE / 2f) {
            meleeCooldown    = MELEE_COOLDOWN;
            meleeFlashTimer  = MELEE_FLASH_DURATA;
            return true;
        }
        return false;
    }

    public int getMeleeDanno() { return MELEE_DANNO; }

    @Override
    public void draw(Graphics2D g2, BufferedImage ignoredSprite) {
        if (morto) return;

        // ── Anello melee visibile quando attacca ──────────────────────────────
        if (meleeFlashTimer > 0) {
            float progress = (float) meleeFlashTimer / MELEE_FLASH_DURATA;
            int   radius   = MELEE_RANGE + size / 2;
            int   cx       = (int)(x + size / 2f);
            int   cy       = (int)(y + size / 2f);
            // Cerchio esterno — raggio pieno
            int alpha = (int)(200 * progress);
            g2.setColor(new Color(255, 60, 60, alpha));
            g2.setStroke(new java.awt.BasicStroke(3f));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            // Fill semitrasparente
            g2.setColor(new Color(255, 40, 40, (int)(40 * progress)));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setStroke(new java.awt.BasicStroke(1f));
        }

        // ── Sprite ────────────────────────────────────────────────────────────
        if (imgSk != null) {
            // Flash rosso sullo sprite quando attacca
            if (meleeFlashTimer > MELEE_FLASH_DURATA / 2) {
                java.awt.AlphaComposite ac = java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.45f);
                g2.setComposite(ac);
                g2.setColor(new Color(255, 50, 50));
                g2.fillRect((int) x, (int) y, size, size);
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));
            }
            g2.drawImage(imgSk, (int) x, (int) y, size, size, null);
        } else {
            g2.setColor(meleeFlashTimer > 0 ? new Color(255, 60, 60) : new Color(180, 30, 30));
            g2.fillRect((int) x, (int) y, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("!!", (int) x + size/2 - 6, (int) y + size/2);
        }
    }

    @Override
    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    public int  getVita()    { return vitaAttuale; }
    public int  getVitaMax() { return vitaMax; }
}