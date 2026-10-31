import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ast.interfaces.AstI;

import automate.AutomateI;
import automate.ConstructionAutomate;
import automate.Etat;

import parser.Parser;

/**
 * Exporte au format Graphviz (.dot) l'AFN-ε et l'AFD minimal (sans
 * puits) d'une regex, pour les figures du rapport. Les états sont
 * numérotés q0, q1… dans l'ordre de lecture : parcours en largeur
 * depuis l'état initial (q0), les états finaux en dernier. Cette
 * numérotation diffère donc de celle de {@code --debug}. Les
 * transitions qui relient les mêmes états sont regroupées en une
 * seule flèche (« a, e »). Pas de titre dans le graphe : la légende
 * de la figure LaTeX le donne.
 *
 * <p>Lancement (depuis la racine, après {@code ant compile-bench}) :
 * {@code java -cp bin:bin-bench ExportDot <regex> <préfixe>} écrit
 * {@code <préfixe>_afn.dot} et {@code <préfixe>_afd_min.dot}. Si
 * Graphviz est installé : {@code dot -Tpdf f.dot -o f.pdf}.</p>
 */
public class ExportDot {

    private ExportDot() {
    }

    /**
     * Écrit les deux fichiers .dot de la regex.
     *
     * @param args la regex et le préfixe des fichiers de sortie
     * @throws IOException si un fichier ne peut pas être écrit
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("usage : ExportDot <regex> <préfixe>");
            System.exit(2);
        }

        String regex = args[0];
        AstI ast = new Parser(regex).parse();
        ConstructionAutomate c = new ConstructionAutomate(ast);

        ecrire(Path.of(args[1] + "_afn.dot"), dot(c.getAfn()));
        ecrire(Path.of(args[1] + "_afd_min.dot"), dot(c.getAfdMinimal()));
    }

    private static void ecrire(Path fichier, String contenu)
            throws IOException {
        Files.writeString(fichier, contenu, StandardCharsets.UTF_8);
        System.out.println(fichier);
    }

    // États dans l'ordre de lecture : parcours en largeur depuis
    // l'initial (successeurs pris dans l'ordre de getEtats()), états
    // non atteints ensuite, et les états finaux à la fin
    private static List<Etat> ordreDeLecture(AutomateI<?> automate) {

        List<Etat> creation = automate.getEtats();
        List<Etat> parcours = new ArrayList<>();
        Set<Etat> vus = new HashSet<>();
        Deque<Etat> file = new ArrayDeque<>();

        file.add(automate.getInitial());
        vus.add(automate.getInitial());

        while (!file.isEmpty()) {

            Etat e = file.poll();
            parcours.add(e);

            List<Etat> suivants = new ArrayList<>();
            Map<String, ?> depuis = automate.getTransitions().get(e);

            if (depuis != null) {
                for (Object v : depuis.values()) {
                    suivants.addAll(destinations(v));
                }
            }

            suivants.sort((a, b) ->
                Integer.compare(creation.indexOf(a), creation.indexOf(b)));

            for (Etat d : suivants) {
                if (vus.add(d)) {
                    file.add(d);
                }
            }
        }

        for (Etat e : creation) {
            if (vus.add(e)) {
                parcours.add(e);
            }
        }

        List<Etat> ordre = new ArrayList<>();
        for (Etat e : parcours) {
            if (!automate.getFinaux().contains(e)) {
                ordre.add(e);
            }
        }
        for (Etat e : parcours) {
            if (automate.getFinaux().contains(e)) {
                ordre.add(e);
            }
        }

        return ordre;
    }

    // Texte .dot d'un AFN-ε ou d'un AFD
    private static String dot(AutomateI<?> automate) {

        List<Etat> etats = ordreDeLecture(automate);
        StringBuilder sb = new StringBuilder();

        sb.append("digraph automate {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=circle, fontname=\"Helvetica\"];\n");
        sb.append("  edge [fontname=\"Helvetica\"];\n");
        sb.append("  debut [shape=point, width=0];\n");

        for (int i = 0; i < etats.size(); i++) {
            boolean fin = automate.getFinaux().contains(etats.get(i));
            sb.append("  q").append(i)
                .append(fin ? " [shape=doublecircle];\n" : ";\n");
        }

        sb.append("  debut -> q")
            .append(etats.indexOf(automate.getInitial())).append(";\n");

        // (source, destination) -> symboles triés
        Map<Integer, Map<Integer, Set<String>>> fleches = new TreeMap<>();

        for (int i = 0; i < etats.size(); i++) {

            Map<String, ?> depuis =
                automate.getTransitions().get(etats.get(i));

            if (depuis == null) {
                continue;
            }

            for (Map.Entry<String, ?> e : depuis.entrySet()) {
                for (Etat d : destinations(e.getValue())) {
                    fleches.computeIfAbsent(i, k -> new TreeMap<>())
                        .computeIfAbsent(etats.indexOf(d),
                            k -> new TreeSet<>())
                        .add(symbole(e.getKey()));
                }
            }
        }

        for (Map.Entry<Integer, Map<Integer, Set<String>>> s
                : fleches.entrySet()) {
            for (Map.Entry<Integer, Set<String>> d
                    : s.getValue().entrySet()) {
                sb.append("  q").append(s.getKey())
                    .append(" -> q").append(d.getKey())
                    .append(" [label=\"")
                    .append(echapper(String.join(", ", d.getValue())))
                    .append("\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // Destinations : un ensemble d'états (AFN) ou un seul état (AFD)
    private static List<Etat> destinations(Object valeur) {

        List<Etat> liste = new ArrayList<>();

        if (valeur instanceof Etat) {
            liste.add((Etat) valeur);
        } else {
            for (Object o : (Set<?>) valeur) {
                liste.add((Etat) o);
            }
        }

        return liste;
    }

    private static String symbole(String s) {
        return s.equals("EPSILON") ? "ε" : s;
    }

    private static String echapper(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
