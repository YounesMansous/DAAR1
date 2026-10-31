
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import ast.interfaces.AstI;
import automate.ConstructionAutomate;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import parser.Parser;

public class TestPireCas {

    private static double mesurer(Matcher m, String ligne) {

        long deb = System.nanoTime();

        return (System.nanoTime() - deb) / 1_000_000.0;
    }

    private static double[] mesurerPlusieurs(Matcher m, String ligne) {

        for (int i = 0; i < 3; i++) {
            mesurer(m, ligne);
        }

        int reps = 10;
        double somme = 0, somme2 = 0;

        for (int i = 0; i < reps; i++) {

            double t = mesurer(m, ligne);
            somme += t;
            somme2 += t * t;
        }

        double moy = somme / reps;
        double ecart = Math.sqrt(Math.max(0, somme2 / reps - moy * moy));

        return new double[] {moy, ecart};
    }

    public static void main(String[] args) throws Exception {

        List<String> regexs = new ArrayList<>();

        // recuperer les regex de input1 jusqu'a input6
        for (int i = 1; i <= 6; i++) {

            Path fich = Path.of("inputs", "input" + i + ".txt");

            for (String s : Files.readAllLines(fich, StandardCharsets.UTF_8)) {

                if (!s.isBlank() && !s.trim().startsWith("#")) {
                    regexs.add(s);
                }
            }
        }

        int[] tailles = {1000, 3000, 6000, 10000, 20000, 200000, 1000000};

        try (PrintStream out = SortieTest.ouvrir("pire_cas")) {

            out.println("TEST DES CAS DEFAVORABLES");
            out.println("Texte compose uniquement de a");
            out.println("3 chauffes et 10 mesures par configuration");
            out.println();

            out.println("regex\ttaille\tnaif_moy_ms\tnaif_ecart_ms\tunePasse_moy_ms\tunePasse_ecart_ms\ttrouve_naif\ttrouve_unePasse");

            for (String regex : regexs) {

                try {
                    AstI ast = new Parser(regex).parse();

                    Matcher naif = new MatcherNaif(
                        new ConstructionAutomate(ast).getAfdMinimal()
                    );

                    Matcher uneP = new MatcherUnePasse(
                        new ConstructionAutomate(
                            MatcherUnePasse.envelopper(ast)
                        ).getAfdMinimal()
                    );

                    for (int n : tailles) {

                        String ligne = "a".repeat(n);

                        boolean b1 = naif.contientMotif(ligne);
                        boolean b2 = uneP.contientMotif(ligne);

                        double[] t1 = null;

                        // ne pas lancer le naif sur les tres grandes tailles
                        if (n <= 20000) {
                            t1 = mesurerPlusieurs(naif, ligne);
                        }

                        double[] t2 = mesurerPlusieurs(uneP, ligne);

                        out.printf(Locale.ROOT,
                            "%s\t%d\t%s\t%s\t%.4f\t%.4f\t%b\t%b%n",
                            regex, n,
                            t1 == null ? "NA" : String.format(Locale.ROOT, "%.4f", t1[0]),
                            t1 == null ? "NA" : String.format(Locale.ROOT, "%.4f", t1[1]),
                            t2[0], t2[1],
                            b1, b2
                        );

                        System.out.println("Terminé : " + regex + " / " + n);
                    }

                } catch (Exception e) {
                    out.println("ERREUR\t" + regex + "\t" + e.getMessage());
                }
            }
        }

        System.out.println("TestPireCas terminé");
    }
}
