
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

public class TestAutomates {

    public static void main(String[] args) throws Exception {

        int erreurs = 0;
        int nbRegex = 0;
        long nbTests = 0;

        List<String> regexs = new ArrayList<>();
        List<String> lignes = new ArrayList<>();

        // recuperer les regex de input1 jusqu'a input6
        for (int i = 1; i <= 6; i++) {

            Path fich = Path.of("inputs", "input" + i + ".txt");

            for (String s : Files.readAllLines(fich, StandardCharsets.UTF_8)) {
                if (!s.isBlank() && !s.trim().startsWith("#")) {
                    regexs.add(s);
                }
            }
        }

        // on recupere les lignes de nos fichiers data
        try (var fichiers = Files.list(Path.of("data"))) {

            for (Path fich : fichiers
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted().toList()) {

                lignes.addAll(Files.readAllLines(fich, StandardCharsets.UTF_8));
            }
        }

        try (PrintStream out = SortieTest.ouvrir("automates")) {

            out.println("TEST DES AUTOMATES");
            out.println("Regex | AFN | AFD | AFD minimal | Divergences");
            out.println();

            for (String regex : regexs) {

                try {
                    AstI ast = new Parser(regex).parse();
                    ConstructionAutomate cons = new ConstructionAutomate(ast);

                    Matcher naif = new MatcherNaif(cons.getAfdMinimal());

                    AstI astU = MatcherUnePasse.envelopper(ast);
                    ConstructionAutomate consU = new ConstructionAutomate(astU);
                    Matcher uneP = new MatcherUnePasse(consU.getAfdMinimal());

                    int diff = 0;

                    for (String s : lignes) {

                        if (naif.contientMotif(s) != uneP.contientMotif(s)) {
                            diff++;
                        }
                        nbTests++;
                    }

                    erreurs += diff;
                    nbRegex++;

                    out.printf("%s | %d | %d | %d | %d%n",
                        regex,
                        cons.getAfn().getEtats().size(),
                        cons.getAfd().getEtats().size(),
                        cons.getAfdMinimal().getEtats().size(),
                        diff
                    );

                } catch (Exception e) {
                    erreurs++;
                    out.println(regex + " | ERREUR : " + e.getMessage());
                }
            }

            out.println();
            out.println("Regex testees : " + nbRegex);
            out.println("Comparaisons : " + nbTests);
            out.println("Erreurs / divergences : " + erreurs);
        }

        System.out.println("TestAutomates terminé");
        System.out.println("Regex testees : " + nbRegex);
        System.out.println("Comparaisons : " + nbTests);
        System.out.println("Erreurs : " + erreurs);

        if (erreurs != 0) {
            throw new AssertionError("Des erreurs ont été trouvées : " + erreurs);
        }
    }
}
