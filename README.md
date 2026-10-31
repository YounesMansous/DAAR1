# egrep-clone

Notre projet 1 de DAAR : une version Java de `egrep`. On donne une regex et un fichier, et le programme affiche les lignes qui la contiennent, comme `grep -E`.

On gère l'ERE simplifiée du sujet : `( )`, `|`, concaténation, `*`, `.` et lettres.

La recherche passe par un automate (AFN-ε, AFD, puis AFD minimal). Si la regex est un simple mot, on utilise KMP.

## Compiler et lancer

Il faut Java 11 ou plus, Ant (ou `make`) et une locale UTF-8 (`LC_ALL=C.UTF-8`), sinon les accents de la regex sont mal lus.

```bash
ant jar          # ou make : crée egrep-clone.jar
java -jar egrep-clone.jar "regex" [fichier] [auto|kmp|unepasse|naif]
```

- Sans fichier, on lit `data/text1.txt`.

- `auto` (par défaut) prend KMP pour un simple mot, sinon l'automate une passe. `naif` relance l'automate depuis chaque position de la ligne.

- Sans argument, le programme demande la regex, le fichier et l'algorithme.

- Seules les lignes trouvées vont sur la sortie standard ; l'algorithme et le nombre de lignes vont sur la sortie d'erreur.

Exemple du sujet (30 lignes, comme grep) :

```bash
java -jar egrep-clone.jar 'S(a|g|r)*on' data/text8.txt
```

## Tests

```bash
ant test         # ou make test, depuis la racine
```

Les tests prennent les 34 regex de `inputs/` et les 14 textes de `data/` :

- `TestCorrection` compare le naïf, l'une passe et KMP à `java.util.regex`, ligne par ligne : 8 007 480 comparaisons, 0 erreur ;

- `TestAutomates` compte les états des automates et vérifie que le naïf et l'une passe trouvent les mêmes lignes : 3 490 440 comparaisons, 0 différence.

Pour les temps : `ant pirecas` (environ 4 minutes) compare le naïf et l'une passe sur une longue ligne de « a », et `java -cp bin:bin-tests TestPerformance` mesure chaque algorithme sur les textes. Tous les résultats sont dans `resultats/`.

## Organisation du code

| Dans `src/` | Rôle |
|---|---|
| `Main` | lit les arguments, choisit l'algorithme, affiche les lignes |
| `parser/`, `ast/` | lisent la regex et construisent l'arbre |
| `automate/` | AFN-ε, AFD, minimisation de Moore |
| `experimentation/` | matchers naïf (O(n²)) et une passe (O(n)), lecture du fichier |
| `kmp/` | KMP, en O(n + m) |

## Différences avec `grep -E`

- `)` sans `(` ou `*` au début : grep les prend pour des lettres, nous on affiche une erreur.

- `+ ? [ ] { } ^ $ \` sont des lettres pour nous, des opérateurs pour grep.

- Un fichier qui n'est pas en UTF-8 est refusé.

- Avec un emoji (au-delà de U+FFFF), notre point lit un demi-caractère : sur `a😀b`, grep trouve `a.b` et nous `a..b`.
  Aucun texte de `data/` n'en contient.

## Auteurs

FELLAGUE Moustafa, MANSOUS Younes
