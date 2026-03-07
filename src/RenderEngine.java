import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * RenderEngine.java
 * Contiene tutta la logica di disegno del gioco.
 * Riceve per costruttore tutti i dati necessari (GameState, ResourceLoader,
 * RoomManager, UIManager) e non modifica alcuno stato.
 *
 * Il metodo principale è render(Graphics2D, int, int) che viene chiamato
 * da paintComponent() nel JPanel principale.
 */
public class RenderEngine {

    private final GameState      state;
    private final ResourceLoader res;
    private final RoomManager    roomMgr;
    private final UIManager      ui;
    private final FullscreenManager fullscreen;

    // Riferimento ai pugni attivi (gestiti dal GameLoop)
    private List<Pugno> pugniAttivi;
    // Proiettili cannone (tile mondo 5)
    private List<BossProjectile> proiettiliCannone = new java.util.ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    public RenderEngine(GameState state, ResourceLoader res,
                        RoomManager roomMgr, UIManager ui,
                        FullscreenManager fullscreen) {
        this.state      = state;
        this.res        = res;
        this.roomMgr    = roomMgr;
        this.ui         = ui;
        this.fullscreen = fullscreen;
    }

    public void setPugniAttivi(List<Pugno> pugni) {
        this.pugniAttivi = pugni;
    }

    public void setProiettiliCannone(List<BossProjectile> proj) {
        this.proiettiliCannone = proj;
    }

    // ── Entry point rendering ─────────────────────────────────────────────────

    public void render(Graphics2D g2, int panelWidth, int panelHeight) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        // Aggiorna volume audio da impostazioni
        state.audio.setVolumeMusica(state.impostazioni.volumeMusica);
        state.audio.setVolumeEffetti(state.impostazioni.volumeEffetti);

        // Gestione musica in base allo stato
        aggiornaMusica();

        // Aggiorna layout bottoni a ogni frame (idempotente se dimensioni invariate)
        ui.ricalcolaBottoni(panelWidth, panelHeight);

        // Disegna direttamente nelle dimensioni reali del pannello — nessuna scala,
        // nessuna banda nera, niente deformazione
        switch (state.statoGioco) {
            case MENU                  -> disegnaMenu(g2, panelWidth, panelHeight);
            case IMPOSTAZIONI          -> disegnaImpostazioni(g2, panelWidth, panelHeight);
            case CONTROLLI             -> disegnaControlli(g2, panelWidth, panelHeight);
            case SELEZIONE_PERSONAGGIO -> disegnaSelezionePersonaggio(g2, panelWidth, panelHeight);
            case SELEZIONE_MODALITA    -> disegnaSelezioneModalita(g2, panelWidth, panelHeight);
            case TETRIS                -> disegnaTetris(g2, panelWidth, panelHeight);
            case GIOCO                 -> disegnaGioco(g2, panelWidth, panelHeight);
            case PAUSA -> {
                java.awt.geom.AffineTransform t1 = g2.getTransform();
                disegnaGioco(g2, panelWidth, panelHeight);
                g2.setTransform(t1);  // ripristina prima di disegnare la pausa
                disegnaPausa(g2, panelWidth, panelHeight);
            }
            case GAME_OVER -> {
                java.awt.geom.AffineTransform t2 = g2.getTransform();
                disegnaGioco(g2, panelWidth, panelHeight);
                g2.setTransform(t2);  // ripristina prima di disegnare game over
                disegnaGameOver(g2, panelWidth, panelHeight);
            }
            case VITTORIA_STORIA       -> disegnaVittoriaStoria(g2, panelWidth, panelHeight);
            case UFFICIO               -> disegnaUfficio(g2, panelWidth, panelHeight);
        }
    }

    // ── Menu Principale ───────────────────────────────────────────────────────

    private void disegnaMenu(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Sfondo del gioco ──────────────────────────────────────────────────
        if (res.imgSfondoMenu != null) {
            g2.drawImage(res.imgSfondoMenu, 0, 0, W, H, null);
        } else {
            for (int y = 0; y < H; y++) {
                float t = (float)y / H;
                g2.setColor(new Color((int)(138+12*(1-t)), (int)(110+8*(1-t)), (int)(48+6*(1-t))));
                g2.drawLine(0, y, W, y);
            }
        }

        // ── Overlay minimo (non oscurare troppo lo sfondo) ────────────────────
        g2.setColor(new Color(0,0,0,18));
        g2.fillRect(0, 0, W, H);

        // ── Occhi animati ─────────────────────────────────────────────────────
        disegnaOcchiMenuSfondo(g2, W, H, ms);

        // ── Pannello compatto — solo bottoni, centrato nella metà bassa ────────
        int btnW = (int)(W * 0.32f);
        int btnH = (int)(H * 0.090f);
        int btnX = W/2 - btnW/2;
        int gap  = (int)(H * 0.022f);
        int nBtn = 4;
        int totBtnH = nBtn * btnH + (nBtn-1) * gap;
        int panPad  = (int)(H * 0.028f);
        int panW    = btnW + (int)(W*0.05f);
        int panH    = totBtnH + panPad*2;
        int panX    = W/2 - panW/2;
        // Centrato verticalmente nella metà inferiore dello schermo
        int panY    = (int)(H * 0.52f) - panH/2;

        // Pannello sfondo scuro semitrasparente con bordo ocra
        for (int i = 5; i > 0; i--) {
            g2.setColor(new Color(80, 50, 5, Math.max(0, 10*i)));
            g2.fillRoundRect(panX-i*2, panY-i, panW+i*4, panH+i*2, 12, 12);
        }
        g2.setColor(new Color(12, 8, 2, 195));
        g2.fillRoundRect(panX, panY, panW, panH, 10, 10);
        g2.setColor(new Color(55, 32, 6));   g2.drawRoundRect(panX,   panY,   panW,   panH,   10,10);
        g2.setColor(new Color(110, 65, 16)); g2.drawRoundRect(panX+1, panY+1, panW-2, panH-2, 9, 9);
        g2.setColor(new Color(165, 100, 28));g2.drawRoundRect(panX+2, panY+2, panW-4, panH-4, 8, 8);
        // Corner ornamentali
        int[][] panCorners = {{panX+3,panY+3},{panX+panW-11,panY+3},{panX+3,panY+panH-11},{panX+panW-11,panY+panH-11}};
        for (int[] c : panCorners) {
            g2.setColor(new Color(200,140,35)); g2.fillRect(c[0],c[1],8,8);
            g2.setColor(new Color(85,50,8));    g2.fillRect(c[0]+2,c[1]+2,4,4);
        }

        // ── Bottoni ────────────────────────────────────────────────────────────
        int firstBtnY = panY + panPad;
        ui.btnGioca.setBounds(btnX, firstBtnY,                btnW, btnH);
        ui.btnImpostazioni.setBounds(btnX, firstBtnY+(btnH+gap),   btnW, btnH);
        ui.btnControlli.setBounds(btnX, firstBtnY+(btnH+gap)*2, btnW, btnH);
        ui.btnEsciMenu.setBounds(btnX, firstBtnY+(btnH+gap)*3, btnW, btnH);

        ui.btnGioca.draw(g2);
        ui.btnImpostazioni.draw(g2);
        ui.btnControlli.draw(g2);
        ui.btnEsciMenu.draw(g2);


        // ── Footer ────────────────────────────────────────────────────────────
        int fFs = Math.max(9, (int)(H * 0.018f));
        g2.setFont(new Font("Consolas", Font.PLAIN, fFs));
        g2.setColor(new Color(80, 60, 15, 170));
        g2.drawString("F11 = Fullscreen", 16, H - 14);
        g2.drawString("v1.0", W - 46, H - 14);
    }

    /** Occhi animati che fluttuano sullo sfondo del menu.
     *  Usa due varianti (occhio1/occhio2) alternate per varietà visiva. */
    private void disegnaOcchiMenuSfondo(Graphics2D g2, int W, int H, long ms) {
        java.awt.image.BufferedImage eye1 = res.imgOcchioMenu;
        java.awt.image.BufferedImage eye2 = res.imgOcchioMenu2 != null ? res.imgOcchioMenu2 : eye1;
        if (eye1 == null) return;

        // xRel, yRel, scaleFactor, speedMul, phaseSeed, useVariant2
        float[][] dati = {
                {0.07f, 0.58f, 1.00f, 1.00f,  0f, 0},  // grande sinistra
                {0.87f, 0.52f, 0.90f, 1.25f,  2f, 1},  // grande destra
                {0.18f, 0.82f, 0.70f, 0.85f,  5f, 0},  // medio sinistra basso
                {0.80f, 0.80f, 0.68f, 1.10f,  8f, 1},  // medio destra basso
                {0.48f, 0.90f, 0.48f, 1.55f, 12f, 0},  // piccolo centro basso
                {0.25f, 0.42f, 0.38f, 0.70f, 17f, 1},  // piccolo sinistra medio
                {0.75f, 0.38f, 0.34f, 1.80f, 22f, 0},  // piccolo destra medio
        };
        int baseSize = (int)(H * 0.20f);

        for (float[] d : dati) {
            float cx2 = d[0]*W, cy2 = d[1]*H;
            float scale = d[2], spd = d[3], seed = d[4];
            boolean useV2 = (d[5] > 0.5f);
            java.awt.image.BufferedImage img = useV2 ? eye2 : eye1;

            // Fluttuazione sinusoidale
            float fx = (float)(Math.sin(ms*0.00095*spd + seed)       * W*0.013);
            float fy = (float)(Math.cos(ms*0.00078*spd + seed*1.3)   * H*0.011);
            // Pulse respiratorio
            float pulse = 0.91f + 0.09f*(float)Math.sin(ms*0.0021*spd + seed*0.8);
            int sz = (int)(baseSize * scale * pulse);

            // Blink: ogni tanto l'occhio si "schiaccia" sull'asse Y
            float blinkPhase = (float)((ms*0.0004*spd + seed*3.7) % (2*Math.PI));
            float blinkScale = 1.0f;
            if (blinkPhase > 5.8f) { // finestra breve del blink
                float bv = (blinkPhase - 5.8f) / (2*(float)Math.PI - 5.8f);
                blinkScale = 0.15f + 0.85f*(float)Math.abs(Math.sin(bv*Math.PI));
            }

            float alpha = 0.40f + 0.18f*(float)Math.sin(ms*0.0012*spd + seed*1.1);
            alpha = Math.max(0.15f, Math.min(0.62f, alpha));

            int ax = (int)(cx2+fx) - sz/2;
            int ay = (int)(cy2+fy) - sz/2;
            int drawH = (int)(sz * blinkScale);
            int drawY = ay + (sz - drawH)/2; // centra verticalmente

            double rot = 0.10 * Math.sin(ms*0.00055*spd + seed);
            java.awt.geom.AffineTransform savedAt = g2.getTransform();
            g2.rotate(rot, ax+sz/2.0, ay+sz/2.0);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(img, ax, drawY, sz, drawH, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setTransform(savedAt);
        }
    }

    /** Sfondo notturno con stelle, luna e silhouette città pixel-art.
     *  Ancora usato da GameOver e VittoriaStoria. */
    private void sfondoCitta(Graphics2D g2, int W, int H, long ms, boolean pioggia) {
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < H; y++) {
            float t = (float)y/H;
            g2.setColor(new Color((int)(8+20*(1-t)), (int)(6+14*(1-t)), (int)(22+42*(1-t))));
            g2.drawLine(0, y, W, y);
        }
        for (int i = 0; i < 80; i++) {
            int sx = rng.nextInt(W); int sy = rng.nextInt((int)(H*0.58f));
            float br = 0.5f+0.5f*(float)Math.sin(ms*0.002+i*1.3);
            g2.setColor(new Color(200,200,255, Math.max(0,Math.min(255,(int)(70+160*br)))));
            g2.fillRect(sx, sy, (i%5==0)?3:2, (i%5==0)?3:2);
        }
        float sp = (ms%8000)/8000f;
        if (sp < 0.07f) {
            float pct = sp/0.07f, sa = Math.max(0f,1f-pct);
            g2.setColor(new Color(255,255,200, Math.max(0,Math.min(255,(int)(210*sa)))));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine((int)(W*0.15f+W*0.55f*pct),(int)(H*0.04f+H*0.12f*pct),
                    (int)(W*0.15f+W*0.55f*pct-45*(1-pct)),(int)(H*0.04f+H*0.12f*pct-22*(1-pct)));
            g2.setStroke(new BasicStroke(1f));
        }
        int lX=(int)(W*0.80f), lY=(int)(H*0.13f), lR=(int)(W*0.052f+(int)(W*0.003f*Math.sin(ms*0.0008)));
        for (int r=lR+36; r>lR; r-=5) {
            float a=1f-(float)(r-lR)/36f;
            g2.setColor(new Color(180,200,255, Math.max(0,Math.min(255,(int)(22*a)))));
            g2.fillOval(lX-r,lY-r,r*2,r*2);
        }
        g2.setColor(new Color(215,220,255)); g2.fillOval(lX-lR,lY-lR,lR*2,lR*2);
        g2.setColor(new Color(15,15,35,150)); g2.fillOval(lX-lR+lR/3,lY-lR,lR*2,lR*2);
        g2.setColor(new Color(185,190,230,180));
        g2.fillOval(lX-lR/3,lY-lR/5,lR/3,lR/4); g2.fillOval(lX+lR/5,lY+lR/5,lR/4,lR/5);
        int cityY=(int)(H*0.54f), bW=W/18;
        g2.setColor(new Color(8,6,16));
        for (int i=0; i<21; i++) {
            rng.setSeed(i*137L);
            int bH=(int)(H*(0.08f+rng.nextFloat()*0.22f));
            int bX=i*bW-bW/5;
            g2.fillRect(bX,cityY-bH,bW-2,bH+H);
            for (int wy=cityY-bH+6; wy<cityY-4; wy+=(int)(H*0.042f)) {
                for (int wx=bX+4; wx<bX+bW-8; wx+=(int)(bW*0.42f)) {
                    long wSeed=i*1000L+wy+wx;
                    if (((ms/3000+wSeed)%5)!=0) {
                        int wa=Math.max(0,Math.min(255,110+(int)(55*Math.sin(ms*0.001+wSeed))));
                        g2.setColor(new Color(255,220,100,wa));
                        g2.fillRect(wx,wy,(int)(bW*0.22f),(int)(H*0.024f));
                    }
                }
            }
        }
        g2.setColor(new Color(5,4,12)); g2.fillRect(0,cityY,W,H-cityY);
        for (int ry=cityY; ry<cityY+(int)(H*0.05f); ry++) {
            float ta=1f-(float)(ry-cityY)/(H*0.05f);
            g2.setColor(new Color(200,200,120, Math.max(0,Math.min(255,(int)(14*ta)))));
            g2.drawLine(lX-6,ry,lX+6,ry);
        }
        if (pioggia) {
            int rainSeed=(int)(ms/30);
            java.util.Random rainRng=new java.util.Random(rainSeed);
            g2.setStroke(new BasicStroke(1f));
            for (int i=0; i<120; i++) {
                int rx=rainRng.nextInt(W+40)-20;
                int ry=(int)((rainRng.nextInt(H)+(ms/15.0))%H);
                g2.setColor(new Color(150,180,255,60+rainRng.nextInt(80)));
                g2.drawLine(rx,ry,rx-3,ry+8+rainRng.nextInt(14));
            }
            g2.setStroke(new BasicStroke(1f));
        }
    }


    private void disegnaImpostazioni(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();
        Impostazioni imp = state.impostazioni;

        // ── Sfondo + occhi semitrasparenti ────────────────────────────────────
        if (res.imgSfondoMenu != null) g2.drawImage(res.imgSfondoMenu,0,0,W,H,null);
        else { for(int y=0;y<H;y++){float t=(float)y/H;g2.setColor(new Color((int)(138+12*(1-t)),(int)(110+8*(1-t)),(int)(48+6*(1-t))));g2.drawLine(0,y,W,y);} }
        disegnaOcchiMenuSfondo(g2, W, H, ms);
        // Overlay scuro cinematico
        g2.setColor(new Color(8,5,1,190)); g2.fillRect(0,0,W,H);

        // ── Particelle fluttuanti ambientali ──────────────────────────────────
        java.util.Random rp = new java.util.Random(11);
        for (int i=0; i<18; i++) {
            float px=(float)(rp.nextFloat()*W + Math.sin(ms*0.00085+i*0.9)*18);
            float py=(float)(rp.nextFloat()*H + Math.cos(ms*0.00072+i*1.2)*14);
            float pa=0.25f+0.35f*(float)Math.sin(ms*0.002+i*1.8);
            int sz=1+(i%3);
            g2.setColor(new Color(220,160,40,Math.max(0,Math.min(255,(int)(120*pa)))));
            g2.fillRect((int)px,(int)py,sz,sz);
        }

        // ── Pannello centrale con alone animato ───────────────────────────────
        int panW=(int)(W*0.54f), panH=(int)(H*0.62f);
        int panX=W/2-panW/2, panY=(int)(H*0.13f);
        float glowPulse = 0.6f+0.4f*(float)Math.sin(ms*0.0018);
        for (int i=8;i>0;i--) {
            int a=Math.max(0,Math.min(255,(int)(18*i*glowPulse)));
            g2.setColor(new Color(180,110,20,a));
            g2.fillRoundRect(panX-i*3,panY-i*2,panW+i*6,panH+i*4,14,14);
        }
        g2.setColor(new Color(14,9,2,238)); g2.fillRoundRect(panX,panY,panW,panH,10,10);
        // Bordo triplo ocra
        g2.setColor(new Color(55,32,6));   g2.drawRoundRect(panX,   panY,   panW,   panH,   10,10);
        g2.setColor(new Color(115,68,18)); g2.drawRoundRect(panX+1, panY+1, panW-2, panH-2, 9, 9);
        g2.setColor(new Color(175,108,32));g2.drawRoundRect(panX+2, panY+2, panW-4, panH-4, 8, 8);
        // Corner decorativi
        int[][] corns={{panX+3,panY+3},{panX+panW-11,panY+3},{panX+3,panY+panH-11},{panX+panW-11,panY+panH-11}};
        for (int[] c2:corns){g2.setColor(new Color(220,150,40));g2.fillRect(c2[0],c2[1],8,8);g2.setColor(new Color(90,52,8));g2.fillRect(c2[0]+2,c2[1]+2,4,4);}
        // Scanline decorativa
        g2.setColor(new Color(255,180,50,12));
        for (int sy=panY+4;sy<panY+panH;sy+=4) g2.drawLine(panX+3,sy,panX+panW-3,sy);

        // ── Titolo animato ────────────────────────────────────────────────────
        int titFs=Math.max(20,(int)(H*0.072f));
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)titFs):new Font("Consolas",Font.BOLD,titFs));
        FontMetrics fmT=g2.getFontMetrics();
        String titStr="IMPOSTAZIONI";
        int titX=W/2-fmT.stringWidth(titStr)/2;
        int titY2=panY+(int)(H*0.074f);
        // Alone titolo pulsante
        float titGlow=0.7f+0.3f*(float)Math.sin(ms*0.0025);
        for (int i=3;i>0;i--) {
            g2.setColor(new Color(220,160,40,Math.max(0,(int)(30*titGlow*i))));
            g2.drawString(titStr,titX-i,titY2);g2.drawString(titStr,titX+i,titY2);
            g2.drawString(titStr,titX,titY2-i);g2.drawString(titStr,titX,titY2+i);
        }
        g2.setColor(new Color(45,26,4,180)); g2.drawString(titStr,titX+2,titY2+2);
        g2.setColor(new Color(235,192,80));  g2.drawString(titStr,titX,titY2);
        // Linea ornamentale
        int lhw=(int)(panW*0.36f);
        g2.setColor(new Color(110,65,14,210)); g2.fillRect(W/2-lhw,panY+(int)(H*0.093f),lhw*2,2);
        g2.setColor(new Color(215,158,48,130)); g2.fillRect(W/2-lhw+3,panY+(int)(H*0.093f),lhw*2-6,1);
        // Rombo centrale animato
        float rmbPulse=0.7f+0.3f*(float)Math.sin(ms*0.003);
        g2.setColor(new Color(220,150,40,Math.max(0,Math.min(255,(int)(255*rmbPulse)))));
        int ry0=panY+(int)(H*0.090f);
        g2.fillPolygon(new int[]{W/2,W/2-6,W/2,W/2+6},new int[]{ry0-3,ry0+3,ry0+8,ry0+3},4);

        // ── Slider layout ─────────────────────────────────────────────────────
        int LX=ui._impLabelX,CX2=ui._impCtrlX,SW=ui._impSw;
        int s=ui._impStartY,RH=ui._impRigaH,SH=ui._impSh;
        int sliderX=CX2+SW+5;
        int sliderW=ui.btnMusPiu.bounds!=null?ui.btnMusPiu.bounds.x-sliderX-5:(int)(W*0.13);
        int lbFs=Math.max(12,(int)(H*0.029f));

        // Riga Musica — highlight della riga attiva
        float notePulse=0.75f+0.25f*(float)Math.sin(ms*0.0028);
        // Sfondo riga con lieve glow
        g2.setColor(new Color(30,18,4,180)); g2.fillRoundRect(panX+8,s-SH/2-4,panW-16,SH+8,7,7);
        g2.setColor(new Color(90,52,10,Math.max(0,Math.min(255,(int)(100*notePulse))))); g2.drawRoundRect(panX+8,s-SH/2-4,panW-16,SH+8,7,7);
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)lbFs):new Font("Consolas",Font.BOLD,lbFs));
        g2.setColor(new Color(220,160,50,Math.max(0,Math.min(255,(int)(220*notePulse)))));
        g2.drawString("♪",LX,s+SH/2+3);
        g2.setColor(new Color(240,222,172)); g2.drawString("MUSICA",LX+(int)(lbFs*1.3f),s+SH/2+3);
        ui.btnMusMeno.draw(g2);
        disegnaSliderPixel(g2,sliderX,s+2,sliderW,SH-4,imp.volumeMusica,new Color(140,75,10),new Color(235,152,42));
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)(lbFs-1)):new Font("Consolas",Font.BOLD,lbFs-1));
        g2.setColor(new Color(228,178,72)); g2.drawString(imp.volumeMusica+"%",sliderX+sliderW+8,s+SH/2+3);
        ui.btnMusPiu.draw(g2);

        // Riga Effetti
        float effPulse=0.75f+0.25f*(float)Math.sin(ms*0.0028+1.2f);
        g2.setColor(new Color(30,18,4,180)); g2.fillRoundRect(panX+8,s+RH-SH/2-4,panW-16,SH+8,7,7);
        g2.setColor(new Color(30,90,40,Math.max(0,Math.min(255,(int)(100*effPulse))))); g2.drawRoundRect(panX+8,s+RH-SH/2-4,panW-16,SH+8,7,7);
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)lbFs):new Font("Consolas",Font.BOLD,lbFs));
        g2.setColor(new Color(100,205,122,Math.max(0,Math.min(255,(int)(210*effPulse)))));
        g2.drawString("♦",LX,s+RH+SH/2+3);
        g2.setColor(new Color(200,242,202)); g2.drawString("EFFETTI",LX+(int)(lbFs*1.3f),s+RH+SH/2+3);
        ui.btnEffMeno.draw(g2);
        disegnaSliderPixel(g2,sliderX,s+RH+2,sliderW,SH-4,imp.volumeEffetti,new Color(30,115,52),new Color(82,205,102));
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)(lbFs-1)):new Font("Consolas",Font.BOLD,lbFs-1));
        g2.setColor(new Color(132,228,152)); g2.drawString(imp.volumeEffetti+"%",sliderX+sliderW+8,s+RH+SH/2+3);
        ui.btnEffPiu.draw(g2);

        ui.btnChiudiImpostazioni.draw(g2);
    }

    private void disegnaPannelloPixel(Graphics2D g2, int x, int y, int w, int h, Color bg, Color border) {
        for (int i=4;i>0;i--){g2.setColor(new Color(border.getRed(),border.getGreen(),border.getBlue(),Math.max(0,15*i)));g2.fillRect(x-i*2,y-i,w+i*4,h+i*2);}
        g2.setColor(bg); g2.fillRect(x,y,w,h);
        g2.setColor(new Color(Math.max(0,border.getRed()-50),Math.max(0,border.getGreen()-40),Math.max(0,border.getBlue()-40)));g2.drawRect(x,y,w,h);
        g2.setColor(border);g2.drawRect(x+1,y+1,w-2,h-2);
        g2.setColor(new Color(Math.min(255,border.getRed()+60),Math.min(255,border.getGreen()+50),Math.min(255,border.getBlue()+50)));g2.drawRect(x+2,y+2,w-4,h-4);
        int[][] cc={{x+2,y+2},{x+w-10,y+2},{x+2,y+h-10},{x+w-10,y+h-10}};
        for (int[] c2:cc){g2.setColor(new Color(200,170,255));g2.fillRect(c2[0],c2[1],8,8);g2.setColor(new Color(100,70,160));g2.fillRect(c2[0]+2,c2[1]+2,4,4);}
    }

    private void disegnaLineaOrnamentale(Graphics2D g2, int cx, int y, int halfW) {
        g2.setColor(new Color(60,50,100)); g2.fillRect(cx-halfW,y,halfW*2,2);
        g2.setColor(new Color(120,90,180,160)); g2.fillRect(cx-halfW,y,halfW*2,1);
        for (int s=-1;s<=1;s+=2){int rx=cx+s*(halfW-4);g2.setColor(new Color(180,140,255));g2.fillRect(rx-2,y-2,4,2);g2.fillRect(rx-2,y+2,4,2);g2.fillRect(rx-4,y,2,2);g2.fillRect(rx+2,y,2,2);}
        g2.setColor(new Color(220,200,255));g2.fillRect(cx-2,y-2,4,6);
    }

    private void disegnaSliderPixel(Graphics2D g2, int x, int y, int w, int h,
                                    int valore, Color colPieno, Color colLuce) {
        g2.setColor(new Color(12,8,2)); g2.fillRect(x,y,w,h);
        g2.setColor(new Color(28,18,6));
        for (int px2=x+3;px2<x+w-2;px2+=4) g2.fillRect(px2,y+h/2-1,2,2);
        int fill=Math.max(0,(int)(w*valore/100.0));
        if (fill>0) {
            g2.setColor(colPieno); g2.fillRect(x,y,fill,h);
            g2.setColor(new Color(Math.min(255,colLuce.getRed()),Math.min(255,colLuce.getGreen()),Math.min(255,colLuce.getBlue()),140));
            g2.fillRect(x,y,fill,h/3);
            g2.setColor(new Color(0,0,0,40));
            for (int px2=x+8;px2<x+fill-2;px2+=8) g2.fillRect(px2,y,1,h);
        }
        if (fill>3&&fill<w-3){g2.setColor(colLuce);g2.fillRect(x+fill-3,y-2,6,h+4);g2.setColor(new Color(255,255,255,180));g2.fillRect(x+fill-1,y-1,2,h/2);}
        g2.setColor(new Color(50,30,5));g2.drawRect(x,y,w,h);
        g2.setColor(new Color(90,55,12,120));g2.drawRect(x+1,y+1,w-2,h-2);
    }

    private void disegnaSlider(Graphics2D g2, int x, int y, int w, int h,
                               int valore, Color colorePieno, Color coloreLuce) {
        disegnaSliderPixel(g2,x,y,w,h,valore,colorePieno,coloreLuce);
    }

    // ── Controlli ────────────────────────────────────────────────────────────

    private void disegnaControlli(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Sfondo + occhi + overlay ──────────────────────────────────────────
        if (res.imgSfondoMenu!=null) g2.drawImage(res.imgSfondoMenu,0,0,W,H,null);
        else { for(int y=0;y<H;y++){float t=(float)y/H;g2.setColor(new Color((int)(138+12*(1-t)),(int)(110+8*(1-t)),(int)(48+6*(1-t))));g2.drawLine(0,y,W,y);} }
        disegnaOcchiMenuSfondo(g2,W,H,ms);
        g2.setColor(new Color(8,5,1,190)); g2.fillRect(0,0,W,H);

        // ── Particelle fluttuanti ─────────────────────────────────────────────
        java.util.Random rp=new java.util.Random(99);
        for (int i=0;i<22;i++) {
            float px2=(float)(rp.nextFloat()*W+Math.sin(ms*0.00082+i*1.1)*16);
            float py2=(float)(rp.nextFloat()*H+Math.cos(ms*0.00068+i*1.4)*12);
            float pa2=0.2f+0.3f*(float)Math.sin(ms*0.0018+i*2.1);
            g2.setColor(new Color(220,160,40,Math.max(0,Math.min(255,(int)(100*pa2)))));
            g2.fillRect((int)px2,(int)py2,1+(i%3),1+(i%3));
        }

        // ── Pannello con alone animato ────────────────────────────────────────
        int panX=(int)(W*0.04f),panY=(int)(H*0.08f);
        int panW=(int)(W*0.88f),panH=(int)(H*0.79f);
        float glowP=0.55f+0.45f*(float)Math.sin(ms*0.0016);
        for (int i=8;i>0;i--){
            g2.setColor(new Color(175,108,18,Math.max(0,Math.min(255,(int)(16*i*glowP)))));
            g2.fillRoundRect(panX-i*2,panY-i,panW-16+i*4,panH+i*2,12,12);
        }
        g2.setColor(new Color(14,9,2,235)); g2.fillRoundRect(panX,panY,panW-18,panH,8,8);
        g2.setColor(new Color(52,30,5));   g2.drawRoundRect(panX,   panY,   panW-18,panH,  8,8);
        g2.setColor(new Color(112,64,16)); g2.drawRoundRect(panX+1, panY+1, panW-20,panH-2,7,7);
        g2.setColor(new Color(172,104,28));g2.drawRoundRect(panX+2, panY+2, panW-22,panH-4,6,6);
        int[][] cc2={{panX+3,panY+3},{panX+panW-27,panY+3},{panX+3,panY+panH-11},{panX+panW-27,panY+panH-11}};
        for (int[] c2:cc2){g2.setColor(new Color(220,150,40));g2.fillRect(c2[0],c2[1],8,8);g2.setColor(new Color(90,52,8));g2.fillRect(c2[0]+2,c2[1]+2,4,4);}
        // Scanline decorativa
        g2.setColor(new Color(255,175,45,10));
        for (int sy=panY+4;sy<panY+panH;sy+=4) g2.drawLine(panX+3,sy,panX+panW-21,sy);

        // ── Titolo animato ────────────────────────────────────────────────────
        int titFs=Math.max(18,(int)(H*0.070f));
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)titFs):new Font("Consolas",Font.BOLD,titFs));
        FontMetrics fmT=g2.getFontMetrics();
        String titStr="CONTROLLI";
        int titX=W/2-fmT.stringWidth(titStr)/2;
        int titY2=panY+(int)(H*0.055f);
        float titGlow=0.65f+0.35f*(float)Math.sin(ms*0.0022);
        for (int i=3;i>0;i--){
            g2.setColor(new Color(220,155,38,Math.max(0,(int)(28*titGlow*i))));
            g2.drawString(titStr,titX-i,titY2);g2.drawString(titStr,titX+i,titY2);
            g2.drawString(titStr,titX,titY2-i);g2.drawString(titStr,titX,titY2+i);
        }
        g2.setColor(new Color(45,25,4,175)); g2.drawString(titStr,titX+2,titY2+2);
        g2.setColor(new Color(235,192,78));  g2.drawString(titStr,titX,titY2);
        // Linea decorativa
        int lhw2=(int)(panW*0.30f);
        g2.setColor(new Color(108,62,13,215)); g2.fillRect(W/2-lhw2,panY+(int)(H*0.070f),lhw2*2,2);
        g2.setColor(new Color(212,155,46,130)); g2.fillRect(W/2-lhw2+3,panY+(int)(H*0.070f),lhw2*2-6,1);
        float rmbP2=0.65f+0.35f*(float)Math.sin(ms*0.0030);
        g2.setColor(new Color(220,148,38,Math.max(0,Math.min(255,(int)(255*rmbP2)))));
        int ry1=panY+(int)(H*0.067f);
        g2.fillPolygon(new int[]{W/2,W/2-6,W/2,W/2+6},new int[]{ry1-3,ry1+3,ry1+8,ry1+3},4);

        // ── Clip + scroll ─────────────────────────────────────────────────────
        Shape oldClip=g2.getClip();
        g2.setClip(panX+3,panY+3,panW-22,panH-6);
        int scrollY=state.controlliScrollY;
        int col1X=panX+(int)(W*0.022f),col2X=panX+(int)(W*0.46f);
        int startY=panY+(int)(H*0.102f)-scrollY;
        int rigaH=(int)(H*0.090f);

        disegnaSezioneLabel(g2,col1X,startY-4,"GIOCO",W,H);
        startY+=(int)(H*0.032f);
        disegnaTastoInfo(g2,col1X,startY,         "W A S D","Movimento");
        disegnaTastoInfo(g2,col2X,startY,         "Frecce", "Sparo direzionale");
        disegnaTastoInfo(g2,col1X,startY+rigaH,   "Z",      "Attacco corpo a corpo");
        disegnaTastoInfo(g2,col2X,startY+rigaH,   "ESC",    "Pausa");
        disegnaTastoInfo(g2,col1X,startY+rigaH*2, "F11",    "Fullscreen");
        disegnaTastoInfo(g2,col2X,startY+rigaH*2, "Click",  "Interagisci / Acquista");
        disegnaTastoInfo(g2,col1X,startY+rigaH*3, "INVIO",  "Conferma dialogo");
        disegnaTastoInfo(g2,col2X,startY+rigaH*3, "Q",      "Esci (in pausa)");

        int tetY=startY+rigaH*4+(int)(H*0.045f);
        disegnaSezioneLabel(g2,col1X,tetY-4,"TETRIS  (stanza iniziale)",W,H);
        tetY+=(int)(H*0.032f);
        disegnaTastoInfo(g2,col1X,tetY,         "A / D",  "Muovi pezzo");
        disegnaTastoInfo(g2,col2X,tetY,         "W",      "Ruota pezzo");
        disegnaTastoInfo(g2,col1X,tetY+rigaH,   "S",      "Scendi veloce");
        disegnaTastoInfo(g2,col2X,tetY+rigaH,   "SPAZIO", "Caduta istantanea");
        disegnaTastoInfo(g2,col1X,tetY+rigaH*2, "ESC",    "Salta Tetris");
        disegnaTastoInfo(g2,col2X,tetY+rigaH*2, "500+ pt","CURA | 1500: VEL | 3000: DANNO | 5000: MELEE | 6000: TUTTO");

        int contenutoFine=tetY+rigaH*3+(int)(H*0.05f);
        int scrollMax=Math.max(0,contenutoFine-(panY+panH)+(int)(H*0.02f)+scrollY);
        if (state.controlliScrollY>scrollMax) state.controlliScrollY=scrollMax;
        g2.setClip(oldClip);

        // ── Scrollbar ─────────────────────────────────────────────────────────
        int sbX=panX+panW-17,sbH=panH;
        g2.setColor(new Color(10,6,1)); g2.fillRect(sbX,panY,13,sbH);
        g2.setColor(new Color(30,16,4));
        for (int py2=panY+3;py2<panY+sbH-3;py2+=4) g2.fillRect(sbX+2,py2,9,2);
        g2.setColor(new Color(68,38,7)); g2.drawRect(sbX,panY,13,sbH);
        if (scrollMax>0) {
            int thumbH=Math.max(30,(int)((float)panH/(contenutoFine-(panY-scrollY))*sbH));
            int thumbY=panY+(int)((float)state.controlliScrollY/scrollMax*(sbH-thumbH));
            float tGlow=0.6f+0.4f*(float)Math.sin(ms*0.002);
            g2.setColor(new Color(130,74,14)); g2.fillRect(sbX+1,thumbY,11,thumbH);
            g2.setColor(new Color(182,112,30,Math.max(0,Math.min(255,(int)(200*tGlow))))); g2.fillRect(sbX+2,thumbY+1,9,3);
            g2.setColor(new Color(215,148,44)); g2.fillRect(sbX+2,thumbY+2,3,3);g2.fillRect(sbX+2,thumbY+thumbH-5,3,3);
            g2.setColor(new Color(202,132,34)); g2.drawRect(sbX+1,thumbY,11,thumbH);
        }
        float arrGlow=0.55f+0.45f*(float)Math.sin(ms*0.0025);
        g2.setColor(new Color(182,112,26,Math.max(0,Math.min(255,(int)(220*arrGlow)))));
        g2.fillPolygon(new int[]{sbX+6,sbX+2,sbX+11},new int[]{panY+4,panY+12,panY+12},3);
        g2.fillPolygon(new int[]{sbX+6,sbX+2,sbX+11},new int[]{panY+sbH-4,panY+sbH-12,panY+sbH-12},3);

        ui.btnChiudiControlli.draw(g2);
    }

    private void disegnaSezioneLabel(Graphics2D g2, int x, int y, String testo, int W, int H) {
        long ms=System.currentTimeMillis();
        int lFs=Math.max(10,(int)(H*0.023f));
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)lFs):new Font("Consolas",Font.BOLD,lFs));
        FontMetrics fm=g2.getFontMetrics(); int tw2=fm.stringWidth(testo);
        // Linea con gradiente
        float lp=0.65f+0.35f*(float)Math.sin(ms*0.0015+x*0.001);
        g2.setColor(new Color(85,48,8,Math.max(0,Math.min(255,(int)(200*lp)))));
        g2.fillRect(x,y+lFs/2,(int)(W*0.46f),2);
        g2.setColor(new Color(185,112,26,Math.max(0,Math.min(255,(int)(150*lp)))));
        g2.fillRect(x,y+lFs/2,(int)(W*0.46f),1);
        // Punti decorativi
        for (int i=0;i<5;i++){g2.setColor(new Color(162,92,22,Math.max(0,185-i*32)));g2.fillRect(x+tw2+10+i*5,y+lFs/2-1,3,4);}
        // Testo con alone
        for (int i=2;i>0;i--){g2.setColor(new Color(220,155,38,Math.max(0,18*i)));g2.drawString(testo,x-i,y+lFs);g2.drawString(testo,x+i,y+lFs);}
        g2.setColor(new Color(38,20,3,165)); g2.drawString(testo,x+1,y+lFs+1);
        g2.setColor(new Color(228,178,66));  g2.drawString(testo,x,y+lFs);
    }

    private void disegnaTastoInfo(Graphics2D g2, int x, int y, String tasto, String descrizione) {
        int tw=132,th=36;
        // Sfondo tasto con effetto 3D
        g2.setColor(new Color(18,10,2)); g2.fillRect(x,y,tw,th);
        // Top-left highlight
        g2.setColor(new Color(118,68,16)); g2.fillRect(x,y,tw,3); g2.fillRect(x,y,3,th);
        // Bottom-right ombra
        g2.setColor(new Color(5,2,0));    g2.fillRect(x+tw-3,y,3,th); g2.fillRect(x,y+th-3,tw,3);
        // Inset glow
        g2.setColor(new Color(185,115,32,85)); g2.fillRect(x+3,y+3,tw-6,3);
        // Testo tasto con glow dorato
        int tFs=Math.max(10,th-14);
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)tFs):new Font("Consolas",Font.BOLD,tFs));
        FontMetrics fm=g2.getFontMetrics();
        int tx=x+(tw-fm.stringWidth(tasto))/2, ty=y+(th+fm.getAscent())/2-4;
        // Alone testo
        g2.setColor(new Color(235,188,72,55)); g2.drawString(tasto,tx-1,ty); g2.drawString(tasto,tx+1,ty); g2.drawString(tasto,tx,ty-1); g2.drawString(tasto,tx,ty+1);
        g2.setColor(new Color(235,188,72)); g2.drawString(tasto,tx,ty);
        // Descrizione
        int dFs=Math.max(10,th-15);
        g2.setFont(res.fontCustom!=null?res.fontCustom.deriveFont(Font.PLAIN,(float)dFs):new Font("Consolas",Font.PLAIN,dFs));
        g2.setColor(new Color(195,162,98)); g2.drawString(descrizione,x+tw+10,ty);
    }

    public void scrollControlli(int delta) {
        state.controlliScrollY=Math.max(0,state.controlliScrollY+delta*28);
    }




    // ── Selezione Personaggio ─────────────────────────────────────────────────

    private void disegnaSelezionePersonaggio(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Sfondo: tramonto viola/arancio ────────────────────────────────────
        for (int y = 0; y < H; y++) {
            float t = (float) y / H;
            int r  = (int)(220 - 80  * t);
            int gv = (int)(100 - 60  * t);
            int b  = (int)(180 - 60  * t);
            g2.setColor(new Color(Math.max(0,r), Math.max(0,gv), Math.max(0,b)));
            g2.drawLine(0, y, W, y);
        }

        // ── Stelle (zona alta) ────────────────────────────────────────────────
        java.util.Random rng = new java.util.Random(7);
        for (int i = 0; i < 50; i++) {
            int sx = rng.nextInt(W);
            int sy = rng.nextInt((int)(H * 0.45f));
            float blink = 0.5f + 0.5f * (float)Math.sin(ms * 0.0015 + i * 0.9);
            g2.setColor(new Color(255, 240, 200, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(160 * blink)))))));
            g2.fillRect(sx, sy, 2, 2);
        }

        // ── Silhouette città bassa ────────────────────────────────────────────
        int edH = (int)(H * 0.28f);
        g2.setColor(new Color(18, 14, 28));
        g2.fillRect(0, H - edH, W, edH);
        // Finestre sparse accese
        java.util.Random bldRng = new java.util.Random(13);
        for (int col = 0; col < 14; col++) {
            for (int row = 1; row <= 4; row++) {
                if (bldRng.nextFloat() < 0.30f) {
                    int fw = (int)(W * 0.018f), fh = (int)(H * 0.028f);
                    int fx = (int)(W * 0.065f * col) + bldRng.nextInt(10);
                    int fy = H - edH + (int)(H * 0.055f * row);
                    int amber = 160 + bldRng.nextInt(80);
                    g2.setColor(new Color(amber, (int)(amber * 0.75f), 50, 180));
                    g2.fillRect(fx, fy, fw, fh);
                }
            }
        }

        // ── Pannello titolo ────────────────────────────────────────────────────
        int titFs = Math.max(18, (int)(H * 0.07f));
        // Ombra
        g2.setColor(new Color(40, 0, 60, 180));
        drawTextCentered(g2, "SCEGLI IL TUO PERSONAGGIO", W/2 + 2, (int)(H*0.10f)+2, titFs);
        // Testo
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)titFs)
                : new Font("Consolas", Font.BOLD, titFs));
        g2.setColor(new Color(255, 230, 160));
        drawTextCentered(g2, "SCEGLI IL TUO PERSONAGGIO", W/2, (int)(H*0.10f), titFs);

        // Sottolineatura decorativa
        int lineW = Math.min(W/2, (int)(W*0.5f));
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 180, 80, 160));
        g2.drawLine(W/2 - lineW/2, (int)(H*0.10f)+6, W/2 + lineW/2, (int)(H*0.10f)+6);
        g2.setStroke(new BasicStroke(1f));

        // Hint navigazione
        int hintFs = Math.max(9, (int)(H * 0.025f));
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(220, 200, 255, 180));
        String hint = "[Frecce/Mouse] naviga    [INVIO/Click] conferma    [ESC] indietro";
        g2.drawString(hint, W/2 - g2.getFontMetrics().stringWidth(hint)/2, (int)(H*0.17f));

        // Hint combo segreto
        int comboB = state.sistemaPersonaggi.getContatoreBCombo();
        if (comboB > 0) {
            g2.setColor(new Color(255, 220, 80, 200));
            g2.setFont(new Font("Consolas", Font.BOLD, hintFs));
            g2.drawString("B x " + comboB + " / 5", W - (int)(W*0.12f), (int)(H*0.06f));
        }

        boolean segretoAttivo = state.sistemaPersonaggi.isSegretoAttivo();
        int numMostra = segretoAttivo ? 5 : 4;

        for (int i = 0; i < numMostra; i++) {
            java.awt.Rectangle r = ui.rectsSelezionePG[i];
            if (r == null) continue;
            int rx = r.x, ry = r.y, rw = r.width, rh = r.height;

            DatiPersonaggio pg      = ui.listaPersonaggi.get(i);
            boolean         lock    = !state.sistemaPersonaggi.isSbloccato(i);
            boolean         segreto = (i == SistemaPersonaggi.INDICE_SEGRETO);
            boolean         sel     = (i == state.indicePersonaggioSelezionato) && !lock;

            // ── Sfondo card con gradiente simulato ────────────────────────────
            if (segreto) {
                // Card oro scuro per il personaggio segreto
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    g2.setColor(new Color(
                            (int)(80 + 40*tt), (int)(45 + 20*tt), (int)(0 + 10*tt),
                            sel ? 200 : 140));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            } else if (lock) {
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    g2.setColor(new Color(12, (int)(12+8*tt), (int)(22+15*tt), 160));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            } else {
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    int base = sel ? 60 : 30;
                    int cr = Math.min(255, Math.max(0, (int)(base + 20*tt)));
                    int cg = Math.min(255, Math.max(0, (int)(base*0.6f)));
                    int cb = Math.min(255, Math.max(0, (int)(base*1.4f + 30*tt)));
                    g2.setColor(new Color(cr, cg, cb, sel ? 210 : 160));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            }

            // ── Bordo ─────────────────────────────────────────────────────────
            float strokeW = sel ? 3f : 1.5f;
            Color bordo = segreto ? new Color(255, 210, 60)
                    : lock    ? new Color(45, 45, 70)
                    : sel     ? new Color(255, 200, 80)
                    : new Color(120, 90, 160);
            // Glow per selezionato
            if (sel) {
                float glow = 0.6f + 0.4f * (float)Math.sin(ms * 0.004);
                g2.setColor(new Color(255, 200, 80, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(60 * glow)))))));
                g2.setStroke(new BasicStroke(7f));
                g2.drawRoundRect(rx-2, ry-2, rw+4, rh+4, 14, 14);
            }
            g2.setColor(bordo);
            g2.setStroke(new BasicStroke(strokeW));
            g2.drawRoundRect(rx, ry, rw, rh, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            int imgSize = (int)(rw * 0.52f);
            int imgX    = rx + (rw - imgSize) / 2;
            int imgY    = ry + (int)(rh * 0.05f);
            int nomeY   = ry + (int)(rh * 0.60f);
            int descY   = ry + (int)(rh * 0.70f);
            int statsY  = ry + (int)(rh * 0.80f);
            int cardFs  = Math.max(9,  (int)(rw * 0.105f));
            int smallFs = Math.max(8,  (int)(rw * 0.082f));

            if (lock) {
                // Immagine svanita
                if (pg.imgIcona != null) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, 0.18f));
                    g2.drawImage(pg.imgIcona, imgX, imgY, imgSize, imgSize, null);
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, 1f));
                }
                // Nome grigio
                g2.setFont(res.fontCustomBold != null
                        ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)cardFs)
                        : new Font("Consolas", Font.BOLD, cardFs));
                g2.setColor(new Color(70, 65, 90));
                FontMetrics fmL = g2.getFontMetrics();
                g2.drawString(pg.nome, rx + (rw - fmL.stringWidth(pg.nome))/2, nomeY);
                // Icona lucchetto (ASCII)
                int lockFs = Math.max(14, (int)(rh * 0.14f));
                g2.setFont(new Font("Consolas", Font.BOLD, lockFs));
                g2.setColor(new Color(80, 75, 105));
                String lockLbl = "[X]";
                FontMetrics fmLk = g2.getFontMetrics();
                g2.drawString(lockLbl, rx + (rw - fmLk.stringWidth(lockLbl))/2,
                        ry + (int)(rh * 0.72f));
                // Testo sblocco
                g2.setFont(new Font("Arial", Font.PLAIN, smallFs));
                g2.setColor(new Color(110, 100, 135));
                String[] righe = state.sistemaPersonaggi.testoSblocco(i).split("\n");
                for (int rr = 0; rr < righe.length; rr++) {
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(righe[rr],
                            rx + (rw - fm2.stringWidth(righe[rr]))/2,
                            statsY + rr * (smallFs + 3));
                }
                continue;
            }

            // ── Icona personaggio ─────────────────────────────────────────────
            if (pg.imgIcona != null) {
                // Alone colorato sotto l'icona del selezionato
                if (sel) {
                    float al = 0.4f + 0.4f * (float)Math.sin(ms * 0.004);
                    g2.setColor(new Color(255, 210, 80, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(50*al)))))));
                    g2.fillOval(imgX - 6, imgY + imgSize - 12, imgSize + 12, 24);
                }
                g2.drawImage(pg.imgIcona, imgX, imgY, imgSize, imgSize, null);
            } else if (segreto) {
                g2.setFont(new Font("Serif", Font.BOLD, (int)(rh * 0.22f)));
                g2.setColor(new Color(255, 215, 0));
                String star = "*";
                FontMetrics fmS = g2.getFontMetrics();
                g2.drawString(star, rx + (rw - fmS.stringWidth(star))/2,
                        imgY + imgSize - 4);
            } else {
                g2.setColor(new Color(130, 100, 180));
                g2.fillOval(imgX, imgY, imgSize, imgSize);
            }

            // ── Nome ──────────────────────────────────────────────────────────
            g2.setFont(res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)cardFs)
                    : new Font("Consolas", Font.BOLD, cardFs));
            // Ombra nome
            g2.setColor(new Color(0, 0, 0, 100));
            FontMetrics fmN = g2.getFontMetrics();
            g2.drawString(pg.nome, rx + (rw - fmN.stringWidth(pg.nome))/2 + 1, nomeY+1);
            // Nome
            g2.setColor(segreto ? new Color(255, 215, 60)
                    : sel    ? new Color(255, 230, 100)
                    : new Color(230, 215, 255));
            g2.drawString(pg.nome, rx + (rw - fmN.stringWidth(pg.nome))/2, nomeY);

            // ── Linea separatore ──────────────────────────────────────────────
            g2.setColor(new Color(180, 140, 255, 60));
            g2.drawLine(rx + rw/5, nomeY + 4, rx + rw*4/5, nomeY + 4);

            // ── Descrizione ───────────────────────────────────────────────────
            g2.setFont(new Font("Arial", Font.ITALIC, smallFs));
            g2.setColor(segreto ? new Color(255, 195, 80) : new Color(190, 175, 220));
            FontMetrics fmD = g2.getFontMetrics();
            g2.drawString(pg.descrizione,
                    rx + (rw - fmD.stringWidth(pg.descrizione))/2, descY);

            // ── Stats ─────────────────────────────────────────────────────────
            g2.setFont(new Font("Consolas", Font.BOLD, smallFs));
            int icoS = smallFs;
            // Vita
            if (res.imgCuore != null)
                g2.drawImage(res.imgCuore, rx + 6, statsY - icoS + 2, icoS, icoS, null);
            g2.setColor(new Color(255, 110, 110));
            g2.drawString("" + pg.vitaMax, rx + 8 + icoS, statsY);
            // Velocità e danno
            g2.setColor(new Color(100, 210, 255));
            g2.drawString("V:" + pg.velocitaBase, rx + 6, statsY + smallFs + 4);
            g2.setColor(new Color(255, 200, 80));
            g2.drawString("D:" + pg.dannoBase, rx + rw/2, statsY + smallFs + 4);
        }
    }


    // ── Selezione Modalità ────────────────────────────────────────────────────

    private void disegnaSelezioneModalita(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Sfondo: cielo notturno blu-indaco ─────────────────────────────────
        for (int y = 0; y < H; y++) {
            float t = (float) y / H;
            int r  = (int)(10  + 15  * (1-t));
            int gv = (int)(10  + 10  * (1-t));
            int b  = (int)(50  + 50  * (1-t));
            g2.setColor(new Color(r, gv, b));
            g2.drawLine(0, y, W, y);
        }

        // ── Stelle ────────────────────────────────────────────────────────────
        java.util.Random rng = new java.util.Random(99);
        for (int i = 0; i < 80; i++) {
            int sx = rng.nextInt(W);
            int sy = rng.nextInt((int)(H * 0.60f));
            float blink = 0.5f + 0.5f * (float)Math.sin(ms * 0.0013 + i * 1.1);
            int sz = rng.nextFloat() < 0.15f ? 3 : 2;
            g2.setColor(new Color(200, 215, 255, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(180 * blink)))))));
            g2.fillRect(sx, sy, sz, sz);
        }

        // ── Luna piena a destra ───────────────────────────────────────────────
        float lunaPulse = 0.97f + 0.03f * (float)Math.sin(ms * 0.0009);
        int lunaR = (int)(W * 0.048f * lunaPulse);
        int lunaX = (int)(W * 0.84f), lunaY = (int)(H * 0.16f);
        for (int r = lunaR + 35; r > lunaR; r -= 5) {
            float a = 1f - (float)(r - lunaR) / 35f;
            g2.setColor(new Color(180, 200, 255, Math.max(0, Math.min(255, (int)(20 * a)))));
            g2.fillOval(lunaX - r, lunaY - r, r*2, r*2);
        }
        g2.setColor(new Color(215, 225, 255));
        g2.fillOval(lunaX - lunaR, lunaY - lunaR, lunaR*2, lunaR*2);
        g2.setColor(new Color(12, 12, 50, 130));
        g2.fillOval(lunaX - lunaR + lunaR/3, lunaY - lunaR, lunaR*2, lunaR*2);

        // ── Silhouette città ─────────────────────────────────────────────────
        int edH = (int)(H * 0.30f);
        g2.setColor(new Color(10, 10, 20));
        g2.fillRect(0, H - edH, W, edH);
        java.util.Random bldRng = new java.util.Random(55);
        for (int col = 0; col < 16; col++) {
            for (int row = 1; row <= 5; row++) {
                if (bldRng.nextFloat() < 0.22f) {
                    int fw = (int)(W * 0.016f), fh = (int)(H * 0.024f);
                    int fx = (int)(W * 0.058f * col) + bldRng.nextInt(8);
                    int fy = H - edH + (int)(H * 0.048f * row);
                    int amber = 130 + bldRng.nextInt(60);
                    g2.setColor(new Color(amber, (int)(amber*0.7f), 30, 160));
                    g2.fillRect(fx, fy, fw, fh);
                }
            }
        }

        // ── Titolo ────────────────────────────────────────────────────────────
        int titFs = Math.max(18, (int)(H * 0.07f));
        g2.setColor(new Color(20, 10, 50, 180));
        drawTextCentered(g2, "SELEZIONA LA SFIDA", W/2 + 2, (int)(H*0.10f)+2, titFs);
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)titFs)
                : new Font("Consolas", Font.BOLD, titFs));
        g2.setColor(new Color(180, 210, 255));
        drawTextCentered(g2, "SELEZIONA LA SFIDA", W/2, (int)(H*0.10f), titFs);

        int lineW = (int)(W * 0.40f);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(120, 160, 255, 140));
        g2.drawLine(W/2 - lineW/2, (int)(H*0.10f)+6, W/2 + lineW/2, (int)(H*0.10f)+6);
        g2.setStroke(new BasicStroke(1f));

        int hintFs = Math.max(9, (int)(H * 0.025f));
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(160, 185, 230, 180));
        String hint = "[Frecce/Mouse] naviga    [INVIO/Click] conferma    [ESC] indietro";
        g2.drawString(hint, W/2 - g2.getFontMetrics().stringWidth(hint)/2, (int)(H*0.17f));

        // ── Card modalità ─────────────────────────────────────────────────────
        String[] nomi  = { "STORIA CLASSICA", "MODALITA INFINITA" };
        String[] desc1 = { "Sconfiggi 4 Boss", "Sopravvivi all'infinito!" };
        String[] desc2 = { "per salvare il cantiere", "Nemici sempre piu forti." };
        BufferedImage[] icone = { res.imgIconaStoria, res.imgIconaInfinita };

        // Palette diversa per ciascuna card
        int[][] colTop = { {50,30,90}, {15,55,90} };    // indigo vs blu oceano
        int[][] colBot = { {25,15,50}, {8,28,55} };
        Color[] bordoNorm  = { new Color(100,70,180), new Color(40,110,200) };
        Color[] bordoSel   = { new Color(200,160,255), new Color(80,180,255) };
        Color[] nomeColore = { new Color(220,180,255), new Color(130,210,255) };

        for (int i = 0; i < 2; i++) {
            java.awt.Rectangle rect = ui.rectsSelezioneModalita[i];
            if (rect == null) continue;
            boolean sel = (i == state.indiceModalitaSelezionata);

            // Gradiente card
            for (int dy = 0; dy < rect.height; dy++) {
                float t = (float) dy / rect.height;
                int r  = Math.min(255, Math.max(0, (int)(colTop[i][0] + (colBot[i][0]-colTop[i][0])*t)));
                int gv = Math.min(255, Math.max(0, (int)(colTop[i][1] + (colBot[i][1]-colTop[i][1])*t)));
                int b  = Math.min(255, Math.max(0, (int)(colTop[i][2] + (colBot[i][2]-colTop[i][2])*t)));
                int alpha = sel ? 230 : 170;
                g2.setColor(new Color(r, gv, b, alpha));
                g2.drawLine(rect.x+1, rect.y+dy, rect.x+rect.width-2, rect.y+dy);
            }

            // Glow selezione
            if (sel) {
                float glow = 0.5f + 0.5f * (float)Math.sin(ms * 0.004);
                g2.setColor(new Color(
                        bordoSel[i].getRed(), bordoSel[i].getGreen(),
                        bordoSel[i].getBlue(), Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(55 * glow)))))));
                g2.setStroke(new BasicStroke(8f));
                g2.drawRoundRect(rect.x-3, rect.y-3, rect.width+6, rect.height+6, 18, 18);
            }

            // Bordo
            g2.setColor(sel ? bordoSel[i] : bordoNorm[i]);
            g2.setStroke(new BasicStroke(sel ? 3f : 1.8f));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);
            g2.setStroke(new BasicStroke(1f));

            int imgSize = (int)(rect.width * 0.42f);
            int imgX    = rect.x + (rect.width - imgSize) / 2;
            int imgY    = rect.y + (int)(rect.height * 0.07f);
            int nomeY   = rect.y + (int)(rect.height * 0.60f);
            int d1Y     = rect.y + (int)(rect.height * 0.73f);
            int d2Y     = rect.y + (int)(rect.height * 0.84f);
            int nomeFs  = Math.max(11, (int)(rect.width * 0.092f));
            int descFs  = Math.max(9,  (int)(rect.width * 0.075f));

            // Icona
            if (icone[i] != null) {
                if (sel) {
                    float al = 0.35f + 0.35f * (float)Math.sin(ms * 0.003);
                    g2.setColor(new Color(bordoSel[i].getRed(), bordoSel[i].getGreen(),
                            bordoSel[i].getBlue(), Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(45*al)))))));
                    g2.fillOval(imgX - 8, imgY + imgSize - 14, imgSize + 16, 28);
                }
                g2.drawImage(icone[i], imgX, imgY, imgSize, imgSize, null);
            } else {
                // Fallback testuale elegante
                g2.setFont(res.fontCustomBold != null
                        ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)(rect.height * 0.24f))
                        : new Font("Consolas", Font.BOLD, (int)(rect.height * 0.24f)));
                g2.setColor(sel ? bordoSel[i] : bordoNorm[i]);
                String fb = i == 0 ? "S" : "INF";
                FontMetrics fmFb = g2.getFontMetrics();
                g2.drawString(fb, rect.x + (rect.width - fmFb.stringWidth(fb))/2,
                        imgY + imgSize);
            }

            // Nome
            g2.setColor(new Color(0, 0, 0, 100));
            g2.setFont(res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)nomeFs)
                    : new Font("Consolas", Font.BOLD, nomeFs));
            FontMetrics fmNm = g2.getFontMetrics();
            g2.drawString(nomi[i], rect.x + (rect.width - fmNm.stringWidth(nomi[i]))/2+1, nomeY+1);
            g2.setColor(sel ? bordoSel[i] : nomeColore[i]);
            g2.drawString(nomi[i], rect.x + (rect.width - fmNm.stringWidth(nomi[i]))/2, nomeY);

            // Separatore
            g2.setColor(new Color(bordoNorm[i].getRed(), bordoNorm[i].getGreen(),
                    bordoNorm[i].getBlue(), 80));
            g2.drawLine(rect.x + rect.width/5, nomeY+5,
                    rect.x + rect.width*4/5, nomeY+5);

            // Descrizione
            g2.setFont(new Font("Arial", Font.ITALIC, descFs));
            g2.setColor(new Color(190, 200, 230));
            FontMetrics fmD = g2.getFontMetrics();
            g2.drawString(desc1[i], rect.x + (rect.width - fmD.stringWidth(desc1[i]))/2, d1Y);
            g2.setFont(new Font("Arial", Font.PLAIN, descFs));
            g2.setColor(new Color(150, 165, 200));
            g2.drawString(desc2[i], rect.x + (rect.width - fmD.stringWidth(desc2[i]))/2, d2Y);
        }
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void disegnaPausa(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Overlay scuro con vignettatura ────────────────────────────────────
        g2.setColor(new Color(0,0,0,165));
        g2.fillRect(0,0,W,H);
        // Vignetta radiale
        for (int i=8;i>0;i--) {
            float t=(float)i/8f;
            g2.setColor(new Color(0,0,0,Math.max(0,(int)(60*t))));
            int vx=(int)(W*0.1f*t),vy=(int)(H*0.1f*t);
            g2.drawRect(vx,vy,W-vx*2,H-vy*2);
        }

        // ── Pannello con alone animato ────────────────────────────────────────
        int BH=ui.btnRiprendi.bounds.height, GAP=(int)(H*0.022);
        int panW=(int)(W*0.32), panX=W/2-panW/2;
        int btnY=ui.btnRiprendi.bounds.y, titY2=btnY-(int)(H*0.07);
        int panY=titY2-(int)(H*0.06);
        int panH=(BH+GAP)*4+(int)(H*0.13);

        float glowP=0.55f+0.45f*(float)Math.sin(ms*0.0018);
        for (int i=7;i>0;i--) {
            g2.setColor(new Color(160,100,20,Math.max(0,Math.min(255,(int)(14*i*glowP)))));
            g2.fillRoundRect(panX-i*2,panY-i,panW+i*4,panH+i*2,14,14);
        }
        g2.setColor(new Color(14,9,2,240)); g2.fillRoundRect(panX,panY,panW,panH,12,12);
        g2.setColor(new Color(52,30,5));   g2.drawRoundRect(panX,   panY,   panW,   panH,   12,12);
        g2.setColor(new Color(112,64,16)); g2.drawRoundRect(panX+1, panY+1, panW-2, panH-2, 11,11);
        g2.setColor(new Color(172,104,28));g2.drawRoundRect(panX+2, panY+2, panW-4, panH-4, 10,10);
        // Corner
        int[][] cc={{panX+3,panY+3},{panX+panW-11,panY+3},{panX+3,panY+panH-11},{panX+panW-11,panY+panH-11}};
        for (int[] c2:cc){g2.setColor(new Color(220,150,40));g2.fillRect(c2[0],c2[1],8,8);g2.setColor(new Color(90,52,8));g2.fillRect(c2[0]+2,c2[1]+2,4,4);}
        // Scanline
        g2.setColor(new Color(255,175,45,8));
        for (int sy=panY+4;sy<panY+panH;sy+=4) g2.drawLine(panX+3,sy,panX+panW-3,sy);

        // ── Titolo PAUSA animato ──────────────────────────────────────────────
        int titFs=(int)(H*0.065f);
        g2.setFont(res.fontCustomBold!=null?res.fontCustomBold.deriveFont(Font.PLAIN,(float)titFs):new Font("Consolas",Font.BOLD,titFs));
        FontMetrics fmT=g2.getFontMetrics();
        int txp=W/2-fmT.stringWidth("PAUSA")/2;
        int typ=titY2+(int)(H*0.044f);
        float tGlow=0.65f+0.35f*(float)Math.sin(ms*0.0025);
        for (int i=3;i>0;i--){g2.setColor(new Color(220,155,38,Math.max(0,(int)(26*tGlow*i))));g2.drawString("PAUSA",txp-i,typ);g2.drawString("PAUSA",txp+i,typ);g2.drawString("PAUSA",txp,typ-i);g2.drawString("PAUSA",txp,typ+i);}
        g2.setColor(new Color(45,25,4,175)); g2.drawString("PAUSA",txp+2,typ+2);
        g2.setColor(new Color(235,192,78));  g2.drawString("PAUSA",txp,typ);
        // Linea
        int lhw=(int)(panW*0.32f);
        g2.setColor(new Color(108,62,13,215)); g2.fillRect(W/2-lhw,titY2+(int)(H*0.054f),lhw*2,2);
        g2.setColor(new Color(212,155,46,130)); g2.fillRect(W/2-lhw+3,titY2+(int)(H*0.054f),lhw*2-6,1);
        float rmbP=0.65f+0.35f*(float)Math.sin(ms*0.0030);
        g2.setColor(new Color(220,148,38,Math.max(0,Math.min(255,(int)(255*rmbP)))));
        int rmbY=titY2+(int)(H*0.051f);
        g2.fillPolygon(new int[]{W/2,W/2-5,W/2,W/2+5},new int[]{rmbY-2,rmbY+3,rmbY+7,rmbY+3},4);

        // ── Bottoni con priorità mouse/tastiera ───────────────────────────────
        MenuButton[] btnsPausa={ui.btnRiprendi,ui.btnImpostazioniPausa,ui.btnMenuPrincipalePausa,ui.btnEsciPausa};
        boolean mouseAttivo=false;
        for (MenuButton b:btnsPausa) if (b.hover){mouseAttivo=true;break;}
        for (int i=0;i<btnsPausa.length;i++){
            if (!mouseAttivo) btnsPausa[i].hover=(i==state.indiceBtnPausa);
            btnsPausa[i].draw(g2);
        }
    }


    // ── Game Over ─────────────────────────────────────────────────────────────

    private void disegnaGameOver(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();
        sfondoCitta(g2, W, H, ms, true);

        // ── Pannello testo centrale ────────────────────────────────────────────
        int panW = (int)(W*0.58f), panH = (int)(H*0.38f);
        int panX = W/2-panW/2, panY = (int)(H*0.33f);
        disegnaPannelloPixel(g2, panX, panY, panW, panH, new Color(8,6,22,220), new Color(60,70,120));

        // ── Titolo ─────────────────────────────────────────────────────────────
        int titFs = Math.max(22, (int)(H*0.092f));
        g2.setFont(res.fontCustomBold!=null ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)titFs) : new Font("Consolas",Font.BOLD,titFs));
        g2.setColor(new Color(0,0,30,200));    drawTextCentered(g2,"NON CE L'HAI FATTA.",W/2+3,panY+(int)(panH*0.36f)+3,titFs);
        g2.setColor(new Color(200,215,255));   drawTextCentered(g2,"NON CE L'HAI FATTA.",W/2,  panY+(int)(panH*0.36f),  titFs);
        // Linea
        disegnaLineaOrnamentale(g2, W/2, panY+(int)(panH*0.46f), (int)(panW*0.40f));

        // ── Sottotitolo ────────────────────────────────────────────────────────
        int subFs = Math.max(12, (int)(H*0.028f));
        g2.setFont(res.fontCustom!=null ? res.fontCustom.deriveFont(Font.PLAIN,(float)subFs) : new Font("Consolas",Font.PLAIN,subFs));
        g2.setColor(new Color(155,170,215,210));
        drawTextCentered(g2,"L'ufficio aspettera ancora.",W/2,panY+(int)(panH*0.57f),subFs);

        // ── Stats ──────────────────────────────────────────────────────────────
        int stFs = Math.max(10, (int)(H*0.024f));
        g2.setFont(res.fontCustomBold!=null ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)stFs) : new Font("Consolas",Font.BOLD,stFs));
        g2.setColor(new Color(130,148,195,200));
        String stats = "Mondo "+state.mondoAttuale+"  |  Stanza "+state.stanzaNelMondo+"  |  Monete "+state.monete;
        drawTextCentered(g2, stats, W/2, panY+(int)(panH*0.72f), stFs);
        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            int tot = (state.mondoAttuale-1)*GameState.STANZA_BOSS+state.stanzaNelMondo;
            g2.setColor(new Color(175,155,98,200));
            drawTextCentered(g2,"Stanze totali: "+tot, W/2, panY+(int)(panH*0.86f), stFs);
        }

        ui.btnRiprova.draw(g2);
        ui.btnMenuPrincipaleGO.draw(g2);
        ui.btnEsciGO.draw(g2);
    }


    // ── Vittoria Storia ───────────────────────────────────────────────────────

    // ── Stanza Ufficio ────────────────────────────────────────────────────────

    private void disegnaUfficio(Graphics2D g2, int W, int H) {
        // Sfondo ufficio — grigio caldo con finestra
        g2.setColor(new Color(45, 45, 55));
        g2.fillRect(0, 0, W, H);

        // Finestra sulla parete di fondo (luce diurna)
        int finW = (int)(W * 0.25f), finH = (int)(H * 0.30f);
        int finX = W / 2 - finW / 2, finY = (int)(H * 0.08f);
        g2.setColor(new Color(180, 220, 255, 80));
        g2.fillRect(finX, finY, finW, finH);
        g2.setColor(new Color(200, 240, 255, 140));
        g2.fillRect(finX + 10, finY + 10, finW / 2 - 12, finH / 2 - 12);
        g2.fillRect(finX + finW / 2 + 2, finY + 10, finW / 2 - 12, finH / 2 - 12);
        g2.fillRect(finX + 10, finY + finH / 2 + 2, finW / 2 - 12, finH / 2 - 12);
        g2.fillRect(finX + finW / 2 + 2, finY + finH / 2 + 2, finW / 2 - 12, finH / 2 - 12);
        // Cornice finestra
        g2.setColor(new Color(100, 85, 70));
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(finX, finY, finW, finH);
        g2.drawLine(finX + finW / 2, finY, finX + finW / 2, finY + finH);
        g2.drawLine(finX, finY + finH / 2, finX + finW, finY + finH / 2);
        g2.setStroke(new BasicStroke(1f));

        // Pavimento
        g2.setColor(new Color(100, 75, 50));
        g2.fillRect(0, (int)(H * 0.72f), W, H);
        g2.setColor(new Color(120, 90, 60));
        int listW = W / 8;
        for (int i = 0; i < 9; i++) {
            g2.drawLine(i * listW, (int)(H * 0.72f), i * listW, H);
        }

        // Scrivania del Capo
        int deskW = (int)(W * 0.35f), deskH = (int)(H * 0.12f);
        int deskX = W / 2 - deskW / 2, deskY = (int)(H * 0.58f);
        g2.setColor(new Color(90, 60, 30));
        g2.fillRect(deskX, deskY, deskW, deskH);
        g2.setColor(new Color(120, 85, 45));
        g2.fillRect(deskX + 4, deskY + 4, deskW - 8, 18);
        g2.setColor(new Color(60, 40, 20));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(deskX, deskY, deskW, deskH);
        g2.setStroke(new BasicStroke(1f));

        // NPC Capo
        int capoSize = (int)(H * 0.28f);
        int capoX = W / 2 - capoSize / 2;
        int capoY = deskY - capoSize + 10;
        if (res.imgCapo != null) {
            g2.drawImage(res.imgCapo, capoX, capoY, capoSize, capoSize, null);
        } else {
            // Fallback: omino seduto con giacca
            g2.setColor(new Color(40, 60, 100));
            g2.fillRoundRect(capoX + capoSize/4, capoY + capoSize/3,
                    capoSize/2, capoSize * 2/3, 8, 8);
            g2.setColor(new Color(220, 180, 140));
            int tH = capoSize / 3;
            g2.fillOval(capoX + capoSize/3, capoY, capoSize/3, tH);
            g2.setColor(new Color(30, 30, 40));
            g2.fillOval(capoX + capoSize/3 + tH/6, capoY + tH/3, tH/5, tH/5);
            g2.fillOval(capoX + capoSize/2 + 2, capoY + tH/3, tH/5, tH/5);
        }

        // Banner "UFFICIO" in alto
        String banTxt = "UFFICIO";
        int banFs = Math.max(18, (int)(H * 0.042f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) banFs)
                : new Font("Consolas", Font.BOLD, banFs));
        FontMetrics fmBan = g2.getFontMetrics();
        int banW = fmBan.stringWidth(banTxt) + 30;
        int banX = W / 2 - banW / 2;
        int banY = 12;
        g2.setColor(new Color(30, 30, 50, 210));
        g2.fillRoundRect(banX, banY, banW, banFs + 16, 10, 10);
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(banTxt, W / 2 - fmBan.stringWidth(banTxt) / 2, banY + banFs + 2);

        // Dialogo col Capo (usa stesso sistema narrazione JRPG)
        if (state.dialogoNarrazione.isAttivo()) {
            disegnaDialogoNarrazione(g2, W, H);
        } else {
            // Fallback hint se dialogo non avviato ancora
            g2.setFont(new Font("Consolas", Font.ITALIC, Math.max(10, (int)(H * 0.018f))));
            g2.setColor(new Color(200, 200, 200, 160));
            String hint2 = "[ INVIO per continuare ]";
            g2.drawString(hint2, W / 2 - g2.getFontMetrics().stringWidth(hint2) / 2,
                    H - 20);
        }
    }

    // ── Schermata Finale ──────────────────────────────────────────────────────

    private void disegnaVittoriaStoria(Graphics2D g2, int W, int H) {
        long ms = System.currentTimeMillis();

        // ── Sfondo — alba con sole che sorge ──────────────────────────────────
        for (int y = 0; y < H; y++) {
            float t = (float)y/H;
            int r  = Math.min(255,(int)(200+55*(1-t)));
            int gv = Math.min(255,(int)(130+90*(1-t)));
            int b  = Math.min(255,(int)(80 +90*(1-t)));
            g2.setColor(new Color(r,gv,b)); g2.drawLine(0,y,W,y);
        }
        // Sole pulsante
        float pulse = 0.96f+0.04f*(float)Math.sin(ms*0.002);
        int solR = (int)(W*0.09f*pulse), solX=W/2, solY=(int)(H*0.26f);
        for (int r=solR+60; r>solR; r-=8) {
            float a = 1f-(float)(r-solR)/60f;
            g2.setColor(new Color(255,220,80, Math.max(0,Math.min(255,(int)(50*a)))));
            g2.fillOval(solX-r,solY-r,r*2,r*2);
        }
        g2.setColor(new Color(255,235,110)); g2.fillOval(solX-solR,solY-solR,solR*2,solR*2);
        // Raggi solari pixel
        g2.setColor(new Color(255,240,150,60));
        for (int ang=0; ang<360; ang+=30) {
            double rad = Math.toRadians(ang+ms*0.02);
            int rx1=(int)(solX+Math.cos(rad)*(solR+8)), ry1=(int)(solY+Math.sin(rad)*(solR+8));
            int rx2=(int)(solX+Math.cos(rad)*(solR+22)), ry2=(int)(solY+Math.sin(rad)*(solR+22));
            g2.setStroke(new BasicStroke(3f)); g2.drawLine(rx1,ry1,rx2,ry2);
        }
        g2.setStroke(new BasicStroke(1f));
        // Nuvole pixel
        java.util.Random cn = new java.util.Random(33);
        for (int i=0; i<5; i++) {
            int cx2=(int)(cn.nextFloat()*W+(ms*0.008*(i%2==0?1:-1))%(W+200)-100);
            int cy2=(int)(H*0.12f)+i*20;
            float ca=0.25f+0.15f*(float)Math.sin(ms*0.001+i);
            g2.setColor(new Color(255,255,255, Math.max(0,Math.min(255,(int)(160*ca)))));
            int cw=(int)(W*0.10f)+i*15, ch2=(int)(H*0.04f);
            g2.fillRoundRect(cx2,cy2,cw,ch2,ch2,ch2);
            g2.fillRoundRect(cx2+cw/4,cy2-ch2/2,cw/2,ch2,ch2/2,ch2/2);
        }
        // Città silhouette illuminata dal sole
        g2.setColor(new Color(28,28,38));
        int cityY=(int)(H*0.54f), bW=W/18;
        java.util.Random rc = new java.util.Random(42);
        for (int i=0; i<21; i++) {
            rc.setSeed(i*137L);
            int bH=(int)(H*(0.08f+rc.nextFloat()*0.20f));
            int bX=i*bW-bW/5;
            g2.fillRect(bX,cityY-bH,bW-2,bH+H);
            g2.setColor(new Color(255,220,100,180));
            for (int wy=cityY-bH+6; wy<cityY-4; wy+=(int)(H*0.042f)) {
                for (int wx=bX+4; wx<bX+bW-8; wx+=(int)(bW*0.42f)) {
                    g2.fillRect(wx,wy,(int)(bW*0.22f),(int)(H*0.024f));
                }
            }
            g2.setColor(new Color(28,28,38));
        }
        g2.setColor(new Color(20,20,30)); g2.fillRect(0,cityY,W,H-cityY);

        // ── Pannello testo ─────────────────────────────────────────────────────
        int panW=(int)(W*0.60f), panH=(int)(H*0.42f);
        int panX=W/2-panW/2, panY=(int)(H*0.30f);
        disegnaPannelloPixel(g2, panX, panY, panW, panH, new Color(20,12,8,215), new Color(140,90,30));

        // Corner gold
        int[][] corners={{panX+2,panY+2},{panX+panW-10,panY+2},{panX+2,panY+panH-10},{panX+panW-10,panY+panH-10}};
        for (int[] c2 : corners) {
            g2.setColor(new Color(255,210,80)); g2.fillRect(c2[0],c2[1],8,8);
            g2.setColor(new Color(180,130,20)); g2.fillRect(c2[0]+2,c2[1]+2,4,4);
        }

        // ── Titolo ─────────────────────────────────────────────────────────────
        int titFs = Math.max(22, (int)(H*0.092f));
        g2.setFont(res.fontCustomBold!=null ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)titFs) : new Font("Consolas",Font.BOLD,titFs));
        g2.setColor(new Color(80,50,0,160));  drawTextCentered(g2,"SEI ARRIVATO.",W/2+3,panY+(int)(panH*0.32f)+3,titFs);
        g2.setColor(new Color(255,240,180));  drawTextCentered(g2,"SEI ARRIVATO.",W/2,  panY+(int)(panH*0.32f),  titFs);
        // Linea dorata
        FontMetrics fmT = g2.getFontMetrics();
        int tw = fmT.stringWidth("SEI ARRIVATO.");
        g2.setColor(new Color(180,120,20,180)); g2.fillRect(W/2-tw/2-6,panY+(int)(panH*0.42f),tw+12,2);
        g2.setColor(new Color(255,210,80,100)); g2.fillRect(W/2-tw/2-6,panY+(int)(panH*0.42f),tw+12,1);

        // ── Sottotitolo ────────────────────────────────────────────────────────
        int subFs = Math.max(12, (int)(H*0.030f));
        g2.setFont(res.fontCustom!=null ? res.fontCustom.deriveFont(Font.PLAIN,(float)subFs) : new Font("Consolas",Font.PLAIN,subFs));
        g2.setColor(new Color(255,255,220,210));
        String sub = "Con "+(GameState.MELEE_DELAY>0?"un po' di ritardo.":"ritardo.");
        drawTextCentered(g2, sub, W/2, panY+(int)(panH*0.54f), subFs);

        // ── Monete ─────────────────────────────────────────────────────────────
        int mFs = Math.max(11, (int)(H*0.026f));
        g2.setFont(res.fontCustomBold!=null ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)mFs) : new Font("Consolas",Font.BOLD,mFs));
        g2.setColor(new Color(255,215,0));
        drawTextCentered(g2,"Monete guadagnate: "+state.monete, W/2, panY+(int)(panH*0.70f), mFs);

        // ── Codice debug ────────────────────────────────────────────────────────
        if (state.notaRaccolta) {
            int codFs = Math.max(10,(int)(H*0.022f));
            g2.setFont(res.fontCustom!=null ? res.fontCustom.deriveFont(Font.PLAIN,(float)codFs) : new Font("Consolas",Font.ITALIC,codFs));
            g2.setColor(new Color(180,255,180,200));
            drawTextCentered(g2,"Codice trovato: "+GameState.CODICE_DEBUG, W/2, panY+(int)(panH*0.84f), codFs);
        }

        ui.btnMenuPrincipaleVittoria.draw(g2);
    }


    // ── Tetris ────────────────────────────────────────────────────────────────

    private void disegnaTetris(Graphics2D g2, int W, int H) {
        TetrisGame t = state.tetris;

        // ── Font helpers ──────────────────────────────────────────────────────
        java.util.function.Function<Float, java.awt.Font> cf  =
                s -> res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN,  s)
                        : new Font("Consolas", Font.BOLD,  s.intValue());
        java.util.function.Function<Float, java.awt.Font> cfl =
                s -> res.fontCustom != null ? res.fontCustom.deriveFont(Font.PLAIN, s)
                        : new Font("Consolas", Font.PLAIN, s.intValue());

        // ── Dimensioni celle proporzionali ────────────────────────────────────
        // Riserviamo il 38% della larghezza per i due pannelli laterali (19% ciascuno)
        // e il resto per shell+griglia. La griglia occupa al max il 90% dell'altezza.
        int availForShell = (int)(W * 0.62f);
        int cellH = Math.min((int)(H * 0.043f),
                Math.min((int)(availForShell * 0.72f / TetrisGame.COLS),
                        (int)(H * 0.9f / TetrisGame.ROWS)));
        int cellW = cellH;
        int gridW = TetrisGame.COLS * cellW;
        int gridH = TetrisGame.ROWS * cellH;

        // ── Gameboy shell ─────────────────────────────────────────────────────
        int shellPadX = (int)(cellW * 1.4f);
        int shellPadT = (int)(cellH * 2.8f);
        int shellPadB = (int)(cellH * 3.2f);
        int shellW    = gridW + shellPadX * 2;
        int shellH    = gridH + shellPadT + shellPadB;
        int shellX    = W / 2 - shellW / 2;
        int shellY    = Math.max(4, H / 2 - shellH / 2);

        // Coordinate griglia dentro il body
        int gridX = shellX + shellPadX;
        int gridY = shellY + shellPadT;

        // Pannelli laterali — usano tutto lo spazio disponibile a sx e dx del body
        int sideGap = Math.max(6, (int)(cellW * 0.7f));
        int sideW   = Math.max((int)(cellW * 4f),
                Math.min((shellX - sideGap),        // spazio a sinistra
                        (W - shellX - shellW - sideGap))); // spazio a destra
        int leftX   = shellX - sideGap - sideW;
        int rightX  = shellX + shellW + sideGap;
        int sideY   = shellY;
        int sideH   = shellH;
        // Clamp: se non c'è spazio, i pannelli si sovrappongono ma non escono dallo schermo
        if (leftX < 2) leftX = 2;
        if (rightX + sideW > W - 2) sideW = W - 2 - rightX;

        // ── Sfondo pagina ─────────────────────────────────────────────────────
        g2.setColor(new Color(8, 8, 18));
        g2.fillRect(0, 0, W, H);

        // ── Gameboy body ──────────────────────────────────────────────────────
        // Ombra profonda
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(shellX + 6, shellY + 8, shellW, shellH, 28, 28);
        // Body principale — grigio scuro stile plastica
        GradientPaint bodyGrad = new GradientPaint(
                shellX, shellY, new Color(52, 52, 68),
                shellX, shellY + shellH, new Color(30, 30, 42));
        g2.setPaint(bodyGrad);
        g2.fillRoundRect(shellX, shellY, shellW, shellH, 28, 28);
        // Bordo esterno
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(90, 90, 120));
        g2.drawRoundRect(shellX, shellY, shellW, shellH, 28, 28);
        // Bordo interno luminoso
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(130, 130, 170, 80));
        g2.drawRoundRect(shellX + 3, shellY + 3, shellW - 6, shellH - 6, 25, 25);
        g2.setStroke(new BasicStroke(1f));

        // Schermino LCD — cornice attorno alla griglia
        int lcdPad = (int)(cellW * 0.35f);
        int lcdX   = gridX - lcdPad;
        int lcdY   = gridY - lcdPad;
        int lcdW   = gridW + lcdPad * 2;
        int lcdH   = gridH + lcdPad * 2;
        // Cornice esterna LCD
        g2.setColor(new Color(20, 20, 30));
        g2.fillRoundRect(lcdX - 4, lcdY - 4, lcdW + 8, lcdH + 8, 8, 8);
        g2.setColor(new Color(70, 70, 100));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(lcdX - 4, lcdY - 4, lcdW + 8, lcdH + 8, 8, 8);
        g2.setStroke(new BasicStroke(1f));
        // Schermo LCD (verde verdino tipico gameboy)
        g2.setColor(new Color(15, 22, 18));
        g2.fillRect(lcdX, lcdY, lcdW, lcdH);

        // Logo "GAME BOY" style sotto la griglia dentro il body
        String logo = "WHAT TETRIS";
        int logoFs = Math.max(9, (int)(cellH * 0.55f));
        // Riduci se troppo largo per il body
        g2.setFont(cf.apply((float) logoFs));
        while (logoFs > 8 && g2.getFontMetrics().stringWidth(logo) > shellW - shellPadX * 2) {
            logoFs--;
            g2.setFont(cf.apply((float) logoFs));
        }
        g2.setColor(new Color(100, 100, 140));
        FontMetrics fmLogo = g2.getFontMetrics();
        g2.drawString(logo, shellX + (shellW - fmLogo.stringWidth(logo)) / 2,
                gridY + gridH + (int)(cellH * 1.1f));

        // ── Pannello sinistro (PUNTEGGIO · LIVELLO · RIGHE) ──────────────────
        disegnaPannelloLaterale(g2, leftX,  sideY, sideW, sideH, cellH, cf, cfl, t, false);

        // ── Pannello destro (NEXT · PREMIO) ──────────────────────────────────
        disegnaPannelloLaterale(g2, rightX, sideY, sideW, sideH, cellH, cf, cfl, t, true);

        if (t == null) return;

        // ── Timer riquadro in alto ─────────────────────────────────────────────
        int sec     = t.secondiRimanenti();
        boolean urg = sec < 15;
        int timerBoxH = (int)(shellPadT * 0.72f);
        int timerBoxW = (int)(shellW * 0.62f);
        int timerBoxX = shellX + (shellW - timerBoxW) / 2;
        int timerBoxY = shellY + (shellPadT - timerBoxH) / 2;

        // Sfondo timer
        g2.setColor(urg ? new Color(80, 10, 10, 220) : new Color(20, 20, 40, 220));
        g2.fillRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 10, 10);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(urg ? new Color(255, 60, 60) : new Color(80, 80, 140));
        g2.drawRoundRect(timerBoxX, timerBoxY, timerBoxW, timerBoxH, 10, 10);
        g2.setStroke(new BasicStroke(1f));

        // Contenuto timer
        int timerLabelFs = Math.max(9, (int)(timerBoxH * 0.30f));
        int timerValFs   = Math.max(12, (int)(timerBoxH * 0.52f));
        String timerStr  = sec / 60 + ":" + String.format("%02d", sec % 60);
        g2.setFont(cfl.apply((float) timerLabelFs));
        FontMetrics fmTL = g2.getFontMetrics();
        g2.setColor(urg ? new Color(255, 120, 120) : new Color(150, 150, 200));
        g2.drawString("TEMPO", timerBoxX + (timerBoxW - fmTL.stringWidth("TEMPO")) / 2,
                timerBoxY + timerLabelFs + 3);
        g2.setFont(cf.apply((float) timerValFs));
        FontMetrics fmTV = g2.getFontMetrics();
        g2.setColor(urg ? Color.RED : Color.WHITE);
        g2.drawString(timerStr, timerBoxX + (timerBoxW - fmTV.stringWidth(timerStr)) / 2,
                timerBoxY + timerBoxH - 5);

        // ── Celle griglia ─────────────────────────────────────────────────────
        for (int r = 0; r < TetrisGame.ROWS; r++) {
            for (int c = 0; c < TetrisGame.COLS; c++) {
                int val = t.grid[r][c];
                if (val == 0) {
                    g2.setColor(new Color(18, 26, 20));
                    g2.fillRect(gridX + c*cellW, gridY + r*cellH, cellW - 1, cellH - 1);
                } else {
                    disegnaCellaTetris(g2, gridX + c*cellW, gridY + r*cellH,
                            cellW - 1, cellH - 1, TetrisGame.COLORI[val - 1]);
                }
            }
        }

        // ── Pezzo corrente + ghost ────────────────────────────────────────────
        if (!t.gameOver && !t.completato) {
            int col = TetrisGame.COLORI[t.tipoPezzo];
            // Ghost
            int ghostY2 = t.pezzoY;
            outer:
            while (true) {
                int ny = ghostY2 + 1;
                for (int[] cell : t.getCelleCorrente()) {
                    int gx = cell[0], gy = ny + (cell[1] - t.pezzoY);
                    if (gx < 0 || gx >= TetrisGame.COLS || gy >= TetrisGame.ROWS) break outer;
                    if (gy >= 0 && t.grid[gy][gx] != 0) break outer;
                }
                ghostY2 = ny;
            }
            int ghostDy = ghostY2 - t.pezzoY;
            if (ghostDy > 0) {
                for (int[] cell : t.getCelleCorrente()) {
                    int cx = gridX + cell[0] * cellW;
                    int cy = gridY + (cell[1] + ghostDy) * cellH;
                    if (cell[1] + ghostDy >= 0) {
                        int r2 = (col >> 16) & 0xFF, gv = (col >> 8) & 0xFF, b2 = col & 0xFF;
                        g2.setColor(new Color(r2, gv, b2, 55));
                        g2.fillRect(cx, cy, cellW - 1, cellH - 1);
                    }
                }
            }
            // Pezzo vero
            for (int[] cell : t.getCelleCorrente()) {
                int cx = gridX + cell[0] * cellW;
                int cy = gridY + cell[1] * cellH;
                if (cell[1] >= 0)
                    disegnaCellaTetris(g2, cx, cy, cellW - 1, cellH - 1, col);
            }
        }

        // Linee griglia leggere sul LCD
        g2.setColor(new Color(25, 38, 28, 80));
        for (int c = 1; c < TetrisGame.COLS; c++)
            g2.drawLine(gridX + c*cellW - 1, gridY, gridX + c*cellW - 1, gridY + gridH);
        for (int r = 1; r < TetrisGame.ROWS; r++)
            g2.drawLine(gridX, gridY + r*cellH - 1, gridX + gridW, gridY + r*cellH - 1);

        // ── Istruzioni centrate in basso ──────────────────────────────────────
        if (!t.completato && !t.gameOver) {
            int iFs = Math.max(9, (int)(cellH * 0.55f));
            g2.setFont(cfl.apply((float) iFs));
            g2.setColor(new Color(80, 80, 110));
            String istr = "A/D muovi   W ruota   S scendi   SPAZIO caduta   ESC salta";
            FontMetrics fmI = g2.getFontMetrics();
            g2.drawString(istr, W / 2 - fmI.stringWidth(istr) / 2,
                    shellY + shellH + (int)(cellH * 0.9f));
        }

        // ── Overlay fine partita ──────────────────────────────────────────────
        if (t.completato || t.gameOver) {
            g2.setColor(new Color(0, 0, 0, 175));
            g2.fillRect(0, 0, W, H);
            int oFs1 = (int)(cellH * 1.6f);
            int oFs2 = (int)(cellH * 1.05f);
            int oFs3 = (int)(cellH * 0.80f);
            String msg = t.gameOver ? "GAME OVER" : "TEMPO SCADUTO!";
            g2.setFont(cf.apply((float) oFs1));
            FontMetrics fm1 = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(msg, W/2 - fm1.stringWidth(msg)/2, H/2 - cellH);
            g2.setFont(cfl.apply((float) oFs2));
            FontMetrics fm2 = g2.getFontMetrics();
            String ptsTxt = "Punteggio: " + t.punteggio;
            g2.setColor(new Color(200, 200, 255));
            g2.drawString(ptsTxt, W/2 - fm2.stringWidth(ptsTxt)/2, H/2 + (int)(cellH * 0.7f));
            String premioTxt = t.getPowerUp().equals("NESSUNO")
                    ? "Nessun power-up" : "Power-up: " + t.getPowerUp();
            g2.setFont(cf.apply((float) oFs2));
            FontMetrics fm3 = g2.getFontMetrics();
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(premioTxt, W/2 - fm3.stringWidth(premioTxt)/2, H/2 + (int)(cellH * 2.0f));
            g2.setFont(cfl.apply((float) oFs3));
            FontMetrics fm4 = g2.getFontMetrics();
            g2.setColor(new Color(160, 160, 200));
            String hint = "INVIO per continuare";
            g2.drawString(hint, W/2 - fm4.stringWidth(hint)/2, H/2 + (int)(cellH * 3.8f));
        }
    }

    /**
     * Disegna un pannello laterale del Gameboy (sx o dx).
     * sx=false → pannello sinistra: PUNTEGGIO, LIVELLO, RIGHE
     * sx=true  → pannello destra: NEXT piece, PREMIO
     */
    private void disegnaPannelloLaterale(Graphics2D g2,
                                         int px, int py, int pw, int ph, int cellH,
                                         java.util.function.Function<Float, java.awt.Font> cf,
                                         java.util.function.Function<Float, java.awt.Font> cfl,
                                         TetrisGame t, boolean isDestro) {

        // Sfondo pannello
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(px + 3, py + 4, pw, ph, 14, 14);
        GradientPaint panGrad = new GradientPaint(
                px, py, new Color(38, 36, 54),
                px, py + ph, new Color(22, 20, 34));
        g2.setPaint(panGrad);
        g2.fillRoundRect(px, py, pw, ph, 14, 14);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(80, 75, 110));
        g2.drawRoundRect(px, py, pw, ph, 14, 14);
        g2.setStroke(new BasicStroke(1f));

        if (t == null) return;

        int pad     = (int)(cellH * 0.4f);
        // Scala i font anche in base alla larghezza del pannello per evitare overflow
        int labelFs = Math.max(9,  Math.min((int)(cellH * 0.72f), (int)(pw * 0.18f)));
        int valFs   = Math.max(11, Math.min((int)(cellH * 0.92f), (int)(pw * 0.22f)));
        int cy      = py + pad + labelFs;

        if (!isDestro) {
            // ── Pannello SINISTRO: PUNTEGGIO · LIVELLO · RIGHE ───────────────
            String[][] voci = {
                    {"PUNTEGGIO", "" + t.punteggio},
                    {"LIVELLO",   "" + t.livello},
                    {"RIGHE",     "" + t.righe},
            };
            Color[] coloriVal = {
                    Color.WHITE,
                    new Color(255, 220, 80),
                    new Color(100, 220, 100),
            };
            for (int i = 0; i < voci.length; i++) {
                // Separatore
                if (i > 0) {
                    g2.setColor(new Color(70, 65, 100, 120));
                    g2.drawLine(px + pad, cy - labelFs, px + pw - pad, cy - labelFs);
                }
                g2.setFont(cfl.apply((float) labelFs));
                FontMetrics fmL = g2.getFontMetrics();
                g2.setColor(new Color(140, 135, 180));
                g2.drawString(voci[i][0], px + (pw - fmL.stringWidth(voci[i][0])) / 2, cy);
                cy += (int)(labelFs * 1.1f);
                g2.setFont(cf.apply((float) valFs));
                FontMetrics fmV = g2.getFontMetrics();
                g2.setColor(coloriVal[i]);
                g2.drawString(voci[i][1], px + (pw - fmV.stringWidth(voci[i][1])) / 2, cy);
                cy += (int)(valFs * 1.5f);
            }
        } else {
            // ── Pannello DESTRO: NEXT + PREMIO ───────────────────────────────
            // NEXT label
            g2.setFont(cfl.apply((float) labelFs));
            FontMetrics fmL = g2.getFontMetrics();
            g2.setColor(new Color(140, 135, 180));
            g2.drawString("PROSSIMO", px + (pw - fmL.stringWidth("PROSSIMO")) / 2, cy);
            cy += (int)(labelFs * 0.5f);

            // Next piece centrata
            int cellN  = Math.max(8, (int)(cellH * 0.75f));
            int nextW  = 4 * cellN;
            int nextX  = px + (pw - nextW) / 2;
            int nextYs = cy + 4;
            int nc     = TetrisGame.COLORI[t.tipoNext];
            for (int[] cell : t.getCelleNext())
                disegnaCellaTetris(g2, nextX + cell[0]*cellN, nextYs + cell[1]*cellN,
                        cellN - 1, cellN - 1, nc);
            cy += 4 * cellN + (int)(cellH * 0.6f);

            // Separatore
            g2.setColor(new Color(70, 65, 100, 120));
            g2.drawLine(px + pad, cy, px + pw - pad, cy);
            cy += (int)(labelFs * 0.9f);

            // PREMIO label
            g2.setFont(cfl.apply((float) labelFs));
            FontMetrics fmP = g2.getFontMetrics();
            g2.setColor(new Color(140, 135, 180));
            g2.drawString("PREMIO", px + (pw - fmP.stringWidth("PREMIO")) / 2, cy);
            cy += (int)(labelFs * 1.1f);

            String pu = t.getPowerUp();
            String puLabel = switch (pu) {
                case "CURA"     -> "+VITA";
                case "VELOCITA" -> "+VEL";
                case "DANNO"    -> "+DANNO";
                case "TUTTO"    -> "TUTTO!";
                default         -> "NESSUNO";
            };
            Color puColor = switch (pu) {
                case "CURA"     -> new Color(100, 220, 100);
                case "VELOCITA" -> new Color(100, 180, 255);
                case "DANNO"    -> new Color(255, 100, 100);
                case "TUTTO"    -> new Color(255, 215, 0);
                default         -> new Color(140, 140, 160);
            };
            g2.setFont(cf.apply((float) valFs));
            FontMetrics fmPV = g2.getFontMetrics();
            g2.setColor(puColor);
            g2.drawString(puLabel, px + (pw - fmPV.stringWidth(puLabel)) / 2, cy);
            cy += (int)(valFs * 1.5f);

            // Soglia prossimo premio
            String soglia = switch (pu) {
                case "TUTTO"    -> "";
                case "MELEE"   -> "6000: TUTTO";
                case "DANNO"    -> "5000: MELEE";
                case "VELOCITA" -> "3000: DANNO";
                case "CURA"     -> "1500: VEL";
                default         -> "500: CURA";
            };
            if (!soglia.isEmpty()) {
                int sFs = Math.max(8, (int)(labelFs * 0.80f));
                g2.setFont(cfl.apply((float) sFs));
                FontMetrics fmS = g2.getFontMetrics();
                g2.setColor(new Color(110, 105, 145));
                g2.drawString(soglia, px + (pw - fmS.stringWidth(soglia)) / 2, cy);
            }
        }
    }

    private void disegnaCellaTetris(Graphics2D g2, int x, int y, int w, int h, int rgb) {
        int r = (rgb >> 16) & 0xFF, gv = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        g2.setColor(new Color(r, gv, b));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(Math.min(255, r + 60), Math.min(255, gv + 60), Math.min(255, b + 60)));
        g2.drawLine(x, y, x + w, y);
        g2.drawLine(x, y, x, y + h);
        g2.setColor(new Color(Math.max(0, r - 60), Math.max(0, gv - 60), Math.max(0, b - 60)));
        g2.drawLine(x + w, y, x + w, y + h);
        g2.drawLine(x, y + h, x + w, y + h);
    }

    // ── Gioco ─────────────────────────────────────────────────────────────────

    private void disegnaGioco(Graphics2D g2, int W, int H) {
        // Applica scala + offset per adattare il gameplay alla finestra reale
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);

        // Sfondo: riempie tutta la finestra col colore muro del tema
        TileSet tsBg = tileSetCorrente();
        g2.setColor(tsBg.coloreTemaMuro);
        g2.fillRect(0, 0, W, H);

        // Salva transform, applica scala gameplay
        java.awt.geom.AffineTransform baseTransform = g2.getTransform();
        g2.translate(gox, goy);
        g2.scale(gs, gs);

        if (roomMgr.inStanzaBonus) {
            disegnaAmbiente(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            for (Cura     c  : roomMgr.getCureBonus())   c.draw(g2);
            for (ShopItem si : roomMgr.getItemsBonus())  si.draw(g2);
            if (pugniAttivi != null) for (Pugno p : pugniAttivi) p.draw(g2);
            if (proiettiliCannone != null) for (BossProjectile bp : proiettiliCannone) bp.draw(g2);
            for (Nemico n : roomMgr.getNemiciBonus()) {
                if (n instanceof NemicoForte) {
                    n.draw(g2, res.getNemicoForteSprite(state.mondoAttuale));
                    n.disegnaBarraVita(g2);
                } else {
                    n.draw(g2, res.getNemicoSprite(state.mondoAttuale));
                    n.disegnaBarraVita(g2);
                }
            }
            disegnaGiocatore(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            if (state.meleeTimer > 0) disegnaMelee(g2);
            g2.setTransform(baseTransform);
            disegnaHUD(g2, W, H);
            disegnaBannerArdua(g2, W, H);
            if (state.arduaRicompensaTimer > 0) disegnaPopupRicompensaArdua(g2, W, H);
            if (state.dialogoNarrazione.isAttivo()) disegnaDialogoNarrazione(g2, W, H);
            return;
        }

        if (roomMgr.inStanzaShop) {
            disegnaStanzaShop(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            for (Shopkeeper sk : roomMgr.getShopkeeperShop()) sk.draw(g2);
            for (ShopItem   si : roomMgr.getItemsShop())      si.draw(g2);
            if (pugniAttivi != null) for (Pugno p : pugniAttivi) p.draw(g2);
            if (proiettiliCannone != null) for (BossProjectile bp : proiettiliCannone) bp.draw(g2);
            for (Nemico n : roomMgr.getShopNemici()) { n.draw(g2, null); n.disegnaBarraVita(g2); }
            disegnaGiocatore(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
            g2.setTransform(baseTransform);
            disegnaHUD(g2, W, H);
            state.dialogoShopkeeper.disegna(g2);
            if (state.dialogoShopkeeper.isVisibile())
                disegnaSceltaShopkeeper(g2, W, H);
            // Dialogo narrazione shopkeeper (JRPG) — sopra il vecchio dialogo
            if (state.dialogoNarrazione.isAttivo())
                disegnaDialogoNarrazione(g2, W, H);
            return;
        }

        disegnaAmbiente(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        disegnaOggetti(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);

        // Disegna la nota in Casa
        Nota notaCasa = roomMgr.getNotaCasa();
        if (notaCasa != null) notaCasa.draw(g2);

        if (pugniAttivi != null) for (Pugno p : pugniAttivi) p.draw(g2);
        if (proiettiliCannone != null) for (BossProjectile bp : proiettiliCannone) bp.draw(g2);
        disegnaNemici(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        disegnaGiocatore(g2, GameState.LARGHEZZA_GIOCO, GameState.ALTEZZA_GIOCO);
        if (state.meleeTimer > 0) disegnaMelee(g2);

        // Ripristina transform e disegna HUD sopra (non scalato)
        g2.setTransform(baseTransform);
        disegnaHUD(g2, W, H);

        // Banner stanza Casa (prima stanza mondo 1)
        if (state.mondoAttuale == 1 && state.stanzaNelMondo == 1
                && state.stanzaCasaVisitata) {
            disegnaBannerCasa(g2, W, H);
        }

        // Banner stanza bonus — solo quando SEI dentro, non nella stanza che porta alla porta
        if (roomMgr.inStanzaBonus) {
            disegnaBannerArdua(g2, W, H);
        }

        // Dialogo narrazione (boss intro + shopkeeper JRPG)
        if (state.dialogoNarrazione.isAttivo()) {
            disegnaDialogoNarrazione(g2, W, H);
        }

        // Popup nota con codice debug
        if (state.mostraNota) {
            disegnaPopupNota(g2, W, H);
        }

        // Dialogo Casa
        if (state.mostraDialogoCasa) {
            disegnaDialogoCasa(g2, W, H);
        }
    }

    // ── Stanza Shop ───────────────────────────────────────────────────────────

    private void disegnaStanzaShop(Graphics2D g2, int W, int H) {
        TileSet ts = tileSetCorrente();

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                if (TileSet.isMuro(i, j)) {
                    if (ts.imgMuro != null)
                        g2.drawImage(ts.imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(40, 20, 10)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (ts.imgPavimento != null)
                        g2.drawImage(ts.imgPavimento, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(new Color(80, 60, 40)); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        // Targa "NEGOZIO" in cima con font custom
        String negozioTxt = "NEGOZIO";
        Font fontNegozio = res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, 30f)
                : new Font("Consolas", Font.BOLD, 28);
        g2.setFont(fontNegozio);
        FontMetrics fmN = g2.getFontMetrics();
        int negX = W / 2 - fmN.stringWidth(negozioTxt) / 2;
        int negY = GameState.TILE_SIZE - 10;
        // Ombra
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(negozioTxt, negX + 2, negY + 2);
        // Testo dorato
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(negozioTxt, negX, negY);

        // Porta sud (per uscire) al centro in basso
        int portaSudX = (GameState.COL_TOTALI / 2) * GameState.TILE_SIZE;
        int portaSudY = (GameState.RIG_TOTALI - 1) * GameState.TILE_SIZE;
        if (res.imgPorta != null) {
            g2.drawImage(res.imgPorta, portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
        } else {
            g2.setColor(new Color(0, 200, 100, 180));
            g2.fillRect(portaSudX, portaSudY, GameState.TILE_SIZE, GameState.TILE_SIZE);
        }


        // Oggetti shop
        for (Shopkeeper sk : roomMgr.getShopkeeperShop()) sk.draw(g2);
        for (ShopItem   si : roomMgr.getItemsShop())      si.draw(g2);
    }


    /** Scala del gameplay: adatta le coordinate tile/giocatore alla finestra reale. */
    private double gameScale(int W, int H) {
        return Math.min(
                (double) W / GameState.LARGHEZZA_GIOCO,
                (double) H / GameState.ALTEZZA_GIOCO
        );
    }
    /** Offset X per centrare il gioco nella finestra reale. */
    private int gameOffX(int W, int H) {
        return (W - (int)(GameState.LARGHEZZA_GIOCO * gameScale(W, H))) / 2;
    }
    /** Offset Y per centrare il gioco nella finestra reale. */
    private int gameOffY(int W, int H) {
        return (H - (int)(GameState.ALTEZZA_GIOCO * gameScale(W, H))) / 2;
    }

    // ── Font helpers ──────────────────────────────────────────────────────────



    /**
     * Disegna testo usando (in ordine di priorità):
     *  1. fontCustom (PHONIXEA.ttf) se caricato
     *  2. BitmapFont (font.png) se disponibile
     *  3. Font Java corrente come fallback
     */
    private void drawText(Graphics2D g2, String testo, int x, int y, int altPx) {
        if (res.fontCustom != null) {
            java.awt.Font f = res.fontCustom.deriveFont((float) altPx);
            g2.setFont(f);
            g2.drawString(testo, x, y);
        } else if (res.bitmapFont != null && res.bitmapFont.isDisponibile()) {
            float sc = res.bitmapFont.scalaPer(altPx);
            int topY = y - (int)(res.bitmapFont.getCharH() * sc);
            res.bitmapFont.disegna(g2, testo.toUpperCase(), x, topY, sc);
        } else {
            g2.drawString(testo, x, y);
        }
    }

    private void drawTextCentered(Graphics2D g2, String testo, int centroX, int y, int altPx) {
        if (res.fontCustom != null) {
            java.awt.Font f = res.fontCustom.deriveFont((float) altPx);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, centroX - fm.stringWidth(testo)/2, y);
        } else if (res.bitmapFont != null && res.bitmapFont.isDisponibile()) {
            float sc = res.bitmapFont.scalaPer(altPx);
            int topY = y - (int)(res.bitmapFont.getCharH() * sc);
            res.bitmapFont.disegnaCentrato(g2, testo.toUpperCase(), centroX, topY, sc);
        } else {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, centroX - fm.stringWidth(testo)/2, y);
        }
    }


    private void disegnaAmbiente(Graphics2D g2, int W, int H) {
        TileSet ts = tileSetCorrente();

        for (int i = 0; i < GameState.COL_TOTALI; i++) {
            for (int j = 0; j < GameState.RIG_TOTALI; j++) {
                int px = i * GameState.TILE_SIZE;
                int py = j * GameState.TILE_SIZE;
                if (TileSet.isMuro(i, j)) {
                    if (ts.imgMuro != null)
                        g2.drawImage(ts.imgMuro, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(ts.coloreTemaMuro); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                } else {
                    if (ts.imgPavimento != null)
                        g2.drawImage(ts.imgPavimento, px, py, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                    else { g2.setColor(ts.coloreTemaFondo); g2.fillRect(px, py, GameState.TILE_SIZE, GameState.TILE_SIZE); }
                }
            }
        }

        // ── Ostacoli inagibili sovrapposti alle tile ─────────────────────────────
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        if (ostacoli != null && state.statoGioco == GameState.StatoGioco.GIOCO) {
            int mondoIdx = Math.min(4, Math.max(0, state.mondoAttuale - 1));
            BufferedImage imgOst = res.imgOstacoloPerMondo[Math.min(4, mondoIdx)];
            if (imgOst == null) imgOst = res.imgOstacolo;
            for (int[] o : ostacoli) {
                int ox = o[0] * GameState.TILE_SIZE;
                int oy = o[1] * GameState.TILE_SIZE;
                if (imgOst != null) {
                    g2.drawImage(imgOst, ox, oy, GameState.TILE_SIZE, GameState.TILE_SIZE, null);
                } else {
                    g2.setColor(new Color(40, 20, 10, 200));
                    g2.fillRect(ox + 4, oy + 4, GameState.TILE_SIZE - 8, GameState.TILE_SIZE - 8);
                    g2.setColor(new Color(100, 50, 20));
                    g2.setStroke(new java.awt.BasicStroke(3));
                    g2.drawLine(ox + 8, oy + 8, ox + GameState.TILE_SIZE - 8, oy + GameState.TILE_SIZE - 8);
                    g2.drawLine(ox + GameState.TILE_SIZE - 8, oy + 8, ox + 8, oy + GameState.TILE_SIZE - 8);
                    g2.setStroke(new java.awt.BasicStroke(1));
                }
            }
        }

        // ── Tile effetto sovrapposti al pavimento (veleno/ghiaccio/fuoco/cannone)
        java.util.List<TileEffetto> tileEff = roomMgr.getTileEffettoCorrenti();
        if (tileEff != null && !tileEff.isEmpty()) {
            for (TileEffetto te : tileEff) {
                BufferedImage imgTe = switch (te.tipo) {
                    case VELENO         -> res.imgTileVeleno;
                    case GHIACCIO       -> res.imgTileGhiaccio;
                    case GHIACCIO_FORTE -> res.imgTileGhiaccioForte != null ? res.imgTileGhiaccioForte : res.imgTileGhiaccio;
                    case FUOCO          -> res.imgTileFuoco;
                    case CANNONE        -> res.imgTileCannone;
                };
                te.draw(g2, GameState.TILE_SIZE, imgTe);
            }
        }

        final int T = GameState.TILE_SIZE;

        // ── Porta sinistra (torna indietro) ───────────────────────────────────
        int portaY  = (GameState.RIG_TOTALI / 2) * T;
        if (state.indiceStanzaMemoria > 0) {
            if (res.imgPorta != null) g2.drawImage(res.imgPorta, 0, portaY, T, T, null);
            else { g2.setColor(new Color(150, 100, 50)); g2.fillRect(0, portaY, T, T); }
        }

        // ── Porta destra (avanza) — appare SOLO se stanza pulita ──────────────
        int  portaDX    = (GameState.COL_TOTALI - 1) * T;
        boolean stanzaPulita = roomMgr.getNemiciCorrenti().isEmpty();
        boolean bossUscita   = state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.bossSpawnato && state.bossSconfitto;

        if (bossUscita) {
            // Porta speciale cyan dopo il boss
            g2.setColor(new Color(0, 255, 255, 180));
            g2.fillRect(portaDX, portaY, T, T);
        } else if (stanzaPulita || state.stanzaNelMondo == 1) {
            // Porta normale visibile solo a stanza pulita (o stanza 1 che è sempre vuota)
            if (res.imgPorta != null) g2.drawImage(res.imgPorta, portaDX, portaY, T, T, null);
            else { g2.setColor(new Color(150, 100, 50)); g2.fillRect(portaDX, portaY, T, T); }
        }
        // Se nemici ancora vivi: non disegna nulla → il muro appare chiuso

        // ── Porta nord SHOP — solo stanza 1 + shopSbloccato ──────────────────
        if (state.stanzaNelMondo == 1 && state.shopSbloccato) {
            int portaShopX = (GameState.COL_TOTALI / 2) * T;
            if (res.imgShopDoor != null) {
                g2.drawImage(res.imgShopDoor, portaShopX, 0, T, T, null);
            } else {
                g2.setColor(new Color(255, 215, 0, 200));
                g2.fillRect(portaShopX, 0, T, T);
            }
        }

        // ── Porta sud STANZA BONUS ─────────────────────────────────────────────
        if (state.stanzaNelMondo == state.stanzaConPortaArdua && stanzaPulita && !state.ardua_completed) {
            int pdX = (GameState.COL_TOTALI / 2) * T - T / 2; // larga 2 tile
            int pdY = (GameState.RIG_TOTALI - 1) * T;
            int pdW = T * 2;
            int pdH = T;
            long pt  = System.currentTimeMillis();
            float pp = 0.65f + 0.35f * (float)Math.sin(pt * 0.005);
            // Sfondo rosso scuro
            g2.setColor(new Color(90, 0, 0, Math.max(0, Math.min(255, (int)(230 * pp)))));
            g2.fillRect(pdX, pdY, pdW, pdH);
            // Bordo brillante
            g2.setColor(new Color(255, 60, 60, Math.max(0, Math.min(255, (int)(255 * pp)))));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRect(pdX, pdY, pdW, pdH);
            g2.setStroke(new BasicStroke(1f));
            // Freccia giù + scritta B
            g2.setFont(res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)(T - 2))
                    : new Font("Consolas", Font.BOLD, T - 2));
            g2.setColor(new Color(255, 200, 200));
            FontMetrics fmP = g2.getFontMetrics();
            String lblP = "B";
            g2.drawString(lblP, pdX + (pdW - fmP.stringWidth(lblP)) / 2,
                    pdY + (pdH + fmP.getAscent()) / 2 - 3);
        }
    }

    /** Porta nord di uscita dalla stanza bonus (visibile solo quando pulita). */
    /** Popup ricompensa ardua — mostra il boost ottenuto al centro dello schermo. */
    private void disegnaPopupRicompensaArdua(Graphics2D g2, int W, int H) {
        if (state.arduaRicompensaMsg == null || state.arduaRicompensaMsg.isEmpty()) return;
        float alpha = Math.min(1f, state.arduaRicompensaTimer / 60f);
        int boxW = (int)(W * 0.55f);
        int boxH = (int)(H * 0.12f);
        int boxX = W / 2 - boxW / 2;
        int boxY = (int)(H * 0.38f);
        g2.setColor(new Color(20, 0, 0, Math.max(0, Math.min(255, (int)(210 * alpha)))));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setColor(new Color(255, 80, 80, Math.max(0, Math.min(255, (int)(230 * alpha)))));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setStroke(new BasicStroke(1f));
        int fs = Math.max(14, (int)(H * 0.045f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs)
                : new Font("Consolas", Font.BOLD, fs));
        g2.setColor(new Color(255, 220, 220, Math.max(0, Math.min(255, (int)(255 * alpha)))));
        FontMetrics fm = g2.getFontMetrics();
        String msg = state.arduaRicompensaMsg;
        g2.drawString(msg, W/2 - fm.stringWidth(msg)/2, boxY + (boxH + fm.getAscent())/2 - 4);
    }

    private void disegnaOggetti(Graphics2D g2, int W, int H) {
        for (Cura       c  : roomMgr.getCureCorrenti())         c.draw(g2);
        for (Moneta     m  : roomMgr.getMoneteCorrenti())       m.draw(g2);
        for (Shopkeeper sk : roomMgr.getShopkeepersCorrenti())  sk.draw(g2);
        for (ShopItem   si : roomMgr.getShopItemsCorrenti())    si.draw(g2);
    }

    private void disegnaNemici(Graphics2D g2, int W, int H) {
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if (n instanceof Boss) {
                n.draw(g2, res.getBossSprite(state.mondoAttuale));
                // Boss: solo barra HUD grande, NON quella piccola
            } else if (n instanceof NemicoForte) {
                n.draw(g2, res.getNemicoForteSprite(state.mondoAttuale));
                n.disegnaBarraVita(g2);
            } else {
                n.draw(g2, res.getNemicoSprite(state.mondoAttuale));
                n.disegnaBarraVita(g2);
            }
        }
    }

    private void disegnaGiocatore(Graphics2D g2, int W, int H) {
        // Lampeggio durante invulnerabilità
        if (state.invulnerabile && state.timerInvulnerabilita % 10 >= 5) return;

        int px = (int) state.x, py = (int) state.y;
        int ps = GameState.PG_SIZE;

        // ── Effetto burn: alone arancione pulsante ────────────────────────────
        if (state.burnAttivo) {
            float burnPulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() * 0.012f);
            int   glowAlpha = Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(120 * burnPulse)))));
            int   glowSize  = (int)(ps * 1.35f);
            int   glowOff   = (glowSize - ps) / 2;
            g2.setColor(new Color(255, 80, 0, glowAlpha / 2));
            g2.fillOval(px - glowOff - 4, py - glowOff - 4, glowSize + 8, glowSize + 8);
            g2.setColor(new Color(255, 140, 0, glowAlpha));
            g2.fillOval(px - glowOff, py - glowOff, glowSize, glowSize);
        }

        // Slow: alone azzurro tenue
        if (state.slowAttivo && !state.freezeAttivo) {
            float sp = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.008f);
            g2.setColor(new Color(100, 200, 255, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(70 * sp)))))));
            g2.fillOval(px - 3, py - 3, ps + 6, ps + 6);
        }

        // Freeze: alone blu + cristalli
        if (state.freezeAttivo) {
            float fp = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() * 0.005f);
            int glowSz = (int)(ps * 1.4f), glowOf = (glowSz - ps) / 2;
            g2.setColor(new Color(60, 160, 255, Math.max(0, Math.min(255, Math.max(0, Math.min(255, (int)(140 * fp)))))));
            g2.fillOval(px - glowOf, py - glowOf, glowSz, glowSz);
            g2.setColor(new Color(180, 230, 255, 200));
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawOval(px - glowOf, py - glowOf, glowSz, glowSz);
            g2.setStroke(new java.awt.BasicStroke(1f));
        }

        // ── Sprite giocatore ──────────────────────────────────────────────────
        BufferedImage imgPG = ui.listaPersonaggi
                .get(state.indicePersonaggioSelezionato).imgGioco;
        if (imgPG != null) {
            g2.drawImage(imgPG, px, py, ps, ps, null);
        } else {
            g2.setColor(Color.CYAN);
            g2.fillOval(px, py, ps, ps);
        }

        // ── Overlay burn PNG sopra il personaggio ─────────────────────────────
        if (state.burnAttivo && res.imgBurn != null) {
            float alpha = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.015f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(res.imgBurn, px, py, ps, ps, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else if (state.burnAttivo) {
            g2.setColor(new Color(255, 80, 0, 110));
            g2.fillRect(px, py, ps, ps);
        }

        // Overlay freeze (cristallo semitrasparente blu)
        if (state.freezeAttivo) {
            g2.setColor(new Color(120, 200, 255, 160));
            g2.fillRect(px, py, ps, ps);
            // Linee ghiaccio
            g2.setColor(new Color(200, 240, 255, 200));
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawLine(px + ps/2, py + 2, px + ps/2, py + ps - 2);
            g2.drawLine(px + 2, py + ps/2, px + ps - 2, py + ps/2);
            g2.drawLine(px + 4, py + 4, px + ps - 4, py + ps - 4);
            g2.drawLine(px + ps - 4, py + 4, px + 4, py + ps - 4);
            g2.setStroke(new java.awt.BasicStroke(1f));
        }
    }

    // ── Dialogo Narrazione JRPG (condiviso boss + shopkeeper) ────────────────

    // ── Attacco melee — effetto visivo per personaggio ────────────────────────

    private void disegnaMelee(Graphics2D g2) {
        int cx = state.meleeHitX;
        int cy = state.meleeHitY;
        int t  = state.meleeTimer;        // frame rimanenti (da MELEE_DURATION a 0)
        int D  = GameState.MELEE_DURATION;
        float prog = 1f - (float) t / D;  // 0.0 = inizio, 1.0 = fine

        // Direzione attacco
        int fx = state.facingX, fy = state.facingY;
        double angolo = Math.atan2(fy, fx == 0 && fy == 0 ? 1 : fy == 0 ? fx : fy);
        if (fx != 0 || fy != 0) angolo = Math.atan2(fy, fx);

        java.awt.Composite origComp = g2.getComposite();
        java.awt.Stroke    origStroke = g2.getStroke();

        float alpha = Math.max(0f, 1f - prog * 1.4f);

        switch (state.indicePersonaggioSelezionato) {

            case 0 -> {
                // BELLGERD — fendente: arco di lama giallo-bianco
                int r = 52;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.85f));
                // Alone
                g2.setColor(new Color(255, 240, 100, 80));
                g2.fillOval(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2);
                // Arco luminoso
                g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 230, 60));
                int startAng = (int) Math.toDegrees(angolo) - 70;
                g2.drawArc(cx - r, cy - r, r * 2, r * 2, startAng, 140);
                // Filo della lama
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(Color.WHITE);
                g2.drawArc(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8, startAng + 10, 120);
            }

            case 1 -> {
                // VLAD — stilettata: linea veloce viola verso il target
                int len = 65;
                float fadeAlpha = alpha * 0.9f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                int x2 = cx + (int)(fx * len), y2 = cy + (int)(fy * len);
                int x0 = cx - (int)(fx * 20), y0 = cy - (int)(fy * 20);
                // Glow viola
                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(180, 50, 255, 80));
                g2.drawLine(x0, y0, x2, y2);
                // Lama principale
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(220, 140, 255));
                g2.drawLine(x0, y0, x2, y2);
                // Punta brillante
                g2.setColor(Color.WHITE);
                g2.fillOval(x2 - 5, y2 - 5, 10, 10);
            }

            case 2 -> {
                // PAUL — martellata: cerchio d'impatto rosso-arancio con onde
                int r1 = (int)(30 + prog * 35);
                int r2 = (int)(50 + prog * 25);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.7f));
                // Onda esterna
                g2.setStroke(new BasicStroke(5f));
                g2.setColor(new Color(255, 80, 0, 120));
                g2.drawOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
                // Cerchio impatto
                g2.setColor(new Color(255, 120, 30, 160));
                g2.fillOval(cx - r1, cy - r1, r1 * 2, r1 * 2);
                // Nucleo
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(255, 220, 80));
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                // Crepe (4 linee radiali)
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 60, 0, Math.max(0, Math.min(255, (int)(200 * alpha)))));
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4 + i * Math.PI / 2;
                    int ex = (int)(cx + Math.cos(a) * r2);
                    int ey = (int)(cy + Math.sin(a) * r2);
                    g2.drawLine(cx, cy, ex, ey);
                }
            }

            case 3 -> {
                // JUICY — schianto: onda di pressione verde + schiacciata
                int r = (int)(40 + prog * 30);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.75f));
                // Aura verde grassa
                g2.setColor(new Color(50, 220, 80, 100));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                // Bordo
                g2.setStroke(new BasicStroke(6f));
                g2.setColor(new Color(80, 255, 120));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                // Nucleo bianco
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(180, 255, 180));
                g2.fillOval(cx - 18, cy - 18, 36, 36);
                // Impatto: 3 linee a cuneo nella direzione
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(50, 220, 80, Math.max(0, Math.min(255, (int)(200 * alpha)))));
                for (int i = -1; i <= 1; i++) {
                    double a = angolo + i * 0.35;
                    int ex = (int)(cx + Math.cos(a) * (r + 20));
                    int ey = (int)(cy + Math.sin(a) * (r + 20));
                    g2.drawLine(cx, cy, ex, ey);
                }
            }

            case 4 -> {
                // G.O.D. — esplosione divina: stella a 8 punte oro con glow totale
                int r = (int)(55 + prog * 40);
                // Alone esterno ampio
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.4f));
                g2.setColor(new Color(255, 255, 200, 80));
                g2.fillOval(cx - r - 20, cy - r - 20, (r + 20) * 2, (r + 20) * 2);
                // Raggi della stella (8 direzioni)
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.9f));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    int ex = (int)(cx + Math.cos(a) * r);
                    int ey = (int)(cy + Math.sin(a) * r);
                    // Raggio esterno oro
                    g2.setColor(new Color(255, 200, 0, Math.max(0, Math.min(255, (int)(200 * alpha)))));
                    g2.drawLine(cx, cy, ex, ey);
                    // Raggio bianco interno
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(Color.WHITE);
                    g2.drawLine(cx, cy, (int)(cx + Math.cos(a) * r * 0.5), (int)(cy + Math.sin(a) * r * 0.5));
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                // Nucleo oro brillante
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(255, 220, 0));
                g2.fillOval(cx - 22, cy - 22, 44, 44);
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 10, cy - 10, 20, 20);
            }
        }

        g2.setComposite(origComp);
        g2.setStroke(origStroke);
    }

    private void disegnaSceltaShopkeeper(Graphics2D g2, int W, int H) {
        if (!state.dialogoShopkeeper.isVisibile()) return;

        // ── Dim overlay ───────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, W, H);

        // ── Layout box (identico al dialogo narrazione) ───────────────────────
        int boxH  = (int)(H * 0.30f);
        int boxY  = H - boxH - (int)(H * 0.03f);
        int boxX  = (int)(W * 0.04f);
        int boxW  = W - boxX * 2;
        int pad   = (int)(W * 0.018f);
        int sprSz = boxH - pad * 2;
        int sprX  = boxX + pad;
        int sprY  = boxY + pad;
        int txtX  = sprX + sprSz + pad;
        int txtW  = boxW - sprSz - pad * 3;

        // Colore bordo rosso-arancio (come nemico/boss)
        Color borderColor = new Color(220, 80, 60);
        Color bgColor     = new Color(22, 8, 8, 248);

        // Ombra
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(boxX + 5, boxY + 5, boxW, boxH, 18, 18);

        // Sfondo
        g2.setColor(bgColor);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        // Bordo esterno
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(borderColor);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        // Bordo interno
        g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                borderColor.getBlue(), 70));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 15, 15);
        g2.setStroke(new BasicStroke(1f));

        // ── Sprite shopkeeper ─────────────────────────────────────────────────
        BufferedImage sprSk = res.imgShopkeeper;
        if (sprSk != null) {
            g2.setColor(new Color(20, 15, 35));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(sprSk, sprX, sprY, sprSz, sprSz, null);
        }

        // ── Nome (font custom) ────────────────────────────────────────────────
        int nomeFs = Math.max(13, (int)(H * 0.028f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));
        FontMetrics fmN = g2.getFontMetrics();
        String nomeShop = "NEGOZIANTE";
        int nomeBgW = fmN.stringWidth(nomeShop) + 18;
        Color nomeColor = new Color(255, 120, 80);
        g2.setColor(new Color(nomeColor.getRed() / 4, nomeColor.getGreen() / 4,
                nomeColor.getBlue() / 4, 210));
        g2.fillRoundRect(txtX - 2, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);
        g2.setColor(nomeColor);
        g2.drawString(nomeShop, txtX + 6, sprY + fmN.getAscent());

        // ── Testo domanda ─────────────────────────────────────────────────────
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();
        int lineY = sprY + fmN.getHeight() + (int)(H * 0.013f);

        // Riga 1: domanda
        String domanda = "Vuoi attaccare il negoziante?";
        disegnaRigaDialogo(g2, domanda, txtX, lineY);
        lineY += fmT.getHeight() + 3;

        // Riga 2: conseguenza in grigio
        int avvFs = Math.max(9, (int)(H * 0.017f));
        g2.setFont(new Font("Consolas", Font.PLAIN, avvFs));
        g2.setColor(new Color(180, 130, 120));
        g2.drawString("Otterrai 20 monete, ma perderai lo shop.", txtX, lineY);

        // ── Bottoni SI / NO nello stile del box ──────────────────────────────
        int btnW   = (int)(txtW * 0.28f);
        int btnH   = Math.max(30, (int)(H * 0.055f));
        int btnGap = (int)(txtW * 0.06f);
        int btnY   = boxY + boxH - btnH - pad;
        int btnX0  = txtX;

        int scelta = state.dialogoShopkeeper.getScelta(); // 0=No, 1=Si

        String[] etichette = { "NO", "SI" };
        Color[] coloriSfondo = {
                new Color(20, 50, 20),
                new Color(60, 15, 15)
        };
        Color[] coloriSel = {
                new Color(40, 120, 40),
                new Color(150, 30, 30)
        };
        Color[] coloriBordo = {
                new Color(60, 180, 60),
                new Color(220, 60, 60)
        };

        for (int i = 0; i < 2; i++) {
            boolean sel = (i == scelta);
            int bx = btnX0 + i * (btnW + btnGap);

            // Ombra
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(bx + 3, btnY + 3, btnW, btnH, 10, 10);

            // Sfondo
            g2.setColor(sel ? coloriSel[i] : coloriSfondo[i]);
            g2.fillRoundRect(bx, btnY, btnW, btnH, 10, 10);

            // Bordo
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
            g2.setColor(sel ? coloriBordo[i] : new Color(80, 80, 80));
            g2.drawRoundRect(bx, btnY, btnW, btnH, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Glow se selezionato
            if (sel) {
                g2.setColor(new Color(coloriBordo[i].getRed(),
                        coloriBordo[i].getGreen(), coloriBordo[i].getBlue(), 40));
                g2.fillRoundRect(bx - 3, btnY - 3, btnW + 6, btnH + 6, 13, 13);
            }

            // Testo bottone (font custom)
            int btnFs = Math.max(12, (int)(H * 0.025f));
            g2.setFont(res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) btnFs)
                    : new Font("Consolas", Font.BOLD, btnFs));
            FontMetrics fmB = g2.getFontMetrics();
            g2.setColor(sel ? Color.WHITE : new Color(160, 160, 160));
            int lx = bx + (btnW - fmB.stringWidth(etichette[i])) / 2;
            int ly = btnY + (btnH + fmB.getAscent() - fmB.getDescent()) / 2;
            // Ombra testo
            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString(etichette[i], lx + 1, ly + 1);
            g2.setColor(sel ? Color.WHITE : new Color(160, 160, 160));
            g2.drawString(etichette[i], lx, ly);
        }

        // ── Hint tasti ────────────────────────────────────────────────────────
        int hintFs = Math.max(9, (int)(H * 0.015f));
        g2.setFont(new Font("Consolas", Font.ITALIC, hintFs));
        g2.setColor(new Color(120, 100, 100));
        String hint = "A/D per scegliere   INVIO per confermare   ESC per annullare";
        g2.drawString(hint, boxX + pad, boxY + boxH - 6);
    }

    private void disegnaDialogoNarrazione(Graphics2D g2, int W, int H) {
        DialogoNarrazione.Pagina pag = state.dialogoNarrazione.getPagina();
        if (pag == null) return;

        // ── Layout ────────────────────────────────────────────────────────────
        int boxH  = (int)(H * 0.30f);
        int boxY  = H - boxH - (int)(H * 0.03f);
        int boxX  = (int)(W * 0.04f);
        int boxW  = W - boxX * 2;
        int pad   = (int)(W * 0.018f);
        int sprSz = boxH - pad * 2;

        boolean isLeft = pag.isLeft;
        int sprX = isLeft ? boxX + pad : boxX + boxW - pad - sprSz;
        int sprY = boxY + pad;
        int txtX = isLeft ? sprX + sprSz + pad : boxX + pad;
        int txtW = boxW - sprSz - pad * 3;

        // ── Dim sfondo ────────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, W, H);

        // ── Box principale ────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(boxX + 5, boxY + 5, boxW, boxH, 18, 18);

        // Colore bordo: viola per protagonista, rosso-arancio per boss/nemico
        Color borderColor = isLeft ? new Color(160, 100, 255) : new Color(220, 80, 60);
        Color bgColor     = isLeft ? new Color(12, 8, 22, 248) : new Color(22, 8, 8, 248);
        g2.setColor(bgColor);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(borderColor);
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2.setStroke(new BasicStroke(1f));
        // Bordo interno
        g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                borderColor.getBlue(), 70));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 15, 15);

        // ── Sprite ────────────────────────────────────────────────────────────
        if (pag.sprite != null) {
            g2.setColor(new Color(20, 15, 35));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprSz + 6, sprSz + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(pag.sprite, sprX, sprY, sprSz, sprSz, null);
        }

        // ── Nome (font custom) ────────────────────────────────────────────────
        int nomeFs = Math.max(13, (int)(H * 0.028f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));
        FontMetrics fmN = g2.getFontMetrics();
        int nomeBgW = fmN.stringWidth(pag.nome) + 18;
        // Sfondo nome
        Color nomeColor = isLeft ? new Color(255, 215, 0) : new Color(255, 120, 80);
        g2.setColor(new Color(nomeColor.getRed() / 4, nomeColor.getGreen() / 4,
                nomeColor.getBlue() / 4, 210));
        int nomeBgX = isLeft ? txtX - 2 : txtX - 2;
        g2.fillRoundRect(nomeBgX, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);
        g2.setColor(nomeColor);
        g2.drawString(pag.nome, txtX + 6, sprY + fmN.getAscent());

        // ── Testo con wrap ────────────────────────────────────────────────────
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();
        int lineY  = sprY + fmN.getHeight() + (int)(H * 0.013f);
        int lineH  = fmT.getHeight() + 3;

        String[] parole = pag.testo.split(" ");
        StringBuilder riga = new StringBuilder();
        for (String p : parole) {
            String prova = riga.isEmpty() ? p : riga + " " + p;
            if (fmT.stringWidth(prova) > txtW) {
                disegnaRigaDialogo(g2, riga.toString(), txtX, lineY);
                lineY += lineH;
                riga = new StringBuilder(p);
            } else {
                riga = new StringBuilder(prova);
            }
        }
        if (!riga.isEmpty()) disegnaRigaDialogo(g2, riga.toString(), txtX, lineY);

        // ── Indicatore pagina ─────────────────────────────────────────────────
        int tot = state.dialogoNarrazione.getTotale();
        int cur = state.dialogoNarrazione.getIndice() + 1;
        // Pallini paginazione
        int dotR = Math.max(4, (int)(H * 0.007f));
        int dotY = boxY + boxH - dotR * 2 - (int)(H * 0.012f);
        int dotSpacing = dotR * 3;
        int dotsW = tot * dotSpacing - dotR;
        int dotStartX = boxX + (boxW - dotsW) / 2;
        for (int i = 0; i < tot; i++) {
            g2.setColor(i + 1 == cur ? borderColor : new Color(80, 75, 100));
            g2.fillOval(dotStartX + i * dotSpacing, dotY, dotR * 2, dotR * 2);
        }

        // ▼ lampeggiante
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (blink) {
            int hFs = Math.max(9, (int)(H * 0.016f));
            g2.setFont(new Font("Arial", Font.ITALIC, hFs));
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(),
                    borderColor.getBlue(), 200));
            String hint = cur < tot ? "[ INVIO per continuare ]" : "[ INVIO per iniziare ]";
            FontMetrics fmH = g2.getFontMetrics();
            g2.drawString(hint, boxX + boxW - fmH.stringWidth(hint) - pad,
                    boxY + boxH - pad / 2);
        }
    }

    private void disegnaRigaDialogo(Graphics2D g2, String testo, int x, int y) {
        // Glow leggero
        g2.setColor(new Color(200, 170, 255, 45));
        g2.drawString(testo, x + 1, y + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(testo, x, y);
    }

    // ── Popup Nota (codice debug) ─────────────────────────────────────────────

    private void disegnaPopupNota(Graphics2D g2, int W, int H) {
        // Overlay scuro
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        // Box centrale stile "foglio trovato"
        int bw = (int)(W * 0.52f), bh = (int)(H * 0.48f);
        int bx = W / 2 - bw / 2,  by = H / 2 - bh / 2;

        // Ombra
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(bx + 6, by + 6, bw, bh, 16, 16);

        // Sfondo pergamena
        g2.setColor(new Color(240, 232, 195));
        g2.fillRoundRect(bx, by, bw, bh, 16, 16);
        g2.setColor(new Color(180, 160, 100));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(bx, by, bw, bh, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        // Righe orizzontali stile carta a righe
        g2.setColor(new Color(180, 165, 120, 120));
        int rigaH = (int)(H * 0.04f);
        for (int r = by + 55; r < by + bh - 20; r += rigaH) {
            g2.drawLine(bx + 20, r, bx + bw - 20, r);
        }

        int pad = (int)(W * 0.03f);

        // Intestazione — font custom, stile titolo scritto a mano
        int titoloFs = Math.max(14, (int)(H * 0.032f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) titoloFs)
                : new Font("Consolas", Font.BOLD, titoloFs));
        g2.setColor(new Color(80, 50, 20));
        String titolo = "NOTA DI SERVIZIO";
        FontMetrics fmT = g2.getFontMetrics();
        g2.drawString(titolo, W / 2 - fmT.stringWidth(titolo) / 2, by + 38);

        // Separatore
        g2.setColor(new Color(140, 110, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(bx + pad, by + 48, bx + bw - pad, by + 48);
        g2.setStroke(new BasicStroke(1f));

        // Corpo del testo
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.PLAIN, testoFs));
        g2.setColor(new Color(50, 35, 15));
        FontMetrics fmB = g2.getFontMetrics();

        String[] righe = {
                "A chiunque trovi questo foglio,",
                "",
                "Se il sistema smette di rispondere,",
                "usa questo codice per il pannello",
                "di diagnostica:",
                "",
        };

        int ly = by + 62;
        for (String riga : righe) {
            g2.drawString(riga, bx + pad, ly);
            ly += fmB.getHeight() + 2;
        }

        // Codice debug — grande, evidenziato
        int codiceFs = Math.max(18, (int)(H * 0.045f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) codiceFs)
                : new Font("Consolas", Font.BOLD, codiceFs));
        FontMetrics fmC = g2.getFontMetrics();
        String codice = GameState.CODICE_DEBUG;

        // Sfondo evidenziatore giallo
        int cw = fmC.stringWidth(codice) + 20;
        int ch = fmC.getHeight() + 8;
        int cx = W / 2 - cw / 2;
        g2.setColor(new Color(255, 230, 60, 200));
        g2.fillRoundRect(cx, ly - fmC.getAscent() - 4, cw, ch, 8, 8);
        g2.setColor(new Color(160, 120, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cx, ly - fmC.getAscent() - 4, cw, ch, 8, 8);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(60, 30, 0));
        g2.drawString(codice, W / 2 - fmC.stringWidth(codice) / 2, ly);
        ly += fmC.getHeight() + 8;

        // Firma
        g2.setFont(new Font("Consolas", Font.ITALIC, Math.max(10, testoFs - 2)));
        g2.setColor(new Color(100, 70, 30));
        String firma = "- Il Capo";
        g2.drawString(firma, bx + bw - pad - g2.getFontMetrics().stringWidth(firma), by + bh - 22);

        // Hint chiudi
        g2.setFont(new Font("Consolas", Font.ITALIC, Math.max(9, (int)(H * 0.016f))));
        g2.setColor(new Color(100, 80, 50));
        String hint = "[ INVIO per chiudere ]";
        g2.drawString(hint, W / 2 - g2.getFontMetrics().stringWidth(hint) / 2, by + bh + 18);
    }

    private void disegnaDialogoCasa(Graphics2D g2, int W, int H) {
        // ── Layout ────────────────────────────────────────────────────────────
        int boxH   = (int)(H * 0.28);
        int boxY   = H - boxH - (int)(H * 0.03);
        int boxX   = (int)(W * 0.04);
        int boxW   = W - boxX * 2;
        int pad    = (int)(W * 0.018);

        // Sprite size
        int sprW   = boxH - pad * 2;
        int sprH   = sprW;
        int sprX   = boxX + pad;
        int sprY   = boxY + pad;

        // Testo area
        int txtX   = sprX + sprW + pad;
        int txtW   = boxW - sprW - pad * 3;

        // ── Sfondo scuro semitrasparente su tutta la scena ────────────────────
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, W, H);

        // ── Box dialogo ───────────────────────────────────────────────────────
        // Ombra
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(boxX + 4, boxY + 4, boxW, boxH, 16, 16);
        // Sfondo
        g2.setColor(new Color(12, 8, 20, 245));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        // Bordo luminoso
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(180, 120, 255));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        // Linea interna di accento
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(100, 60, 160, 120));
        g2.drawRoundRect(boxX + 3, boxY + 3, boxW - 6, boxH - 6, 13, 13);

        // ── Sprite personaggio ────────────────────────────────────────────────
        java.awt.image.BufferedImage sprPg =
                res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
        if (sprPg != null) {
            // Riquadro sprite con bordo
            g2.setColor(new Color(30, 20, 50));
            g2.fillRoundRect(sprX - 3, sprY - 3, sprW + 6, sprH + 6, 8, 8);
            g2.setColor(new Color(180, 120, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(sprX - 3, sprY - 3, sprW + 6, sprH + 6, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            g2.drawImage(sprPg, sprX, sprY, sprW, sprH, null);
        }

        // ── Nome personaggio (font custom) ────────────────────────────────────
        DatiPersonaggio pg = ui.listaPersonaggi.get(
                Math.min(state.indicePersonaggioSelezionato, ui.listaPersonaggi.size() - 1));
        String nome = pg.nome.toUpperCase();

        int nomeFs  = Math.max(13, (int)(H * 0.028));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) nomeFs)
                : new Font("Consolas", Font.BOLD, nomeFs));

        FontMetrics fmN = g2.getFontMetrics();
        int nomeY = sprY + fmN.getAscent();

        // Piccolo sfondo dietro il nome
        int nomeBgW = fmN.stringWidth(nome) + 16;
        g2.setColor(new Color(120, 60, 200, 200));
        g2.fillRoundRect(txtX - 2, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);

        g2.setColor(new Color(255, 220, 80));
        g2.drawString(nome, txtX + 6, nomeY + 1);

        // ── Testo dialogo (wrapping manuale) ─────────────────────────────────
        String testo = "WHAT? I'VE PLAYED TOO MUCH, IM GONNA BE LATE!!!";
        int testoFs  = Math.max(11, (int)(H * 0.022));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();

        // Wrap su più righe
        int lineY    = nomeY + fmN.getHeight() + (int)(H * 0.012);
        int lineH    = fmT.getHeight() + 3;
        String[] parole = testo.split(" ");
        StringBuilder riga = new StringBuilder();
        for (String parola : parole) {
            String prova = riga.isEmpty() ? parola : riga + " " + parola;
            if (fmT.stringWidth(prova) > txtW) {
                // disegna riga corrente con leggero effetto glow
                g2.setColor(new Color(200, 160, 255, 60));
                g2.drawString(riga.toString(), txtX + 1, lineY + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(riga.toString(), txtX, lineY);
                lineY += lineH;
                riga = new StringBuilder(parola);
            } else {
                riga = new StringBuilder(prova);
            }
        }
        if (!riga.isEmpty()) {
            g2.setColor(new Color(200, 160, 255, 60));
            g2.drawString(riga.toString(), txtX + 1, lineY + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(riga.toString(), txtX, lineY);
        }

        // ── Indicatore "premi INVIO" lampeggiante ────────────────────────────
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (blink) {
            int hintFs = Math.max(9, (int)(H * 0.016));
            g2.setFont(new Font("Arial", Font.ITALIC, hintFs));
            g2.setColor(new Color(160, 120, 220));
            String hint = "[ INVIO per continuare ]";
            g2.drawString(hint,
                    boxX + boxW - g2.getFontMetrics().stringWidth(hint) - pad,
                    boxY + boxH - pad / 2);
        }
    }

    private void disegnaBannerCasa(Graphics2D g2, int W, int H) {
        // Calcola posizione banda superiore
        double gs = gameScale(W, H);
        int gH = (int)(GameState.ALTEZZA_GIOCO * gs);
        int goy = (H - gH) / 2;
        int banH = Math.max(28, goy > 10 ? goy - 4 : 28);
        int banY = goy > banH ? (goy - banH) / 2 : 2;
        int fs = Math.max(11, banH - 8);

        g2.setColor(new Color(30, 20, 50, 200));
        g2.fillRoundRect(W/2 - (int)(W*0.15), banY, (int)(W*0.30), banH, 8, 8);

        g2.setFont(res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs)
                : new Font("Consolas", Font.BOLD, fs));
        g2.setColor(new Color(255, 200, 80));
        String txt = "CASA";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, W/2 - fm.stringWidth(txt)/2, banY + banH/2 + fm.getAscent()/2 - 2);
    }

    private void disegnaBannerArdua(Graphics2D g2, int W, int H) {
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);
        int    gW  = (int)(GameState.LARGHEZZA_GIOCO * gs);

        long ms = System.currentTimeMillis();
        float pulse = 0.75f + 0.25f * (float)Math.sin(ms * 0.005);
        int fs = Math.max(8, (int)(gs * 11));
        Font fontBold = res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs)
                : new Font("Consolas", Font.BOLD, fs);

        if (roomMgr.inStanzaBonus) {
            // ── Pannello malus verticale a destra del gioco ─────────────────
            java.util.List<String[]> righe = new java.util.ArrayList<>();
            righe.add(new String[]{"STANZA BONUS", "header"});
            righe.add(new String[]{"─────────────", "sep"});
            if (state.arduaMalusDanno    > 0) righe.add(new String[]{"-" + state.arduaMalusDanno + " DANNO",    "malus"});
            if (state.arduaMalusVelocita > 0) righe.add(new String[]{"-" + (int)state.arduaMalusVelocita + " VELOCITA", "malus"});
            if (state.arduaMalusFireRate > 0) righe.add(new String[]{"FUOCO LENTO", "malus"});
            if (righe.size() == 2) righe.add(new String[]{"nessun malus", "note"});

            g2.setFont(fontBold);
            FontMetrics fm = g2.getFontMetrics();
            int lineH = fs + 5;
            int panW = 0;
            for (String[] r : righe) {
                panW = Math.max(panW, fm.stringWidth(r[0]));
            }
            panW += 18;
            int panH = righe.size() * lineH + 14;
            int panX = gox + (int)(gs * 4);      // angolo in alto a sinistra del game area
            int panY = goy + (int)(gs * 4);

            // Sfondo
            g2.setColor(new Color(80, 0, 0, Math.max(0, Math.min(255, (int)(210 * pulse)))));
            g2.fillRoundRect(panX, panY, panW, panH, 8, 8);
            g2.setColor(new Color(220, 50, 50, Math.max(0, Math.min(255, (int)(230 * pulse)))));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(panX, panY, panW, panH, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Righe
            for (int i = 0; i < righe.size(); i++) {
                String lbl = righe.get(i)[0];
                String tipo = righe.get(i)[1];
                int ty = panY + 6 + lineH/2 + fm.getAscent()/2 + i * lineH;
                int tx = panX + (panW - fm.stringWidth(lbl)) / 2;
                g2.setColor(
                        "header".equals(tipo) ? new Color(255, 200, 200) :
                                "malus".equals(tipo)  ? new Color(255, 120, 120) :
                                        "sep".equals(tipo)    ? new Color(180, 60, 60, 150) :
                                                new Color(180, 160, 160));
                g2.drawString(lbl, tx, ty);
            }
        }
    }

    private void disegnaHUD(Graphics2D g2, int W, int H) {
        double gs  = gameScale(W, H);
        int    gox = gameOffX(W, H);
        int    goy = gameOffY(W, H);
        int    gW  = (int)(GameState.LARGHEZZA_GIOCO * gs);
        int    gH  = (int)(GameState.ALTEZZA_GIOCO   * gs);

        int bandaB = H - goy - gH;
        int bandaR = W - gox - gW;
        int bandaL = gox;

        boolean hudBasso    = bandaB >= 36;
        boolean hudDestra   = !hudBasso && bandaR >= 110;
        boolean hudSinistra = !hudBasso && !hudDestra && bandaL >= 110;

        int fs  = Math.max(11, (int)(gs * 14));
        int ico = Math.max(14, (int)(gs * 20));

        if (hudBasso) {
            int hx = gox, hy = goy + gH;
            int hw = gW,  hh = bandaB;
            g2.setColor(new Color(10, 10, 25, 230));
            g2.fillRect(hx, hy, hw, hh);
            g2.setColor(new Color(60, 60, 100));
            g2.drawLine(hx, hy, hx + hw, hy);

            int cy = hy + hh / 2;
            int cx = hx + 10;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, cx + i * (ico + 3), cy - ico/2, ico, ico, null);
            cx += state.vite * (ico + 3) + 12;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, cx, cy - ico/2, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, cx + ico + 4, cy + fs/3);
            cx += ico + 4 + g2.getFontMetrics().stringWidth("" + state.monete) + 18;
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, fs));
            g2.setColor(ts.coloreTemaUI);
            String mondoStr = state.modalitaScelta == GameState.Modalita.STORIA
                    ? "M" + state.mondoAttuale + ": " + ts.nomeMondo
                    : "Inf M" + state.mondoAttuale + ": " + ts.nomeMondo;
            drawText(g2, mondoStr, cx, cy + fs/3, fs);
            cx += g2.getFontMetrics().stringWidth(mondoStr) + 18;
            g2.setColor(Color.WHITE);
            String stanzaLbl = "Stanza " + state.stanzaNelMondo + "/" + GameState.STANZA_BOSS;
            g2.drawString(stanzaLbl, cx, cy + fs/3);
            cx += g2.getFontMetrics().stringWidth(stanzaLbl) + 10;

            // ── Indicatore porta bonus disponibile ────────────────────────────
            if (state.stanzaNelMondo == state.stanzaConPortaArdua
                    && !state.ardua_completed && !roomMgr.inStanzaBonus) {
                long msHUD = System.currentTimeMillis();
                boolean blinkBonus = (msHUD / 400) % 2 == 0;
                if (blinkBonus) {
                    int bW = Math.max(28, (int)(W * 0.055f));
                    int bH = Math.max(14, (int)(H * 0.028f));
                    int bX = cx;
                    int bY = cy - bH/2 - 1;
                    float pp = 0.7f + 0.3f * (float)Math.sin(msHUD * 0.005);
                    // Sfondo pillola rossa
                    g2.setColor(new Color(160, 0, 0, Math.max(0, Math.min(255, (int)(200*pp)))));
                    g2.fillRoundRect(bX, bY, bW, bH, 6, 6);
                    g2.setColor(new Color(255, 80, 80, Math.max(0, Math.min(255, (int)(240*pp)))));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(bX, bY, bW, bH, 6, 6);
                    g2.setStroke(new BasicStroke(1f));
                    // Testo "BONUS"
                    int bFs = Math.max(7, (int)(bH * 0.65f));
                    g2.setFont(res.fontCustomBold != null
                            ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)bFs)
                            : new Font("Consolas", Font.BOLD, bFs));
                    g2.setColor(new Color(255, 210, 210));
                    FontMetrics fmB = g2.getFontMetrics();
                    String bLbl = "BONUS";
                    g2.drawString(bLbl, bX + (bW - fmB.stringWidth(bLbl))/2,
                            bY + (bH + fmB.getAscent())/2 - 2);
                }
            }

            // Indicatore burn
            if (state.burnAttivo) {
                boolean blinkBurn = (System.currentTimeMillis() / 300) % 2 == 0;
                if (blinkBurn) {
                    int burnX = cx;
                    if (res.imgBurn != null)
                        g2.drawImage(res.imgBurn, burnX, cy - ico/2, ico, ico, null);
                    else {
                        g2.setColor(new Color(255, 80, 0));
                        g2.fillOval(burnX, cy - ico/2, ico, ico);
                    }
                    g2.setFont(new Font("Consolas", Font.BOLD, fs));
                    g2.setColor(new Color(255, 140, 0));
                    g2.drawString("BURN!", burnX + ico + 4, cy + fs/3);
                }
            }

            // ── Indicatori stato: freeze / slow — colonna verticale sotto HUD ─
            int statoY  = cy + ico + 6;   // partono sotto la riga vita
            int statoFs = Math.max(7, fs - 1);
            Font statoFont = res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)statoFs)
                    : new Font("Consolas", Font.BOLD, statoFs);
            g2.setFont(statoFont);
            FontMetrics fmSt = g2.getFontMetrics();

            if (state.freezeAttivo) {
                int rimasti = state.freezeTimer;
                int totale  = GameState.FREEZE_DURATA;
                float prog  = Math.max(0f, rimasti / (float)totale);
                int barW    = Math.max(60, (int)(W * 0.09f));
                int barH    = Math.max(8,  (int)(H * 0.013f));
                // Sfondo pillola
                g2.setColor(new Color(20, 40, 80, 210));
                g2.fillRoundRect(gox + 8, statoY, barW + 40, barH + 6, 6, 6);
                // Barra progresso blu ghiaccio
                g2.setColor(new Color(80, 180, 255, 220));
                g2.fillRoundRect(gox + 8, statoY, (int)((barW + 40) * prog), barH + 6, 6, 6);
                // Bordo
                g2.setColor(new Color(160, 220, 255, 200));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(gox + 8, statoY, barW + 40, barH + 6, 6, 6);
                // Etichetta
                String lblFrz = "FREEZE";
                g2.setColor(new Color(200, 240, 255));
                g2.drawString(lblFrz, gox + 12, statoY + barH + fmSt.getAscent() - 1);
                statoY += barH + 10;
            }

            if (state.slowAttivo && !state.freezeAttivo) {
                int rimasti = state.slowTimer;
                int totale  = GameState.SLOW_DURATA;
                float prog  = Math.max(0f, rimasti / (float)totale);
                int barW    = Math.max(50, (int)(W * 0.075f));
                int barH    = Math.max(8,  (int)(H * 0.013f));
                g2.setColor(new Color(20, 50, 70, 210));
                g2.fillRoundRect(gox + 8, statoY, barW + 40, barH + 6, 6, 6);
                g2.setColor(new Color(60, 160, 220, 200));
                g2.fillRoundRect(gox + 8, statoY, (int)((barW + 40) * prog), barH + 6, 6, 6);
                g2.setColor(new Color(120, 200, 255, 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(gox + 8, statoY, barW + 40, barH + 6, 6, 6);
                String lblSl = "LENTO";
                g2.setColor(new Color(160, 220, 255));
                g2.drawString(lblSl, gox + 12, statoY + barH + fmSt.getAscent() - 1);
            }

            // Indicatore melee sbloccato
            if (state.meleeUnlocked) {
                int meleeX = W - (int)(W * 0.22f);
                int meleeY = cy;
                String meleeLbl = state.getMeleeNome();
                boolean inCooldown = state.meleeCooldown > 0;

                // Sfondo pill — larghezza adattiva al nome
                g2.setFont(new Font("Consolas", Font.BOLD, Math.max(8, fs - 2)));
                FontMetrics fmLbl = g2.getFontMetrics();
                int pillW = ico + 8 + fmLbl.stringWidth(meleeLbl) + 10;
                g2.setColor(new Color(20, 10, 40, 200));
                g2.fillRoundRect(meleeX - 4, meleeY - ico/2 - 2, pillW, ico + 4, 8, 8);

                // Icona tasto Z
                int kSize = ico;
                g2.setColor(inCooldown ? new Color(60, 40, 80) : new Color(160, 80, 255));
                g2.fillRoundRect(meleeX, meleeY - kSize/2, kSize, kSize, 5, 5);
                g2.setColor(inCooldown ? new Color(90, 70, 110) : new Color(200, 140, 255));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(meleeX, meleeY - kSize/2, kSize, kSize, 5, 5);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Consolas", Font.BOLD, fs));
                g2.setColor(inCooldown ? new Color(100, 80, 120) : Color.WHITE);
                FontMetrics fmZ = g2.getFontMetrics();
                g2.drawString("Z", meleeX + (kSize - fmZ.stringWidth("Z"))/2,
                        meleeY - kSize/2 + (kSize + fmZ.getAscent())/2 - 2);

                // Nome attacco
                int lblX = meleeX + kSize + 5;
                g2.setFont(new Font("Consolas", Font.BOLD, Math.max(8, fs - 2)));
                g2.setColor(inCooldown ? new Color(120, 90, 160) : new Color(200, 140, 255));
                g2.drawString(meleeLbl, lblX, meleeY - 1);

                // Barra cooldown
                if (inCooldown) {
                    int barW = fmLbl.stringWidth(meleeLbl);
                    int barH = 4;
                    float fill = 1f - (float) state.meleeCooldown / GameState.MELEE_DELAY;
                    g2.setColor(new Color(40, 20, 60));
                    g2.fillRoundRect(lblX, meleeY + 3, barW, barH, 3, 3);
                    g2.setColor(new Color(160, 80, 255));
                    g2.fillRoundRect(lblX, meleeY + 3, (int)(barW * fill), barH, 3, 3);
                }
            }

            // Flash nome attacco sopra il personaggio
            if (state.meleeNomeTimer > 0 && state.meleeUnlocked) {
                float fadeAlpha = Math.min(1f, state.meleeNomeTimer / 20f);
                double gs2 = gameScale(W, H);
                int pgScreenX = gox + (int)(state.x * gs2) + (int)(GameState.PG_SIZE * gs2 / 2);
                int pgScreenY = goy + (int)(state.y * gs2) - Math.max(0, Math.min(255, (int)(12 * gs2)));
                int flashFs = Math.max(11, (int)(gs2 * 16));
                g2.setFont(res.fontCustomBold != null
                        ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) flashFs)
                        : new Font("Consolas", Font.BOLD, flashFs));
                FontMetrics fmF = g2.getFontMetrics();
                String nome = state.getMeleeNome();
                int nx = pgScreenX - fmF.stringWidth(nome) / 2;
                // Ombra
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha * 0.6f));
                g2.setColor(new Color(60, 0, 120));
                g2.drawString(nome, nx + 1, pgScreenY + 1);
                // Testo viola brillante
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                g2.setColor(new Color(220, 160, 255));
                g2.drawString(nome, nx, pgScreenY);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

        } else if (hudDestra || hudSinistra) {
            int px = hudDestra ? gox + gW : 0;
            int pw = hudDestra ? bandaR   : bandaL;
            g2.setColor(new Color(10, 10, 25, 230));
            g2.fillRect(px, 0, pw, H);
            g2.setColor(new Color(60, 60, 100));
            int lineX = hudDestra ? px : px + pw - 1;
            g2.drawLine(lineX, 0, lineX, H);

            int lx = px + pw / 2;
            int ly = 20;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, lx - ico/2, ly + i * (ico + 3), ico, ico, null);
            ly += state.vite * (ico + 3) + 14;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, lx - ico/2, ly, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, lx - ico/2, ly + ico + fs + 2);
            ly += ico + fs + 16;
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, Math.max(9, fs - 2)));
            g2.setColor(ts.coloreTemaUI);
            String mn = "M" + state.mondoAttuale;
            g2.drawString(mn, lx - g2.getFontMetrics().stringWidth(mn)/2, ly);
            ly += fs + 6;
            g2.setColor(Color.WHITE);
            String st = state.stanzaNelMondo + "/" + GameState.STANZA_BOSS;
            g2.drawString(st, lx - g2.getFontMetrics().stringWidth(st)/2, ly);

        } else {
            // Overlay compatto angolo top-left del gioco
            int pad = 6;
            g2.setColor(new Color(10, 10, 25, 190));
            g2.fillRoundRect(gox + 4, goy + 4, (int)(gW * 0.38), ico + fs + pad * 3, 6, 6);
            int cx = gox + 10;
            for (int i = 0; i < state.vite; i++)
                if (res.imgCuore != null)
                    g2.drawImage(res.imgCuore, cx + i * (ico + 2), goy + pad, ico, ico, null);
            cx += state.vite * (ico + 2) + 8;
            if (res.imgMoneta != null) g2.drawImage(res.imgMoneta, cx, goy + pad, ico, ico, null);
            g2.setColor(Color.WHITE);
            g2.setFont(res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fs) : new Font("Consolas", Font.BOLD, fs));
            g2.drawString("" + state.monete, cx + ico + 3, goy + pad + ico - 2);
            TileSet ts = tileSetCorrente();
            g2.setFont(new Font("Consolas", Font.BOLD, fs));
            g2.setColor(ts.coloreTemaUI);
            g2.drawString("M" + state.mondoAttuale + " St." + state.stanzaNelMondo, gox + 10, goy + pad + ico + fs + 4);
        }

        disegnaUIBoss(g2, W, H, gox, goy, gW, gH);
    }
    private void disegnaUIBoss(Graphics2D g2, int W, int H, int gox, int goy, int gW, int gH) {
        if (state.stanzaNelMondo != GameState.STANZA_BOSS
                || !state.bossSpawnato || state.bossSconfitto) return;

        Boss bossCorrente = null;
        for (Nemico n : roomMgr.getNemiciCorrenti()) {
            if (n instanceof Boss b) { bossCorrente = b; break; }
        }
        if (bossCorrente == null) return;

        String[] nomiUI = {
                "MANNIE", "PRESAGIO", "RE FORNO", "GELO", "YABBADUHLON"
        };
        String nomeBoss = nomiUI[((state.mondoAttuale - 1) % 5)];

        // Barra boss: larga 50% della stanza, centrata, nella banda superiore
        int barW    = (int)(gW * 0.5);
        int barH    = Math.max(18, (int)(goy * 0.35));  // usa la banda superiore
        int fontSize = Math.max(12, (int)(goy * 0.30));
        int hpFs    = Math.max(10, (int)(goy * 0.22));
        int padX    = Math.max(12, barW / 20);
        int padY    = Math.max(6,  (int)(goy * 0.08));
        int panW    = barW + padX * 2;
        int panH    = padY + fontSize + padY / 2 + barH + padY / 2 + hpFs + padY;
        int uiX     = gox + gW / 2 - barW / 2;
        // Centra il pannello verticalmente nella banda superiore
        int panY    = (goy - panH) / 2;
        if (panY < 2) panY = 2;

        // coordinate interne
        int nomeY = panY + padY + fontSize;
        int barY  = nomeY + padY / 2;
        int hpY   = barY + barH + hpFs;

        // Pannello sfondo
        g2.setColor(new Color(15, 5, 5, 220));
        g2.fillRoundRect(uiX - padX, panY, panW, panH, 12, 12);
        g2.setColor(new Color(100, 20, 20));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(uiX - padX, panY, panW, panH, 12, 12);
        g2.setStroke(new BasicStroke(1f));

        // Nome boss
        g2.setFont(res.fontCustomBold != null ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)fontSize) : new Font("Consolas", Font.BOLD, fontSize));
        g2.setColor(new Color(255, 200, 80));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(nomeBoss, uiX + (barW - fm.stringWidth(nomeBoss)) / 2, nomeY);

        // Barra vita
        g2.setColor(new Color(40, 10, 10));
        g2.fillRect(uiX, barY, barW, barH);
        float perc = (float) bossCorrente.getVita() / bossCorrente.getVitaMax();
        Color colVita = perc > 0.5f ? new Color(200, 40, 40)
                : perc > 0.25f ? new Color(220, 120, 0)
                : new Color(255, 40, 40);
        g2.setColor(colVita);
        g2.fillRect(uiX, barY, (int)(barW * perc), barH);
        g2.setColor(new Color(80, 20, 20));
        g2.drawRect(uiX, barY, barW, barH);

        // HP text
        g2.setFont(new Font("Consolas", Font.BOLD, hpFs));
        g2.setColor(Color.WHITE);
        String hp = bossCorrente.getVita() + " / " + bossCorrente.getVitaMax();
        g2.drawString(hp, uiX + (barW - g2.getFontMetrics().stringWidth(hp)) / 2, hpY);

        // Timer — in alto a destra
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        g2.setFont(new Font("Consolas", Font.BOLD, Math.max(12, H / 28)));
        g2.setColor(state.tempoRimanenteBoss < 600
                ? (blink ? Color.RED : new Color(200, 80, 80))
                : new Color(200, 200, 200));
        g2.drawString("BOSS " + (state.tempoRimanenteBoss / 60) + "s",
                W - (int)(W * 0.1), goy + (int)(H * 0.04));
    }
    // ── Utilità UI ────────────────────────────────────────────────────────────

    /** Sfondo scuro pieno con leggero alone centrale */
    /** Avvia la musica corretta in base allo stato del gioco. */
    private void aggiornaMusica() {
        switch (state.statoGioco) {
            case MENU, IMPOSTAZIONI, CONTROLLI,
                 SELEZIONE_PERSONAGGIO, SELEZIONE_MODALITA ->
                    state.audio.suonaMusica(AudioManager.MENU);

            case GIOCO -> {
                if (state.stanzaNelMondo == GameState.STANZA_BOSS
                        && state.bossSpawnato && !state.bossSconfitto) {
                    state.audio.suonaMusica(AudioManager.BOSS);
                } else {
                    state.audio.suonaMusicaMondo(state.mondoAttuale);
                }
            }

            case GAME_OVER ->
                    state.audio.suonaMusica(AudioManager.SCONFITTA);

            case VITTORIA_STORIA ->
                    state.audio.suonaMusica(AudioManager.VITTORIA);

            case UFFICIO ->
                    state.audio.suonaMusica(AudioManager.VITTORIA);

            case TETRIS ->
                    state.audio.suonaMusica(AudioManager.TETRIS);

            case PAUSA -> { /* non cambia la musica durante la pausa */ }
        }
    }

    /** Restituisce il TileSet corretto: Casa (mondo 0) nella stanza 1 del mondo 1. */
    private TileSet tileSetCorrente() {
        // La stanza Casa è: mondo 1, stanza 1, dopo il Tetris (stanzaCasaVisitata viene
        // impostato da RoomManager appena genera la stanza, prima del rendering)
        boolean isCasa = state.mondoAttuale == 1
                && state.stanzaNelMondo == 1
                && state.stanzaCasaVisitata;
        return TileSet.perMondo(isCasa ? 0 : state.mondoAttuale, res);
    }

    private void sfondoOverlay(Graphics2D g2, int W, int H, Color base) {
        g2.setColor(base);
        g2.fillRect(0, 0, W, H);
        for (int r = 250; r > 0; r -= 12) {
            int alpha = (int)(15 * (1.0 - r / 250.0));
            g2.setColor(new Color(60, 60, 140, alpha));
            g2.fillOval(W/2 - r, H/2 - r, r*2, r*2);
        }
    }
}