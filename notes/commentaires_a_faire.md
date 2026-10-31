# Commentaires à faire

Chaque commentaire se place en `//` juste au-dessus de la ligne indiquée, dans le style de ceux qui existent déjà (minuscule au début, « on », phrase courte). Les numéros de ligne sont ceux d'aujourd'hui : ils se décalent dès que tu ajoutes un commentaire, alors commence par le bas de chaque fichier.

---

## src/Main.java

### 1. `src/Main.java:26`, début du mode interactif
Code : `if (args.length == 0) {`
```java
// sans argument on passe en mode interactif : on demande la regex, le fichier puis l'algo
```
Ma version :

### 2. `src/Main.java:35`, fichier par défaut
Code : `if (!saisie.isBlank()) {`
```java
// si l'utilisateur appuie juste sur Entree on garde data/text1.txt
```
Ma version :

### 3. `src/Main.java:49`, choix de l'algo en mode interactif
Code : `switch (choix) {`
```java
// on traduit le numero choisi en nom d'algo, et on prend auto si le choix n'est pas reconnu
```
Ma version :

### 4. `src/Main.java:56`, lecture des arguments
Code : `else {`
```java
// sinon on lit les arguments : la regex, puis le fichier et l'algo s'ils sont donnes
```
Ma version :

### 5. `src/Main.java:74`, choix automatique de la méthode
Code : `if (algo.equals("auto")) {`
```java
// en auto on prend KMP si le motif est une simple suite de lettres, sinon l'automate une passe
```
Ma version :

### 6. `src/Main.java:80`, KMP sur un motif non littéral
Code : `if (motif == null) {`
```java
// KMP ne sait pas gerer | * . ni le mot vide, donc on s'arrete avec un message
```
Ma version :

### 7. `src/Main.java:90`, automate de R ou de .*R
Code : `AstI astU = algo.equals("unepasse")`
```java
// pour l'une passe on construit l'automate de .*R, pour le naif celui de R
```
Ma version :

### 8. `src/Main.java:112`, lecture du fichier
Code : `try (BufferedReader lect =`
```java
// on lit le fichier en UTF-8 pour que les accents et les apostrophes soient bien lus
```
Ma version :

### 9. `src/Main.java:117`, messages sur la sortie d'erreur
Code : `System.err.println();`
```java
// l'algo et le nombre de lignes vont sur stderr pour que stdout ait seulement les lignes, comme grep
```
Ma version :

### 10. `src/Main.java:128`, erreurs
Code : `} catch (Exception e) {`
```java
// regex invalide, fichier introuvable ou texte pas en UTF-8 : on affiche l'erreur sur stderr
```
Ma version :

---

## src/kmp/Kmp.java

### 11. `src/kmp/Kmp.java:21`, au-dessus de `calculerEchec`
Code : `private static int[] calculerEchec(char[] p) {`
```java
// pi[i] = longueur du plus long bord de p[0..i], c'est a dire un debut du motif qui est aussi une fin
```
Ma version :

### 12. `src/kmp/Kmp.java:24`, dans `calculerEchec`
Code : `int k = 0;`
```java
// k = longueur du bord qu'on essaie de prolonger avec le caractere p[i]
```
Ma version :

### 13. `src/kmp/Kmp.java:33`, dans `calculerEchec`
Code : `if (p[i] == p[k]) {`
```java
// le caractere prolonge le bord, donc le bord grandit de 1
```
Ma version :

### 14. `src/kmp/Kmp.java:59`, dans `contientMotif` (la reprise par π)
Code : `while (q > 0 && c != p[q]) {`
```java
// echec : au lieu de repartir de 0, on garde le plus long debut du motif deja lu (pi) et on recompare c
```
Ma version :

### 15. `src/kmp/Kmp.java:63`, dans `contientMotif`
Code : `if (c == p[q]) {`
```java
// le caractere correspond, on avance d'une case dans le motif
```
Ma version :

### 16. `src/kmp/Kmp.java:83`, au-dessus de `motifLitteral`
Code : `public static String motifLitteral(AstI ast) {`
```java
// renvoie le mot si la regex n'a que des lettres a la suite, sinon null (on utilise alors l'automate)
```
Ma version :

### 17. `src/kmp/Kmp.java:90`, au-dessus de `collecter` (utilisée par `motifLitteral`)
Code : `private static boolean collecter(AstI noeud, StringBuilder mot) {`
```java
// on parcourt l'arbre de gauche a droite et on s'arrete des qu'un noeud n'est ni une lettre ni une concatenation
```
Ma version :

---

## src/experimentation/FileSearcher.java

### 18. `src/experimentation/FileSearcher.java:14`, au-dessus de `rechercher`
Code : `public ResultatRecherche rechercher(BufferedReader lecteur)`
```java
// on lit le texte ligne par ligne et on garde celles qui contiennent le motif
```
Ma version :

### 19. `src/experimentation/FileSearcher.java:21`, dans `rechercher`
Code : `while ((ligne = lecteur.readLine()) != null) {`
```java
// readLine enleve deja le retour a la ligne, donc le matcher voit la ligne sans son \n
```
Ma version :

---

## Classes de src/automate/

### 20. `src/automate/ConstructionAutomate.java:6`, au-dessus de la classe
Code : `public class ConstructionAutomate {`

Déjà là (ligne 5) : « cette classe regroupe les differentes etapes pour construire notre afd minimisé a partir de l'AST ». Ta version peut le remplacer.
```java
// enchaine AFN, AFD puis AFD minimal a partir de l'AST, et garde chaque etape
```
Ma version :

### 21. `src/automate/Transition.java:3`, au-dessus de la classe
Code : `public class Transition{`
```java
// une fleche de l'automate : un etat de depart, un etat d'arrivee et le symbole lu
```
Ma version :

### 22. `src/automate/Etat.java:3`, au-dessus de la classe
Code : `public class Etat {`
```java
// un etat n'a aucun contenu : deux etats sont differents simplement parce que ce sont deux objets differents
```
Ma version :

---

## Classes de src/ast/

### 23. `src/ast/AstChar.java:6`
Code : `public class AstChar implements AstI{`
```java
// une feuille de l'arbre : un caractere a lire tel quel
```
Ma version :

### 24. `src/ast/AstConcat.java:6`
Code : `public class AstConcat implements AstI{`
```java
// deux morceaux de regex a la suite : d'abord le fils gauche, puis le fils droit
```
Ma version :

### 25. `src/ast/AstDot.java:6`
Code : `public class AstDot implements AstI{`
```java
// le point : une feuille qui accepte n'importe quel caractere
```
Ma version :

### 26. `src/ast/AstFactory.java:6`
Code : `public class AstFactory implements AstFactoryI{`
```java
// regroupe la creation des noeuds de l'arbre, utilisee par le parser et par MatcherUnePasse
```
Ma version :

### 27. `src/ast/AstOr.java:6`
Code : `public class AstOr implements AstI {`
```java
// l'alternative | : on accepte ce que reconnait le fils gauche ou le fils droit
```
Ma version :

### 28. `src/ast/AstRepeat.java:6`
Code : `public class AstRepeat implements AstI{`
```java
// l'etoile * : son seul fils (rendu comme fils gauche) peut etre repete 0, 1 ou plusieurs fois
```
Ma version :

### 29. `src/ast/AstVide.java:7`
Code : `public class AstVide implements AstI {`
```java
// le mot vide, cree pour une regex vide, a| ou () : il est trouve dans toutes les lignes
```
Ma version :

---

## Tests (tests/)

### 30. `tests/TestCorrection.java:85`, au-dessus du `main`
Code : `public static void main(String[] args) throws Exception {`
```java
// compare naif, une passe et KMP a java.util.regex sur chaque ligne de data, pour chaque regex de inputs
```
Ma version :

### 31. `tests/TestAutomates.java:17`, au-dessus du `main`
Code : `public static void main(String[] args) throws Exception {`
```java
// pour chaque regex, compte les etats des automates et verifie que naif et une passe trouvent les memes lignes
```
Ma version :

### 32. `tests/TestPireCas.java:47`, au-dessus du `main`
Code : `public static void main(String[] args) throws Exception {`
```java
// mesure naif et une passe sur une ligne de a de plus en plus longue (le naif seulement jusqu'a 20000)
```
Ma version :

### 33. `tests/TestPerformance.java:61`, au-dessus du `main`
Code : `public static void main(String[] args) throws Exception {`
```java
// mesure le temps du naif, de l'une passe et de KMP (motifs litteraux) pour chaque regex et chaque texte de data, sans compter la lecture
```
Ma version :
