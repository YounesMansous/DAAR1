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

## b. `Fo.g` dans deux livres : le symbole AUTRE

**AFD minimal de R** (5 états, sans le puits) : alphabet `[AUTRE, F, g, o]`.

```
q1 --F--> q2 --o--> q3 --{AUTRE, F, g, o}--> q4 --g--> q0 (final)
```

**Ce que recouvre AUTRE, d'après le code.** Lors de la construction des sous-ensembles (`Determinisation`), l'alphabet de l'AFD contient les lettres de la regex, plus `Afd.AUTRE` si la regex contient un point. `move(S, AUTRE)` ne suit que les transitions sur `.`. Dans la table (`AfdCompile`) :

- pour chaque caractère de code < 256 qui n'est pas dans l'alphabet, la colonne reçoit une copie de la transition sur AUTRE (`table[i][c] = alphabet.contains(symbole) ? … : autre`). Pour `Fo.g`, cela concerne toutes les lettres sauf F, o et g, ainsi que les espaces, la ponctuation et les lettres accentuées comme `é` ou `à`, dont le code est inférieur à 256 ;
- la colonne 256 (`COLONNE_AUTRE`) sert aux caractères de code ≥ 256 absents de l'alphabet, par exemple `’`, `“` ou `—` : `suivant` les y envoie après une recherche dichotomique sans résultat dans la table de secours ;
- on ne compare donc jamais de chaînes pendant la recherche. AUTRE n'existe qu'à la construction : à la lecture, c'est une valeur recopiée dans une colonne.

### Le tour du monde (Verne) : le point ne lit jamais AUTRE dans une occurrence

Texte : `data/tour-du-monde-800.txt`. **652 lignes trouvées**, pour nous comme pour grep. Il y a 655 occurrences, toutes `Fogg` : 653 dans le mot `Fogg`, une dans `--Fogg` et une dans `Fogg--il`.

Dans les 655 occurrences, le point lit toujours le second « g » de `Fogg`. Comme `g` est dans l'alphabet, c'est la transition `q3 --g--> q4` qui sert, pas la colonne AUTRE. AUTRE sert bien pendant la lecture du texte, mais seulement dans des essais qui échouent. Après `Fo`, le point lit `r` dans `For`, `Fort`, `Forster` et `Formose`, `u` dans `Foundation` (licence Gutenberg) et `-` dans `Fo-Kien`. Cela fait 35 lectures par AUTRE, toutes suivies d'un caractère autre que `g`, donc d'une absence de transition (puits retiré).

### Don Quixote : le point lit AUTRE dans chaque occurrence

Texte : `data/don-quixote-996.txt`. **8 lignes trouvées**, pour nous comme pour grep, avec 8 occurrences, toutes `Forg`. Les mots sont `Forgive` (7) et `Forgotten` (1, ligne 6032). Dans les 8 occurrences, le point lit `r`, qui n'est pas dans l'alphabet {F, o, g} : c'est la colonne AUTRE qui le fait passer de q3 à q4.

Exemple, ligne 18448 : « “Forgive me, child, for waking thee, but I do so that thou mayest have »

| Caractère lu | Transition (AFD minimal de R) |
|---|---|
| `F` | q1 → q2 |
| `o` | q2 → q3 |
| `r` | q3 → q4 par **AUTRE** (`r` ∉ {F, o, g} : sa colonne contient la transition sur AUTRE) |
| `g` | q4 → q0, état final : la ligne est trouvée |

Le `“` qui ouvre la ligne (U+201C, code ≥ 256) passe lui aussi par AUTRE dans le matcher une passe : il n'est pas dans l'alphabet, donc `suivant` le lit dans la colonne 256.

```bash
for F in data/tour-du-monde-800.txt data/don-quixote-996.txt; do
  command grep -E -c 'Fo.g' $F; java -jar egrep-clone.jar -c 'Fo.g' $F    # Verne 652 / 652 ; Don Quixote 8 / 8
  command grep -E -o 'Fo.g' $F | cut -c3 | sort | uniq -c               # Verne : 655 g ; Don Quixote : 8 r
  command grep -o -E '[[:alpha:]-]*Fo.g[[:alpha:]-]*' $F | sort | uniq -c
done
command grep -o 'Fo.' data/tour-du-monde-800.txt | sort | uniq -c       # 655 Fog, 24 Fou, 10 For, 1 Fo-
command grep -n -E 'Fo.g' data/don-quixote-996.txt                      # lignes 6032, 18448, …
echo x | java -jar egrep-clone.jar --algo=automate --matcher=naif --debug 'Fo.g'
```

## c. KMP : le motif `anas` sur le mot « ananas » de Candide

**Fonction d'échec π** (`Kmp.getEchec()`, identique à `--debug`) et **table CarryOver sous la forme du cours**, avec m + 1 = 5 cases : CarryOver[0] = −1 ; pour 1 ≤ i ≤ m, f = π[i−1], et CarryOver[i] = CarryOver[f] si i < m et P[f] = P[i], sinon f.

| i | 0 | 1 | 2 | 3 | 4 |
|---|---|---|---|---|---|
| P[i] | a | n | a | s | — |
| π[i] | 0 | 0 | 1 | 0 | — |
| CarryOver[i] | −1 | 0 | −1 | 1 | 0 |

Le calcul, case par case :
- i = 1 : f = π[0] = 0 ; P[0] = `a` ≠ P[1] = `n`, donc CarryOver[1] = f = 0 ;
- i = 2 : f = π[1] = 0 ; P[0] = `a` = P[2] = `a`, donc CarryOver[2] = CarryOver[0] = −1. Si `a` échoue en position 2, il échouerait aussi contre P[0] = `a` : on avance directement dans le texte ;
- i = 3 : f = π[2] = 1 ; P[1] = `n` ≠ P[3] = `s`, donc CarryOver[3] = 1 ;
- i = 4 = m : f = π[3] = 0, donc CarryOver[4] = 0 (où reprendre après une occurrence complète).

C'est exactement la table de `mami` dans le cours : `TraceKmp mami` donne aussi π = [0, 0, 1, 0] et CarryOver = [−1, 0, −1, 1, 0]. Notre `Kmp` n'utilise pas CarryOver : il reprend en q = π[q−1]. Sa boucle `while` refait donc la comparaison que CarryOver[2] = −1 évite, mais la complexité reste O(n + m).

**La trace où π sert** (Candide, ligne 3658 : « limons, des ananas, des dattes, des pistaches, du café de Moka qui ») :

| position | caractère | q avant | retour par π | q après |
|---|---|---|---|---|
| 11 | (espace) | 0 | | 0 |
| 12 | a | 0 | | 1 |
| 13 | n | 1 | | 2 |
| 14 | a | 2 | | 3 |
| 15 | n | 3 | q = π[2] = 1 | 2 |
| 16 | a | 2 | | 3 |
| 17 | s | 3 | | 4 (trouvé) |

En position 15, `ana` est reconnu et on attend `s`, mais on lit `n`. π[2] = 1 (c'est aussi CarryOver[3] = 1) indique que le dernier `a` lu, en position 14, peut commencer une nouvelle occurrence. On passe donc à q = 1 et on compare de nouveau le même `n` à P[1] = `n`, sans reculer dans le texte. Un KMP faux, qui remettrait q à 0, comparerait `n` à `a`, puis lirait `a` et `s` sans retrouver le début, et manquerait l'occurrence `anas` qui commence en 14. `TraceKmp` le confirme : dans Candide, sur 12 964 retours, un seul est utile (celui-ci), et le KMP faux se trompe sur une seule ligne, la 3658. C'est pourquoi `anas` est le seul motif du banc qui révèle cette erreur.

**Et le motif `ananas` ?** π = [0, 0, 1, 2, 3, 0]. Un retour par π n'y servirait qu'avec la suite « ananan », absente de tous nos textes (`grep -c ananan` donne 0 partout). L'occurrence « des ananas » se lit donc d'une traite, sans aucun retour.

```bash
java -cp bin:bin-bench TraceKmp anas data/candide-4650.txt   # π, CarryOver, trace de la ligne 3658
java -cp bin:bin-bench TraceKmp mami                         # même table que anas
java -cp bin:bin-bench TraceKmp ananas data/candide-4650.txt data/tour-du-monde-800.txt   # 0 retour utile
echo x | java -jar egrep-clone.jar --debug anas              # table π
command grep -c ananan data/*.txt                            # 0 partout
```
