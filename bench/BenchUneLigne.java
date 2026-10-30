import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

import ast.interfaces.AstI;

import automate.ConstructionAutomate;

import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;

import parser.Parser;

/**
 * Mesure « livre sur une seule ligne » : matcher naïf contre matcher
 * une passe sur The Yellow Wallpaper, découpé en lignes comme dans le
 * fichier, puis recollé en une seule ligne (les retours à la ligne
 * remplacés par des espaces, en mémoire : aucun fichier n'est écrit
 * dans {@code data/}).
 *
 * <p>Même protocole que {@code Benchmark} : {@value #CHAUFFE}
 * chauffes puis {@value #MESURES} mesures {@code nanoTime}, construction
 * et recherche chronométrées à part, une JVM par matcher. Si une
 * itération dépasse {@value #LIMITE_MS} ms, la configuration s'arrête
 * et le CSV reçoit une ligne de statut « &gt; 10 s ».</p>
 *
 * <p>Lancement (depuis la racine, après {@code ant compile-bench}) :
 * {@code java -Xmx1g -cp bin:bin-bench BenchUneLigne <naif|unepasse>},
 * qui ajoute ses lignes à {@code resultats/bench_une_ligne.csv}.</p>
 */
public class BenchUneLigne {

    private static final int CHAUFFE = 5;
    private static final int MESURES = 20;
    private static final double LIMITE_MS = 10_000;

    private static final Path TEXTE =
        Path.of("data", "yellow-wallpaper-1952.txt");
    private static final Path SORTIE =
        Path.of("resultats", "bench_une_ligne.csv");

    private static final String ENTETE = "regex,forme,nb_lignes_texte,"
        + "longueur_max_ligne,matcher,iteration,t_construction_ms,"
        + "t_recherche_ms,nb_lignes_trouvees,etats_min,statut";

    // .* qui ne trouve rien et commence par la lettre la plus
    // fréquente du texte (e) ; puis une regex sans .*, sans résultat
    private static final String[] REGEX = {"e.*zq", "S(a|g|r)*on"};

    private BenchUneLigne() {
    }

    /**
     * Mesure un matcher sur les deux formes du texte et les deux regex.
     *
     * @param args {@code naif} ou {@code unepasse}
     * @throws IOException si le texte ne peut pas être lu ou le CSV
     *         écrit
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 1
                || !List.of("naif", "unepasse").contains(args[0])) {
            System.err.println("usage : BenchUneLigne <naif|unepasse>");
            System.exit(2);
        }

        String matcher = args[0];
        List<String> lignes =
            Files.readAllLines(TEXTE, StandardCharsets.UTF_8);
        List<String> uneLigne = List.of(String.join(" ", lignes));

        boolean nouveau = !Files.exists(SORTIE) || Files.size(SORTIE) == 0;

        try (BufferedWriter ecrivain = Files.newBufferedWriter(
                SORTIE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             PrintWriter csv = new PrintWriter(ecrivain)) {

            if (nouveau) {
                csv.println(ENTETE);
            }

            for (String r : REGEX) {
                mesurer(csv, r, "lignes", lignes, matcher);
                mesurer(csv, r, "une_ligne", uneLigne, matcher);
            }
        }
    }

    private static void mesurer(PrintWriter csv, String regex, String forme,
            List<String> lignes, String matcher) {

        int longueurMax = 0;
        for (String l : lignes) {
            longueurMax = Math.max(longueurMax, l.length());
        }

        double somme = 0;
        long trouvees = -1;
        int etats = -1;

        for (int i = 0; i < CHAUFFE + MESURES; i++) {

            long t0 = System.nanoTime();
            AstI ast = new Parser(regex).parse();
            if (matcher.equals("unepasse")) {
                ast = MatcherUnePasse.envelopper(ast);
            }
            ConstructionAutomate c = new ConstructionAutomate(ast);
            Matcher m = matcher.equals("naif")
                ? new MatcherNaif(c.getCompile())
                : new MatcherUnePasse(c.getCompile());
            long t1 = System.nanoTime();
            trouvees = compter(m, lignes);
            long t2 = System.nanoTime();

            etats = c.getAfdMinimal().getEtats().size();
            double tConstruction = (t1 - t0) / 1e6;
            double tRecherche = (t2 - t1) / 1e6;

            if (tRecherche > LIMITE_MS) {
                csv.printf(Locale.ROOT, "\"%s\",%s,%d,%d,%s,%d,%.4f,,,%d,"
                    + "\"> 10 s\"%n", regex, forme, lignes.size(),
                    longueurMax, matcher, i + 1, tConstruction, etats);
                System.out.printf(Locale.ROOT,
                    "%-12s %-9s %-8s > 10 s (itération %d), arrêt%n",
                    regex, forme, matcher, i + 1);
                return;
            }

            if (i < CHAUFFE) {
                continue;
            }

            somme += tRecherche;

            csv.printf(Locale.ROOT, "\"%s\",%s,%d,%d,%s,%d,%.4f,%.4f,%d,%d,"
                + "ok%n", regex, forme, lignes.size(), longueurMax,
                matcher, i - CHAUFFE + 1, tConstruction, tRecherche,
                trouvees, etats);
        }

        System.out.printf(Locale.ROOT,
            "%-12s %-9s %-8s recherche %10.3f ms  %d lignes trouvées%n",
            regex, forme, matcher, somme / MESURES, trouvees);
    }

    private static long compter(Matcher m, List<String> lignes) {
        long n = 0;
        for (String ligne : lignes) {
            if (m.contientMotif(ligne)) {
                n++;
            }
        }
        return n;
    }
}
