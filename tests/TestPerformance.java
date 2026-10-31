
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import ast.interfaces.AstI;
import automate.ConstructionAutomate;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import kmp.Kmp;
import parser.Parser;

public class TestPerformance {

    private static volatile long garde;

    private static double mesurer(Matcher m, List<String> lignes) {
        long deb = System.nanoTime();
        long nb = 0;

        for (String l : lignes) {
            if (m.contientMotif(l)) nb++;
        }

        garde = nb;
        return (System.nanoTime() - deb) / 1_000_000.0;
    }

    private static void mesurerPlusieurs(PrintStream out, String fich,
            String regex, String algo, Matcher m, List<String> lignes,
            long taille, double construction) {

        for (int i = 0; i < 3; i++) {
            mesurer(m, lignes);
        }

        int reps = 10;
        double somme = 0, somme2 = 0;
        long trouves = 0;

        for (int i = 0; i < reps; i++) {

            double t = mesurer(m, lignes);
            somme += t;
            somme2 += t * t;
            trouves = garde;
        }

        double moy = somme / reps;
        double ecart = Math.sqrt(Math.max(0, somme2 / reps - moy * moy));

        out.printf(Locale.ROOT,
            "%s\t%s\t%s\t%d\t%d\t%d\t%.4f\t%.4f\t%.4f%n",
            fich, regex, algo, taille, lignes.size(),
            trouves, construction, moy, ecart);
    }

    public static void main(String[] args) throws Exception {

        List<String> regexs = new ArrayList<>();
        List<Path> fichiers;

        // recuperer les regex depuis input1 jusqu'a input6
        for (int i = 1; i <= 6; i++) {

            Path fich = Path.of("inputs", "input" + i + ".txt");

            for (String s : Files.readAllLines(fich, StandardCharsets.UTF_8)) {
                if (!s.isBlank() && !s.trim().startsWith("#")) {
                    regexs.add(s);
                }
            }
        }

        // recuperer les fichiers txt du dossier data
        try (var stream = Files.list(Path.of("data"))) {

            fichiers = stream
                .filter(p -> p.toString().endsWith(".txt"))
                .sorted()
                .toList();
        }

        try (PrintStream out = SortieTest.ouvrir("performance")) {

            out.println("TEST DE PERFORMANCE");
            out.println("3 chauffes et 10 mesures par configuration");
            out.println("Lecture des fichiers exclue du temps de recherche");
            out.println();

            out.println("fichier\tregex\talgo\tcaracteres\tlignes\ttrouvees\tconstruction_ms\tmoyenne_ms\tecart_type_ms");

            for (Path fich : fichiers) {

                List<String> lignes =
                    Files.readAllLines(fich, StandardCharsets.UTF_8);

                long taille = 0;

                for (String l : lignes) {
                    taille += l.length();
                }

                for (String regex : regexs) {

                    try {
                        AstI ast = new Parser(regex).parse();

                        long deb = System.nanoTime();
                        ConstructionAutomate cons = new ConstructionAutomate(ast);
                        Matcher naif = new MatcherNaif(cons.getAfdMinimal());
                        double consNaif = (System.nanoTime() - deb) / 1_000_000.0;

                        deb = System.nanoTime();
                        AstI astU = MatcherUnePasse.envelopper(ast);
                        ConstructionAutomate consU = new ConstructionAutomate(astU);
                        Matcher uneP = new MatcherUnePasse(consU.getAfdMinimal());
                        double consUneP = (System.nanoTime() - deb) / 1_000_000.0;

                        String lit = Kmp.motifLitteral(ast);

                        if (lit != null) {
                            deb = System.nanoTime();
                            Matcher kmp = new Kmp(lit);
                            double consKmp = (System.nanoTime() - deb) / 1_000_000.0;

                            mesurerPlusieurs(out, fich.getFileName().toString(),
                                regex, "KMP", kmp, lignes, taille, consKmp);
                        }

                        mesurerPlusieurs(out, fich.getFileName().toString(),
                            regex, "naif", naif, lignes, taille, consNaif);

                        mesurerPlusieurs(out, fich.getFileName().toString(),
                            regex, "unePasse", uneP, lignes, taille, consUneP);

                    } catch (Exception e) {
                        out.println("ERREUR\t" + fich.getFileName()
                            + "\t" + regex + "\t" + e.getMessage());
                    }
                }

                System.out.println("Terminé : " + fich.getFileName());
            }
        }

        System.out.println("TestPerformance terminé");
    }
}
