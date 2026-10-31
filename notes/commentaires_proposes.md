# Commentaires proposés

Ces 20 endroits sont les plus importants à expliquer dans le code. Pour chacun : où il se trouve, le code concerné, un commentaire proposé et une ligne vide pour ta version.

Les commentaires proposés disent **pourquoi** le code fait ça, en `//` (ou en `#` pour le script), à placer juste au-dessus des lignes citées. Ils ne répètent pas la Javadoc (`/** */`). Quand un commentaire `//` existe déjà à côté, il est cité dans « Déjà là » : ta version peut le remplacer ou le compléter.

Quelques mots qui reviennent :
- **puits** : un état « poubelle ». On n'en sort jamais et il n'est jamais final : y arriver veut dire « le motif ne sera plus trouvé à partir d'ici » ;
- **AUTRE** : un symbole unique pour tous les caractères qui n'apparaissent pas dans la regex ;
- **bord** (KMP) : un morceau du motif qui est à la fois son début et sa fin, sans être le motif entier. Exemple : `a` est un bord de `ana`.

Les numéros de ligne sont ceux du commit 7a12583. Ils changeront dès que tu ajouteras des commentaires : commence par le bas de chaque fichier.

---

## Determinisation

### 1. `src/automate/Determinisation.java:84`, méthode `move`

```java
if (!symbole.equals(Afd.AUTRE)) {
    resultat.addAll(
        afn.destinations(etat, symbole)
```

Commentaire proposé :
```java
// AUTRE = un caractère absent de la regex : aucune lettre ne peut le
// lire, seul le point le peut (c'est la ligne juste en dessous).
```

Ma version :

### 2. `src/automate/Determinisation.java:90`, méthode `move`

```java
resultat.addAll(
    afn.destinations(etat, ".")
);
```

Commentaire proposé :
```java
// Le point lit n'importe quel caractère, donc aussi la lettre qu'on
// lit en ce moment : dans « Fo.g », il peut lire le g de « Fogg ».
```

Ma version :

### 3. `src/automate/Determinisation.java:127`, méthode `convertir` (ajout d'AUTRE à l'alphabet)

```java
if (symbole.equals(".")) {
    alphabet.add(Afd.AUTRE);
}
```

Déjà là (lignes 118-119) : « Lettres de la regex, plus AUTRE pour les caracteres qui n'y figurent pas mais que le point accepte ».

Commentaire proposé :
```java
// AUTRE seulement s'il y a un point : sans point, un caractère absent
// de la regex ne mène nulle part, pas besoin de lui donner un symbole.
```

Ma version :

### 4. `src/automate/Determinisation.java:164`, méthode `convertir` (ensemble vide ignoré)

```java
if (destination.isEmpty()) {
    continue;
}
```

Commentaire proposé :
```java
// Ensemble vide = le motif ne peut plus continuer à partir de ce début.
// On ne crée pas d'état pour ça : la flèche absente suffit à le dire.
```

Ma version :

---

## Minimisation

### 5. `src/automate/Minimisation.java:361`, méthode `minimiser` (ajout du puits avant Moore)

```java
Afd accessible = supprimerInaccessibles();

Afd complet = completerAutomate(accessible);
```

Déjà là (au-dessus de `completerAutomate`, lignes 120-123) : « Moore a besoin d'un automate complet : sinon une transition absente n'aurait pas de classe d'arrivee a comparer. »

Commentaire proposé :
```java
// Moore compare où va chaque état, lettre par lettre. Une flèche absente
// n'irait « nulle part » : le puits donne une adresse à ce « nulle part ».
```

Ma version :

### 6. `src/automate/Minimisation.java:378`, méthode `minimiser` (appel de `retirerPuits`)

```java
}

return retirerPuits(construireAfdMinimal(complet, partitions));
```

Commentaire proposé :
```java
// Le puits ne servait qu'à Moore. Si on le gardait, chaque essai du
// matcher naïf lirait la ligne jusqu'au bout au lieu de s'arrêter.
```

Ma version :

### 7. `src/automate/Minimisation.java:402`, méthode `retirerPuits`

```java
Set<Etat> utiles = new HashSet<>(automate.getFinaux());
Deque<Etat> file = new ArrayDeque<>(automate.getFinaux());

```

Déjà là (au-dessus de la méthode, lignes 381-386) : « Parcours en largeur a l'envers depuis les finaux ».

Commentaire proposé :
```java
// On remonte les flèches depuis les états finaux : un seul parcours
// trouve tous les états utiles, au lieu d'un test par état.
```

Ma version :

---

## AfdCompile

### 8. `src/automate/AfdCompile.java:106`, constructeur `AfdCompile` (copie de la colonne AUTRE)

```java
table[i][c] = alphabet.contains(symbole)
    ? indice(afd.transition(etat, symbole), indices)
    : autre;
```

Commentaire proposé :
```java
// On recopie AUTRE dans la colonne de chaque caractère absent de la
// regex : pendant la recherche, on lit la case sans aucun test.
```

Ma version :

### 9. `src/automate/AfdCompile.java:140`, méthode `suivant` (caractères de code < 256)

```java
if (c < COLONNE_AUTRE) {
    return table[etat][c];
}
```

Commentaire proposé :
```java
// Cas le plus fréquent : lettres, espaces, ponctuation et accents
// comme « é » ont un code < 256. Une seule case à lire.
```

Ma version :

### 10. `src/automate/AfdCompile.java:146`, méthode `suivant` (caractères de code ≥ 256)

```java
int k = Arrays.binarySearch(caracteresHauts, c);
…
return table[etat][COLONNE_AUTRE];
```

Commentaire proposé :
```java
// Pas une colonne par caractère possible (65 536) : table 255 fois plus
// grosse. « ’ », « “ »… : petite liste triée si la regex les a, sinon AUTRE.
```

Ma version :

---

## Matchers

### 11. `src/experimentation/MatcherNaif.java:56`, méthode `contientMotif`

```java
if (etat == AfdCompile.AUCUNE) {
    break;
}
```

Commentaire proposé :
```java
// Pas de flèche : le motif ne commence pas à « debut ». Mais il peut
// commencer plus loin, donc break (essai suivant), pas return false.
```

Ma version :

### 12. `src/experimentation/MatcherNaif.java:60`, méthode `contientMotif`

```java
if (automate.estFinal(etat)) {
    return true;
}
```

Commentaire proposé :
```java
// grep affiche la ligne entière : il suffit d'une occurrence, pas
// besoin de chercher la plus longue ni de les compter.
```

Ma version :

### 13. `src/experimentation/MatcherUnePasse.java:47`, méthode `envelopper`

```java
return AstFactory.createConcat(
    AstFactory.createRepeat(AstFactory.createDot()),
    r);
```

Commentaire proposé :
```java
// Le « .* » devant R avale tout ce qui précède l'occurrence : l'automate
// peut commencer R à n'importe quelle position, sans qu'on le relance.
```

Ma version :

### 14. `src/experimentation/MatcherUnePasse.java:70`, méthode `contientMotif`

```java
for (int i = 0; i < longueur; i++) {

    etat = automate.suivant(etat, ligne.charAt(i));
```

Commentaire proposé :
```java
// Chaque caractère est lu une seule fois. Même un livre entier sur une
// seule ligne se lit vite, là où le naïf repart de chaque position.
```

Ma version :

---

## Kmp

### 15. `src/kmp/Kmp.java:63`, méthode `calculerEchec`

```java
int k = 0;

for (int i = 1; i < p.length; i++) {
```

Déjà là : ligne 62, « k = longueur du bord courant de p[0..i-1] » ; ligne 67, « Le bord ne se prolonge pas : on essaie le bord du bord ».

Commentaire proposé :
```java
// On commence à i = 1 : un seul caractère n'a jamais de bord
// (un début qui est aussi une fin, sans être le mot entier), donc π[0] = 0.
```

Ma version :

### 16. `src/kmp/Kmp.java:105`, méthode `contientMotif` (boucle de reprise)

```java
while (q > 0 && c != p[q]) {
    q = echec[q - 1];
}
```

Commentaire proposé :
```java
// Ex. avec « anas » : on a lu « ana », on lit « n » au lieu de « s ».
// Le dernier « a » peut commencer le motif : q = π[2] = 1, on recompare « n ».
```

Ma version :

---

## Main

### 17. `src/Main.java:240`, méthode `executer` (choix entre KMP et l'automate)

```java
boolean utiliserKmp = algo.equals("kmp")
    || (algo.equals("auto") && motifLitteral != null);
```

Déjà là (ligne 229) : « KMP ne s'applique qu'a une concatenation de lettres ».

Commentaire proposé :
```java
// Pour une simple suite de lettres, KMP trouve les mêmes lignes que
// l'automate, mais sa table se construit bien plus vite.
```

Ma version :

### 18. `src/Main.java:422`, méthode `avertirSiMotifMalDecode` (avertissement de locale)

```java
if (encodage.isEmpty() || estUtf8(encodage)) {
    return;
}
```

Déjà là (au-dessus de la méthode, lignes 409-415) : la cause (arguments décodés en ASCII sans locale UTF-8).

Commentaire proposé :
```java
// Encodage inconnu : on se tait plutôt que de donner une fausse alerte.
// En UTF-8, le motif est arrivé intact : rien à signaler.
```

Ma version :

---

## tests/verifier.sh

### 19. `tests/verifier.sh:56`, choix de la locale

```bash
export LC_ALL=$LOCALE
echo "locale utilisée pour java et grep : $LC_ALL"

```

Déjà là (lignes 40-44) : pourquoi java a besoin d'une locale UTF-8, et le choix C.UTF-8, sinon en_US.UTF-8.

Commentaire proposé :
```bash
# export, et pas seulement LC_ALL=... devant grep : java est lancé plus
# loin par xargs, dans d'autres processus, qui doivent hériter de la locale.
```

Ma version :

### 20. `tests/verifier.sh:125`, fonction `comparer` (les 5 configurations)

```bash
case "$config" in
    defaut)   ;;
    naif)     options=(--algo=automate --matcher=naif) ;;
```

Commentaire proposé :
```bash
# Une configuration par chemin du code : par défaut, les littéraux passent
# par KMP, donc on force aussi l'automate sur eux ; --algo=kmp, lui, ne
# marche que sur les littéraux (sinon code 2).
```

Ma version :
