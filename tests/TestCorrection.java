
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import ast.interfaces.AstI;
import automate.ConstructionAutomate;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import kmp.Kmp;
import parser.Parser;

public class TestCorrection {

    private static long total = 0;
    private static long erreurs = 0;
    private static int errExec = 0;
    private static int nbRegex = 0;

    private static void tester(PrintStream out, String regex, String fich,
            int num, String algo, boolean obtenu, boolean attendu) {

        total++;

        if (obtenu != attendu) {
            erreurs++;
            out.printf(
                "ECHEC | regex=%s | fichier=%s | ligne=%d | algo=%s | obtenu=%s | attendu=%s%n",
                regex, fich, num, algo, obtenu, attendu
            );
        }
    }

    private static void verifier(PrintStream out, String regex, Path fich) throws Exception {

        AstI ast = new Parser(regex).parse();
        Pattern ref = Pattern.compile(regex);

        Matcher naif = new MatcherNaif(
            new ConstructionAutomate(ast).getAfdMinimal()
        );

        Matcher uneP = new MatcherUnePasse(
            new ConstructionAutomate(
                MatcherUnePasse.envelopper(ast)
            ).getAfdMinimal()
        );

        String mot = Kmp.motifLitteral(ast);
        Matcher kmp = mot == null ? null : new Kmp(mot);

        List<String> lignes = Files.readAllLines(fich, StandardCharsets.UTF_8);

        long diff = 0;

        for (int i = 0; i < lignes.size(); i++) {

            String s = lignes.get(i);
            boolean attendu = ref.matcher(s).find();

            long avant = erreurs;

            tester(out, regex, fich.getFileName().toString(),
                i + 1, "naif", naif.contientMotif(s), attendu);

            tester(out, regex, fich.getFileName().toString(),
                i + 1, "unePasse", uneP.contientMotif(s), attendu);

            if (kmp != null) {
                tester(out, regex, fich.getFileName().toString(),
                    i + 1, "KMP", kmp.contientMotif(s), attendu);
            }

            diff += erreurs - avant;
        }

        out.printf("%s | %s | lignes=%d | erreurs=%d%n",
            regex, fich.getFileName(), lignes.size(), diff);
    }

    // compare naif, une passe et KMP a java.util.regex sur chaque ligne de data, pour chaque regex de inputs
    public static void main(String[] args) throws Exception {

        List<String> regexs = new ArrayList<>();
        List<Path> fichiers = new ArrayList<>();

        // recuperer les regex de input1 jusqu'a input6
        for (int i = 1; i <= 6; i++) {

            Path fich = Path.of("inputs", "input" + i + ".txt");

            for (String s : Files.readAllLines(fich, StandardCharsets.UTF_8)) {

                if (!s.isBlank() && !s.trim().startsWith("#")) {
                    regexs.add(s);
                }
            }
        }

        // recuperer les fichiers txt de data
        try (var stream = Files.list(Path.of("data"))) {

            fichiers = stream
                .filter(p -> p.toString().endsWith(".txt"))
                .sorted()
                .collect(Collectors.toList());
        }

        try (PrintStream out = SortieTest.ouvrir("correction")) {

            out.println("TEST DE CORRECTION");
            out.println("Comparaison avec java.util.regex.Pattern.find()");
            out.println();

            for (String regex : regexs) {

                nbRegex++;

                for (Path fich : fichiers) {

                    try {
                        verifier(out, regex, fich);
                    }
                    catch (Exception e) {
                        errExec++;

                        out.printf("ERREUR | regex=%s | fichier=%s | %s%n",
                            regex, fich.getFileName(), e.getMessage());
                    }
                }
            }

            out.println();
            out.println("Regex testees : " + nbRegex);
            out.println("Fichiers testes : " + fichiers.size());
            out.println("Comparaisons : " + total);
            out.println("Reussis : " + (total - erreurs));
            out.println("Comparaisons echouees : " + erreurs);
            out.println("Erreurs d'execution : " + errExec);
        }

        System.out.println("TestCorrection terminé");
        System.out.println("Regex : " + nbRegex);
        System.out.println("Fichiers : " + fichiers.size());
        System.out.println("Comparaisons : " + total);
        System.out.println("Erreurs : " + erreurs);
        System.out.println("Erreurs d'execution : " + errExec);

        if (erreurs != 0 || errExec != 0) {
            throw new AssertionError(
                "Tests échoués : " + erreurs
                + " comparaisons et " + errExec + " erreurs d'execution"
            );
        }
    }
}
