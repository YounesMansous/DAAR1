# Scénarios d'utilisation

Pour chaque scénario : la situation, une commande d'exemple, la méthode choisie par le programme, le chiffre qui le prouve avec son CSV source, et la figure. Les temps « dans la JVM » sont des moyennes ± écart-type sur 20 mesures après 5 chauffes, lignes déjà en mémoire, une JVM par configuration. Les temps « de bout en bout » sont des moyennes sur 10 lancements du jar, démarrage de la JVM compris. Le protocole et la machine sont décrits dans `bench/README.md`. Toutes les commandes se lancent depuis la racine, en locale UTF-8 (`export LC_ALL=C.UTF-8`).

## 1. Le lecteur : chercher un nom dans un livre

**Situation** : on cherche toutes les lignes où apparaît un personnage. Le motif est un mot sans opérateur.

```bash
java -jar egrep-clone.jar --stats -c Passepartout data/tour-du-monde-800.txt
# Algorithme : kmp (motif de 12 caracteres) ; 423 lignes (grep -E -c : 423)
```

**Méthode** : avec `--algo=auto` (défaut), `Kmp.motifLitteral` reconnaît un littéral : KMP remplace tout le pipeline. Il n'y a ni AFN-ε, ni construction des sous-ensembles, ni minimisation, seulement la fonction d'échec π, calculée en O(m), puis une lecture en O(n) qui ne recule jamais dans le texte.

**Chiffres** (`resultats/bench_jvm.csv`, expérience `a`, *Don Quixote*, 2,2 Mio) :
- recherche : de 3,3 à 5,8 ms pour KMP, contre 7,1 à 9,5 ms pour l'automate une passe, sur les 8 littéraux de 2 à 70 caractères. Par exemple, `urpasses all` : 3,81 ± 0,02 ms contre 7,96 ± 0,15 ms ;
- construction : de 0,008 à 0,032 ms pour la table π, contre 0,45 à 10,5 ms pour l'automate. Pour le littéral de 70 caractères, il faut construire un AFN-ε de 140 états et un AFD de 71 états ;
- à 32 Mio (expérience `c`, `together`) : KMP 79,6 ± 5,3 ms, automate une passe 104,7 ± 1,0 ms, matcher naïf 101,2 ± 11,2 ms.

**Figure** : `rapport/figures/fig_kmp_vs_automate.png`.

## 2. Le chercheur, puis l'archiviste : une expression courante

**Situation** : le chercheur veut toutes les variantes d'un nom (`Sargon`, `Saron`, `Sagon`…) dans un livre. L'archiviste lance la même recherche sur un corpus de 32 Mo.

```bash
java -jar egrep-clone.jar -c 'S(a|g|r)*on' data/babylon-56667.txt                                 # 30 lignes (grep : 30)
java -jar egrep-clone.jar --algo=automate --matcher=naif -c 'S(a|g|r)*on' data/babylon-56667.txt  # 30 lignes
```

**Méthode** : la regex n'est pas littérale, donc on utilise l'automate. Par défaut, c'est le matcher une passe : AFD minimal de `.*R`, lecture de chaque ligne une seule fois. On peut demander le matcher naïf, qui relance l'AFD minimal de R depuis chaque position. Ici, les deux AFD minimaux ont 4 états (AFN-ε de 18 états, AFD de 7 états).

**Chiffres, le chercheur** (`resultats/bench_jvm.csv`, expérience `b_textes`, *Don Quixote*) : **matcher naïf 4,51 ± 0,24 ms, matcher une passe 7,15 ± 0,03 ms**. Le matcher naïf, en O(n²) dans le pire cas, est le plus rapide sur ce texte réel. Comme l'état puits est retiré, chaque essai meurt dès le premier caractère qui n'est pas `S`. Une hypothèse, non vérifiée par profilage : les essais du naïf sont indépendants, alors que le matcher une passe enchaîne des accès `table[etat][c]` qui dépendent les uns des autres.

**Chiffres, l'archiviste** (même CSV, expérience `c_echelle`, fichiers générés de 1 à 32 Mio) : **à 32 Mio, matcher naïf 68,0 ± 4,8 ms, matcher une passe 104,9 ± 0,7 ms**, 349 lignes. Le temps croît linéairement avec la taille du fichier : de 1 à 32 Mio, il est multiplié par 33,5 pour le naïf (2,03 → 68,0 ms) et par 28,4 pour le matcher une passe (3,70 → 104,9 ms). De bout en bout (`resultats/bench_bout_en_bout.csv`, 32 Mio), il faut 407 ± 18 ms au programme complet, contre 62,7 ± 0,5 ms à grep.

**Figures** : `rapport/figures/fig_naif_vs_unepasse_textes.png` (le chercheur), `rapport/figures/fig_passage_echelle.png` (l'archiviste).

## 3. Le piège : le livre sur une seule ligne

**Situation** : un texte dont les retours à la ligne ont disparu, par exemple une page web aplatie ou un export mal converti. Ici, c'est *The Yellow Wallpaper* (50 833 caractères), dont les 1 227 lignes sont recollées avec des espaces.

```bash
tr '\n' ' ' < data/yellow-wallpaper-1952.txt > /tmp/une_ligne.txt
java -jar egrep-clone.jar --algo=automate --matcher=naif     --stats -c 'e.*zq' /tmp/une_ligne.txt   # recherche 364 ms
java -jar egrep-clone.jar --algo=automate --matcher=unepasse --stats -c 'e.*zq' /tmp/une_ligne.txt   # recherche 11 ms
```

(Ce sont des lancements uniques, sans chauffe : 364,2 ms contre 11,3 ms. Le banc ci-dessous donne les mesures stables.)

**Méthode** : le **matcher une passe** (le défaut). Le matcher naïf relance l'AFD de R depuis chaque `e`, la lettre la plus fréquente du texte (4 559 occurrences), et `.*` le fait lire jusqu'au bout de la ligne. Comme `zq` n'apparaît nulle part, il n'y a jamais d'état final pour l'arrêter. Le matcher une passe lit chaque caractère une seule fois.

**Chiffres** (`resultats/bench_une_ligne.csv`, `BenchUneLigne.java`, même protocole que `bench/`) :

| Regex | Forme du texte | Matcher naïf | Matcher une passe |
|---|---|---|---|
| `e.*zq` (avec `.*`, aucun résultat) | 1 227 lignes | 1,31 ± 2,90 ms (médiane 0,65) | 0,63 ± 0,96 ms (médiane 0,22) |
| `e.*zq` | **une seule ligne** | **362,5 ± 1,8 ms** | **0,161 ± 0,003 ms** |
| `S(a\|g\|r)*on` (sans `.*`, aucun résultat) | 1 227 lignes | 0,198 ± 0,034 ms | 0,240 ± 0,061 ms |
| `S(a\|g\|r)*on` | une seule ligne | 0,162 ± 0,009 ms | 0,161 ± 0,003 ms |

Sur une seule ligne, `e.*zq` est **2 250 fois plus lent** avec le matcher naïf. Aucune exécution ne dépasse 10 s : la limite prévue n'a pas servi. Le chiffre correspond à la théorie. Le naïf calcule 116 288 082 transitions (la somme, sur chaque `e`, de la longueur de la fin de ligne), soit 3,1 ns par transition, du même ordre que le pire cas `a.*b` de l'expérience `b_pire_cas` (1,25 × 10⁹ transitions en 3 548 ms, soit 2,8 ns). Sans `.*`, les essais du naïf meurent au premier caractère, et la forme du texte n'a plus d'effet.

Les écarts-types de la forme « 1 227 lignes » de `e.*zq` sont élevés parce que c'est le premier cas mesuré dans chaque JVM : après 5 chauffes, la compilation par le JIT n'est pas finie (le naïf passe de 13,6 ms à la 1re mesure à 0,65 ms à la 2e). C'est pourquoi la médiane est donnée à côté.

**Refaire la mesure** :

```bash
export LC_ALL=C.UTF-8
ant compile-bench
rm -f resultats/bench_une_ligne.csv
for m in naif unepasse; do java -Xmx1g -cp bin:bin-bench BenchUneLigne $m; done
python3 bench/graphique_une_ligne.py
```

**Figure** : `rapport/figures/fig_une_ligne.png` (échelle logarithmique).

## 4. Le motif monstre : `(a|b)*a(a|b)…(a|b)`

**Situation** : un motif court dont l'automate déterministe explose : « un `a` suivi d'exactement n lettres a ou b ».

```bash
echo abaaaaaaaa | java -jar egrep-clone.jar --stats '(a|b)*a(a|b)(a|b)(a|b)(a|b)(a|b)(a|b)(a|b)(a|b)'
# .*R : AFN-ε 62 états, AFD 514, AFD minimal 512 ; construction 105 ms (lancement unique, sans chauffe)
```

**Méthode** : l'automate, puisque le motif n'est pas littéral et que KMP est exclu. Le coût vient de la construction des sous-ensembles : chaque état de l'AFD doit retenir lesquelles des n + 1 dernières lettres sont des `a`. La minimisation ne peut rien enlever, car tous ces états sont distincts.

**Chiffres** (`resultats/bench_jvm.csv`, expérience `d_explosion`, automate de R) :

| n | AFN-ε | AFD | AFD minimal | Construction (une passe, `.*R`) |
|---|---|---|---|---|
| 1 | 16 | 5 | 4 | 0,17 ± 0,02 ms |
| 4 | 34 | 33 | 32 | 1,28 ± 0,07 ms |
| 8 | 58 | 513 | 512 | 22,5 ± 2,9 ms |

L'AFN-ε a 6n + 10 états, l'AFD 2^(n+1) + 1 et l'AFD minimal 2^(n+1) (512 pour n = 8, aussi pour `.*R`). C'est exactement la borne théorique : l'AFN croît linéairement, l'AFD exponentiellement. La recherche reste en O(n) par ligne, mais la table occupe 512 × 257 entiers.

**Figure** : `rapport/figures/fig_etats_automates.png`.

## 5. L'impatient : notre programme contre grep, de bout en bout

**Situation** : on tape une commande et on attend le résultat. Ce qui compte, c'est le temps total, démarrage compris.

```bash
time java -jar egrep-clone.jar -c the data/yellow-wallpaper-1952.txt   # 395
time command grep -E -c the data/yellow-wallpaper-1952.txt             # 395
```

**Méthode** : celle de `--algo=auto` (KMP pour `the`, sinon l'automate une passe). Ici, ce n'est pas l'algorithme qui décide : le temps fixe de la JVM domine.

**Chiffres** (`resultats/bench_bout_en_bout.csv`, 8 regex × 3 fichiers) :
- 50 Kio (*The Yellow Wallpaper*) : 67,5 à 99,7 ms pour nous, 1,3 à 1,9 ms pour grep, donc grep est **43 à 75 fois** plus rapide ;
- 2,2 Mio (*Don Quixote*) : 116 à 162 ms contre 1,8 à 8,0 ms, soit 19 à 69 fois ;
- 32 Mio : 334 à 421 ms contre 10,5 à 80,2 ms, soit 5,2 à 32 fois ;
- sur un fichier vide (10 lancements, mesurés à part le 31/10/2026 avec `time.perf_counter`) : 59,6 ± 3,7 ms pour le jar, 1,2 ± 0,0 ms pour grep. Le démarrage de la JVM représente donc à lui seul la plus grande partie des 67 à 100 ms mesurés sur 50 Kio.

L'écart se réduit quand le fichier grossit, car le temps fixe compte de moins en moins, mais il reste d'au moins 5 fois : la lecture et le décodage UTF-8 des lignes en `String` dominent nos temps, qui varient peu selon la regex.

**Figure** : `rapport/figures/fig_bout_en_bout.png`.
