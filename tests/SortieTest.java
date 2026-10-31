import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SortieTest {
    public static PrintStream ouvrir(String nom) throws IOException {
        Path dir = Path.of("resultats");
        Files.createDirectories(dir);
        Path fichier = dir.resolve(nom + ".txt");
        System.out.println("Résultat enregistré : " + fichier);
        return new PrintStream(Files.newOutputStream(fichier), true, StandardCharsets.UTF_8);
    }
}
