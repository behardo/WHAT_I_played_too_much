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
        // Bottone lingua
        ui.btnLingua.label = Lang.t("btn.lingua");
        ui.btnLingua.draw(g2);


        // ── Footer ────────────────────────────────────────────────────────────
        int fFs = Math.max(9, (int)(H * 0.018f));
        g2.setFont(new Font("Consolas", Font.PLAIN, fFs));
        g2.setColor(new Color(80, 60, 15, 170));
        g2.drawString(Lang.t("footer.fullscreen"), 16, H - 14);
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
        // cityY più basso = edifici più bassi, più cielo visibile
        int cityY=(int)(H*0.72f), bW=W/18;
        g2.setColor(new Color(8,6,16));
        for (int i=0; i<21; i++) {
            rng.setSeed(i*137L);
            // Altezza edifici: max 28% di H → non superano il 44% dall'alto
            int bH=(int)(H*(0.06f+rng.nextFloat()*0.22f));
            int bX=i*bW-bW/5;
            g2.fillRect(bX, cityY-bH, bW-2, bH+(H-cityY));
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
        // Suolo piatto sotto la skyline
        g2.setColor(new Color(5,4,12)); g2.fillRect(0,cityY,W,H-cityY);
        // Riflesso luna sul suolo
        for (int ry=cityY; ry<cityY+(int)(H*0.04f); ry++) {
            float ta=1f-(float)(ry-cityY)/(H*0.04f);
            g2.setColor(new Color(200,200,120, Math.max(0,Math.min(255,(int)(12*ta)))));
            g2.drawLine(lX-6,ry,lX+6,ry);
        }
        if (pioggia) {
            // Pioggia clippata: cade solo sopra la skyline (cielo) — non attraversa gli edifici
            g2.setClip(0, 0, W, cityY);
            int rainSeed=(int)(ms/30);
            java.util.Random rainRng=new java.util.Random(rainSeed);
            g2.setStroke(new BasicStroke(1f));
            for (int i=0; i<130; i++) {
                int rx=rainRng.nextInt(W+40)-20;
                int ry=(int)((rainRng.nextInt(cityY)+(ms/15.0))%cityY);
                int alpha=55+rainRng.nextInt(70);
                g2.setColor(new Color(150,180,255,alpha));
                g2.drawLine(rx,ry,rx-3,ry+9+rainRng.nextInt(12));
            }
            g2.setStroke(new BasicStroke(1f));
            g2.setClip(null); // ripristina clip
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
        String titStr=Lang.t("imp.titolo");
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
        g2.setColor(new Color(240,222,172)); g2.drawString(Lang.t("imp.musica"),LX+(int)(lbFs*1.3f),s+SH/2+3);
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
        g2.setColor(new Color(200,242,202)); g2.drawString(Lang.t("imp.effetti"),LX+(int)(lbFs*1.3f),s+RH+SH/2+3);
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
        String titStr=Lang.t("ctrl.titolo");
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

        disegnaSezioneLabel(g2,col1X,startY-4,Lang.t("ctrl.sezione.gioco"),W,H);
        startY+=(int)(H*0.032f);
        disegnaTastoInfo(g2,col1X,startY,         "W A S D",Lang.t("ctrl.wasd"));
        disegnaTastoInfo(g2,col2X,startY,         "Frecce", Lang.t("ctrl.frecce"));
        disegnaTastoInfo(g2,col1X,startY+rigaH,   "Z",      Lang.t("ctrl.z"));
        disegnaTastoInfo(g2,col2X,startY+rigaH,   "ESC",    Lang.t("ctrl.esc"));
        disegnaTastoInfo(g2,col1X,startY+rigaH*2, "F11",    Lang.t("ctrl.f11"));
        disegnaTastoInfo(g2,col2X,startY+rigaH*2, "Click",  Lang.t("ctrl.click"));
        disegnaTastoInfo(g2,col1X,startY+rigaH*3, "INVIO",  Lang.t("ctrl.invio"));
        disegnaTastoInfo(g2,col2X,startY+rigaH*3, "Q",      Lang.t("ctrl.q"));

        int tetY=startY+rigaH*4+(int)(H*0.045f);
        disegnaSezioneLabel(g2,col1X,tetY-4,Lang.t("ctrl.sezione.tetris"),W,H);
        tetY+=(int)(H*0.032f);
        disegnaTastoInfo(g2,col1X,tetY,         "A / D",  Lang.t("ctrl.ad"));
        disegnaTastoInfo(g2,col2X,tetY,         "W",      Lang.t("ctrl.w"));
        disegnaTastoInfo(g2,col1X,tetY+rigaH,   "S",      Lang.t("ctrl.s"));
        disegnaTastoInfo(g2,col2X,tetY+rigaH,   "SPAZIO", Lang.t("ctrl.spazio"));
        disegnaTastoInfo(g2,col1X,tetY+rigaH*2, "ESC",    Lang.t("ctrl.escsalta"));
        disegnaTastoInfo(g2,col2X,tetY+rigaH*2, "500+ pt",Lang.t("ctrl.punteggi"));

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
        float sec = ms * 0.001f;

        // ══ SFONDO: cielo notturno profondo blu-indaco ════════════════════════
        for (int y = 0; y < H; y++) {
            float t = (float) y / H;
            // Da blu-indaco scuro in alto a blu-notte con tocco teal in basso
            int r  = (int)(8  + 14 * (1-t));
            int gv = (int)(6  + 18 * (1-t));
            int b  = (int)(28 + 42 * (1-t));
            g2.setColor(new Color(r, gv, b));
            g2.drawLine(0, y, W, y);
        }

        // ── Nebulosa sfumata al centro ─────────────────────────────────────────
        float nebPulse = 0.55f + 0.45f * (float)Math.sin(sec * 0.4f);
        for (int ring = 8; ring >= 1; ring--) {
            int nw = (int)(W * 0.55f * ring / 8f);
            int nh = (int)(H * 0.55f * ring / 8f);
            int nx = W/2 - nw/2;
            int ny = H/2 - nh/2;
            int alpha = Math.max(0, (int)(18 * nebPulse * (1f - ring / 9f)));
            g2.setColor(new Color(40, 30, 90, alpha));
            g2.fillOval(nx, ny, nw, nh);
        }

        // ── Stelle con scintillio ──────────────────────────────────────────────
        java.util.Random rng = new java.util.Random(7);
        for (int i = 0; i < 90; i++) {
            int sx   = rng.nextInt(W);
            int sy   = rng.nextInt(H);
            float speed = 0.0008f + rng.nextFloat() * 0.002f;
            float blink  = 0.4f + 0.6f * (float)Math.sin(ms * speed + i * 1.3f);
            int   sz   = rng.nextFloat() < 0.12f ? 3 : 2;
            // Stelle più alte sono più luminose
            float brightnessBoost = 1f - (float)sy / H * 0.5f;
            int alpha = Math.max(0, Math.min(255, (int)(200 * blink * brightnessBoost)));
            // Variazione colore: bianche, bluastre, oro rare
            int[] cols = rng.nextFloat() < 0.15f
                    ? new int[]{255, 230, 130}   // stelle oro
                    : rng.nextFloat() < 0.3f
                    ? new int[]{180, 210, 255} // stelle blu
                    : new int[]{255, 248, 230}; // stelle bianche
            g2.setColor(new Color(cols[0], cols[1], cols[2], alpha));
            g2.fillRect(sx, sy, sz, sz);
            // Alone a croce sulle stelle grandi
            if (sz == 3 && blink > 0.75f) {
                int ca = Math.max(0, (int)(alpha * 0.4f));
                g2.setColor(new Color(cols[0], cols[1], cols[2], ca));
                g2.drawLine(sx-2, sy+1, sx+4, sy+1);
                g2.drawLine(sx+1, sy-2, sx+1, sy+4);
            }
        }

        // ── Pianeti decorativi sullo sfondo ────────────────────────────────────
        // Pianeta grande a sinistra
        int p1x = (int)(W * 0.07f), p1y = (int)(H * 0.12f), p1r = (int)(H * 0.08f);
        g2.setColor(new Color(45, 35, 80, 120));
        g2.fillOval(p1x - p1r, p1y - p1r, p1r*2, p1r*2);
        g2.setColor(new Color(80, 60, 130, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(p1x - (int)(p1r*1.6f), p1y - (int)(p1r*0.35f), (int)(p1r*3.2f), (int)(p1r*0.7f));
        g2.setStroke(new BasicStroke(1f));
        // Pianeta piccolo a destra
        int p2x = (int)(W * 0.91f), p2y = (int)(H * 0.18f), p2r = (int)(H * 0.04f);
        g2.setColor(new Color(30, 55, 70, 100));
        g2.fillOval(p2x - p2r, p2y - p2r, p2r*2, p2r*2);

        // ── Silhouette edifici in basso (skyline) ──────────────────────────────
        int edH = (int)(H * 0.24f);
        // Gradiente edifici
        for (int y = H - edH; y < H; y++) {
            float t = (float)(y - (H - edH)) / edH;
            g2.setColor(new Color((int)(8+6*t), (int)(8+5*t), (int)(18+10*t)));
            g2.drawLine(0, y, W, y);
        }
        // Profilo edifici con altezze variabili
        java.util.Random bldRng = new java.util.Random(42);
        int numBld = 22;
        int bldW = W / numBld;
        for (int b = 0; b < numBld; b++) {
            int bx   = b * bldW;
            int bh   = (int)(edH * (0.3f + bldRng.nextFloat() * 0.65f));
            int by2  = H - bh;
            // Corpo edificio
            int dark = 12 + bldRng.nextInt(10);
            g2.setColor(new Color(dark, dark, dark + 8));
            g2.fillRect(bx, by2, bldW - 1, bh);
            // Finestre animate (alcune lampeggiano)
            int wCols = 2 + bldRng.nextInt(3);
            int wRows = bh / 14;
            java.util.Random wRng = new java.util.Random(b * 100 + 7);
            for (int wc = 0; wc < wCols; wc++) {
                for (int wr = 1; wr <= wRows; wr++) {
                    if (wRng.nextFloat() < 0.35f) {
                        int wx = bx + 3 + wc * (bldW / (wCols+1));
                        int wy = by2 + wr * 12;
                        // Alcune finestre lampeggiano lentamente
                        float flicker = wRng.nextFloat() < 0.08f
                                ? (float)Math.abs(Math.sin(sec * 1.2f + b*0.7f + wr))
                                : 1f;
                        int amb = (int)((140 + wRng.nextInt(80)) * flicker);
                        if (amb > 40) {
                            g2.setColor(new Color(
                                    Math.min(255, amb),
                                    Math.min(255, (int)(amb * 0.78f)),
                                    Math.min(255, (int)(amb * 0.25f)), 200));
                            g2.fillRect(wx, wy, 4, 5);
                        }
                    }
                }
            }
        }

        // ── Particelle luminose fluttuanti ─────────────────────────────────────
        java.util.Random pRng = new java.util.Random(55);
        for (int i = 0; i < 28; i++) {
            float baseX  = pRng.nextFloat() * W;
            float baseY  = pRng.nextFloat() * (H * 0.75f);
            float driftX = (float)Math.sin(sec * 0.3f + i * 0.8f) * 14f;
            float driftY = (float)Math.cos(sec * 0.22f + i * 1.1f) * 9f
                    - sec * (5f + pRng.nextFloat() * 8f) % (H * 0.75f);
            float px     = (baseX + driftX + W) % W;
            float py     = ((baseY + driftY) % (H * 0.75f) + H * 0.75f) % (H * 0.75f);
            float bright = 0.5f + 0.5f * (float)Math.sin(sec * 1.5f + i * 2.1f);
            int   palpha = Math.max(0, Math.min(255, (int)(180 * bright)));
            // Colori particelle: azzurro, oro, viola
            int[][] palette = {{120,180,255},{255,210,80},{180,120,255},{100,230,200}};
            int[] col = palette[i % palette.length];
            g2.setColor(new Color(col[0], col[1], col[2], palpha));
            g2.fillOval((int)px - 2, (int)py - 2, 4, 4);
            // Scia leggera
            g2.setColor(new Color(col[0], col[1], col[2], palpha / 4));
            g2.fillOval((int)px - 3, (int)py + 3, 6, 3);
        }

        // ── Titolo ────────────────────────────────────────────────────────────
        int titFs = Math.max(18, (int)(H * 0.067f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)titFs)
                : new Font("Consolas", Font.BOLD, titFs));
        // Glow titolo
        float titGlow = 0.7f + 0.3f * (float)Math.sin(sec * 1.2f);
        for (int gi = 3; gi >= 1; gi--) {
            g2.setColor(new Color(100, 160, 255, Math.max(0, (int)(35 * titGlow / gi))));
            drawTextCentered(g2, Lang.t("pg.scegli"), W/2 - gi, (int)(H*0.095f), titFs);
            drawTextCentered(g2, Lang.t("pg.scegli"), W/2 + gi, (int)(H*0.095f), titFs);
        }
        g2.setColor(new Color(0, 10, 30, 160));
        drawTextCentered(g2, Lang.t("pg.scegli"), W/2 + 2, (int)(H*0.095f)+2, titFs);
        g2.setColor(new Color(210, 235, 255));
        drawTextCentered(g2, Lang.t("pg.scegli"), W/2, (int)(H*0.095f), titFs);

        // Sottolineatura animata
        int lineW = (int)(W * 0.38f);
        float lineAnim = 0.6f + 0.4f * (float)Math.sin(sec * 0.9f);
        int lineAlpha = Math.max(0, Math.min(255, (int)(200 * lineAnim)));
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(120, 180, 255, lineAlpha));
        g2.drawLine(W/2 - lineW/2, (int)(H*0.095f)+7, W/2 + lineW/2, (int)(H*0.095f)+7);
        g2.setStroke(new BasicStroke(1f));

        // Hint navigazione
        int hintFs = Math.max(9, (int)(H * 0.024f));
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(150, 175, 220, 170));
        String hint = Lang.t("pg.hint");
        g2.drawString(hint, W/2 - g2.getFontMetrics().stringWidth(hint)/2, (int)(H*0.165f));

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
            int rx = r.x, rw = r.width, rh = r.height;

            DatiPersonaggio pg      = ui.listaPersonaggi.get(i);
            boolean         lock    = !state.sistemaPersonaggi.isSbloccato(i);
            boolean         segreto = (i == SistemaPersonaggi.INDICE_SEGRETO);
            boolean         sel     = (i == state.indicePersonaggioSelezionato) && !lock;

            // ── Bob verticale animato (solo card sbloccate/selezionate) ────────
            float bobOffset = sel
                    ? (float)Math.sin(sec * 2.2f + i * 0.9f) * 5f
                    : (float)Math.sin(sec * 0.8f + i * 1.4f) * 2.5f;
            int ry = r.y + (int)bobOffset;

            // ── Sfondo card ───────────────────────────────────────────────────
            if (segreto) {
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    g2.setColor(new Color(
                            (int)(70 + 50*tt), (int)(42 + 22*tt), (int)(5 + 12*tt),
                            sel ? 215 : 150));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            } else if (lock) {
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    g2.setColor(new Color(10, (int)(10+8*tt), (int)(20+18*tt), 155));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            } else {
                // Colori card per personaggio: ogni pg ha sfumatura unica
                int[][] cardPalette = {
                        {25, 30, 65},   // BELLGERD — blu notte
                        {15, 45, 25},   // VLAD — verde scuro
                        {55, 22, 15},   // PAUL — rosso mattone
                        {20, 20, 50},   // JUICY — indaco
                        {60, 42,  5},   // D.I.T.T.O. — oro
                };
                int[] cp = cardPalette[Math.min(i, cardPalette.length-1)];
                for (int dy = 0; dy < rh; dy++) {
                    float tt = (float) dy / rh;
                    int cr = Math.min(255, (int)(cp[0] + (sel?30:14)*tt));
                    int cg = Math.min(255, (int)(cp[1] + (sel?22:10)*tt));
                    int cb = Math.min(255, (int)(cp[2] + (sel?35:18)*tt));
                    g2.setColor(new Color(cr, cg, cb, sel ? 215 : 165));
                    g2.drawLine(rx+1, ry+dy, rx+rw-2, ry+dy);
                }
            }

            // ── Bordi e glow ─────────────────────────────────────────────────
            Color bordo = segreto ? new Color(255, 200, 50)
                    : lock    ? new Color(35, 38, 65)
                    : sel     ? new Color(140, 200, 255)
                    : new Color(70, 90, 140);
            if (sel) {
                // Glow esterno multilivello pulsante
                float glow = 0.55f + 0.45f * (float)Math.sin(sec * 2.5f);
                int[] glowLayers = {9, 6, 4};
                int[] glowAlphas = {25, 45, 70};
                for (int gl = 0; gl < 3; gl++) {
                    int ga = Math.max(0, Math.min(255, (int)(glowAlphas[gl] * glow)));
                    g2.setColor(new Color(100, 180, 255, ga));
                    g2.setStroke(new BasicStroke(glowLayers[gl]));
                    g2.drawRoundRect(rx - glowLayers[gl]/2, ry - glowLayers[gl]/2,
                            rw + glowLayers[gl], rh + glowLayers[gl], 14, 14);
                }
            } else if (segreto) {
                // Glow oro per D.I.T.T.O.
                float glow2 = 0.5f + 0.5f * (float)Math.sin(sec * 1.5f + i);
                g2.setColor(new Color(255, 190, 30, Math.max(0,(int)(50*glow2))));
                g2.setStroke(new BasicStroke(5f));
                g2.drawRoundRect(rx-2, ry-2, rw+4, rh+4, 14, 14);
            }
            g2.setColor(bordo);
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
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
                            java.awt.AlphaComposite.SRC_OVER, 0.15f));
                    g2.drawImage(pg.imgIcona, imgX, imgY, imgSize, imgSize, null);
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, 1f));
                }
                g2.setFont(res.fontCustomBold != null
                        ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)cardFs)
                        : new Font("Consolas", Font.BOLD, cardFs));
                g2.setColor(new Color(60, 58, 82));
                FontMetrics fmL = g2.getFontMetrics();
                g2.drawString(pg.nome, rx + (rw - fmL.stringWidth(pg.nome))/2, nomeY);
                int lockFs = Math.max(14, (int)(rh * 0.13f));
                g2.setFont(new Font("Consolas", Font.BOLD, lockFs));
                g2.setColor(new Color(65, 62, 95));
                String lockLbl = "[X]";
                FontMetrics fmLk = g2.getFontMetrics();
                g2.drawString(lockLbl, rx + (rw - fmLk.stringWidth(lockLbl))/2,
                        ry + (int)(rh * 0.72f));
                g2.setFont(new Font("Arial", Font.PLAIN, smallFs));
                g2.setColor(new Color(95, 88, 125));
                // Testo sblocco recuperato LIVE dalla lingua corrente
                String sbloccoKey = switch (i) {
                    case 1 -> "pg.sblocco.1";
                    case 2 -> "pg.sblocco.2";
                    case 3 -> "pg.sblocco.3";
                    case 4 -> "pg.sblocco.4";
                    default -> "";
                };
                if (!sbloccoKey.isEmpty()) {
                    // Linea 1 e linea 2 come chiavi separate per evitare problemi di escape
                    String r1 = Lang.t(sbloccoKey + ".r1");
                    String r2 = Lang.t(sbloccoKey + ".r2");
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(r1, rx + (rw - fm2.stringWidth(r1))/2, statsY);
                    if (!r2.equals(sbloccoKey + ".r2")) { // chiave non trovata = fallback
                        g2.drawString(r2, rx + (rw - fm2.stringWidth(r2))/2, statsY + smallFs + 3);
                    }
                }
                continue;
            }

            // ── Sprite con effetti ─────────────────────────────────────────────
            if (pg.imgIcona != null) {
                if (sel) {
                    // Alone ellittico colorato sotto lo sprite (riflesso)
                    float glowA = 0.4f + 0.4f * (float)Math.sin(sec * 2.2f);
                    g2.setColor(new Color(100, 180, 255, Math.max(0,(int)(55*glowA))));
                    g2.fillOval(imgX - 8, imgY + imgSize - 10, imgSize + 16, 20);
                    // Alone attorno allo sprite (multi-layer)
                    for (int gl = 3; gl >= 1; gl--) {
                        int ga = Math.max(0, Math.min(255, (int)(30 * glowA / gl)));
                        g2.setColor(new Color(120, 190, 255, ga));
                        g2.fillOval(imgX - gl*2, imgY - gl*2, imgSize + gl*4, imgSize + gl*4);
                    }
                }
                // Sprite con leggero scale-up se selezionato
                int drawSize = sel ? (int)(imgSize * 1.06f) : imgSize;
                int drawX    = rx + (rw - drawSize) / 2;
                int drawY    = imgY + (imgSize - drawSize) / 2;
                g2.drawImage(pg.imgIcona, drawX, drawY, drawSize, drawSize, null);
            } else if (segreto) {
                g2.setFont(new Font("Serif", Font.BOLD, (int)(rh * 0.22f)));
                g2.setColor(new Color(255, 215, 0));
                String star = "*";
                FontMetrics fmS = g2.getFontMetrics();
                g2.drawString(star, rx + (rw - fmS.stringWidth(star))/2, imgY + imgSize - 4);
            } else {
                g2.setColor(new Color(90, 70, 140));
                g2.fillOval(imgX, imgY, imgSize, imgSize);
            }

            // ── Nome ──────────────────────────────────────────────────────────
            g2.setFont(res.fontCustomBold != null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)cardFs)
                    : new Font("Consolas", Font.BOLD, cardFs));
            g2.setColor(new Color(0, 0, 0, 110));
            FontMetrics fmN = g2.getFontMetrics();
            g2.drawString(pg.nome, rx + (rw - fmN.stringWidth(pg.nome))/2 + 1, nomeY+1);
            g2.setColor(segreto ? new Color(255, 210, 50)
                    : sel    ? new Color(210, 235, 255)
                    : new Color(180, 200, 240));
            g2.drawString(pg.nome, rx + (rw - fmN.stringWidth(pg.nome))/2, nomeY);

            // Linea separatore
            int sepAlpha = sel ? 120 : 55;
            g2.setColor(new Color(120, 160, 255, sepAlpha));
            g2.drawLine(rx + rw/5, nomeY + 4, rx + rw*4/5, nomeY + 4);

            // ── Descrizione ───────────────────────────────────────────────────
            g2.setFont(new Font("Arial", Font.ITALIC, smallFs));
            g2.setColor(segreto ? new Color(255, 195, 70) : new Color(160, 180, 225));
            FontMetrics fmD = g2.getFontMetrics();
            g2.drawString(pg.descrizione(), rx + (rw - fmD.stringWidth(pg.descrizione()))/2, descY);

            // ── Stats ─────────────────────────────────────────────────────────
            g2.setFont(new Font("Consolas", Font.BOLD, smallFs));
            int icoS = smallFs;
            if (res.imgCuore != null)
                g2.drawImage(res.imgCuore, rx + 6, statsY - icoS + 2, icoS, icoS, null);
            g2.setColor(new Color(255, 100, 110));
            g2.drawString("" + pg.vitaMax, rx + 8 + icoS, statsY);
            g2.setColor(new Color(80, 200, 255));
            g2.drawString("V:" + pg.velocitaBase, rx + 6, statsY + smallFs + 4);
            g2.setColor(new Color(255, 195, 60));
            g2.drawString("D:" + pg.dannoBase, rx + rw/2, statsY + smallFs + 4);
        }
    }


    // ── Selezione Modalità ────────────────────────────────────────────────────

    private void disegnaSelezioneModalita(Graphics2D g2, int W, int H) {
        long ms  = System.currentTimeMillis();
        float sec = ms * 0.001f;

        // ══ SFONDO: cielo notturno blu-indaco profondo ════════════════════════
        for (int y = 0; y < H; y++) {
            float t = (float) y / H;
            g2.setColor(new Color((int)(5+10*(1-t)), (int)(5+12*(1-t)), (int)(18+35*(1-t))));
            g2.drawLine(0, y, W, y);
        }

        // ── Stelle animate con scintillio ─────────────────────────────────────
        java.util.Random rngSt = new java.util.Random(31);
        for (int i = 0; i < 110; i++) {
            int sx = rngSt.nextInt(W), sy = rngSt.nextInt((int)(H * 0.72f));
            float spd = 0.0007f + rngSt.nextFloat() * 0.0025f;
            float blink = 0.35f + 0.65f * (float)Math.sin(ms * spd + i * 1.7f);
            int sz = rngSt.nextFloat() < 0.1f ? 3 : 2;
            int alpha = Math.max(0, Math.min(255, (int)(210 * blink * (1f - (float)sy/H*0.35f))));
            int[] c = rngSt.nextFloat()<0.12f ? new int[]{255,230,120}
                    : rngSt.nextFloat()<0.25f  ? new int[]{160,200,255}
                    : new int[]{255,250,240};
            g2.setColor(new Color(c[0],c[1],c[2],alpha));
            g2.fillRect(sx, sy, sz, sz);
            if (sz==3 && blink>0.8f) {
                g2.setColor(new Color(c[0],c[1],c[2], Math.max(0,(int)(alpha*0.3f))));
                g2.drawLine(sx-3,sy+1,sx+5,sy+1); g2.drawLine(sx+1,sy-3,sx+1,sy+5);
            }
        }

        // ── Luna grande con alone e cratere ───────────────────────────────────
        float lunaPulse = 0.97f + 0.03f * (float)Math.sin(sec * 0.4f);
        int lunaR = (int)(W * 0.072f * lunaPulse);
        int lunaX = (int)(W * 0.80f), lunaY = (int)(H * 0.20f);
        // Alone esterno multi-strato
        for (int ring = 8; ring >= 1; ring--) {
            int rr = lunaR + ring * 8;
            float a = 1f - (float)ring / 9f;
            g2.setColor(new Color(200, 220, 255, Math.max(0, (int)(18 * a * lunaPulse))));
            g2.fillOval(lunaX - rr, lunaY - rr, rr*2, rr*2);
        }
        // Luna piena base (bianco-crema caldo)
        for (int r2 = lunaR; r2 >= 0; r2--) {
            float t = 1f - (float)r2/lunaR;
            g2.setColor(new Color(
                    Math.min(255,(int)(230 + 20*t)),
                    Math.min(255,(int)(235 + 15*t)),
                    Math.min(255,(int)(210 + 30*t)), 255));
            g2.fillOval(lunaX-r2, lunaY-r2, r2*2, r2*2);
        }
        // Ombra lieve (effetto sfumato lato sinistro)
        g2.setColor(new Color(8, 10, 28, 30));
        g2.fillOval(lunaX - lunaR + lunaR/4, lunaY - lunaR, lunaR*2, lunaR*2);
        // Cratere 1
        g2.setColor(new Color(180, 190, 210, 80));
        g2.fillOval(lunaX - (int)(lunaR*0.25f), lunaY - (int)(lunaR*0.3f),
                (int)(lunaR*0.28f), (int)(lunaR*0.18f));
        // Cratere 2
        g2.setColor(new Color(180, 190, 210, 60));
        g2.fillOval(lunaX + (int)(lunaR*0.15f), lunaY + (int)(lunaR*0.15f),
                (int)(lunaR*0.18f), (int)(lunaR*0.12f));
        // Bordo sottile
        g2.setColor(new Color(220, 228, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(lunaX - lunaR, lunaY - lunaR, lunaR*2, lunaR*2);
        g2.setStroke(new BasicStroke(1f));

        // ── Nuvole leggere che passano davanti alla luna ───────────────────────
        for (int nc = 0; nc < 3; nc++) {
            float cx = ((sec * (18f + nc * 7f) + nc * W * 0.35f) % (W * 1.4f)) - W * 0.2f;
            float cy = lunaY + nc * (int)(H * 0.035f) - (int)(H * 0.04f);
            int cw = (int)(W * (0.14f + nc * 0.04f)), ch = (int)(H * 0.04f);
            // Nuvola = ellissi sovrapposte
            g2.setColor(new Color(180, 195, 230, 22 - nc * 5));
            g2.fillOval((int)cx, (int)cy, cw, ch);
            g2.fillOval((int)(cx + cw*0.25f), (int)(cy - ch*0.3f), (int)(cw*0.55f), (int)(ch*0.7f));
            g2.fillOval((int)(cx + cw*0.55f), (int)(cy - ch*0.1f), (int)(cw*0.45f), (int)(ch*0.6f));
        }

        // ── Albero grande a sinistra ───────────────────────────────────────────
        // Tronco
        int trX = (int)(W * 0.11f), trYbase = H, trH = (int)(H * 0.62f);
        int trW = (int)(W * 0.018f);
        // Tronco con leggero movimento nel vento
        float wind = (float)Math.sin(sec * 0.6f) * 2f;
        int[] trunkX = {trX - trW/2, trX + trW/2,
                trX + trW/2 + (int)(wind*2), trX - trW/2 + (int)(wind*2),
                trX - trW/3 + (int)(wind*4), trX + trW/3 + (int)(wind*4)};
        // Gradiente tronco (più scuro in basso, più chiaro in alto)
        for (int seg = 0; seg < 20; seg++) {
            float t = (float)seg / 20;
            int ty1 = trYbase - (int)(trH * t), ty2 = trYbase - (int)(trH * (t + 0.05f));
            int ww = Math.max(4, (int)(trW * (1f - t * 0.45f)));
            int windOff = (int)(wind * t * 3);
            g2.setColor(new Color((int)(28+12*t),(int)(18+8*t),(int)(8+4*t)));
            g2.fillRect(trX - ww/2 + windOff, ty2, ww, ty1-ty2+1);
        }
        // Rami principali
        java.util.Random branchRng = new java.util.Random(5);
        int[] branchSegs = {6, 9, 12, 15, 17};
        for (int b = 0; b < branchSegs.length; b++) {
            float t = (float)branchSegs[b] / 20f;
            int bx = trX + (int)(wind * t * 3);
            int by = trYbase - (int)(trH * t);
            int bLen = (int)(W * (0.055f + (1-t)*0.03f));
            int bH   = (int)(H * 0.015f);
            int dir  = (b % 2 == 0) ? 1 : -1;
            float tilt = (float)Math.sin(sec * 0.6f + b * 0.8f) * 2f;
            g2.setColor(new Color(22,14,6));
            // Ramo come linea spessa
            g2.setStroke(new BasicStroke(Math.max(2, (int)(trW * 0.5f * (1-t*0.3f)))));
            g2.drawLine(bx, by, bx + dir * bLen + (int)(tilt*2), by - bH);
            g2.setStroke(new BasicStroke(1f));
        }
        // Chioma — ellissi nere/verde scurissimo a strati
        int foliageLayers = 7;
        for (int layer = foliageLayers; layer >= 0; layer--) {
            float t = (float)layer / foliageLayers;
            float wOff = (float)Math.sin(sec * 0.6f + layer * 0.4f) * (3f * (1f-t));
            int fw = (int)(W * (0.16f + t * 0.06f));
            int fh = (int)(H * (0.18f + t * 0.06f));
            int fx = trX - fw/2 + (int)(wind * (1-t) * 2) + (int)wOff;
            int fy = trYbase - trH - (int)(H * 0.08f) - (int)(fh * t * 0.35f);
            int green = (int)(8 + 12*t), blue = (int)(5 + 8*t);
            int alpha = layer == 0 ? 255 : 200 + layer*6;
            g2.setColor(new Color(6, green, blue, alpha));
            g2.fillOval(fx, fy, fw, fh);
        }
        // Dettagli chioma: piccoli rami visibili
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(15, 10, 4, 180));
        for (int rb = 0; rb < 5; rb++) {
            float t = (float)rb / 5f;
            int rx2 = trX - (int)(W*0.06f) + rb * (int)(W*0.03f);
            int ry2 = trYbase - trH - (int)(H*0.06f) - rb*(int)(H*0.02f);
            float wo = (float)Math.sin(sec * 0.6f + rb) * 3f;
            g2.drawLine(rx2, ry2, rx2 + (int)((rb-2)*14 + wo*2), ry2 - (int)(H*0.04f));
        }
        g2.setStroke(new BasicStroke(1f));

        // ── Corvi che volano ───────────────────────────────────────────────────
        int[][] crowData = {
                // {seed_x, seed_y, speed_mult, size}
                {(int)(W*0.45f), (int)(H*0.12f), 12, 8},
                {(int)(W*0.60f), (int)(H*0.08f), 9,  6},
                {(int)(W*0.35f), (int)(H*0.18f), 7,  7},
                {(int)(W*0.72f), (int)(H*0.14f), 14, 5},
                {(int)(W*0.55f), (int)(H*0.22f), 11, 6},
        };
        for (int[] cr : crowData) {
            float t2 = sec * cr[2] * 0.04f;
            // Vola in un percorso sinusoidale
            float cx2 = ((cr[0] + t2 * W * 0.18f) % (W * 1.3f)) - W * 0.15f;
            float cy2 = cr[1] + (float)Math.sin(t2 * 2.1f + cr[0]) * (H * 0.04f);
            int s = cr[3];
            // Ali in 3 posizioni: su, mezzo, giù (ogni 0.4 secondi)
            float wingPhase = (sec * 3.5f + cr[0] * 0.01f) % 1f;
            int wingY = wingPhase < 0.33f ? -s :
                    wingPhase < 0.66f ? 0  : s/2;
            // Corpo
            g2.setColor(new Color(10, 8, 15, 230));
            g2.fillOval((int)cx2 - s/2, (int)cy2 - s/4, s, s/2);
            // Ali (due triangoli)
            int[] wx1 = {(int)cx2, (int)cx2 - s*2, (int)cx2 - s};
            int[] wy1 = {(int)cy2, (int)cy2 + wingY, (int)cy2 - 1};
            int[] wx2 = {(int)cx2, (int)cx2 + s*2, (int)cx2 + s};
            int[] wy2 = {(int)cy2, (int)cy2 + wingY, (int)cy2 - 1};
            g2.fillPolygon(wx1, wy1, 3);
            g2.fillPolygon(wx2, wy2, 3);
        }

        // ── Erba e terreno in basso ────────────────────────────────────────────
        int grassY = H - (int)(H * 0.08f);
        // Gradiente terreno
        for (int y = grassY; y < H; y++) {
            float t = (float)(y - grassY) / (H - grassY);
            g2.setColor(new Color((int)(8+6*t),(int)(12+8*t),(int)(6+4*t)));
            g2.drawLine(0, y, W, y);
        }
        // Fili d'erba animati
        java.util.Random gRng = new java.util.Random(17);
        for (int gi = 0; gi < 60; gi++) {
            int gx = gRng.nextInt(W);
            int gh = (int)(H * (0.025f + gRng.nextFloat() * 0.04f));
            float gw = (float)Math.sin(sec * 1.2f + gi * 0.5f) * 2.5f;
            g2.setColor(new Color(15, 28+gRng.nextInt(15), 8, 200));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(gx, grassY, gx + (int)gw, grassY - gh);
            g2.setStroke(new BasicStroke(1f));
        }

        // ── Titolo animato ────────────────────────────────────────────────────
        int titFs = Math.max(18, (int)(H * 0.067f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float)titFs)
                : new Font("Consolas", Font.BOLD, titFs));
        float titGlow = 0.65f + 0.35f * (float)Math.sin(sec * 1.1f);
        for (int gi = 3; gi >= 1; gi--) {
            g2.setColor(new Color(200, 220, 255, Math.max(0,(int)(28*titGlow/gi))));
            drawTextCentered(g2, Lang.t("mod.seleziona"), W/2-gi, (int)(H*0.088f), titFs);
            drawTextCentered(g2, Lang.t("mod.seleziona"), W/2+gi, (int)(H*0.088f), titFs);
        }
        g2.setColor(new Color(0,5,20,160));
        drawTextCentered(g2, Lang.t("mod.seleziona"), W/2+2, (int)(H*0.088f)+2, titFs);
        g2.setColor(new Color(230, 240, 255));
        drawTextCentered(g2, Lang.t("mod.seleziona"), W/2, (int)(H*0.088f), titFs);
        float lineA = 0.55f + 0.45f*(float)Math.sin(sec*0.85f);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(180,210,255,Math.max(0,Math.min(255,(int)(180*lineA)))));
        int lW2=(int)(W*0.34f);
        g2.drawLine(W/2-lW2/2,(int)(H*0.088f)+7,W/2+lW2/2,(int)(H*0.088f)+7);
        g2.setStroke(new BasicStroke(1f));

        int hintFs = Math.max(9,(int)(H*0.023f));
        g2.setFont(new Font("Arial", Font.PLAIN, hintFs));
        g2.setColor(new Color(140,165,210,165));
        String hint = Lang.t("mod.hint");
        g2.drawString(hint, W/2-g2.getFontMetrics().stringWidth(hint)/2, (int)(H*0.158f));

        // ── Card modalità ─────────────────────────────────────────────────────
        String[] nomi  = { Lang.t("mod.storia"), Lang.t("mod.infinita") };
        String[] desc1 = { Lang.t("mod.storia.d1"),  Lang.t("mod.infinita.d1") };
        String[] desc2 = { Lang.t("mod.storia.d2"),  Lang.t("mod.infinita.d2") };
        BufferedImage[] icone = { res.imgIconaStoria, res.imgIconaInfinita };

        // Card STORIA: verde notte bosco | INFINITA: blu ghiaccio lunare
        int[][] cardTop = { {18, 32, 18}, {12, 20, 48} };
        int[][] cardBot = { {8,  16, 10}, {5,  10, 28} };
        Color[] bordoN  = { new Color(35,80,40),     new Color(30,65,120) };
        Color[] bordoS  = { new Color(80,180,90),    new Color(80,160,255) };
        Color[] nomeCol = { new Color(150,220,140),  new Color(130,200,255) };

        for (int i = 0; i < 2; i++) {
            java.awt.Rectangle rect = ui.rectsSelezioneModalita[i];
            if (rect == null) continue;
            boolean sel = (i == state.indiceModalitaSelezionata);

            // Bob verticale
            float bob = sel ? (float)Math.sin(sec*2.0f+i*0.6f)*4f
                    : (float)Math.sin(sec*0.7f+i*1.6f)*1.8f;
            int ry2 = rect.y + (int)bob;

            // Gradiente sfondo
            for (int dy=0; dy<rect.height; dy++) {
                float t=(float)dy/rect.height;
                int boost = sel ? 20 : 0;
                int cr=Math.min(255,(int)(cardTop[i][0]+(cardBot[i][0]-cardTop[i][0])*t)+boost);
                int cg=Math.min(255,(int)(cardTop[i][1]+(cardBot[i][1]-cardTop[i][1])*t)+boost);
                int cb=Math.min(255,(int)(cardTop[i][2]+(cardBot[i][2]-cardTop[i][2])*t)+boost);
                g2.setColor(new Color(cr,cg,cb, sel?225:170));
                g2.drawLine(rect.x+1, ry2+dy, rect.x+rect.width-2, ry2+dy);
            }

            // Glow selezione
            if (sel) {
                float glow=0.5f+0.5f*(float)Math.sin(sec*2.3f);
                int[] glL={9,6,4}, glA={20,40,62};
                for (int gl=0; gl<3; gl++) {
                    g2.setColor(new Color(bordoS[i].getRed(),bordoS[i].getGreen(),
                            bordoS[i].getBlue(),Math.max(0,Math.min(255,(int)(glA[gl]*glow)))));
                    g2.setStroke(new BasicStroke(glL[gl]));
                    g2.drawRoundRect(rect.x-glL[gl]/2, ry2-glL[gl]/2,
                            rect.width+glL[gl], rect.height+glL[gl], 16,16);
                }
            }
            g2.setColor(sel ? bordoS[i] : bordoN[i]);
            g2.setStroke(new BasicStroke(sel?2.5f:1.8f));
            g2.drawRoundRect(rect.x, ry2, rect.width, rect.height, 14,14);
            g2.setStroke(new BasicStroke(1f));

            int imgSize=(int)(rect.width*0.42f), imgX=rect.x+(rect.width-imgSize)/2;
            int imgY=ry2+(int)(rect.height*0.07f);
            int nomeY=ry2+(int)(rect.height*0.60f);
            int d1Y=ry2+(int)(rect.height*0.73f), d2Y=ry2+(int)(rect.height*0.84f);
            int nomeFs=Math.max(11,(int)(rect.width*0.088f));
            int descFs=Math.max(9,(int)(rect.width*0.072f));

            if (icone[i] != null) {
                if (sel) {
                    float al=0.35f+0.35f*(float)Math.sin(sec*2.0f+i*0.6f);
                    g2.setColor(new Color(bordoS[i].getRed(),bordoS[i].getGreen(),
                            bordoS[i].getBlue(),Math.max(0,Math.min(255,(int)(45*al)))));
                    g2.fillOval(imgX-8, imgY+imgSize-14, imgSize+16, 28);
                    for (int gl=3; gl>=1; gl--) {
                        g2.setColor(new Color(bordoS[i].getRed(),bordoS[i].getGreen(),
                                bordoS[i].getBlue(),Math.max(0,(int)(25*al/gl))));
                        g2.fillOval(imgX-gl*2, imgY-gl*2, imgSize+gl*4, imgSize+gl*4);
                    }
                }
                int dSz=sel?(int)(imgSize*1.05f):imgSize;
                g2.drawImage(icone[i], rect.x+(rect.width-dSz)/2, imgY+(imgSize-dSz)/2, dSz,dSz,null);
            } else {
                g2.setFont(res.fontCustomBold!=null
                        ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)(rect.height*0.24f))
                        : new Font("Consolas",Font.BOLD,(int)(rect.height*0.24f)));
                g2.setColor(sel?bordoS[i]:bordoN[i]);
                String fb = i==0 ? Lang.t("mod.s.desc") : Lang.t("mod.inf.desc");
                FontMetrics fmFb=g2.getFontMetrics();
                g2.drawString(fb, rect.x+(rect.width-fmFb.stringWidth(fb))/2, imgY+imgSize);
            }

            g2.setFont(res.fontCustomBold!=null
                    ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)nomeFs)
                    : new Font("Consolas",Font.BOLD,nomeFs));
            FontMetrics fmN=g2.getFontMetrics();
            g2.setColor(new Color(0,0,0,115));
            g2.drawString(nomi[i], rect.x+(rect.width-fmN.stringWidth(nomi[i]))/2+1, nomeY+1);
            g2.setColor(sel ? bordoS[i] : nomeCol[i]);
            g2.drawString(nomi[i], rect.x+(rect.width-fmN.stringWidth(nomi[i]))/2, nomeY);

            g2.setColor(new Color(bordoN[i].getRed(),bordoN[i].getGreen(),
                    bordoN[i].getBlue(), sel?100:45));
            g2.drawLine(rect.x+rect.width/5, nomeY+5, rect.x+rect.width*4/5, nomeY+5);

            g2.setFont(new Font("Arial", Font.ITALIC, descFs));
            g2.setColor(sel ? new Color(195,220,200) : new Color(140,170,155));
            FontMetrics fmD=g2.getFontMetrics();
            g2.drawString(desc1[i], rect.x+(rect.width-fmD.stringWidth(desc1[i]))/2, d1Y);
            g2.setFont(new Font("Arial", Font.PLAIN, descFs));
            g2.setColor(sel ? new Color(170,200,180) : new Color(115,148,132));
            g2.drawString(desc2[i], rect.x+(rect.width-fmD.stringWidth(desc2[i]))/2, d2Y);
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
        int txp=W/2-fmT.stringWidth(Lang.t("pausa.titolo"))/2;
        int typ=titY2+(int)(H*0.044f);
        float tGlow=0.65f+0.35f*(float)Math.sin(ms*0.0025);
        for (int i=3;i>0;i--){g2.setColor(new Color(220,155,38,Math.max(0,(int)(26*tGlow*i))));g2.drawString(Lang.t("pausa.titolo"),txp-i,typ);g2.drawString(Lang.t("pausa.titolo"),txp+i,typ);g2.drawString(Lang.t("pausa.titolo"),txp,typ-i);g2.drawString(Lang.t("pausa.titolo"),txp,typ+i);}
        g2.setColor(new Color(45,25,4,175)); g2.drawString(Lang.t("pausa.titolo"),txp+2,typ+2);
        g2.setColor(new Color(235,192,78));  g2.drawString(Lang.t("pausa.titolo"),txp,typ);
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
        long ms  = System.currentTimeMillis();
        java.util.Random rng = new java.util.Random();

        // ══════════════════════════════════════════════════════════════════════
        // SFONDO — statico TV bianco/grigio che lampeggia
        // ══════════════════════════════════════════════════════════════════════
        // Base nera
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, H);

        // Statico: rumore casuale di pixel bianchi/grigi su tutto lo schermo
        rng.setSeed(ms / 40); // cambia ~25fps per effetto statico
        for (int i = 0; i < W * H / 6; i++) {
            int sx = rng.nextInt(W);
            int sy = rng.nextInt(H);
            int br = 30 + rng.nextInt(200);
            int sz = rng.nextInt(3) == 0 ? 2 : 1;
            g2.setColor(new Color(br, br, br, 160 + rng.nextInt(95)));
            g2.fillRect(sx, sy, sz, sz);
        }

        // Linee orizzontali scansione CRT — strisce scure ogni ~4px
        for (int y = 0; y < H; y += 4) {
            g2.setColor(new Color(0, 0, 0, 80));
            g2.drawLine(0, y, W, y);
        }

        // Vignette bordi schermo
        for (int v = 0; v < 80; v++) {
            float a = (float)v / 80f;
            int al = Math.max(0, (int)(180 * (1 - a)));
            g2.setColor(new Color(0, 0, 0, al));
            g2.drawRect(v, v, W - v*2, H - v*2);
        }

        // ══════════════════════════════════════════════════════════════════════
        // MONITOR — cornice CRT al centro
        // ══════════════════════════════════════════════════════════════════════
        // Layout verticale garantito: monitor + controller + bottoni entrano tutti
        int btnH_est = ui.btnRiprova.bounds.height; // altezza bottone stimata
        int ctrlH_est = (int)((W * 0.32f) * 0.48f); // altezza controller stimata
        int gap1 = (int)(H * 0.018f); // gap monitor→controller
        int gap2 = (int)(H * 0.025f); // gap controller→bottoni
        int totalBottom = ctrlH_est + gap1 + gap2 + btnH_est + (int)(H * 0.02f);
        int mH = H - (int)(H * 0.06f) - totalBottom; // monitor prende il resto
        int mW = (int)(W * 0.66f);
        int mX = W/2 - mW/2;
        int mY = (int)(H * 0.06f);

        // Corpo monitor (grigio plastica)
        int depth = (int)(mW * 0.025f);
        // Lato destro (ombra)
        g2.setColor(new Color(55, 52, 50));
        g2.fillRect(mX + mW, mY + depth, depth, mH);
        // Lato basso (ombra)
        g2.fillRect(mX + depth, mY + mH, mW, depth);
        // Faccia principale
        g2.setColor(new Color(88, 84, 78));
        g2.fillRoundRect(mX, mY, mW, mH, 18, 18);
        // Highlight angolo top-left
        g2.setColor(new Color(115, 110, 104));
        g2.fillRoundRect(mX, mY, mW - 8, mH - 8, 18, 18);
        // Cornice plastica interna
        g2.setColor(new Color(40, 38, 35));
        int brd = (int)(mW * 0.032f);
        g2.fillRoundRect(mX + brd, mY + brd, mW - brd*2, mH - brd*2, 8, 8);

        // Schermo (area interna) — la parte con lo statico
        int sX = mX + brd + 4;
        int sY = mY + brd + 4;
        int sW = mW - brd*2 - 8;
        int sH = mH - brd*2 - 8;

        // Clip allo schermo — lo statico già disegnato, ora overlay colorato sul solo schermo
        g2.setClip(sX, sY, sW, sH);

        // Tinta verdina fosforescente sullo statico dello schermo
        g2.setColor(new Color(0, 255, 80, 18));
        g2.fillRect(sX, sY, sW, sH);

        // Linee di scansione più marcate sullo schermo
        for (int y = sY; y < sY + sH; y += 3) {
            g2.setColor(new Color(0, 0, 0, 55));
            g2.drawLine(sX, y, sX + sW, y);
        }

        // Glitch bar orizzontale — una banda che scorre verso il basso
        float glitchY = (float)((ms * 0.12) % (sH + 80)) - 40;
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRect(sX, sY + (int)glitchY, sW, 22);

        // Distorsione geometrica — poche strisce spostate orizzontalmente
        rng.setSeed(ms / 120);
        for (int gi = 0; gi < 5; gi++) {
            int gy = sY + rng.nextInt(sH);
            int gshift = (rng.nextInt(16) - 8);
            int gh = 2 + rng.nextInt(4);
            if (rng.nextInt(3) == 0) { // solo alcune strisce si spostano
                g2.copyArea(sX, gy, sW/2, gh, gshift, 0);
            }
        }

        // ── Testo "HAI PERSO" / "GAME OVER" sul monitor ──────────────────────
        int goFs = Math.max(20, (int)(sH * 0.18f));
        g2.setFont(res.fontCustomBold != null
                ? res.fontCustomBold.deriveFont(Font.PLAIN, (float) goFs)
                : new Font("Monospaced", Font.BOLD, goFs));

        // Effetto glitch: testo sfasato in rosso/ciano
        String goTxt = Lang.t("go.titolo");
        int gShiftX = (int)(3 * Math.sin(ms * 0.008f));
        g2.setColor(new Color(255, 0, 60, 140));
        drawTextCentered(g2, goTxt, W/2 + gShiftX + 3, sY + (int)(sH * 0.38f), goFs);
        g2.setColor(new Color(0, 255, 200, 100));
        drawTextCentered(g2, goTxt, W/2 - gShiftX - 3, sY + (int)(sH * 0.38f), goFs);
        // Testo principale bianco
        g2.setColor(new Color(230, 240, 220));
        drawTextCentered(g2, goTxt, W/2, sY + (int)(sH * 0.38f), goFs);

        // Stats piccole sotto il titolo
        int stFs = Math.max(9, (int)(sH * 0.085f));
        g2.setFont(res.fontCustom != null
                ? res.fontCustom.deriveFont(Font.PLAIN, (float) stFs)
                : new Font("Monospaced", Font.PLAIN, stFs));
        g2.setColor(new Color(180, 220, 180, 200));
        String stats = String.format(Lang.t("go.stats"),
                state.mondoAttuale, state.stanzaNelMondo, state.monete);
        drawTextCentered(g2, stats, W/2, sY + (int)(sH * 0.56f), stFs);

        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            int tot = (state.mondoAttuale - 1) * GameState.STANZA_BOSS + state.stanzaNelMondo;
            g2.setColor(new Color(200, 190, 120, 200));
            drawTextCentered(g2, String.format(Lang.t("go.stanze"), tot),
                    W/2, sY + (int)(sH * 0.67f), stFs);
        }

        // Riflesso vetro sullo schermo
        g2.setColor(new Color(255, 255, 255, 12));
        int[] refX = { sX + 4, sX + sW/3, sX + sW/4, sX + 4 };
        int[] refY = { sY + 4, sY + 4, sY + sH/3, sY + sH/3 };
        g2.fillPolygon(refX, refY, 4);

        g2.setClip(null);

        // Bordo schermo luminoso
        g2.setColor(new Color(20, 20, 20));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(sX - 1, sY - 1, sW + 2, sH + 2, 4, 4);
        g2.setStroke(new BasicStroke(1f));

        // Spia LED rossa lampeggiante in basso a destra del monitor
        float blink = (float)Math.sin(ms * 0.005f);
        int ledAlpha = Math.max(60, Math.min(255, (int)(160 + 95 * blink)));
        int ledX = mX + mW - brd - 10, ledY = mY + mH - brd - 10;
        g2.setColor(new Color(255, 30, 30, ledAlpha / 3));
        g2.fillOval(ledX - 4, ledY - 4, 14, 14);
        g2.setColor(new Color(255, 40, 40, ledAlpha));
        g2.fillOval(ledX, ledY, 6, 6);

        // ══════════════════════════════════════════════════════════════════════
        // CONTROLLER pixel-art — sotto il monitor, a sinistra
        // ══════════════════════════════════════════════════════════════════════
        int ctrlW = (int)(W * 0.32f);
        int ctrlH = (int)(ctrlW * 0.48f);
        int ctrlX = (int)(W * 0.10f);
        int ctrlY = mY + mH + gap1;

        // Corpo controller (grigio scuro pixelato)
        // Forma a trapezio arrotondato
        g2.setColor(new Color(55, 52, 50));    // ombra
        g2.fillRoundRect(ctrlX + 3, ctrlY + 3, ctrlW, ctrlH, 20, 20);
        g2.setColor(new Color(78, 74, 70));    // corpo
        g2.fillRoundRect(ctrlX, ctrlY, ctrlW, ctrlH, 20, 20);
        g2.setColor(new Color(95, 90, 85));    // highlight top
        g2.fillRoundRect(ctrlX, ctrlY, ctrlW, ctrlH/2, 20, 20);
        // Grip sinistro
        g2.setColor(new Color(65, 62, 58));
        g2.fillRoundRect(ctrlX + 4, ctrlY + ctrlH - ctrlH/3, ctrlW/3, ctrlH/3, 10, 10);
        // Grip destro
        g2.fillRoundRect(ctrlX + ctrlW - ctrlW/3 - 4, ctrlY + ctrlH - ctrlH/3, ctrlW/3, ctrlH/3, 10, 10);

        // D-pad (sinistra)
        int dpCX = ctrlX + (int)(ctrlW * 0.27f);
        int dpCY = ctrlY + (int)(ctrlH * 0.44f);
        int dpA  = (int)(ctrlW * 0.06f);
        g2.setColor(new Color(40, 38, 36));
        // croce
        g2.fillRect(dpCX - dpA, dpCY - dpA/2, dpA*2, dpA); // orizzontale
        g2.fillRect(dpCX - dpA/2, dpCY - dpA, dpA, dpA*2); // verticale

        // Analogico sinistro
        int an1X = ctrlX + (int)(ctrlW * 0.38f);
        int an1Y = ctrlY + (int)(ctrlH * 0.65f);
        int anR  = (int)(ctrlW * 0.07f);
        g2.setColor(new Color(40, 38, 36));
        g2.fillOval(an1X - anR - 2, an1Y - anR - 2, (anR+2)*2, (anR+2)*2);
        g2.setColor(new Color(60, 57, 54));
        g2.fillOval(an1X - anR, an1Y - anR, anR*2, anR*2);
        g2.setColor(new Color(80, 76, 72));
        g2.fillOval(an1X - anR/2, an1Y - anR/2, anR, anR);

        // Analogico destro
        int an2X = ctrlX + (int)(ctrlW * 0.65f);
        int an2Y = ctrlY + (int)(ctrlH * 0.65f);
        g2.setColor(new Color(40, 38, 36));
        g2.fillOval(an2X - anR - 2, an2Y - anR - 2, (anR+2)*2, (anR+2)*2);
        g2.setColor(new Color(60, 57, 54));
        g2.fillOval(an2X - anR, an2Y - anR, anR*2, anR*2);
        g2.setColor(new Color(80, 76, 72));
        g2.fillOval(an2X - anR/2, an2Y - anR/2, anR, anR);

        // Pulsanti ABXY (destra)
        int btnCX = ctrlX + (int)(ctrlW * 0.75f);
        int btnCY = ctrlY + (int)(ctrlH * 0.44f);
        int bR    = (int)(ctrlW * 0.05f);
        int[][] btnPos = {{0,-1},{1,0},{0,1},{-1,0}};
        Color[] btnCol = {new Color(200,60,60), new Color(60,160,60),
                new Color(60,100,200), new Color(200,180,60)};
        for (int bi = 0; bi < 4; bi++) {
            int bx = btnCX + (int)(btnPos[bi][0] * bR * 2.2f);
            int by = btnCY + (int)(btnPos[bi][1] * bR * 2.2f);
            g2.setColor(btnCol[bi].darker());
            g2.fillOval(bx - bR, by - bR + 2, bR*2, bR*2);
            g2.setColor(btnCol[bi]);
            g2.fillOval(bx - bR, by - bR, bR*2, bR*2);
        }

        // Start/Select
        int midX = ctrlX + ctrlW/2;
        int midY = ctrlY + (int)(ctrlH * 0.38f);
        int smR  = (int)(ctrlW * 0.032f);
        g2.setColor(new Color(50, 48, 45));
        g2.fillOval(midX - smR*3 - smR, midY - smR, smR*2, smR*2);
        g2.fillOval(midX + smR,          midY - smR, smR*2, smR*2);

        // Cavo controller che pende
        g2.setColor(new Color(40, 38, 36));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cableX = ctrlX + ctrlW/2 - 10;
        int cableY = ctrlY + ctrlH;
        g2.drawLine(cableX, cableY, cableX - 5, cableY + 15);
        g2.setStroke(new BasicStroke(1f));

        // ══════════════════════════════════════════════════════════════════════
        // BOTTONI — centrati orizzontalmente, sotto il controller
        // ══════════════════════════════════════════════════════════════════════
        int btnH    = ui.btnRiprova.bounds.height;
        int bw3     = (int)(W * 0.155f);
        int bg3     = (int)(W * 0.012f);
        int totalBW = bw3 * 3 + bg3 * 2;
        int bx0     = W/2 - totalBW/2;
        int byR     = ctrlY + ctrlH + gap2;

        ui.btnRiprova.setBounds(bx0,              byR, bw3, btnH);
        ui.btnMenuPrincipaleGO.setBounds(bx0 + bw3 + bg3, byR, bw3, btnH);
        ui.btnEsciGO.setBounds(bx0 + (bw3+bg3)*2, byR, bw3, btnH);

        ui.btnRiprova.draw(g2);
        ui.btnMenuPrincipaleGO.draw(g2);
        ui.btnEsciGO.draw(g2);
    }


    // ── Vittoria Storia ───────────────────────────────────────────────────────

    // ── Stanza Ufficio ────────────────────────────────────────────────────────

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
        String banTxt = Lang.t("ufficio.banner");
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
            String hint2 = Lang.t("dial.continua");
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
        g2.setColor(new Color(80,50,0,160));  drawTextCentered(g2,Lang.t("win.titolo"),W/2+3,panY+(int)(panH*0.32f)+3,titFs);
        g2.setColor(new Color(255,240,180));  drawTextCentered(g2,Lang.t("win.titolo"),W/2,  panY+(int)(panH*0.32f),  titFs);
        // Linea dorata
        FontMetrics fmT = g2.getFontMetrics();
        int tw = fmT.stringWidth(Lang.t("win.titolo"));
        g2.setColor(new Color(180,120,20,180)); g2.fillRect(W/2-tw/2-6,panY+(int)(panH*0.42f),tw+12,2);
        g2.setColor(new Color(255,210,80,100)); g2.fillRect(W/2-tw/2-6,panY+(int)(panH*0.42f),tw+12,1);

        // ── Sottotitolo ────────────────────────────────────────────────────────
        int subFs = Math.max(12, (int)(H*0.030f));
        g2.setFont(res.fontCustom!=null ? res.fontCustom.deriveFont(Font.PLAIN,(float)subFs) : new Font("Consolas",Font.PLAIN,subFs));
        g2.setColor(new Color(255,255,220,210));
        String sub = (GameState.MELEE_DELAY>0?Lang.t("win.sub.ritardo"):Lang.t("win.sub.molto"));
        drawTextCentered(g2, sub, W/2, panY+(int)(panH*0.54f), subFs);

        // ── Monete ─────────────────────────────────────────────────────────────
        int mFs = Math.max(11, (int)(H*0.026f));
        g2.setFont(res.fontCustomBold!=null ? res.fontCustomBold.deriveFont(Font.PLAIN,(float)mFs) : new Font("Consolas",Font.BOLD,mFs));
        g2.setColor(new Color(255,215,0));
        drawTextCentered(g2,String.format(Lang.t("win.monete"), state.monete), W/2, panY+(int)(panH*0.70f), mFs);

        // ── Codice debug ────────────────────────────────────────────────────────
        if (state.notaRaccolta) {
            int codFs = Math.max(10,(int)(H*0.022f));
            g2.setFont(res.fontCustom!=null ? res.fontCustom.deriveFont(Font.PLAIN,(float)codFs) : new Font("Consolas",Font.ITALIC,codFs));
            g2.setColor(new Color(180,255,180,200));
            drawTextCentered(g2,String.format(Lang.t("win.codice"), GameState.CODICE_DEBUG), W/2, panY+(int)(panH*0.84f), codFs);
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
        String logo = Lang.t("tet.logo");
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
        g2.drawString(Lang.t("tet.tempo"), timerBoxX + (timerBoxW - fmTL.stringWidth("TEMPO")) / 2,
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
            String istr = Lang.t("tet.istr");
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
            String msg = Lang.t(t.gameOver ? "tet.gameover" : "tet.timeout");
            g2.setFont(cf.apply((float) oFs1));
            FontMetrics fm1 = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(msg, W/2 - fm1.stringWidth(msg)/2, H/2 - cellH);
            g2.setFont(cfl.apply((float) oFs2));
            FontMetrics fm2 = g2.getFontMetrics();
            String ptsTxt = String.format(Lang.t("tet.punteggio"), t.punteggio);
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
            String hint = Lang.t("tet.hint");
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
            g2.drawString(Lang.t("tet.prossimo"), px + (pw - fmL.stringWidth("PROSSIMO")) / 2, cy);
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
            g2.drawString(Lang.t("tet.premio"), px + (pw - fmP.stringWidth("PREMIO")) / 2, cy);
            cy += (int)(labelFs * 1.1f);

            String pu = t.getPowerUp();
            String puLabel = switch (pu) {
                case "CURA"     -> Lang.t("tet.cura");
                case "VELOCITA" -> Lang.t("tet.velocita");
                case "DANNO"    -> Lang.t("tet.danno");
                case "TUTTO"    -> Lang.t("tet.tutto");
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
                case "MELEE"   -> Lang.t("tet.next.melee");
                case "DANNO"    -> Lang.t("tet.next.danno");
                case "VELOCITA" -> Lang.t("tet.next.vel");
                case "CURA"     -> Lang.t("tet.next.cura");
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
        String negozioTxt = Lang.t("shop.banner");
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
        String nomeShop = Lang.t("shop.nome");
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
        String domanda = Lang.t("shop.domanda");
        disegnaRigaDialogo(g2, domanda, txtX, lineY);
        lineY += fmT.getHeight() + 3;

        // Riga 2: conseguenza in grigio
        int avvFs = Math.max(9, (int)(H * 0.017f));
        g2.setFont(new Font("Consolas", Font.PLAIN, avvFs));
        g2.setColor(new Color(180, 130, 120));
        g2.drawString(Lang.t("shop.avviso"), txtX, lineY);

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
        String hint = Lang.t("shop.hint");
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
        int nomeBgW = fmN.stringWidth(pag.getNome()) + 18;
        // Sfondo nome
        Color nomeColor = isLeft ? new Color(255, 215, 0) : new Color(255, 120, 80);
        g2.setColor(new Color(nomeColor.getRed() / 4, nomeColor.getGreen() / 4,
                nomeColor.getBlue() / 4, 210));
        int nomeBgX = isLeft ? txtX - 2 : txtX - 2;
        g2.fillRoundRect(nomeBgX, sprY - 2, nomeBgW, fmN.getHeight() + 4, 6, 6);
        g2.setColor(nomeColor);
        g2.drawString(pag.getNome(), txtX + 6, sprY + fmN.getAscent());

        // ── Testo con wrap ────────────────────────────────────────────────────
        int testoFs = Math.max(11, (int)(H * 0.022f));
        g2.setFont(new Font("Consolas", Font.BOLD, testoFs));
        FontMetrics fmT = g2.getFontMetrics();
        // lineY parte dopo la box nome (height + padding) + gap
        int nomeBgH = fmN.getHeight() + 8;
        int lineY  = sprY + nomeBgH + (int)(H * 0.014f);
        int lineH  = fmT.getHeight() + 3;

        String[] parole = pag.getTesto().split(" ");
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
            String hint = cur < tot ? Lang.t("dial.continua") : Lang.t("dial.inizia");
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
        String titolo = Lang.t("nota.titolo");
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
                Lang.t("nota.riga0"),
                Lang.t("nota.riga1"),
                Lang.t("nota.riga2"),
                Lang.t("nota.riga3"),
                Lang.t("nota.riga4"),
                Lang.t("nota.riga5"),
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
        String firma = Lang.t("nota.firma");
        g2.drawString(firma, bx + bw - pad - g2.getFontMetrics().stringWidth(firma), by + bh - 22);

        // Hint chiudi
        g2.setFont(new Font("Consolas", Font.ITALIC, Math.max(9, (int)(H * 0.016f))));
        g2.setColor(new Color(100, 80, 50));
        String hint = Lang.t("nota.chiudi");
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
        String testo = Lang.t("casa.intro");
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
            String hint = Lang.t("dial.continua");
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
        String txt = Lang.t("banner.casa");
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
            righe.add(new String[]{Lang.t("bonus.titolo"), "header"});
            righe.add(new String[]{"─────────────", "sep"});
            if (state.arduaMalusDanno    > 0) righe.add(new String[]{"-" + state.arduaMalusDanno + " " + Lang.t("bonus.danno"),    "malus"});
            if (state.arduaMalusVelocita > 0) righe.add(new String[]{"-" + (int)state.arduaMalusVelocita + " " + Lang.t("bonus.velocita"), "malus"});
            if (state.arduaMalusFireRate > 0) righe.add(new String[]{Lang.t("bonus.fuocolento"), "malus"});
            if (righe.size() == 2) righe.add(new String[]{Lang.t("bonus.nessunmalus"), "note"});

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
                    ? String.format(Lang.t("hud.mondo"), state.mondoAttuale) + ": " + ts.nomeMondo
                    : "Inf W" + state.mondoAttuale + ": " + ts.nomeMondo;
            drawText(g2, mondoStr, cx, cy + fs/3, fs);
            cx += g2.getFontMetrics().stringWidth(mondoStr) + 18;
            g2.setColor(Color.WHITE);
            String stanzaLbl = String.format(Lang.t("hud.stanza"), state.stanzaNelMondo, GameState.STANZA_BOSS);
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
                    String bLbl = Lang.t("hud.bonus");
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

            // ── Indicatori stato: freeze / slow — stessa riga di burn ─────────
            int statoY  = cy - ico/2;   // allineati con burn
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
                String lblFrz = Lang.t("hud.freeze");
                FontMetrics fmBar = g2.getFontMetrics();
                int barW = fmBar.stringWidth(lblFrz) + 20;
                int barH = Math.max(fmBar.getHeight() + 4, (int)(H * 0.028f));
                g2.setColor(new Color(10, 25, 60, 220));
                g2.fillRoundRect(cx, statoY, barW, barH, barH/2, barH/2);
                g2.setColor(new Color(60, 150, 255, 210));
                g2.fillRoundRect(cx, statoY, Math.max(barH, (int)(barW * prog)), barH, barH/2, barH/2);
                g2.setColor(new Color(140, 210, 255, 200));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(cx, statoY, barW, barH, barH/2, barH/2);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(220, 245, 255));
                int lblX = cx + (barW - fmBar.stringWidth(lblFrz)) / 2;
                int lblY = statoY + (barH + fmBar.getAscent() - fmBar.getDescent()) / 2;
                g2.drawString(lblFrz, lblX, lblY);
            }

            if (state.slowAttivo && !state.freezeAttivo) {
                int rimasti = state.slowTimer;
                int totale  = GameState.SLOW_DURATA;
                float prog  = Math.max(0f, rimasti / (float)totale);
                String lblSl = Lang.t("hud.lento");
                FontMetrics fmBar = g2.getFontMetrics();
                int barW = fmBar.stringWidth(lblSl) + 20;
                int barH = Math.max(fmBar.getHeight() + 4, (int)(H * 0.028f));
                g2.setColor(new Color(10, 35, 50, 220));
                g2.fillRoundRect(cx, statoY, barW, barH, barH/2, barH/2);
                g2.setColor(new Color(40, 130, 200, 200));
                g2.fillRoundRect(cx, statoY, Math.max(barH, (int)(barW * prog)), barH, barH/2, barH/2);
                g2.setColor(new Color(100, 185, 245, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(cx, statoY, barW, barH, barH/2, barH/2);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(200, 235, 255));
                int lblX = cx + (barW - fmBar.stringWidth(lblSl)) / 2;
                int lblY = statoY + (barH + fmBar.getAscent() - fmBar.getDescent()) / 2;
                g2.drawString(lblSl, lblX, lblY);
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
            String mn = String.format(Lang.t("hud.mondo"), state.mondoAttuale);
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
            g2.drawString(String.format(Lang.t("hud.mondo"), state.mondoAttuale) + " St." + state.stanzaNelMondo, gox + 10, goy + pad + ico + fs + 4);
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
                Lang.t("boss.m1.nome"), Lang.t("boss.m2.nome"), Lang.t("boss.m3.nome"),
                Lang.t("boss.m4.nome"), Lang.t("boss.m5.nome")
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