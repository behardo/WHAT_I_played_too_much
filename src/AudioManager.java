import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * AudioManager.java
 * Gestisce tutta la musica e gli effetti sonori del gioco.
 *
 * File audio attesi in /resources (o root classpath):
 *   musica_menu.wav, musica_mondo1-4.wav, musica_boss.wav,
 *   musica_vittoria.wav, musica_sconfitta.wav, musica_tetris.wav
 *   sfx_sparo.wav, sfx_morte_nemico.wav
 *
 * Tutti opzionali: se mancano il gioco gira in silenzio.
 * Formato: .wav PCM.
 */
public class AudioManager {

    // ── Chiavi musiche ────────────────────────────────────────────────────────
    public static final String MENU      = "menu";
    public static final String MONDO1    = "mondo1";
    public static final String MONDO2    = "mondo2";
    public static final String MONDO3    = "mondo3";
    public static final String MONDO4    = "mondo4";
    public static final String BOSS      = "boss";
    public static final String VITTORIA  = "vittoria";
    public static final String SCONFITTA = "sconfitta";
    public static final String TETRIS    = "tetris";

    // ── Chiavi effetti ────────────────────────────────────────────────────────
    public static final String SFX_SPARO        = "sparo";
    public static final String SFX_MORTE_NEMICO = "morte_nemico";

    // ── Stato interno ─────────────────────────────────────────────────────────
    private final Map<String, Clip> musiche = new HashMap<>();
    private final Map<String, Clip> effetti = new HashMap<>();

    private Clip   musicaCorrente  = null;
    private String chiaveCorrente  = null;

    private float volumeMusica  = 0.7f;   // 0.0–1.0
    private float volumeEffetti = 0.8f;

    // Per evitare di applicare il gain ogni singolo frame
    private float lastVolMusica  = -1f;
    private float lastVolEffetti = -1f;

    // Context usato per getResourceAsStream (stesso classpath delle immagini)
    private final Class<?> ctx;

    // ── Costruttori ───────────────────────────────────────────────────────────

    /** Costruttore senza context — usa AudioManager.class come riferimento classpath. */
    public AudioManager() {
        this(AudioManager.class);
    }

    /**
     * Costruttore con context.
     * Passare la classe principale del gioco (es. WhatIvePlayedTooMuch.class)
     * garantisce che i file vengano trovati con lo stesso classpath delle immagini.
     */
    public AudioManager(Class<?> context) {
        this.ctx = context;
        caricaMusica(MENU,      "/musica_menu.wav");
        caricaMusica(MONDO1,    "/musica_mondo1.wav");
        caricaMusica(MONDO2,    "/musica_mondo2.wav");
        caricaMusica(MONDO3,    "/musica_mondo3.wav");
        caricaMusica(MONDO4,    "/musica_mondo4.wav");
        caricaMusica(BOSS,      "/musica_boss.wav");
        caricaMusica(VITTORIA,  "/musica_vittoria.wav");
        caricaMusica(SCONFITTA, "/musica_sconfitta.wav");
        caricaMusica(TETRIS,    "/musica_tetris.wav");

        caricaEffetto(SFX_SPARO,        "/sfx_sparo.wav");
        caricaEffetto(SFX_MORTE_NEMICO, "/sfx_morte_nemico.wav");
    }

    // ── Caricamento ───────────────────────────────────────────────────────────

    private void caricaMusica(String chiave, String path) {
        Clip c = caricaClip(path);
        if (c != null) musiche.put(chiave, c);
    }

    private void caricaEffetto(String chiave, String path) {
        Clip c = caricaClip(path);
        if (c != null) effetti.put(chiave, c);
    }

    private Clip caricaClip(String path) {
        try {
            InputStream is = ctx.getResourceAsStream(path);
            if (is == null) {
                // Prova senza slash iniziale
                is = ctx.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            }
            if (is == null) return null;

            AudioInputStream raw = AudioSystem.getAudioInputStream(new BufferedInputStream(is));

            // Converti a PCM stereo 16-bit 44100 Hz se necessario (compatibilità JVM)
            AudioFormat baseFormat   = raw.getFormat();
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100f, 16, 2, 4, 44100f, false);

            AudioInputStream ais;
            if (AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
                ais = AudioSystem.getAudioInputStream(targetFormat, raw);
            } else {
                ais = raw;
            }

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            // File non trovato o formato non supportato — silenzio
            return null;
        }
    }

    // ── Musica ────────────────────────────────────────────────────────────────

    /** Avvia una musica in loop. Se è già in riproduzione, non fa nulla. */
    public void suonaMusica(String chiave) {
        if (chiave.equals(chiaveCorrente) && musicaCorrente != null
                && musicaCorrente.isRunning()) return;

        fermaMusica();
        Clip clip = musiche.get(chiave);
        if (clip == null) return;

        chiaveCorrente = chiave;
        musicaCorrente = clip;
        clip.setFramePosition(0);
        applicaVolumeClip(clip, volumeMusica);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** Musica per il mondo corrente (1-4+). */
    public void suonaMusicaMondo(int mondo) {
        String chiave = switch (mondo) {
            case 1  -> MONDO1;
            case 2  -> MONDO2;
            case 3  -> MONDO3;
            default -> MONDO4;
        };
        suonaMusica(chiave);
    }

    public void fermaMusica() {
        if (musicaCorrente != null && musicaCorrente.isRunning())
            musicaCorrente.stop();
        musicaCorrente = null;
        chiaveCorrente = null;
    }

    public String getMusicaCorrente() { return chiaveCorrente; }

    // ── Effetti ───────────────────────────────────────────────────────────────

    /** Riproduce un effetto sonoro one-shot. */
    public void suonaEffetto(String chiave) {
        Clip clip = effetti.get(chiave);
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        applicaVolumeClip(clip, volumeEffetti);
        clip.start();
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    /**
     * Imposta il volume della musica (0–100).
     * Chiamabile ogni frame: applica il gain solo se il valore è cambiato.
     */
    public void setVolumeMusica(int vol0_100) {
        float v = Math.max(0, Math.min(100, vol0_100)) / 100f;
        if (Math.abs(v - volumeMusica) < 0.001f) return;   // nessun cambiamento
        volumeMusica = v;
        if (musicaCorrente != null) applicaVolumeClip(musicaCorrente, volumeMusica);
    }

    /**
     * Imposta il volume degli effetti (0–100).
     * Il cambio viene applicato al prossimo suonaEffetto().
     */
    public void setVolumeEffetti(int vol0_100) {
        volumeEffetti = Math.max(0, Math.min(100, vol0_100)) / 100f;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void applicaVolumeClip(Clip clip, float livello) {
        if (clip == null) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = livello <= 0f
                        ? gain.getMinimum()
                        : (float)(20.0 * Math.log10(Math.max(0.0001, livello)));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                // Fallback per sistemi che non supportano MASTER_GAIN
                FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                vol.setValue(Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), livello)));
            }
        } catch (Exception ignored) {}
    }

    // ── Diagnostica ───────────────────────────────────────────────────────────

    /** Stampa in console quali clip sono state caricate con successo. */
    public void stampaStatoCaricamento() {
        System.out.println("[AudioManager] Musiche caricate: " + musiche.keySet());
        System.out.println("[AudioManager] Effetti caricati: " + effetti.keySet());
    }

    // ── Chiusura ─────────────────────────────────────────────────────────────

    public void chiudi() {
        fermaMusica();
        musiche.values().forEach(Clip::close);
        effetti.values().forEach(Clip::close);
    }
}