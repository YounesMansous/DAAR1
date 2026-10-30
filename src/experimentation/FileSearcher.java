package experimentation;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Parcourt un texte ligne par ligne et garde les lignes
 * qui contiennent le motif, avec le {@link Matcher} choisi.
 * N'affiche rien : c'est l'appelant ({@code Main}) qui décide quoi
 * faire du résultat.
 */
public class FileSearcher {

    private final Matcher matcher;

    /**
     * Crée un parcours de texte qui utilise le matcher donné.
     *
     * @param matcher la stratégie de recherche (naïf, une passe ou KMP)
     */
    public FileSearcher(Matcher matcher) {
        this.matcher = matcher;
    }

    /**
     * Lit tout le texte et teste chaque ligne.
     *
     * @param lecteur texte à parcourir (fichier ou entrée standard),
     *                déjà décodé en UTF-8
     * @return les lignes trouvées et les compteurs de lecture
     * @throws IOException en cas d'erreur de lecture
     */
    public ResultatRecherche rechercher(BufferedReader lecteur)
            throws IOException {

        ResultatRecherche resultat = new ResultatRecherche();

        String ligne;

        while ((ligne = lecteur.readLine()) != null) {
            resultat.ajouterLigneLue(ligne, matcher.contientMotif(ligne));
        }

        return resultat;
    }
}
