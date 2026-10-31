
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import ast.interfaces.AstI;
import automate.ConstructionAutomate;
import experimentation.FileSearcher;
import experimentation.Matcher;
import experimentation.MatcherNaif;
import experimentation.MatcherUnePasse;
import experimentation.ResultatRecherche;
import kmp.Kmp;
import parser.Parser;

public class Main {

    public static void main(String[] args) {

        String regex;
        String fich = "data/text1.txt";
        String algo = "auto";

        if (args.length == 0) {
            Scanner sc = new Scanner(System.in);

            System.out.print("Regex : ");
            regex = sc.nextLine();

            System.out.print("Fichier (Entrée pour text1.txt) : ");
            String saisie = sc.nextLine();

            if (!saisie.isBlank()) {
                fich = saisie;
            }

            System.out.println();
            System.out.println("Choisir l'algorithme :");
            System.out.println("1 - Automatique");
            System.out.println("2 - KMP");
            System.out.println("3 - Automate une passe");
            System.out.println("4 - Automate naif");
            System.out.print("Choix : ");

            String choix = sc.nextLine();

            switch (choix) {
                case "2": algo = "kmp"; break;
                case "3": algo = "unepasse"; break;
                case "4": algo = "naif"; break;
                default: algo = "auto";
            }
        }
        else {
            regex = args[0];

            if (args.length > 1) {
                fich = args[1];
            }
            if (args.length > 2) {
                algo = args[2];
            }
        }

        try {
            AstI ast = new Parser(regex).parse();
            String motif = Kmp.motifLitteral(ast);

            Matcher matcher;
            String algoUtilise;

            if (algo.equals("auto")) {
                algo = motif != null ? "kmp" : "unepasse";
            }

            if (algo.equals("kmp")) {

                if (motif == null) {
                    System.out.println("KMP ne fonctionne qu'avec un motif litteral.");
                    return;
                }

                matcher = new Kmp(motif);
                algoUtilise = "KMP";
            }
            else if (algo.equals("unepasse") || algo.equals("naif")) {

                AstI astU = algo.equals("unepasse")
                    ? MatcherUnePasse.envelopper(ast)
                    : ast;

                ConstructionAutomate auto = new ConstructionAutomate(astU);

                matcher = algo.equals("unepasse")
                    ? new MatcherUnePasse(auto.getAfdMinimal())
                    : new MatcherNaif(auto.getAfdMinimal());

                algoUtilise = algo.equals("unepasse")
                    ? "Automate une passe"
                    : "Automate naif";
            }
            else {
                System.out.println("Algorithme inconnu : " + algo);
                return;
            }

            FileSearcher rech = new FileSearcher(matcher);
            ResultatRecherche res;

            try (BufferedReader lect =
                    Files.newBufferedReader(Path.of(fich), StandardCharsets.UTF_8)) {

                res = rech.rechercher(lect);
            }
            System.out.println();
            System.out.println("Algorithme utilise : " + algoUtilise);
            System.out.println();

            for (ResultatRecherche.LigneTrouvee l : res.getLignes()) {
                System.out.println(l.getTexte());
            }

            System.out.println();
            System.out.println("Nombre de lignes trouvees : " + res.getNombreTrouvees());

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}
