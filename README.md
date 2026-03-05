# WHAT I'VE PLAYED TOO MUCH

Un roguelike 2D in Java. Sei in ritardo al lavoro. Invece di andarci, hai passato la notte a giocare. Adesso devi attraversare quattro mondi di nemici e boss per arrivare in ufficio — se ci arrivi.

---

## Come si gioca

| Tasto | Azione |
|-------|--------|
| W A S D | Muovi il personaggio |
| Frecce direzionali | Spara |
| ESC | Pausa |
| F11 | Schermo intero |

---

## Struttura di una run

**1. Tetris**
Prima di entrare si gioca una partita a Tetris da 40 secondi. Il punteggio determina il power-up iniziale.

**2. Casa**
La prima stanza. Contiene il power-up guadagnato col Tetris. Raccoglilo e parti.

**3. Stanze**
Ogni mondo ha 7 stanze con nemici. Eliminali tutti per aprire la porta verso la stanza successiva.

**4. Boss**
La stanza 8 di ogni mondo. Sconfiggi il boss per avanzare.

**5. Negozio**
Appare dopo la prima stanza. Spendi le monete raccolte per comprare potenziamenti.

---

## Power-up Tetris

| Punteggio | Ricompensa |
|-----------|------------|
| Meno di 500 | Solo una cura di base |
| 500+ | Cura extra |
| 1500+ | Velocita aumentata |
| 3000+ | Danno aumentato |
| 6000+ | Tutto quanto |

---

## Modalita

**Storia** — 4 mondi con boss. Completali tutti per finire il gioco.

**Infinita** — I mondi si ripetono all'infinito con difficolta crescente.

---

## Personaggi

| Nome | Vita | Stile |
|------|------|-------|
| BELLGERD | 3 | Equilibrato |
| VLAD | 2 | Veloce ma fragile |
| PAUL | 3 | Lento ma potente |
| JUICY | 5 | Lentissimo, molta vita |
| G.O.D. | 99 | Sbloccabile con una combinazione segreta |

---

## I Mondi e i Boss

**Mondo 1 — Il Cantiere**
Il boss ti insegue e spara un ventaglio a 3 proiettili.

**Mondo 2 — Le Fogne**
Il boss si muove a scatti improvvisi e spara raffiche in 8 direzioni.

**Mondo 3 — La Fornace**
Il boss si carica verso di te ad alta velocita. Se ti colpisce di corpo o con un proiettile di fuoco, prendi danno da bruciatura continuativo per 3 secondi.

**Mondo 4 — Il Castello**
Il boss finale ha tre fasi in base alla vita rimasta:

- Fase 1: si muove specularmente a te rispetto al centro stanza, spara una spirale rotante
- Fase 2: piu veloce, movimento imprevedibile, alterna spirale e burst di proiettili mirati
- Fase 3: abbandona il movimento specchio e ti insegue direttamente, spara spirale e burst insieme, schiva i tuoi proiettili con alta probabilita

---

## Negozio

Avvicinandoti al negoziante parte un dialogo. Puoi scegliere di attaccarlo: ottieni 20 monete ma perdi lo shop per il resto della run.

---

## Compilazione

Richiede Java 17 o superiore.

```bash
javac -cp src src/*.java -d out
java -cp out WhatIvePlayedTooMuch
```

---

## File principali

| File | Ruolo |
|------|-------|
| `WhatIvePlayedTooMuch.java` | Entry point e finestra |
| `GameLoop.java` | Logica di gioco e collisioni |
| `GameState.java` | Stato della run |
| `RenderEngine.java` | Rendering |
| `RoomManager.java` | Generazione stanze |
| `Boss.java` | Comportamento dei boss |
| `TetrisGame.java` | Minigame Tetris |
| `AudioManager.java` | Audio |
| `ResourceLoader.java` | Caricamento asset |

---

## Nota con codice debug

Nella stanza Casa spawna in una posizione random una nota raccoglibile (foglio con punto esclamativo lampeggiante). Raccogliendola appare un popup in stile "carta trovata" con il codice di debug **WIPT-4269**. Se la nota viene trovata, il codice appare anche sulla schermata finale.

La nota serve come easter egg / hint per eventuali sistemi di debug o funzionalita nascoste nel gioco.

Asset: `nota.png`

---

## Ufficio (zona finale)

Dopo aver sconfitto il boss del Castello, invece di passare direttamente alla schermata di vittoria, si entra nella stanza Ufficio. Si trova il NPC **Capo** seduto alla scrivania. Parte automaticamente un dialogo JRPG multi-pagina. Alla fine del dialogo si arriva alla schermata finale.

Asset: `capo.png`
