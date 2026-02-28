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

    // --- MODALITA: Aggiunto stato SELEZIONE_MODALITA ---
    private enum StatoGioco { MENU, SELEZIONE_PERSONAGGIO, SELEZIONE_MODALITA, GIOCO, VITTORIA_STORIA, GAME_OVER, PAUSA }
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

    // Statistiche dinamiche basate sulla scelta PG e Shop
    private float velocita;
    private final int PG_SIZE = 50;
    private int dannoPugno;
    private int viteMaxGiocatore;
    private int vite;

    // Risorse grafiche
    private BufferedImage imgPersonaggioDefault, imgPorta, imgNemico, imgCuore, imgPugno;
    private BufferedImage imgMuroMondo1, imgPavimentoMondo1;
    private BufferedImage imgMuroMondo2, imgPavimentoMondo2;
    private BufferedImage imgBoss, imgCura, imgMoneta;
    private BufferedImage imgNemico2;
    private BufferedImage imgShopDoor, imgShopkeeper, imgItemSpeed, imgItemDamage;
    private BufferedImage imgBossProjectile;
    private BufferedImage imgPersonaggioVeloce, imgPersonaggioForte, imgPersonaggioTank;
    private BufferedImage imgIconaP0, imgIconaP1, imgIconaP2, imgIconaP3;

    // --- MODALITA: Nuove immagini per i menu di selezione ---
    private BufferedImage imgIconaStoria, imgIconaInfinita;
    // --------------------------------------------------------

    // Selezione Personaggio
    private List<DatiPersonaggio> listaPersonaggi = new ArrayList<>();
    private int indicePersonaggioSelezionato = 0;
    private Rectangle[] rectsSelezionePG = new Rectangle[4];

    // --- MODALITA: Selezione Modalità ---
    private enum Modalita { STORIA, INFINITA }
    private Modalita modalitaScelta = Modalita.STORIA;
    private int indiceModalitaSelezionata = 0; // Per navigazione tastiera
    private Rectangle[] rectsSelezioneModalita = new Rectangle[2];
    private final int MONDI_STORIA_MAX = 4; // Storia finisce dopo 4 mondi
    // -------------------------------------

    private int monete = 0;

    // Liste oggetti attivi nel frame corrente
    private List<Pugno> pugniAttivi = new ArrayList<>();

    private boolean up, down, left, right;
    private boolean shootUp, shootDown, shootLeft, shootRight;
    private int cooldownSparo = 0;
    private final int SPARO_DELAY = 12;

    // Liste di Memoria (una per ogni stanza generata)
    private List<Color> memoriaColoriMondo1 = new ArrayList<>();
    private List<Color> memoriaColoriMondo2 = new ArrayList<>();
    private List<List<Nemico>> nemiciPerStanza = new ArrayList<>();
    private List<List<Cura>> curePerStanzaMemoria = new ArrayList<>();
    private List<List<Moneta>> monetePerStanzaMemoria = new ArrayList<>();
    private List<List<Shopkeeper>> shopkeepersPerStanza = new ArrayList<>();
    private List<List<ShopItem>> shopItemsPerStanza = new ArrayList<>();

    private int indiceStanzaMemoria = 0;
    private Random random = new Random();

    // Variabili di stato gioco
    private int mondoAttuale = 1;
    private int stanzaNelMondo = 1;
    private final int STANZA_BOSS = 8;
    private boolean bossSpawnato = false;
    private boolean bossSconfitto = false;

    // Gestione Tempo Boss
    private final int TEMPO_BOSS_MONDO1 = 120 * 60; // 120 secondi (2 minuti)
    private int tempoRimanenteBoss;

    // Sistema Invulnerabilità
    private boolean invulnerabile = false;
    private int timerInvulnerabilita = 0;

    // Hitbox pulsanti Game Over e Vittoria (coordinate logiche)
    private Rectangle btnRiprova = new Rectangle(LARGHEZZA_GIOCO / 2 - 150, ALTEZZA_GIOCO / 2 + 80, 140, 50);
    private Rectangle btnEsci = new Rectangle(LARGHEZZA_GIOCO / 2 + 10, ALTEZZA_GIOCO / 2 + 80, 140, 50);
    private Rectangle btnMenuPrincipale = new Rectangle(LARGHEZZA_GIOCO / 2 - 100, ALTEZZA_GIOCO / 2 + 150, 200, 50);

    // Fullscreen
    private static JFrame finestra;
    private static boolean isFullscreen = false;
    private static GraphicsDevice device;

    public WhatIvePlayedTooMuch() {
        setPreferredSize(new Dimension(LARGHEZZA_GIOCO, ALTEZZA_GIOCO));
        setFocusable(true);

        // Inizializza la prima stanza del Mondo 1 (vuota)
        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanzaMemoria.add(new ArrayList<>());
        monetePerStanzaMemoria.add(new ArrayList<>());
        shopkeepersPerStanza.add(new ArrayList<>());
        shopItemsPerStanza.add(new ArrayList<>());

        // CARICAMENTO RISORSE
        imgPersonaggioDefault = caricaImmagine("/personaggio.png");
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
        imgShopDoor      = caricaImmagine("/shop_door.png");
        imgShopkeeper    = caricaImmagine("/shopkeeper.png");
        imgItemSpeed     = caricaImmagine("/item_speed.png");
        imgItemDamage    = caricaImmagine("/item_damage.png");
        imgBossProjectile = caricaImmagine("/bullet.png");
        imgPersonaggioVeloce = caricaImmagine("/personaggio_veloce.png");
        imgPersonaggioForte  = caricaImmagine("/personaggio_forte.png");
        imgPersonaggioTank   = caricaImmagine("/personaggio_tank.png");
        imgIconaP0 = caricaImmagine("/icona_p0.png");
        imgIconaP1 = caricaImmagine("/icona_p1.png");
        imgIconaP2 = caricaImmagine("/icona_p2.png");
        imgIconaP3 = caricaImmagine("/icona_p3.png");

        // --- MODALITA: Caricamento nuove icone modalità ---
        imgIconaStoria   = caricaImmagine("/icona_storia.png"); // Crea!
        imgIconaInfinita = caricaImmagine("/icona_infinita.png"); // Crea!
        // --------------------------------------------------

        // Inizializzazione Dati e Hitbox Menu
        inizializzaPersonaggi();
        inizializzaRectsSelezionePG();
        // --- MODALITA: Inizializza hitbox menu modalità ---
        inizializzaRectsSelezioneModalita();
        // --------------------------------------------------

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullscreen();
                    return;
                }

                // Gestione Input in base allo Stato
                if (statoGioco == StatoGioco.MENU) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        statoGioco = StatoGioco.SELEZIONE_PERSONAGGIO;
                    }
                } else if (statoGioco == StatoGioco.SELEZIONE_PERSONAGGIO) {
                    gestisciInputSelezionePG(e.getKeyCode());
                } else if (statoGioco == StatoGioco.SELEZIONE_MODALITA) {
                    // --- MODALITA: Navigazione menu modalità ---
                    gestisciInputSelezioneModalita(e.getKeyCode());
                    // -------------------------------------------
                } else if (statoGioco == StatoGioco.PAUSA) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        statoGioco = StatoGioco.GIOCO;
                    } else if (e.getKeyCode() == KeyEvent.VK_Q) {
                        System.exit(0);
                    }
                } else if (statoGioco == StatoGioco.GIOCO) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        statoPrecedente = StatoGioco.GIOCO;
                        statoGioco = StatoGioco.PAUSA;
                    }
                    toggleMovimento(e.getKeyCode(), true);
                    toggleSparo(e.getKeyCode(), true);
                } else if (statoGioco == StatoGioco.VITTORIA_STORIA || statoGioco == StatoGioco.GAME_OVER) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        // Riprova/Torna al menu
                        storiaFinita(); // Metodo comune per reset
                    }
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

    private void inizializzaPersonaggi() {
        listaPersonaggi.add(new DatiPersonaggio("BELLGERD", 3, 6.0f, 1,
                imgIconaP0 != null ? imgIconaP0 : imgPersonaggioDefault, imgPersonaggioDefault, "Equilibrato."));
        listaPersonaggi.add(new DatiPersonaggio("VLAD", 2, 8.5f, 1,
                imgIconaP1 != null ? imgIconaP1 : imgPersonaggioVeloce, imgPersonaggioVeloce, "Veloce ma fragile."));
        listaPersonaggi.add(new DatiPersonaggio("PAUL", 3, 4.5f, 2,
                imgIconaP2 != null ? imgIconaP2 : imgPersonaggioForte, imgPersonaggioForte, "Lento ma potente."));
        listaPersonaggi.add(new DatiPersonaggio("JUICY", 5, 3.5f, 1,
                imgIconaP3 != null ? imgIconaP3 : imgPersonaggioTank, imgPersonaggioTank, "Lentissimo, molta vita."));
    }

    private void inizializzaRectsSelezionePG() {
        int startX = LARGHEZZA_GIOCO / 2 - 320;
        int startY = ALTEZZA_GIOCO / 2 - 100;
        int rectW = 150;
        int rectH = 200;
        int gap = 10;
        for (int i = 0; i < 4; i++) {
            rectsSelezionePG[i] = new Rectangle(startX + (i * (rectW + gap)), startY, rectW, rectH);
        }
    }

    // --- MODALITA: Inizializzazione hitbox menu modalità ---
    private void inizializzaRectsSelezioneModalita() {
        int startX = LARGHEZZA_GIOCO / 2 - 250; // Due grandi pulsanti
        int startY = ALTEZZA_GIOCO / 2 - 120;
        int rectW = 240;
        int rectH = 240;
        int gap = 20;

        for (int i = 0; i < 2; i++) {
            rectsSelezioneModalita[i] = new Rectangle(startX + (i * (rectW + gap)), startY, rectW, rectH);
        }
    }
    // ---------------------------------------------------------

    private void resetGiocatore() {
        this.x = (LARGHEZZA_GIOCO) / 2;
        this.y = (ALTEZZA_GIOCO) / 2;
        pugniAttivi.clear();
        invulnerabile = false;
        up = down = left = right = false;
        shootUp = shootDown = shootLeft = shootRight = false;
    }

    private void resetTotalGame() {
        // Applica statistiche PG
        DatiPersonaggio scelto = listaPersonaggi.get(indicePersonaggioSelezionato);
        this.velocita = scelto.velocitaBase;
        this.dannoPugno = scelto.dannoBase;
        this.viteMaxGiocatore = scelto.vitaMax;
        this.vite = viteMaxGiocatore;

        // --- MODALITA: Reset parametri modalità ---
        mondoAttuale = 1;
        bossSpawnato = false;
        bossSconfitto = false;
        // Punteggio o numero stanze per infinita rimosso per semplicità
        // -------------------------------------------

        resetGiocatore();
        stanzaNelMondo = 1;
        indiceStanzaMemoria = 0;
        monete = 0;

        nemiciPerStanza.clear();
        curePerStanzaMemoria.clear();
        monetePerStanzaMemoria.clear();
        shopkeepersPerStanza.clear();
        shopItemsPerStanza.clear();
        memoriaColoriMondo1.clear();
        memoriaColoriMondo2.clear();

        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanzaMemoria.add(new ArrayList<>());
        monetePerStanzaMemoria.add(new ArrayList<>());
        shopkeepersPerStanza.add(new ArrayList<>());
        shopItemsPerStanza.add(new ArrayList<>());
    }

    private void generaNuovaStanza() {
        if (mondoAttuale % 2 != 0) memoriaColoriMondo1.add(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
        else memoriaColoriMondo2.add(new Color(100 + random.nextInt(50), random.nextInt(100), 150 + random.nextInt(100)));

        List<Nemico> nuoviNemici = new ArrayList<>();
        List<Cura> nuoveCure = new ArrayList<>();
        List<Moneta> nuoveMonete = new ArrayList<>();
        List<Shopkeeper> nuoviShopkeepers = new ArrayList<>();
        List<ShopItem> nuoveItems = new ArrayList<>();

        if (stanzaNelMondo == STANZA_BOSS && !bossSpawnato) {
            // --- MODALITA: Scalabilità Difficoltà Boss in Infinita ---
            // In Storia, la vita è fissa a 100. In Infinita, cresce con il mondo.
            int vitaBoss = 100;
            if (modalitaScelta == Modalita.INFINITA) {
                // Aumenta di 50 HP ogni 2 mondi
                vitaBoss = 100 + ((mondoAttuale - 1) / 2) * 50;
            }
            // -------------------------------------------------------

            Boss b = new Boss(7, 2, TILE_SIZE, vitaBoss); // Costruttore Boss modificato!
            b.caricaProiettile(imgBossProjectile);
            nuoviNemici.add(b);
            bossSpawnato = true;
            bossSconfitto = false;
            tempoRimanenteBoss = TEMPO_BOSS_MONDO1;
        } else if (stanzaNelMondo == STANZA_BOSS && bossSpawnato && !bossSconfitto) {
            // Ricrea boss se sei tornato indietro (semplificato)
            generaNuovaStanza(); // Ricorsivo per semplicità, attento!
            return;
        } else if (stanzaNelMondo < STANZA_BOSS) {

            if (stanzaNelMondo == 4) {
                nuoviShopkeepers.add(new Shopkeeper(7, 1, TILE_SIZE, imgShopkeeper));
                nuoveItems.add(new ShopItem(5, 2, TILE_SIZE, "CURA", 2, imgCura));
                nuoveItems.add(new ShopItem(7, 2, TILE_SIZE, "VELOCITA", 5, imgItemSpeed));
                nuoveItems.add(new ShopItem(9, 2, TILE_SIZE, "DANNO", 7, imgItemDamage));
            } else {
                // --- MODALITA: Scalabilità Difficoltà Nemici in Infinita ---
                // Numero e vita crescono in Infinita.
                int quantiBase = random.nextInt(2) + (stanzaNelMondo / 2) + (mondoAttuale * 2);
                int vitaNemico = 3;
                if (mondoAttuale % 2 == 0) vitaNemico = 5; // Mondo 2/4/6...

                if (modalitaScelta == Modalita.INFINITA && mondoAttuale > 1) {
                    // Aumenta vita nemici ogni 2 mondi
                    vitaNemico += ((mondoAttuale - 1) / 2) * 2;
                    // Aumenta numero nemici
                    quantiBase += (mondoAttuale - 1);
                }
                // -----------------------------------------------------------

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

                    if (mondoAttuale % 2 != 0) nuoviNemici.add(new Nemico(safeX, safeY, TILE_SIZE, vitaNemico));
                    else {
                        if (random.nextFloat() < 0.60f) nuoviNemici.add(new NemicoForte(safeX, safeY, TILE_SIZE, vitaNemico + 3));
                        else nuoviNemici.add(new Nemico(safeX, safeY, TILE_SIZE, vitaNemico));
                    }
                }
            }
        }

        nemiciPerStanza.add(nuoviNemici);
        curePerStanzaMemoria.add(nuoveCure);
        monetePerStanzaMemoria.add(nuoveMonete);
        shopkeepersPerStanza.add(nuoviShopkeepers);
        shopItemsPerStanza.add(nuoveItems);
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

    private void gestisciInputSelezionePG(int k) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
            indicePersonaggioSelezionato--;
            if (indicePersonaggioSelezionato < 0) indicePersonaggioSelezionato = listaPersonaggi.size() - 1;
        } else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            indicePersonaggioSelezionato++;
            if (indicePersonaggioSelezionato >= listaPersonaggi.size()) indicePersonaggioSelezionato = 0;
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            // --- MODALITA: Dopo PG, vai a selezione Modalità ---
            System.out.println("Personaggio scelto: " + listaPersonaggi.get(indicePersonaggioSelezionato).nome);
            statoGioco = StatoGioco.SELEZIONE_MODALITA;
            // ----------------------------------------------------
        }
    }

    // --- MODALITA: Navigazione Tastiera Menu Modalità ---
    private void gestisciInputSelezioneModalita(int k) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
            // Switch tra 0 e 1
            indiceModalitaSelezionata = (indiceModalitaSelezionata == 0) ? 1 : 0;
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            confermaSelezioneModalita(); // Avvia il gioco
        } else if (k == KeyEvent.VK_ESCAPE) {
            // Torna a selezione PG
            statoGioco = StatoGioco.SELEZIONE_PERSONAGGIO;
        }
    }

    // Metodo per avviare il gioco dopo la scelta modalità
    private void confermaSelezioneModalita() {
        modalitaScelta = (indiceModalitaSelezionata == 0) ? Modalita.STORIA : Modalita.INFINITA;
        System.out.println("Modalità confermata: " + modalitaScelta);
        resetTotalGame(); // Inizializza con statistiche PG e modalità
        statoGioco = StatoGioco.GIOCO; // Inizia!
    }
    // ----------------------------------------------------

    private void sparaPugno() {
        if (cooldownSparo > 0) return;
        int dirX = 0, dirY = 0;
        if (shootUp) dirY = -1;
        if (shootDown) dirY = 1;
        if (shootLeft) dirX = -1;
        if (shootRight) dirX = 1;

        if (dirX != 0 || dirY != 0) {
            pugniAttivi.add(new Pugno(x, y, dirX, dirY, imgPugno, dannoPugno));
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

        // Gestione Tempo Boss
        if (stanzaNelMondo == STANZA_BOSS && bossSpawnato && !bossSconfitto) {
            tempoRimanenteBoss--;
            if (tempoRimanenteBoss <= 0) {
                System.out.println("TEMPO SCADUTO! Game Over");
                statoGioco = StatoGioco.GAME_OVER;
            }
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

                if (stanzaNelMondo == STANZA_BOSS &&bossSpawnato) {
                    if (bossSconfitto) {

                        // --- MODALITA: Controllo Vittoria o Passaggio Mondo ---
                        if (modalitaScelta == Modalita.STORIA && mondoAttuale == MONDI_STORIA_MAX) {
                            // STORIA FINITA! Vittoria!
                            System.out.println("STORIA COMPLETATA! Vittoria!");
                            statoGioco = StatoGioco.VITTORIA_STORIA;
                        } else {
                            // Passaggio al Mondo Successivo (Sia Storia <4 che Infinita)
                            mondoAttuale++;
                            System.out.println("Passaggio al MONDO: " + mondoAttuale);

                            // Reset per nuovo mondo (semplificato, mantiene monete/shop upgrades)
                            indiceStanzaMemoria = 0;
                            stanzaNelMondo = 1;
                            bossSpawnato = false;
                            bossSconfitto = false;

                            nemiciPerStanza.clear();
                            curePerStanzaMemoria.clear();
                            monetePerStanzaMemoria.clear();
                            shopkeepersPerStanza.clear();
                            shopItemsPerStanza.clear();
                            memoriaColoriMondo1.clear();
                            memoriaColoriMondo2.clear();

                            // Inizializza Stanza 1 del nuovo mondo
                            memoriaColoriMondo1.add(new Color(60, 60, 80));
                            nemiciPerStanza.add(new ArrayList<>());
                            curePerStanzaMemoria.add(new ArrayList<>());
                            monetePerStanzaMemoria.add(new ArrayList<>());
                            shopkeepersPerStanza.add(new ArrayList<>());
                            shopItemsPerStanza.add(new ArrayList<>());

                            resetGiocatore(); // Centra PG
                        }
                        // --------------------------------------------------------

                    } else x = maxX - 10;
                } else {
                    x = OFFSET * TILE_SIZE + 20;
                    indiceStanzaMemoria++;
                    stanzaNelMondo++;
                    if (indiceStanzaMemoria >= nemiciPerStanza.size()) generaNuovaStanza();
                }
            }
        }

        // Logica Rilevamento Shopkeeper (battuta)
        List<Shopkeeper> shopkeepersCorrenti = shopkeepersPerStanza.get(indiceStanzaMemoria);
        Rectangle hbGiocatore = new Rectangle((int)x, (int)y, PG_SIZE, PG_SIZE);
        for (Shopkeeper sk : shopkeepersCorrenti) {
            if (hbGiocatore.intersects(sk.getHitbox())) {
                sk.attivaBattuta();
            }
        }

        // Logica Acquisto Items
        List<ShopItem> itemsCorrenti = shopItemsPerStanza.get(indiceStanzaMemoria);
        for (ShopItem si : itemsCorrenti) {
            if (si.controllaAcquisto(x, y, PG_SIZE, monete)) {
                monete -= si.getCosto();
                si.setAcquistato();

                if (si.getTipo().equals("CURA")) {
                    vite++;
                    if (vite > viteMaxGiocatore) vite = viteMaxGiocatore; // Rispetta il cap del PG
                } else if (si.getTipo().equals("VELOCITA")) {
                    velocita += 1.5f;
                } else if (si.getTipo().equals("DANNO")) {
                    dannoPugno++;
                }
            }
        }

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

        // Update e collisioni nemici
        List<Nemico> nemiciCorrenti = nemiciPerStanza.get(indiceStanzaMemoria);
        for (Nemico n : nemiciCorrenti) {
            n.update(x, y);

            if (!invulnerabile && n.toccaGiocatore(x, y, PG_SIZE)) {
                riceviDanno();
            }

            if (n instanceof Boss) {
                Boss b = (Boss)n;
                if (!invulnerabile && b.controllaCollisioneProiettili(x, y, PG_SIZE)) {
                    riceviDanno();
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
                        if (n instanceof Boss) bossSconfitto = true;

                        // Drop garantito se stanza pulita (non shop)
                        float mx = n.x;
                        float my = n.y;
                        nemiciCorrenti.remove(j);
                        j--;

                        if (nemiciCorrenti.isEmpty() && stanzaNelMondo != 4) {
                            if (vite < viteMaxGiocatore) {
                                curePerStanzaMemoria.get(indiceStanzaMemoria).add(new Cura((int)mx/TILE_SIZE, (int)my/TILE_SIZE, TILE_SIZE, imgCura));
                            } else {
                                monetePerStanzaMemoria.get(indiceStanzaMemoria).add(new Moneta((int)mx/TILE_SIZE, (int)my/TILE_SIZE, TILE_SIZE, imgMoneta));
                            }
                        }
                    }
                    break;
                }
            }
        }

        // Raccolta oggetti
        List<Cura> cureCorrenti = curePerStanzaMemoria.get(indiceStanzaMemoria);
        for (int i = 0; i < cureCorrenti.size(); i++) {
            if (cureCorrenti.get(i).controllaRaccolta(x, y, PG_SIZE)) {
                vite++; if (vite > viteMaxGiocatore) vite = viteMaxGiocatore; // Rispetta cap PG
                cureCorrenti.remove(i); i--;
            }
        }
        List<Moneta> moneteCorrenti = monetePerStanzaMemoria.get(indiceStanzaMemoria);
        for (int i = 0; i < moneteCorrenti.size(); i++) {
            if (moneteCorrenti.get(i).controllaRaccolta(x, y, PG_SIZE)) {
                monete += moneteCorrenti.get(i).getValore();
                moneteCorrenti.remove(i); i--;
            }
        }

        repaint();
    }

    private void riceviDanno() {
        vite--;
        invulnerabile = true;
        if (vite <= 0) {
            System.out.println("GAME OVER!");
            statoGioco = StatoGioco.GAME_OVER;
        }
    }

    // Metodo comune per reset dopo vittoria/sconfitta
    private void storiaFinita() {
        indicePersonaggioSelezionato = 0; // Resetta scelta
        indiceModalitaSelezionata = 0;
        resetGiocatore();
        nemiciPerStanza.clear(); // Svuota tutto
        // ... (Reset completo invariato) ...
        memoriaColoriMondo1.clear();
        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanzaMemoria.clear();
        monetePerStanzaMemoria.clear();
        shopkeepersPerStanza.clear();
        shopItemsPerStanza.clear();
        memoriaColoriMondo2.clear();

        statoGioco = StatoGioco.MENU; // Torna all'inizio
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

        // Gestione disegni per Stato
        if (statoGioco == StatoGioco.MENU) disegnaMenu(g2);
        else if (statoGioco == StatoGioco.SELEZIONE_PERSONAGGIO) disegnaSelezionePersonaggio(g2);
        else if (statoGioco == StatoGioco.SELEZIONE_MODALITA) disegnaSelezioneModalita(g2); // --- NUOVO ---
        else if (statoGioco == StatoGioco.GIOCO) disegnaGioco(g2);
        else if (statoGioco == StatoGioco.VITTORIA_STORIA) disegnaVittoriaStoria(g2); // --- NUOVO ---
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
        g2.drawString("Premi [INVIO] o [Click] per iniziare", LARGHEZZA_GIOCO/2 - 160, ALTEZZA_GIOCO/2 + 20);
        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.drawString("Premi [F11] per Fullscreen", LARGHEZZA_GIOCO/2 - 100, ALTEZZA_GIOCO/2 + 50);
    }

    private void disegnaSelezionePersonaggio(Graphics2D g2) {
        g2.setColor(new Color(30, 30, 50));
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 36));
        g2.drawString("SCEGLI IL TUO MURATORE", LARGHEZZA_GIOCO / 2 - 220, 80);
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.drawString("[Frecce/Mouse] per navigare, [INVIO/Click] per confermare", LARGHEZZA_GIOCO / 2 - 200, 110);
        for (int i = 0; i < listaPersonaggi.size(); i++) {
            DatiPersonaggio pg = listaPersonaggi.get(i);
            Rectangle rect = rectsSelezionePG[i];
            if (i == indicePersonaggioSelezionato) {
                g2.setColor(new Color(255, 215, 0, 100));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
                g2.setColor(Color.yellow);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            } else {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
                g2.setColor(Color.GRAY);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
            int imgSize = 80;
            if (pg.imgIcona != null) g2.drawImage(pg.imgIcona, rect.x + (rect.width/2 - imgSize/2), rect.y + 20, imgSize, imgSize, null);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.drawString(pg.nome, rect.x + 10, rect.y + imgSize + 50);
            g2.setFont(new Font("Arial", Font.ITALIC, 14));
            g2.drawString(pg.descrizione, rect.x + 10, rect.y + imgSize + 75);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            if (imgCuore != null) g2.drawImage(imgCuore, rect.x + 10, rect.y + 160, 20, 20, null);
            g2.setColor(Color.RED);
            g2.drawString("" + pg.vitaMax, rect.x + 35, rect.y + 175);
            g2.setColor(Color.CYAN);
            g2.drawString("VEL: " + pg.velocitaBase, rect.x + 60, rect.y + 175);
            g2.setColor(Color.WHITE);
            g2.drawString("DMG: " + pg.dannoBase, rect.x + 60, rect.y + 195);
        }
    }

    // --- MODALITA: Schermata Menu Selezione Modalità ---
    private void disegnaSelezioneModalita(Graphics2D g2) {
        g2.setColor(new Color(40, 20, 50));
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 40));
        g2.drawString("SELEZIONA LA SFIDA", LARGHEZZA_GIOCO / 2 - 180, 80);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString("[Frecce/Mouse] per navigare, [INVIO/Click] per confermare, [ESC] per PG", LARGHEZZA_GIOCO / 2 - 270, 115);

        for (int i = 0; i < 2; i++) {
            Rectangle rect = rectsSelezioneModalita[i];

            // Colore sfondo rect (evidenziato se selezionato)
            if (i == indiceModalitaSelezionata) {
                g2.setColor(new Color(173, 216, 230, 100)); // Light Blue trasparente
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
                g2.setColor(Color.CYAN);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            } else {
                g2.setColor(new Color(255, 255, 255, 20)); // Bianco molto trasparente
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
                g2.setColor(Color.GRAY);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            }

            // Icona Modalità
            int imgSize = 120;
            if (i == 0) { // STORIA
                if (imgIconaStoria != null) {
                    g2.drawImage(imgIconaStoria, rect.x + (rect.width/2 - imgSize/2), rect.y + 20, imgSize, imgSize, null);
                } else { // Backup testuale
                    g2.setColor(Color.WHITE); g2.setFont(new Font("Consolas", Font.BOLD, 100));
                    g2.drawString("📖", rect.x + (rect.width/2 - 50), rect.y + 110);
                }
                g2.setColor(Color.WHITE); g2.setFont(new Font("Consolas", Font.BOLD, 28));
                g2.drawString("STORIA CLASSICA", rect.x + 10, rect.y + 170);
                g2.setFont(new Font("Arial", Font.ITALIC, 16));
                g2.drawString("Sconfiggi 4 Boss per vincere!", rect.x + 10, rect.y + 200);
            } else { // INFINITA
                if (imgIconaInfinita != null) {
                    g2.drawImage(imgIconaInfinita, rect.x + (rect.width/2 - imgSize/2), rect.y + 20, imgSize, imgSize, null);
                } else { // Backup testuale
                    g2.setColor(Color.WHITE); g2.setFont(new Font("Consolas", Font.BOLD, 100));
                    g2.drawString("∞", rect.x + (rect.width/2 - 50), rect.y + 110);
                }
                g2.setColor(Color.WHITE); g2.setFont(new Font("Consolas", Font.BOLD, 28));
                g2.drawString("MODALITA INFINITA", rect.x + 10, rect.y + 170);
                g2.setFont(new Font("Arial", Font.ITALIC, 16));
                g2.drawString("Sopravvivi a mondi infiniti!", rect.x + 10, rect.y + 200);
                g2.drawString("Nemici sempre più forti.", rect.x + 10, rect.y + 220);
            }
        }
    }
    // ----------------------------------------------------

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

    // --- MODALITA: Schermata Vittoria Storia ---
    private void disegnaVittoriaStoria(Graphics2D g2) {
        g2.setColor(new Color(20, 100, 20)); // Sfondo verde vittoria
        g2.fillRect(0, 0, LARGHEZZA_GIOCO, ALTEZZA_GIOCO);

        g2.setColor(Color.yellow);
        g2.setFont(new Font("Consolas", Font.BOLD, 60));
        g2.drawString("🎊  STORIA COMPLETATA!  🎊", LARGHEZZA_GIOCO / 2 - 380, ALTEZZA_GIOCO / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 26));
        g2.drawString("Il muratore ha superato tutti i 4 mondi e i loro Boss!", LARGHEZZA_GIOCO / 2 - 300, ALTEZZA_GIOCO / 2 + 20);
        g2.drawString("Monete raccolte: " + monete, LARGHEZZA_GIOCO / 2 - 120, ALTEZZA_GIOCO / 2 + 60);

        // Pulsante Torna al Menu
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(btnMenuPrincipale.x, btnMenuPrincipale.y, btnMenuPrincipale.width, btnMenuPrincipale.height);
        g2.setColor(Color.WHITE);
        g2.drawRect(btnMenuPrincipale.x, btnMenuPrincipale.y, btnMenuPrincipale.width, btnMenuPrincipale.height);
        g2.drawString("MENU PRINCIPALE", btnMenuPrincipale.x + 10, btnMenuPrincipale.y + 32);
    }
    // ------------------------------------------

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

        // --- MODALITA: UI Specifica per Infinita (punteggio, stanze superate) ---
        if (modalitaScelta == Modalita.INFINITA) {
            int stanzeTotaliSuperate = (mondoAttuale - 1) * STANZA_BOSS + stanzaNelMondo;
            g2.drawString("Stanze totali superate: " + stanzeTotaliSuperate, LARGHEZZA_GIOCO / 2 - 150, ALTEZZA_GIOCO / 2 + 50);
        }
        // ------------------------------------------------------------------------

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
        // Disegno Pavimento e Muri (Semplificato backup)
        // Usa Muro1/Pavimento1 per mondi dispari, Muro2/Pavimento2 per pari (o infinita >1)
        BufferedImage imgMuroCorr      = (mondoAttuale % 2 != 0) ? imgMuroMondo1 : imgMuroMondo2;
        BufferedImage imgPavimentoCorr = (mondoAttuale % 2 != 0) ? imgPavimentoMondo1 : imgPavimentoMondo2;

        for (int i = 0; i < COL_TOTALI; i++) {
            for (int j = 0; j < RIG_TOTALI; j++) {
                int px = i * TILE_SIZE;
                int py = j * TILE_SIZE;
                if (i < OFFSET || i >= COL_GIOCO + OFFSET || j < OFFSET || j >= RIG_GIOCO + OFFSET) {
                    if (imgMuroCorr != null) g2.drawImage(imgMuroCorr, px, py, TILE_SIZE, TILE_SIZE, null);
                    else { g2.setColor(Color.BLACK); g2.fillRect(px, py, TILE_SIZE, TILE_SIZE); }
                } else {
                    if (imgPavimentoCorr != null) g2.drawImage(imgPavimentoCorr, px, py, TILE_SIZE, TILE_SIZE, null);
                    else { g2.setColor(Color.GRAY); g2.fillRect(px, py, TILE_SIZE, TILE_SIZE); }
                }
            }
        }

        // Porte
        int portaY = (RIG_TOTALI / 2) * TILE_SIZE;
        if (indiceStanzaMemoria > 0) {
            if (imgPorta != null) g2.drawImage(imgPorta, 0, portaY, TILE_SIZE, TILE_SIZE, null);
        }
        int portaDX = (COL_TOTALI - 1) * TILE_SIZE;

        if (stanzaNelMondo == STANZA_BOSS &&bossSpawnato && bossSconfitto) {
            // Porta finale cyan per passare mondo
            g2.setColor(new Color(0, 255, 255, 150));
            g2.fillRect(portaDX, portaY, TILE_SIZE, TILE_SIZE);
        } else {
            if (imgPorta != null) g2.drawImage(imgPorta, portaDX, portaY, TILE_SIZE, TILE_SIZE, null);
        }

        // Porta Speciale Shop (Stanza 4)
        if (stanzaNelMondo == 4 && mondoAttuale % 2 != 0) { // Shop solo mondi dispari per semplicità
            if (imgShopDoor != null) g2.drawImage(imgShopDoor, 7 * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE, null);
        }

        // Oggetti
        for (Cura c : curePerStanzaMemoria.get(indiceStanzaMemoria)) c.draw(g2);
        for (Moneta m : monetePerStanzaMemoria.get(indiceStanzaMemoria)) m.draw(g2);
        for (Shopkeeper sk : shopkeepersPerStanza.get(indiceStanzaMemoria)) sk.draw(g2);
        for (ShopItem si : shopItemsPerStanza.get(indiceStanzaMemoria)) si.draw(g2);

        // Pugni
        for (Pugno p : pugniAttivi) p.draw(g2);

        // Nemici e Boss
        Boss bossCorrente = null;
        for (Nemico n : nemiciPerStanza.get(indiceStanzaMemoria)) {
            if (n instanceof Boss) {
                bossCorrente = (Boss) n;
                n.draw(g2, imgBoss);
            } else if (n instanceof NemicoForte) n.draw(g2, imgNemico2);
            else n.draw(g2, imgNemico);
        }

        // Personaggio
        if (!invulnerabile || timerInvulnerabilita % 10 < 5) {
            BufferedImage imgGiocoPG = listaPersonaggi.get(indicePersonaggioSelezionato).imgGioco;
            if (imgGiocoPG != null) {
                g2.drawImage(imgGiocoPG, (int)x, (int)y, PG_SIZE, PG_SIZE, null);
            } else {
                g2.setColor(Color.CYAN); g2.fillOval((int)x, (int)y, PG_SIZE, PG_SIZE);
            }
        }

        // UI Standard
        for (int i = 0; i < vite; i++) {
            if (imgCuore != null) g2.drawImage(imgCuore, 20 + (i * 35), 45, 30, 30, null);
        }
        if (imgMoneta != null) g2.drawImage(imgMoneta, 20, 85, 25, 25, null);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("" + monete, 55, 105);
        g2.setFont(new Font("Consolas", Font.BOLD, 22));

        // --- MODALITA: UI Mondo/Stanza specifica per modalità ---
        if (modalitaScelta == Modalita.STORIA) {
            g2.drawString("STORIA: " + mondoAttuale + "/" + MONDI_STORIA_MAX, 20, 25);
        } else {
            g2.drawString("INFINITA - M: " + mondoAttuale, 20, 25);
        }
        g2.drawString("STANZA: " + stanzaNelMondo + "/8", 220, 25);
        // ---------------------------------------------------------

        // UI del Boss (Nome e Barra Vita)
        if (stanzaNelMondo == STANZA_BOSS && bossSpawnato && !bossSconfitto && bossCorrente != null) {
            int uiX = (COL_TOTALI * TILE_SIZE) / 2 - 150;
            int uiY = (RIG_TOTALI * TILE_SIZE) - 60;
            int barW = 300;
            int barH = 20;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(uiX, uiY, barW, barH);
            g2.setColor(Color.RED);
            int larghezzaVita = (int) (((float) bossCorrente.getVita() / bossCorrente.getVitaMax()) * barW);
            g2.fillRect(uiX, uiY, larghezzaVita, barH);
            g2.setColor(Color.BLACK);
            g2.drawRect(uiX, uiY, barW, barH);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.drawString("CAPOCANTIERE CORRUTTO", uiX + 40, uiY - 5);
            g2.setFont(new Font("Consolas", Font.PLAIN, 14));
            g2.drawString(bossCorrente.getVita() + "/" + bossCorrente.getVitaMax() + " HP", uiX + 110, uiY + 15);

            // UI Tempo Limite
            g2.setFont(new Font("Consolas", Font.BOLD, 20));
            g2.setColor(tempoRimanenteBoss < 600 ? Color.RED : Color.WHITE);
            g2.drawString("TEMPO LIMITE: " + (tempoRimanenteBoss / 60) + "s", LARGHEZZA_GIOCO - 200, ALTEZZA_GIOCO - 30);
        }
    }

    @Override public void mouseClicked(MouseEvent e) {

        // Conversione coordinate mouse Fullscreen
        double scaleX = (double) getWidth() / LARGHEZZA_GIOCO;
        double scaleY = (double) getHeight() / ALTEZZA_GIOCO;
        double scale = Math.min(scaleX, scaleY);
        int offsetX = (int) ((getWidth() - LARGHEZZA_GIOCO * scale) / 2);
        int offsetY = (int) ((getHeight() - ALTEZZA_GIOCO * scale) / 2);
        int mouseLogicalX = (int) ((e.getX() - offsetX) / scale);
        int mouseLogicalY = (int) ((e.getY() - offsetY) / scale);
        Point pLogico = new Point(mouseLogicalX, mouseLogicalY);

        // Gestione Click in base allo Stato
        if (statoGioco == StatoGioco.MENU) {
            if (new Rectangle(LARGHEZZA_GIOCO/2 - 160, ALTEZZA_GIOCO/2, 320, 50).contains(pLogico)) {
                statoGioco = StatoGioco.SELEZIONE_PERSONAGGIO;
            }
        } else if (statoGioco == StatoGioco.SELEZIONE_PERSONAGGIO) {
            for (int i = 0; i < 4; i++) {
                if (rectsSelezionePG[i].contains(pLogico)) {
                    indicePersonaggioSelezionato = i;
                    // Vai a scelta modalità
                    statoGioco = StatoGioco.SELEZIONE_MODALITA;
                    break;
                }
            }
        } else if (statoGioco == StatoGioco.SELEZIONE_MODALITA) {
            // --- MODALITA: Click Mouse Menu Modalità ---
            for (int i = 0; i < 2; i++) {
                if (rectsSelezioneModalita[i].contains(pLogico)) {
                    indiceModalitaSelezionata = i;
                    confermaSelezioneModalita(); // Avvia
                    break;
                }
            }
            // -------------------------------------------
        } else if (statoGioco == StatoGioco.GAME_OVER) {
            if (btnRiprova.contains(pLogico)) {
                resetTotalGame(); // Usa statistiche attuali
                statoGioco = StatoGioco.GIOCO;
            } else if (btnEsci.contains(pLogico)) System.exit(0);
        } else if (statoGioco == StatoGioco.VITTORIA_STORIA) {
            // --- MODALITA: Click su pulsante Vittoria ---
            if (btnMenuPrincipale.contains(pLogico)) {
                storiaFinita(); // Resetta e torna a menu
            }
            // --------------------------------------------
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
