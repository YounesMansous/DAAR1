
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

        // sans argument on passe en mode interactif : on demande la regex,
        // le fichier puis l'algo
        if (args.length == 0) {
            Scanner sc = new Scanner(System.in);

            System.out.print("Regex : ");
            regex = sc.nextLine();

            System.out.print("Fichier (Entrée pour text1.txt) : ");
            String saisie = sc.nextLine();

            // Si l"utilisateur appuie juste sur Entree on garde data/text1.txt
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

            // on convertit le numero choisi en nom d'algo, et on prend auto si le choix n'est pas reconu
            switch (choix) {
                case "2": algo = "kmp"; break;
                case "3": algo = "unepasse"; break;
                case "4": algo = "naif"; break;
                default: algo = "auto";
            }
        }
        else {
            // sinon on lit les arguments : la regex, puis le fichier et l'algo s'ils sont donnes
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
            String nomAlgo;

            // en auto on prend KMP si le motif est une simple suite de lettre, sinon l'automate une passe
            if (algo.equals("auto")) {
                algo = motif != null ? "kmp" : "unepasse";
            }

            if (algo.equals("kmp")) {

                // KMP ne sait pas gerer | * . et le mot vide, donc on s'arrete avec un message
                if (motif == null) {
                    System.err.println("KMP ne fonctionne qu'avec un motif litteral.");
                    return;
                }

                matcher = new Kmp(motif);
                nomAlgo = "KMP";
            }
            else if (algo.equals("unepasse") || algo.equals("naif")) {

                // pour l'une passe on construit l'automate de .*R, pour le naif de R
                AstI astU = algo.equals("unepasse")
                    ? MatcherUnePasse.envelopper(ast)
                    : ast;

                ConstructionAutomate cons = new ConstructionAutomate(astU);

                matcher = algo.equals("unepasse")
                    ? new MatcherUnePasse(cons.getAfdMinimal())
                    : new MatcherNaif(cons.getAfdMinimal());

                nomAlgo = algo.equals("unepasse")
                    ? "Automate une passe"
                    : "Automate naif";
            }
            else {
                System.err.println("Algorithme inconnu : " + algo);
                return;
            }

            FileSearcher rech = new FileSearcher(matcher);
            ResultatRecherche res;

            //on lit le fichier en UTF-8 pour que les accents et les apostrophes soit bien lus
            try (BufferedReader lect =
                    Files.newBufferedReader(Path.of(fich), StandardCharsets.UTF_8)) {

                res = rech.rechercher(lect);
            }

            // l'algo et le nombre de lignes vont sur stder pour que stdout ait seulment les lignes trouvees, comme grep
            System.err.println();
            System.err.println("Algorithme utilise : " + nomAlgo);
            System.err.println();

            for (ResultatRecherche.LigneTrouvee l : res.getLignes()) {
                System.out.println(l.getTexte());
            }

            System.err.println();
            System.err.println("Nombre de lignes trouvees : " + res.getNombreTrouvees());

        } catch (Exception e) {
            // regex invalide, fichier introuvable ou texte pas en UTF-8 :on affiche l'erreur sur stderr
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}
