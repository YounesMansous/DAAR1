package experimentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSearcher {

    private final AfdMatcher matcher;

    public FileSearcher(AfdMatcher matcher) {
        this.matcher = matcher;
    }

    public void rechercher(String chemin, boolean afficher)
            throws IOException {

        long lignesLues = 0;
        long lignesTrouvees = 0;
        long caracteresLus = 0;

        long debut = System.nanoTime();

        try (BufferedReader lecteur = Files.newBufferedReader(
                Path.of(chemin),
                StandardCharsets.UTF_8)) {

            String ligne;

            while ((ligne = lecteur.readLine()) != null) {

                lignesLues++;
                caracteresLus += ligne.length();

                if (matcher.contientMotif(ligne)) {

                    lignesTrouvees++;

                    if (afficher) {
                        System.out.println(
                            lignesLues + ": " + ligne
                        );
                    }
                }
            }
        }

        long fin = System.nanoTime();

        double tempsMs = (fin - debut) / 1_000_000.0;

        System.out.println();
        System.out.println("========== RESULTATS ==========");
        System.out.println("Lignes lues       : " + lignesLues);
        System.out.println("Caracteres lus    : " + caracteresLus);
        System.out.println("Lignes trouvees   : " + lignesTrouvees);

        System.out.printf(
            "Temps d'execution : %.3f ms%n",
            tempsMs
        );
    }
}
