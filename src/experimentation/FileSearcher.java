package experimentation;

import java.io.BufferedReader;
import java.io.IOException;

public class FileSearcher {

    private final Matcher matcher;

    public FileSearcher(Matcher matcher) {
        this.matcher = matcher;
    }

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
