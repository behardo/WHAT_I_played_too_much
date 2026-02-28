import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WhatIvePlayedTooMuch extends JPanel implements ActionListener, MouseListener {

    private enum StatoGioco { MENU, GIOCO, GAME_OVER, PAUSA }
    private StatoGioco statoGioco = StatoGioco.MENU;
    private StatoGioco statoPrecedente;

    private final int TILE_SIZE = 64;
    private final int COL_GIOCO = 15;
    private final int RIG_GIOCO = 5;
    private final int OFFSET = 1;
    private final int COL_TOTALI = COL_GIOCO + (OFFSET * 2);
    private final int RIG_TOTALI = RIG_GIOCO + (OFFSET * 2);

    private final int LARGHEZZA_GIOCO = COL_TOTALI * TILE_SIZE;
    private final int ALTEZZA_GIOCO = RIG_TOTALI * TILE_SIZE;

    private float x, y;
    // --- SHOP: Velocità e Danno di base (per upgrade) ---
    private float velocitaDiBase = 6.0f;
    private float velocita = velocitaDiBase;
    private final int PG_SIZE = 50;
    private int dannoDiBase = 1;
    private int dannoPugno = dannoDiBase;
    // ----------------------------------------------------

    // Risorse grafiche
    private BufferedImage imgPersonaggio, imgPorta, imgNemico, imgCuore, imgPugno;
    private BufferedImage imgMuroMondo1, imgPavimentoMondo1;
    private BufferedImage imgMuroMondo2, imgPavimentoMondo2;
    private BufferedImage imgBoss, imgCura, imgMoneta;
    private BufferedImage imgNemico2;
    // --- SHOP: Nuove immagini ---
    private BufferedImage imgShopDoor, imgShopkeeper, imgItemSpeed, imgItemDamage;
    // ----------------------------

    private int monete = 0;

    // Liste oggetti attivi
    private List<Pugno> pugniAttivi = new ArrayList<>();
    private List<Cura> curePerStanzaList = new ArrayList<>();
    private List<Moneta> monetePerStanzaList = new ArrayList<>();

    private boolean up, down, left, right;
    private boolean shootUp, shootDown, shootLeft, shootRight;
    private int cooldownSparo = 0;
    private final int SPARO_DELAY = 12;

    // Gestione Stanze e Mondi
    private List<Color> memoriaColoriMondo1 = new ArrayList<>();
    private List<Color> memoriaColoriMondo2 = new ArrayList<>();
    private List<List<Nemico>> nemiciPerStanza = new ArrayList<>();
    private List<List<Cura>> curePerStanzaMemoria = new ArrayList<>();
    private List<List<Moneta>> monetePerStanzaMemoria = new ArrayList<>();
    // --- SHOP: Memoria per Shopkeepers e ShopItems in ogni stanza ---
    private List<List<Shopkeeper>> shopkeepersPerStanza = new ArrayList<>();
    private List<List<ShopItem>> shopItemsPerStanza = new ArrayList<>();
    // ------------------------------------------------------------------
    private int indiceStanzaMemoria = 0;
    private Random random = new Random();

    // Variabili di stato gioco
    private int mondoAttuale = 1;
    private int stanzaNelMondo = 1;
    private final int STANZA_BOSS = 8;
    private boolean bossSpawnato = false;
    private boolean bossSconfitto = false;

    // Sistema Vite
    private final int VITE_MAX = 3;
    private int vite = VITE_MAX;
    private boolean invulnerabile = false;
    private int timerInvulnerabilita = 0;

    // Hitbox pulsanti Game Over
    private Rectangle btnRiprova = new Rectangle(LARGHEZZA_GIOCO / 2 - 150, ALTEZZA_GIOCO / 2 + 80, 140, 50);
    private Rectangle btnEsci = new Rectangle(LARGHEZZA_GIOCO / 2 + 10, ALTEZZA_GIOCO / 2 + 80, 140, 50);

    // Fullscreen
    private static JFrame finestra;
    private static boolean isFullscreen = false;
    private static GraphicsDevice device;

    public WhatIvePlayedTooMuch() {
        setPreferredSize(new Dimension(LARGHEZZA_GIOCO, ALTEZZA_GIOCO));
        setFocusable(true);
        resetGiocatore();

        // Inizializza la prima stanza del Mondo 1
        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanzaMemoria.add(new ArrayList<>());
        monetePerStanzaMemoria.add(new ArrayList<>());
        // --- SHOP: Stanza 1 vuota ---
        shopkeepersPerStanza.add(new ArrayList<>());
        shopItemsPerStanza.add(new ArrayList<>());
        // ---------------------------

        // CARICAMENTO RISORSE
        imgPersonaggio   = caricaImmagine("/personaggio.png");
        imgPorta         = caricaImmagine("/porta.png");
        imgNemico        = caricaImmagine("/nemico.png");
        imgCuore         = caricaImmagine("/cuore.png");
        imgPugno         = caricaImmagine("/pugno.png");
        imgMuroMondo1      = caricaImmagine("/muro.png");
        imgPavimentoMondo1 = caricaImmagine("/pavimento.png");
        imgMuroMondo2      = caricaImmagine("/muro2.png");
        imgPavimentoMondo2 = caricaImmagine("/pavimento2.png");
        imgBoss            = caricaImmagine("/boss.png");
        imgCura          = caricaImmagine("/cura.png");
        imgMoneta        = caricaImmagine("/coin.png");
        imgNemico2       = caricaImmagine("/nemico2.png");

        // --- SHOP: Caricamento nuove immagini ---
        imgShopDoor      = caricaImmagine("/shop_door.png"); // Crea questo file!
        imgShopkeeper    = caricaImmagine("/shopkeeper.png"); // Crea questo file!
        imgItemSpeed     = caricaImmagine("/item_speed.png"); // Crea questo file!
        imgItemDamage    = caricaImmagine("/item_damage.png"); // Crea questo file!
        // ----------------------------------------

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullscreen();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (statoGioco == StatoGioco.GIOCO) {
                        statoPrecedente = StatoGioco.GIOCO;
                        statoGioco = StatoGioco.PAUSA;
                    } else if (statoGioco == StatoGioco.PAUSA) {
                        statoGioco = StatoGioco.GIOCO;
                    }
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_ENTER && statoGioco == StatoGioco.PAUSA) {
                    statoGioco = StatoGioco.GIOCO;
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_Q && statoGioco == StatoGioco.PAUSA) {
                    System.exit(0);
                    return;
                }

                if (statoGioco == StatoGioco.GIOCO) {
                    toggleMovimento(e.getKeyCode(), true);
                    toggleSparo(e.getKeyCode(), true);
                } else if (statoGioco == StatoGioco.MENU && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    statoGioco = StatoGioco.GIOCO;
                }
            }
            public void keyReleased(KeyEvent e) {
                if (statoGioco == StatoGioco.GIOCO) {
                    toggleMovimento(e.getKeyCode(), false);
                    toggleSparo(e.getKeyCode(), false);
                }
            }
        });

        addMouseListener(this);

        new Timer(16, this).start();
    }

    private BufferedImage caricaImmagine(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) return ImageIO.read(url);
        } catch (IOException e) {}
        return null;
    }

    private void resetGiocatore() {
        this.x = (LARGHEZZA_GIOCO) / 2;
        this.y = (ALTEZZA_GIOCO) / 2;
        pugniAttivi.clear();
        vite = VITE_MAX;
        invulnerabile = false;
        up = down = left = right = false;
        shootUp = shootDown = shootLeft = shootRight = false;
        // --- SHOP: Reset upgrade ---
        velocita = velocitaDiBase;
        dannoPugno = dannoDiBase;
        // ---------------------------
    }

    private void resetTotalGame() {
        resetGiocatore();
        mondoAttuale = 1;
        stanzaNelMondo = 1;
        indiceStanzaMemoria = 0;
        bossSpawnato = false;
        bossSconfitto = false;
        monete = 0;
        nemiciPerStanza.clear();
        curePerStanzaMemoria.clear();
        monetePerStanzaMemoria.clear();
        // --- SHOP ---
        shopkeepersPerStanza.clear();
        shopItemsPerStanza.clear();
        // ------------
        memoriaColoriMondo1.clear();
        memoriaColoriMondo2.clear();

        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanzaMemoria.add(new ArrayList<>());
        monetePerStanzaMemoria.add(new ArrayList<>());
        // --- SHOP ---
        shopkeepersPerStanza.add(new ArrayList<>());
        shopItemsPerStanza.add(new ArrayList<>());
        // ------------
    }

    private void generaNuovaStanza() {
        if (mondoAttuale == 1) memoriaColoriMondo1.add(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
        else memoriaColoriMondo2.add(new Color(100 + random.nextInt(50), random.nextInt(100), 150 + random.nextInt(100)));

        List<Nemico> nuoviNemici = new ArrayList<>();
        List<Cura> nuoveCureMem = new ArrayList<>();
        List<Moneta> nuoveMoneteMem = new ArrayList<>();
        // --- SHOP: Liste per memoria nuova stanza ---
        List<Shopkeeper> nuoviShopkeepers = new ArrayList<>();
        List<ShopItem> nuoveItems = new ArrayList<>();
        // --------------------------------------------

        if (stanzaNelMondo == STANZA_BOSS && !bossSpawnato) {
            nuoviNemici.add(new Boss(7, 2, TILE_SIZE));
            bossSpawnato = true;
            bossSconfitto = false;
        } else if (stanzaNelMondo == STANZA_BOSS && bossSpawnato && !bossSconfitto) {
            nuoviNemici.add(new Boss(7, 2, TILE_SIZE));
        } else if (stanzaNelMondo < STANZA_BOSS) {

            int quantiBase = random.nextInt(2) + (stanzaNelMondo / 2) + (mondoAttuale * 2);

            // --- SHOP: Se è la Stanza 4, spawna lo shopkeeper e gli items ---
            if (stanzaNelMondo == 4) {
                // Posizionamento Shopkeeper (vicino alla porta in alto)
                nuoviShopkeepers.add(new Shopkeeper(7, 1, TILE_SIZE, imgShopkeeper));

                // Posizionamento Items (3 oggetti in vendita)
                nuoveItems.add(new ShopItem(5, 2, TILE_SIZE, "CURA", 2, imgCura)); // Cura standard
                nuoveItems.add(new ShopItem(7, 2, TILE_SIZE, "VELOCITA", 5, imgItemSpeed)); // Velocità
                nuoveItems.add(new ShopItem(9, 2, TILE_SIZE, "DANNO", 7, imgItemDamage)); // Danno Pugno
            } else {
                // Nemici normali negli altri livelli
                for(int i=0; i<quantiBase; i++) {
                    int safeX = 0, safeY = 0;
                    boolean safe = false;
                    while (!safe) {
                        safeX = random.nextInt(COL_GIOCO) + OFFSET;
                        safeY = random.nextInt(RIG_GIOCO) + OFFSET;
                        safe = true;
                        if (safeX >= OFFSET && safeX <= OFFSET + 2 && safeY == 3) safe = false;
                        if (safeX >= COL_TOTALI - 4 && safeX <= COL_TOTALI - 1 && safeY == 3) safe = false;
                    }

                    if (mondoAttuale == 1) {
                        if (stanzaNelMondo <= 4) nuoviNemici.add(new Nemico(safeX, safeY, TILE_SIZE, 3));
                        else {
                            if (random.nextFloat() < 0.60f) nuoviNemici.add(new NemicoForte(safeX, safeY, TILE_SIZE));
                            else nuoviNemici.add(new Nemico(safeX, safeY, TILE_SIZE, 3));
                        }
                    } else if (mondoAttuale == 2) nuoviNemici.add(new NemicoForte(safeX, safeY, TILE_SIZE));
                }
            }
            // ------------------------------------------------------------------
        }

        nemiciPerStanza.add(nuoviNemici);
        curePerStanzaMemoria.add(nuoveCureMem);
        monetePerStanzaMemoria.add(nuoveMoneteMem);
        // --- SHOP: Aggiunta cure alla memoria ---
        shopkeepersPerStanza.add(nuoviShopkeepers);
        shopItemsPerStanza.add(nuoveItems);
        // ----------------------------------------
        pugniAttivi.clear();
    }

    private void toggleMovimento(int k, boolean p) {
        if (k == KeyEvent.VK_W) up = p;
        if (k == KeyEvent.VK_S) down = p;
        if (k == KeyEvent.VK_A) left = p;
        if (k == KeyEvent.VK_D) right = p;
    }

    private void toggleSparo(int k, boolean p) {
        if (k == KeyEvent.VK_UP)    shootUp = p;
        if (k == KeyEvent.VK_DOWN)  shootDown = p;
        if (k == KeyEvent.VK_LEFT)  shootLeft = p;
        if (k == KeyEvent.VK_RIGHT) shootRight = p;
    }

    private void sparaPugno() {
        if (cooldownSparo > 0) return;
        int dirX = 0, dirY = 0;
        if (shootUp) dirY = -1;
        if (shootDown) dirY = 1;
        if (shootLeft) dirX = -1;
        if (shootRight) dirX = 1;

        if (dirX != 0 || dirY != 0) {
            pugniAttivi.add(new Pugno(x, y, dirX, dirY, imgPugno, dannoPugno)); // Passa il danno aggiornato
            cooldownSparo = SPARO_DELAY;
        }
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        finestra.dispose();

        if (isFullscreen) {
            finestra.setUndecorated(true);
            device.setFullScreenWindow(finestra);
        } else {
            finestra.setUndecorated(false);
            device.setFullScreenWindow(null);
            finestra.setPreferredSize(new Dimension(LARGHEZZA_GIOCO, ALTEZZA_GIOCO));
            finestra.pack();
            finestra.setLocationRelativeTo(null);
        }

        finestra.setVisible(true);
        this.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (statoGioco != StatoGioco.GIOCO) {
            repaint();
            return;
        }

        // Limiti
        float minX = OFFSET * TILE_SIZE;
        float maxX = (OFFSET + COL_GIOCO) * TILE_SIZE - PG_SIZE;
        float minY = OFFSET * TILE_SIZE;
        float maxY = (OFFSET + RIG_GIOCO) * TILE_SIZE - PG_SIZE;

        // Movimento
        if (up && y > minY) y -= velocita;
        if (down && y < maxY) y += velocita;

        boolean inZonaPortaY = (y > (RIG_TOTALI * TILE_SIZE) / 2 - TILE_SIZE) && (y < (RIG_TOTALI * TILE_SIZE) / 2 + TILE_SIZE);

        if (left) {
            if (x > minX) x -= velocita;
            else if (inZonaPortaY && indiceStanzaMemoria > 0) {
                x = (COL_TOTALI - OFFSET - 1) * TILE_SIZE - 20;
                indiceStanzaMemoria--;
                stanzaNelMondo--;
            }
        }

        if (right) {
            if (x < maxX) x += velocita;
            else if (inZonaPortaY) {

                if (stanzaNelMondo == STANZA_BOSS && mondoAttuale == 1) {
                    if (bossSconfitto) {
                        mondoAttuale = 2;
                        stanzaNelMondo = 1;
                        indiceStanzaMemoria = 0;
                        nemiciPerStanza.clear();
                        curePerStanzaMemoria.clear();
                        monetePerStanzaMemoria.clear();
                        shopkeepersPerStanza.clear();
                        shopItemsPerStanza.clear();
                        memoriaColoriMondo1.clear();
                        memoriaColoriMondo2.add(new Color(120, 80, 150));
                        nemiciPerStanza.add(new ArrayList<>());
                        curePerStanzaMemoria.add(new ArrayList<>());
                        monetePerStanzaMemoria.add(new ArrayList<>());
                        shopkeepersPerStanza.add(new ArrayList<>());
                        shopItemsPerStanza.add(new ArrayList<>());
                        resetGiocatore();
                    } else x = maxX - 10;
                } else {
                    x = OFFSET * TILE_SIZE + 20;
                    indiceStanzaMemoria++;
                    stanzaNelMondo++;
                    if (indiceStanzaMemoria >= nemiciPerStanza.size()) generaNuovaStanza();
                }
            }
        }

        // --- SHOP: Logica Rilevamento Shopkeeper (battuta) ---
        List<Shopkeeper> shopkeepersCorrenti = shopkeepersPerStanza.get(indiceStanzaMemoria);
        Rectangle hbGiocatore = new Rectangle((int)x, (int)y, PG_SIZE, PG_SIZE);
        for (Shopkeeper sk : shopkeepersCorrenti) {
            if (hbGiocatore.intersects(sk.getHitbox())) {
                sk.attivaBattuta();
            }
        }
        // -----------------------------------------------------

        // --- SHOP: Logica Acquisto Items ---
        List<ShopItem> itemsCorrenti = shopItemsPerStanza.get(indiceStanzaMemoria);
        for (ShopItem si : itemsCorrenti) {
            if (si.controllaAcquisto(x, y, PG_SIZE, monete)) {
                // Acquisto avvenuto!
                monete -= si.getCosto();
                si.setAcquistato();

                // Applica l'effetto dell'upgrade
                if (si.getTipo().equals("CURA")) {
                    vite++;
                    if (vite > VITE_MAX) vite = VITE_MAX;
                } else if (si.getTipo().equals("VELOCITA")) {
                    velocita += 1.5f; // Aumento velocità
                    System.out.println("Upgrade Velocità! Nuova velocità: " + velocita);
                } else if (si.getTipo().equals("DANNO")) {
                    dannoPugno++; // Aumento danno pugno
                    System.out.println("Upgrade Danno! Nuovo danno: " + dannoPugno);
                }
            }
        }
        // -----------------------------------

        // Sparo e cooldown
        if (cooldownSparo > 0) cooldownSparo--;
        sparaPugno();

        // Update Pugni
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno p = pugniAttivi.get(i);
            p.update();
            if (p.daRimuovere) {
                pugniAttivi.remove(i);
                i--;
            }
        }

        // Invulnerabilità
        if (invulnerabile) {
            timerInvulnerabilita++;
            if (timerInvulnerabilita > 60) {
                invulnerabile = false;
                timerInvulnerabilita = 0;
            }
        }

        // Raccolta Oggetti di Cura
        List<Cura> cureCorrentiMem = curePerStanzaMemoria.get(indiceStanzaMemoria);
        for (int i = 0; i < cureCorrentiMem.size(); i++) {
            Cura c = cureCorrentiMem.get(i);
            if (c.controllaRaccolta(x, y, PG_SIZE)) {
                vite++;
                if (vite > VITE_MAX) vite = VITE_MAX;
                cureCorrentiMem.remove(i);
                i--;
            }
        }

        // Raccolta Monete
        List<Moneta> moneteCorrentiMem = monetePerStanzaMemoria.get(indiceStanzaMemoria);
        for (int i = 0; i < moneteCorrentiMem.size(); i++) {
            Moneta m = moneteCorrentiMem.get(i);
            if (m.controllaRaccolta(x, y, PG_SIZE)) {
                monete += m.getValore();
                moneteCorrentiMem.remove(i);
                i--;
            }
        }

        // Update Nemici e collisione
        List<Nemico> nemiciCorrenti = nemiciPerStanza.get(indiceStanzaMemoria);
        for (Nemico n : nemiciCorrenti) {
            n.update(x, y);
            if (!invulnerabile && n.toccaGiocatore(x, y, PG_SIZE)) {
                vite--;
                invulnerabile = true;
                if (vite <= 0) {
                    statoGioco = StatoGioco.GAME_OVER;
                }
            }
        }

        // Collisione Pugni/Nemici
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno p = pugniAttivi.get(i);
            Rectangle hbPugno = p.getHitbox();

            for (int j = 0; j < nemiciCorrenti.size(); j++) {
                Nemico n = nemiciCorrenti.get(j);
                Rectangle hbNemico = n.getHitbox();

                if (hbPugno.intersects(hbNemico)) {
                    n.subisciDanno(p.getDanno());
                    p.daRimuovere = true;

                    if (n.isMorto()) {
                        if (n instanceof Boss) {
                            bossSconfitto = true;
                        }

                        float mx = n.x;
                        float my = n.y;

                        nemiciCorrenti.remove(j);
                        j--;

                        if (nemiciCorrenti.isEmpty() && stanzaNelMondo != 4) { // Niente drop condizionale nella stanza shop
                            if (vite < VITE_MAX) {
                                cureCorrentiMem.add(new Cura((int)mx/TILE_SIZE, (int)my/TILE_SIZE, TILE_SIZE, imgCura));
                            } else {
                                moneteCorrentiMem.add(new Moneta((int)mx/TILE_SIZE, (int)my/TILE_SIZE, TILE_SIZE, imgMoneta));
                            }
                        }
                    }
                    break;
                }
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gestione scaling Fullscreen
        double scaleX = (double) getWidth() / LARGHEZZA_GIOCO;
        double scaleY = (double) getHeight() / ALTEZZA_GIOCO;
        double scale = Math.min(scaleX, scaleY);
        int offsetX = (int) ((getWidth() - LARGHEZZA_GIOCO * scale) / 2);
        int offsetY = (int) ((getHeight() - ALTEZZA_GIOCO * scale) / 2);

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        if (statoGioco == StatoGioco.MENU) disegnaMenu(g2);
        else if (statoGioco == StatoGioco.GIOCO) disegnaGioco(g2);
        else if (statoGioco == StatoGioco.GAME_OVER) disegnaGameOver(g2);
        else if (statoGioco == StatoGioco.PAUSA) disegnaPausa(g2);
    }

    private void disegnaMenu(Graphics2D g2) {
        g2.setColor(new Color(20, 20, 30));
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 40));
        g2.drawString("WHAT: I'VE PLAYED TOO MUCH", LARGHEZZA_GIOCO/2 - 300, ALTEZZA_GIOCO/2 - 50);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Premi [INVIO] per iniziare", LARGHEZZA_GIOCO/2 - 120, ALTEZZA_GIOCO/2 + 20);
        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.drawString("Premi [F11] per Fullscreen", LARGHEZZA_GIOCO/2 - 100, ALTEZZA_GIOCO/2 + 50);
    }

    private void disegnaPausa(Graphics2D g2) {
        disegnaGioco(g2);
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 50));
        g2.drawString("P A U S A", LARGHEZZA_GIOCO / 2 - 120, ALTEZZA_GIOCO / 2 - 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Premi [ESC] o [INVIO] per riprendere", LARGHEZZA_GIOCO / 2 - 160, ALTEZZA_GIOCO / 2 + 30);
        g2.drawString("Premi [Q] per uscire", LARGHEZZA_GIOCO / 2 - 80, ALTEZZA_GIOCO / 2 + 60);
    }

    private void disegnaGameOver(Graphics2D g2) {
        disegnaGioco(g2);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);

        g2.setColor(Color.RED);
        g2.setFont(new Font("Consolas", Font.BOLD, 60));
        g2.drawString("G A M E   O V E R", LARGHEZZA_GIOCO / 2 - 250, ALTEZZA_GIOCO / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.drawString("Mondo " + mondoAttuale + ", Stanza " + stanzaNelMondo + ", Monete: " + monete, LARGHEZZA_GIOCO / 2 - 210, ALTEZZA_GIOCO / 2 + 20);

        // DISEGNO PULSANTI
        g2.setFont(new Font("Arial", Font.BOLD, 22));

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(btnRiprova.x, btnRiprova.y, btnRiprova.width, btnRiprova.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnRiprova.x, btnRiprova.y, btnRiprova.width, btnRiprova.height);
        g2.drawString("RIPROVA", btnRiprova.x + 20, btnRiprova.y + 32);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(btnEsci.x, btnEsci.y, btnEsci.width, btnEsci.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnEsci.x, btnEsci.y, btnEsci.width, btnEsci.height);
        g2.drawString("ESCI", btnEsci.x + 45, btnEsci.y + 32);
    }

    private void disegnaGioco(Graphics2D g2) {
        BufferedImage imgMuroCorr      = (mondoAttuale == 1) ? imgMuroMondo1 : imgMuroMondo2;
        BufferedImage imgPavimentoCorr = (mondoAttuale == 1) ? imgPavimentoMondo1 : imgPavimentoMondo2;
        Color coloreBackupPavimento   = (mondoAttuale == 1) ? memoriaColoriMondo1.get(indiceStanzaMemoria) : memoriaColoriMondo2.get(indiceStanzaMemoria);

        // 1. Pavimento e Muri
        for (int i = 0; i < COL_TOTALI; i++) {
            for (int j = 0; j < RIG_TOTALI; j++) {
                int px = i * TILE_SIZE;
                int py = j * TILE_SIZE;
                if (i < OFFSET || i >= COL_GIOCO + OFFSET || j < OFFSET || j >= RIG_GIOCO + OFFSET) {
                    if (imgMuroCorr != null) g2.drawImage(imgMuroCorr, px, py, TILE_SIZE, TILE_SIZE, null);
                    else { g2.setColor(Color.BLACK); g2.fillRect(px, py, TILE_SIZE, TILE_SIZE); }
                } else {
                    if (imgPavimentoCorr != null) g2.drawImage(imgPavimentoCorr, px, py, TILE_SIZE, TILE_SIZE, null);
                    else { g2.setColor(coloreBackupPavimento); g2.fillRect(px, py, TILE_SIZE, TILE_SIZE); }
                }
            }
        }

        // 2. Porte
        int portaY = (RIG_TOTALI / 2) * TILE_SIZE;
        int portaDX = (COL_TOTALI - 1) * TILE_SIZE;

        if (indiceStanzaMemoria > 0) {
            if (imgPorta != null) g2.drawImage(imgPorta, 0, portaY, TILE_SIZE, TILE_SIZE, null);
            else { g2.setColor(Color.YELLOW); g2.fillRect(0, portaY, TILE_SIZE, TILE_SIZE); }
        }

        if (stanzaNelMondo == STANZA_BOSS && mondoAttuale == 1) {
            if (bossSconfitto) {
                g2.setColor(new Color(0, 255, 255, 150));
                g2.fillRect(portaDX, portaY, TILE_SIZE, TILE_SIZE);
            } else {
                if (imgPorta != null) g2.drawImage(imgPorta, portaDX, portaY, TILE_SIZE, TILE_SIZE, null);
                else { g2.setColor(Color.RED); g2.fillRect(portaDX, portaY, TILE_SIZE, TILE_SIZE); }
            }
        } else {
            // Porta normale destra
            if (imgPorta != null) g2.drawImage(imgPorta, portaDX, portaY, TILE_SIZE, TILE_SIZE, null);
            else { g2.setColor(Color.YELLOW); g2.fillRect(portaDX, portaY, TILE_SIZE, TILE_SIZE); }
        }

        // --- SHOP: Porta speciale nella Stanza 4 (in alto) ---
        if (stanzaNelMondo == 4 && mondoAttuale == 1) {
            // Porta Shop (Griglia X=7, Y=0, in alto al centro)
            if (imgShopDoor != null) g2.drawImage(imgShopDoor, 7 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE, null);
            else { g2.setColor(Color.GREEN); g2.fillRect(7 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE); }
        }
        // -----------------------------------------------------

        // 2b. Disegno Oggetti di Cura (da nemici)
        for (Cura c : curePerStanzaMemoria.get(indiceStanzaMemoria)) {
            c.draw(g2);
        }

        // 2c. Disegno Monete
        for (Moneta m : monetePerStanzaMemoria.get(indiceStanzaMemoria)) {
            m.draw(g2);
        }

        // --- SHOP: 2d. Disegno Shopkeepers e ShopItems ---
        for (Shopkeeper sk : shopkeepersPerStanza.get(indiceStanzaMemoria)) {
            sk.draw(g2);
        }
        for (ShopItem si : shopItemsPerStanza.get(indiceStanzaMemoria)) {
            si.draw(g2);
        }
        // -------------------------------------------------

        // 3. Disegno Pugni
        for (Pugno p : pugniAttivi) {
            p.draw(g2);
        }

        // 4. Nemici
        for (Nemico n : nemiciPerStanza.get(indiceStanzaMemoria)) {
            if (n instanceof Boss) n.draw(g2, imgBoss);
            else if (n instanceof NemicoForte) n.draw(g2, imgNemico2);
            else n.draw(g2, imgNemico);
        }

        // 5. Personaggio
        if (!invulnerabile || timerInvulnerabilita % 10 < 5) {
            if (imgPersonaggio != null) g2.drawImage(imgPersonaggio, (int)x, (int)y, PG_SIZE, PG_SIZE, null);
            else { g2.setColor(Color.CYAN); g2.fillOval((int)x, (int)y, PG_SIZE, PG_SIZE); }
        }

        // 6. UI
        // Vite
        for (int i = 0; i < vite; i++) {
            if (imgCuore != null) g2.drawImage(imgCuore, 20 + (i * 35), 45, 30, 30, null);
            else { g2.setColor(Color.RED); g2.fillOval(20 + (i * 35), 45, 25, 25); }
        }

        // Contatore Monete nell'UI
        if (imgMoneta != null) g2.drawImage(imgMoneta, 20, 85, 25, 25, null);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("" + monete, 55, 105);

        // Info avanzato
        g2.setFont(new Font("Consolas", Font.BOLD, 22));
        g2.drawString("MONDO: " + mondoAttuale, 20, 25);
        g2.drawString("STANZA: " + stanzaNelMondo + "/8", 160, 25);
    }

    // Gestione Clik Mouse
    @Override
    public void mouseClicked(MouseEvent e) {
        if (statoGioco == StatoGioco.GAME_OVER) {

            // Conversione coordinate mouse Fullscreen
            double scaleX = (double) getWidth() / LARGHEZZA_GIOCO;
            double scaleY = (double) getHeight() / ALTEZZA_GIOCO;
            double scale = Math.min(scaleX, scaleY);
            int offsetX = (int) ((getWidth() - LARGHEZZA_GIOCO * scale) / 2);
            int offsetY = (int) ((getHeight() - ALTEZZA_GIOCO * scale) / 2);

            int mouseLogicalX = (int) ((e.getX() - offsetX) / scale);
            int mouseLogicalY = (int) ((e.getY() - offsetY) / scale);
            Point pLogico = new Point(mouseLogicalX, mouseLogicalY);

            if (btnRiprova.contains(pLogico)) {
                resetTotalGame();
                statoGioco = StatoGioco.GIOCO;
            } else if (btnEsci.contains(pLogico)) {
                System.exit(0);
            }
        }
    }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        device = ge.getDefaultScreenDevice();

        finestra = new JFrame("WHAT: I'VE PLAYED TOO MUCH");
        finestra.add(new WhatIvePlayedTooMuch());
        finestra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        finestra.setResizable(false);
        finestra.pack();
        finestra.setLocationRelativeTo(null);
        finestra.setVisible(true);
    }
}