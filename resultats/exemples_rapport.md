# Exemples pour le rapport

Trois exemples tirés des textes de `data/`. Chaque chiffre vient d'une commande donnée sous l'exemple, lancée le 31/10/2026 depuis la racine, en locale UTF-8 (`export LC_ALL=C.UTF-8`), avec le jar construit par `ant clean jar` et les outils de `bench/` compilés par `ant compile-bench`. `grep` désigne GNU grep 3.11 (`command grep` dans le shell de Claude Code).

## a. Fil rouge : `Gr(e|a)*g.r` sur *Metamorphosis* (Kafka)

Texte : `data/metamorphosis-5200.txt`, traduction anglaise (Gutenberg n° 5200), 2 260 lignes, 135 968 caractères.

**Lignes trouvées : 294**, pour nous comme pour grep. Le texte contient 298 occurrences, car quatre lignes contiennent deux fois le nom.

**Mots reconnus** : la partie reconnue est toujours `Gregor`, avec G, r, e, g, o, r (le point lit le « o »). Les 298 occurrences se trouvent dans deux mots distincts :

| Mot du texte | Occurrences |
|---|---|
| `Gregor` | 199 |
| `Gregor’s` | 99 |

Aucun autre mot (`Graeger`, `Grager`…) n'apparaît. L'étoile `(e|a)*` n'est donc lue qu'une fois par occurrence, sur le « e ».

**Taille des automates** :

| Étape | R = `Gr(e\|a)*g.r` (matcher naïf) | `.*R` (matcher une passe, défaut) |
|---|---|---|
| AFN-ε (Aho-Ullman) | 18 états, 20 transitions | 22 états |
| AFD (construction des sous-ensembles) | 8 états, 18 transitions | 11 états |
| AFD minimal, sans l'état puits | 6 états, 12 transitions | 8 états |

L'alphabet de l'AFD est `[AUTRE, G, a, e, g, r]`. Le symbole AUTRE est présent parce que la regex contient un point. L'AFD minimal de R est la chaîne attendue : G, r, une boucle sur {a, e}, g, puis une transition du point sur les six symboles (`AUTRE, G, a, e, g, r`), et r. La minimisation de Moore fusionne en un seul état, avec une boucle sur {a, e}, les trois états de l'AFD atteints après `Gr`, après `Gr…a` et après `Gr…e` (q2, q3 et q4 dans `--debug`) : ils ont les mêmes transitions et aucun n'est final. On passe ainsi de 8 à 6 états. L'AFD minimal de `.*R` a 2 états de plus, qui gardent la trace d'un début possible d'occurrence pendant la lecture (comme les états de KMP).

**Figures** : `rapport/figures/filrouge_afn.dot` et `rapport/figures/filrouge_afd_min.dot`. Les états y sont numérotés comme dans `--debug`. Graphviz n'est pas installé sur la machine : il n'y a pas de PDF. Pour les produire : `dot -Tpdf rapport/figures/filrouge_afn.dot -o rapport/figures/filrouge_afn.pdf` (de même pour `_afd_min`).

```bash
F=data/metamorphosis-5200.txt; R='Gr(e|a)*g.r'
command grep -E -c "$R" $F                                 # 294
java -jar egrep-clone.jar -c "$R" $F                       # 294
command grep -E -o "$R" $F | sort | uniq -c                # 298 Gregor
command grep -E -o "[[:alpha:]’']*$R[[:alpha:]’']*" $F | sort | uniq -c
echo x | java -jar egrep-clone.jar --algo=automate --matcher=naif --debug "$R"   # R : 18 / 8 / 6
java -jar egrep-clone.jar --stats -c "$R" $F               # .*R : 22 / 11 / 8
java -cp bin:bin-bench ExportDot "$R" rapport/figures/filrouge
```

## b. `Fo.g` sur *Le tour du monde en quatre-vingts jours* (Verne) : le symbole AUTRE

Texte : `data/tour-du-monde-800.txt`. **652 lignes trouvées**, pour nous comme pour grep. Il y a 655 occurrences, toutes `Fogg` : 653 dans le mot `Fogg`, une dans `--Fogg` et une dans `Fogg--il`.

**AFD minimal de R** (5 états, sans le puits) : alphabet `[AUTRE, F, g, o]`.

```
q1 --F--> q2 --o--> q3 --{AUTRE, F, g, o}--> q4 --g--> q0 (final)
```

**Ce que recouvre AUTRE, d'après le code.** Lors de la construction des sous-ensembles (`Determinisation`), l'alphabet de l'AFD contient les lettres de la regex, plus `Afd.AUTRE` si la regex contient un point. `move(S, AUTRE)` ne suit que les transitions sur `.`. Dans la table (`AfdCompile`) :

- pour chaque caractère de code < 256 qui n'est pas dans l'alphabet, la colonne reçoit une copie de la transition sur AUTRE (`table[i][c] = alphabet.contains(symbole) ? … : autre`). Pour `Fo.g`, cela concerne toutes les lettres sauf F, o et g, ainsi que les espaces, la ponctuation et les lettres accentuées comme `é` ou `à`, dont le code est inférieur à 256 ;
- la colonne 256 (`COLONNE_AUTRE`) sert aux caractères de code ≥ 256 absents de l'alphabet, par exemple `’`, `“` ou `—` : `suivant` les y envoie après une recherche dichotomique sans résultat dans la table de secours ;
- on ne compare donc jamais de chaînes pendant la recherche. AUTRE n'existe qu'à la construction : à la lecture, c'est une valeur recopiée dans une colonne.

**Une occurrence du texte fait-elle lire AUTRE au point ? Non.** Dans les 655 occurrences, le point lit toujours le second « g » de `Fogg`. Comme `g` est dans l'alphabet, c'est la transition `q3 --g--> q4` qui sert, pas la colonne AUTRE. AUTRE sert bien pendant la lecture du texte, mais seulement dans des essais qui échouent. Après `Fo`, le point lit `r` dans `For`, `Fort`, `Forster` et `Formose`, `u` dans `Foundation` (licence Gutenberg) et `-` dans `Fo-Kien`. Cela fait 35 lectures par AUTRE dans ce livre, toutes suivies d'un caractère autre que `g`, donc d'une absence de transition (puits retiré). Pour voir une occurrence où le point lit AUTRE, il faut changer de texte. Dans *Don Quixote*, les 8 lignes trouvées contiennent `Forgive` (7) ou `Forgotten` (1) : le point y lit `r`, qui n'est pas dans l'alphabet.

```bash
F=data/tour-du-monde-800.txt
command grep -E -c 'Fo.g' $F; java -jar egrep-clone.jar -c 'Fo.g' $F   # 652, 652
command grep -E -o 'Fo.g' $F | cut -c3 | sort | uniq -c             # 655 g
command grep -o 'Fo.' $F | sort | uniq -c                           # 655 Fog, 24 Fou, 10 For, 1 Fo-
echo x | java -jar egrep-clone.jar --algo=automate --matcher=naif --debug 'Fo.g'
command grep -o -E '[[:alpha:]]*Fo.g[[:alpha:]]*' data/don-quixote-996.txt | sort | uniq -c
```

## c. `ananas` et KMP

**Fonction d'échec π** (`Kmp.getEchec()`, identique à `--debug`) et table CarryOver :

| i | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| P[i] | a | n | a | n | a | s |
| π[i] | 0 | 0 | 1 | 2 | 3 | 0 |
| CarryOver[i] : -1, puis π[i-1] | -1 | 0 | 0 | 1 | 2 | 3 |
| CarryOver[i] optimisée | -1 | 0 | -1 | 0 | -1 | 3 |

- **Forme simple** : CarryOver[0] = -1 et CarryOver[i] = π[i-1]. C'est ce que fait notre code : après un échec sur P[q], avec q caractères reconnus, on reprend en q = π[q-1] = CarryOver[q]. La valeur -1 signifie qu'on avance dans le texte et qu'on repart de P[0].
- **Forme optimisée** : on parcourt i de gauche à droite, et si P[i] = P[CarryOver[i]], on pose CarryOver[i] = CarryOver[CarryOver[i]]. En i = 2 et i = 4 (`a`), le repli mènerait à P[0] = `a`, la même lettre qui vient d'échouer : on saute donc directement à -1. En i = 3 (`n`), le repli mènerait à P[1] = `n`, d'où 0. En i = 5 (`s`), P[3] = `n` ≠ `s` : on garde 3. Notre `Kmp` utilise la forme simple : sa boucle `while` refait ces comparaisons inutiles, sans changer la complexité O(n + m).
- Les deux formes sont calculées par `bench/TraceKmp.java`. Il reste à vérifier laquelle correspond aux conventions du cours.

**Trace sur nos textes : pour `ananas`, π ne sert jamais vraiment.** `TraceKmp` reprend la boucle de `Kmp.contientMotif`, contrôlée ligne par ligne contre elle, et compte les retours q = π[q-1] :

| Texte | Lignes trouvées | Retours | dont q' > 0 | Retours utiles (q' > 0 et c = P[q']) | Lignes où un KMP qui remet q à 0 se trompe |
|---|---|---|---|---|---|
| Candide | 1 | 12 963 | 28 (q = 3 → 1) | 0 | 0 |
| Le tour du monde | 1 | 27 806 | 31 (30 fois q = 3 → 1, 1 fois q = 4 → 2) | 0 | 0 |

L'explication tient à la forme du motif. Un retour en q' > 0 n'aide que si le caractère qui vient d'échouer prolonge le bord, c'est-à-dire si c = P[q']. Pour `ananas`, c'est impossible après un échec en q = 3 ou en q = 4, car on retombe sur la lettre qui vient d'échouer (c'est exactement le cas que la forme optimisée de CarryOver supprime). Après un échec en q = 5, cela demande un texte contenant `ananan`, et aucun fichier de `data/` n'en contient (`grep -c ananan` donne 0 partout). L'occurrence `des ananas` (Candide, ligne 3658 ; Verne, ligne 3931) se lit d'une traite, de q = 0 à q = 6, sans aucun retour.

**Le décalage par π sert vraiment avec le motif `anas`, sur ce même « ananas »** (Candide, ligne 3658 : « limons, des ananas, des dattes, … ») :

| position | caractère | q avant | retour par π | q après |
|---|---|---|---|---|
| 11 | (espace) | 0 | | 0 |
| 12 | a | 0 | | 1 |
| 13 | n | 1 | | 2 |
| 14 | a | 2 | | 3 |
| 15 | n | 3 | q = π[2] = 1 | 2 |
| 16 | a | 2 | | 3 |
| 17 | s | 3 | | 4 (trouvé) |

En position 15, `ana` est reconnu et on attend `s`, mais on lit `n`. π[2] = 1 indique que le dernier `a` lu (position 14) peut commencer une nouvelle occurrence : on passe à q = 1 et on compare de nouveau le même `n`, sans reculer dans le texte. Un KMP faux, qui remettrait q à 0, comparerait `n` à `a`, puis lirait `a`, `s` et manquerait l'occurrence `anas`, qui commence en 14. `TraceKmp` le confirme : ce KMP faux se trompe sur 1 ligne de Candide et sur 1 ligne de Verne, celles qui contiennent « ananas ». C'est ce que dit le CLAUDE.md : `anas` est le seul motif du banc qui révèle cette erreur. Pour `anas`, π = [0, 0, 1, 0], CarryOver simple = [-1, 0, 0, 1] et CarryOver optimisée = [-1, 0, -1, 1].

```bash
java -cp bin:bin-bench TraceKmp ananas data/candide-4650.txt data/tour-du-monde-800.txt
java -cp bin:bin-bench TraceKmp anas   data/candide-4650.txt data/tour-du-monde-800.txt
echo x | java -jar egrep-clone.jar --debug ananas        # table π
command grep -c ananan data/*.txt                         # 0 partout
```
