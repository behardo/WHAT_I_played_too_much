import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * UIManager.java
 * Tutti i MenuButton di ogni schermata con posizioni calcolate
 * per lo schermo logico 1088x448px.
 */
public class UIManager {

    public final List<DatiPersonaggio> listaPersonaggi       = new ArrayList<>();
    public final Rectangle[]           rectsSelezionePG      = new Rectangle[5];
    public final Rectangle[]           rectsSelezioneModalita = new Rectangle[2];

    // ── Bottoni ───────────────────────────────────────────────────────────────
    public final MenuButton btnGioca, btnImpostazioni, btnControlli, btnEsciMenu;
    public final MenuButton btnRiprendi, btnImpostazioniPausa, btnMenuPrincipalePausa, btnEsciPausa;
    public final MenuButton btnRiprova, btnMenuPrincipaleGO, btnEsciGO;
    public final MenuButton btnMenuPrincipaleVittoria;
    public final MenuButton btnMusMeno, btnMusPiu, btnEffMeno, btnEffPiu;
    public final MenuButton btnChiudiImpostazioni;
    public final MenuButton btnChiudiControlli;

    // Parametri layout impostazioni usati da RenderEngine per allineare testo/slider
    public int _impLabelX, _impCtrlX, _impSw, _impStartY, _impRigaH, _impSh;

    private final MenuButton[][] tuttiBottoni;

    // ─────────────────────────────────────────────────────────────────────────
    public UIManager(ResourceLoader res) {
        inizializzaPersonaggi(res);
        inizializzaRectsSelezionePG();
        inizializzaRectsSelezioneModalita();

        final int W   = GameState.LARGHEZZA_GIOCO;   // 1088
        final int H   = GameState.ALTEZZA_GIOCO;     // 448
        final int BW  = 250;
        final int BH  = 44;
        final int GAP = 10;
        final int CX  = W / 2 - BW / 2;

        // ── Palette colori ────────────────────────────────────────────────────
        Color nDark  = new Color(30,  30,  55);
        Color hBlu   = new Color(60,  80, 160);
        Color bBlu   = new Color(100, 110, 180);
        Color bBluH  = new Color(160, 180, 255);
        Color nRosso = new Color(55,  20,  20);
        Color hRosso = new Color(140, 40,  40);
        Color bRosso = new Color(160, 80,  80);
        Color bRosH  = new Color(220, 100, 100);
        Color nVerde = new Color(20,  55,  20);
        Color hVerde = new Color(40,  130, 40);
        Color bVerde = new Color(80,  160, 80);
        Color bVerH  = new Color(120, 220, 120);

        // ── Menu Principale ───────────────────────────────────────────────────
        {
            int s = 170;
            btnGioca        = b("GIOCA",        CX, s,             BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioni = b("IMPOSTAZIONI", CX, s+(BH+GAP),   BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnControlli    = b("CONTROLLI",    CX, s+(BH+GAP)*2, BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciMenu     = b("ESCI",         CX, s+(BH+GAP)*3, BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Pausa ─────────────────────────────────────────────────────────────
        {
            int s = 154;
            btnRiprendi            = b("RIPRENDI",     CX, s,             BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioniPausa   = b("IMPOSTAZIONI", CX, s+(BH+GAP),   BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnMenuPrincipalePausa = b("MENU",         CX, s+(BH+GAP)*2, BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciPausa           = b("ESCI",         CX, s+(BH+GAP)*3, BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Game Over ─────────────────────────────────────────────────────────
        {
            int bw3 = 185, g3 = 12;
            int x0  = W/2 - (bw3*3 + g3*2)/2;
            int y0  = 265;
            btnRiprova          = b("RIPROVA", x0,            y0, bw3, BH, nVerde, hVerde, bVerde, bVerH);
            btnMenuPrincipaleGO = b("MENU",    x0+bw3+g3,     y0, bw3, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciGO           = b("ESCI",    x0+(bw3+g3)*2, y0, bw3, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Vittoria ──────────────────────────────────────────────────────────
        btnMenuPrincipaleVittoria = b("MENU PRINCIPALE",
                W/2 - 150, H*3/4 - BH/2, 300, BH, nDark, hBlu, bBlu, bBluH);

        // ── Impostazioni ──────────────────────────────────────────────────────
        {
            final int SH  = 38;
            final int RH  = 55;
            final int SW  = 48;
            final int LX  = W/2 - 220;
            final int CX2 = W/2 + 5;
            int s = 145;

            btnMusMeno            = b("-",        CX2,         s,         SW,  SH, nRosso, hRosso, bRosso, bRosH);
            btnMusPiu             = b("+",        CX2+SW+155,  s,         SW,  SH, nVerde, hVerde, bVerde, bVerH);
            btnEffMeno            = b("-",        CX2,         s+RH,      SW,  SH, nRosso, hRosso, bRosso, bRosH);
            btnEffPiu             = b("+",        CX2+SW+155,  s+RH,      SW,  SH, nVerde, hVerde, bVerde, bVerH);
            btnChiudiImpostazioni = b("INDIETRO", W/2-100,     s+RH*2+10, 200, BH, nDark,  hBlu,   bBlu,   bBluH);

            _impLabelX = LX;   _impCtrlX = CX2;  _impSw = SW;
            _impStartY = s;    _impRigaH = RH;   _impSh = SH;
        }

        // ── Controlli ─────────────────────────────────────────────────────────
        btnChiudiControlli = b("INDIETRO", W/2-100, H-75, 200, BH, nDark, hBlu, bBlu, bBluH);

        tuttiBottoni = new MenuButton[][] {
                { btnGioca, btnImpostazioni, btnControlli, btnEsciMenu },
                { btnRiprendi, btnImpostazioniPausa, btnMenuPrincipalePausa, btnEsciPausa },
                { btnRiprova, btnMenuPrincipaleGO, btnEsciGO },
                { btnMenuPrincipaleVittoria },
                { btnMusMeno, btnMusPiu, btnEffMeno, btnEffPiu, btnChiudiImpostazioni },
                { btnChiudiControlli }
        };
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    private MenuButton b(String label, int x, int y, int w, int h,
                         Color n, Color hov, Color bn, Color bh) {
        return new MenuButton(label, x, y, w, h).setColori(n, hov, bn, bh);
    }

    // ── Hover update ─────────────────────────────────────────────────────────
    public void aggiornaMouse(int mx, int my) {
        for (MenuButton[] g : tuttiBottoni)
            for (MenuButton btn : g)
                btn.aggiornaMouse(mx, my);
    }

    // ── Inizializzazione ──────────────────────────────────────────────────────

    private void inizializzaPersonaggi(ResourceLoader res) {
        // BELLGERD — factotum equilibrato, valigia come arma
        listaPersonaggi.add(new DatiPersonaggio("BELLGERD", 6, 6.5f, 3,
                res.getIconaPerIndice(0), res.imgPersonaggioDefault,
                "Tuttofare. Niente di speciale.",
                res.getBulletPerPG(0)));

        // VLAD — velocissimo, quasi di vetro, chiave inglese letale
        listaPersonaggi.add(new DatiPersonaggio("VLAD", 3, 11.5f, 2,
                res.getIconaPerIndice(1), res.imgPersonaggioVeloce,
                "Veloce. Molto veloce. Troppo.",
                res.getBulletPerPG(1)));

        // PAUL — lentissimo ma distrugge tutto, accetta
        listaPersonaggi.add(new DatiPersonaggio("PAUL", 5, 3.5f, 6,
                res.getIconaPerIndice(2), res.imgPersonaggioForte,
                "Lento. Ogni colpo vale tre.",
                res.getBulletPerPG(2)));

        // JUICY — tank puro, gamepad come arma contundente
        listaPersonaggi.add(new DatiPersonaggio("JUICY", 12, 3.0f, 3,
                res.getIconaPerIndice(3), res.imgPersonaggioTank,
                "Non si ferma. Mai.",
                res.getBulletPerPG(3)));

        // D.I.T.T.O. — segreto, tutto al massimo
        listaPersonaggi.add(new DatiPersonaggio(
                "D.I.T.T.O.", 25, 14.0f, 30,
                res.getIconaPerIndice(4),
                res.imgPersonaggioGod != null ? res.imgPersonaggioGod : res.imgPersonaggioDefault,
                "???",
                res.getBulletPerPG(4)));
    }

    private void inizializzaRectsSelezionePG() {
        // 5 card da 128x200px, gap 10 → tot 680px, startX = (1088-680)/2 = 204
        int rectW = 128, rectH = 200, gap = 10;
        int startX = GameState.LARGHEZZA_GIOCO / 2 - (rectW * 5 + gap * 4) / 2;
        for (int i = 0; i < 5; i++)
            rectsSelezionePG[i] = new Rectangle(startX + i * (rectW + gap), 100, rectW, rectH);
    }

    private void inizializzaRectsSelezioneModalita() {
        // 2 card da 260x240px, gap 20 → tot 540px, startX = (1088-540)/2 = 274
        int rectW = 260, rectH = 240, gap = 20;
        int startX = GameState.LARGHEZZA_GIOCO / 2 - (rectW * 2 + gap) / 2;
        for (int i = 0; i < 2; i++)
            rectsSelezioneModalita[i] = new Rectangle(startX + i * (rectW + gap), 124, rectW, rectH);
    }

    // ── Accesso ───────────────────────────────────────────────────────────────
    public DatiPersonaggio getPersonaggioSelezionato(int i) { return listaPersonaggi.get(i); }
    public int             getNumPersonaggi()               { return listaPersonaggi.size(); }
    /**
     * Ricalcola posizioni di tutti i bottoni e rect in base alle dimensioni
     * reali del pannello. Chiamato ogni volta che la finestra cambia dimensione.
     */
    public void ricalcolaBottoni(int W, int H) {
        final int BW  = (int)(W * 0.23);   // ~25% della larghezza
        final int BH  = (int)(H * 0.098);  // ~10% dell'altezza
        final int GAP = (int)(H * 0.022);
        final int CX  = W / 2 - BW / 2;

        // ── Menu principale ───────────────────────────────────────────────────
        int s1 = (int)(H * 0.38);
        btnGioca.setBounds(CX, s1,             BW, BH);
        btnImpostazioni.setBounds(CX, s1 + (BH+GAP),   BW, BH);
        btnControlli.setBounds(CX, s1 + (BH+GAP)*2, BW, BH);
        btnEsciMenu.setBounds(CX, s1 + (BH+GAP)*3, BW, BH);

        // ── Pausa ─────────────────────────────────────────────────────────────
        int s2 = (int)(H * 0.34);
        btnRiprendi.setBounds(CX, s2,             BW, BH);
        btnImpostazioniPausa.setBounds(CX, s2 + (BH+GAP),   BW, BH);
        btnMenuPrincipalePausa.setBounds(CX, s2 + (BH+GAP)*2, BW, BH);
        btnEsciPausa.setBounds(CX, s2 + (BH+GAP)*3, BW, BH);

        // ── Game Over ─────────────────────────────────────────────────────────
        int bw3 = (int)(W * 0.17), g3 = (int)(W * 0.011);
        int x0 = W/2 - (bw3*3 + g3*2)/2;
        int y0 = (int)(H * 0.59);
        btnRiprova.setBounds(x0,            y0, bw3, BH);
        btnMenuPrincipaleGO.setBounds(x0+bw3+g3,     y0, bw3, BH);
        btnEsciGO.setBounds(x0+(bw3+g3)*2, y0, bw3, BH);

        // ── Vittoria ──────────────────────────────────────────────────────────
        int bvW = (int)(W * 0.27);
        btnMenuPrincipaleVittoria.setBounds(W/2 - bvW/2, (int)(H * 0.75) - BH/2, bvW, BH);

        // ── Impostazioni ──────────────────────────────────────────────────────
        final int SH  = (int)(H * 0.085);
        final int RH  = (int)(H * 0.123);
        final int SW  = (int)(W * 0.044);
        final int LX  = W/2 - (int)(W * 0.202);
        final int CX2 = W/2 + (int)(W * 0.005);
        int si = (int)(H * 0.324);
        btnMusMeno.setBounds(CX2,       si,         SW, SH);
        btnMusPiu.setBounds(CX2+SW+(int)(W*0.142), si, SW, SH);
        btnEffMeno.setBounds(CX2,       si+RH,      SW, SH);
        btnEffPiu.setBounds(CX2+SW+(int)(W*0.142), si+RH, SW, SH);
        btnChiudiImpostazioni.setBounds(W/2-(int)(W*0.092), si+RH*2+10, (int)(W*0.185), BH);
        _impLabelX = LX; _impCtrlX = CX2; _impSw = SW;
        _impStartY = si; _impRigaH = RH;  _impSh = SH;

        // ── Controlli ─────────────────────────────────────────────────────────
        btnChiudiControlli.setBounds(W/2-(int)(W*0.092), (int)(H*0.833), (int)(W*0.184), BH);

        // ── Rect selezione PG ─────────────────────────────────────────────────
        boolean segretoAttivo = false; // non abbiamo ref a SistemaPersonaggi qui, usiamo 4 slot fissi
        int numSlot = 5;
        int rectW = (int)(W * 0.118), rectH = (int)(H * 0.447);
        int gapR  = (int)(W * 0.009);
        int totR  = rectW * numSlot + gapR * (numSlot - 1);
        int sxR   = W/2 - totR/2;
        int syR   = (int)(H * 0.223);
        for (int i = 0; i < numSlot; i++)
            rectsSelezionePG[i] = new java.awt.Rectangle(sxR + i*(rectW+gapR), syR, rectW, rectH);

        // ── Rect selezione modalità ───────────────────────────────────────────
        int rectMW = (int)(W * 0.239), rectMH = (int)(H * 0.536);
        int gapM   = (int)(W * 0.018);
        int totM   = rectMW * 2 + gapM;
        int sxM    = W/2 - totM/2;
        int syM    = (int)(H * 0.277);
        for (int i = 0; i < 2; i++)
            rectsSelezioneModalita[i] = new java.awt.Rectangle(sxM + i*(rectMW+gapM), syM, rectMW, rectMH);
    }

}