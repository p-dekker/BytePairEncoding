import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ParallelBPETokenizer {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int VOCABULARY_SIZE = 1000;

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        String input = exampleText();
        System.out.println("original: " + input);

        String text = normalizeText(input);
        //Step 1: Normalize text;
        System.out.println(text);

        List<Token> tokens = tokenizeToBytes(text);
        System.out.println("Initial Tokens: " + tokens);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        int startCorpusSize;
        do {
            startCorpusSize = tokens.size();
            System.out.println("corpus size: " + startCorpusSize);
            tokens = compress(tokens, executor);
        } while (tokens.size() < startCorpusSize);

        System.out.println("Final Tokens: " + tokens);
    }

    public static String normalizeText(String text) {
        // Step 1: Unicode Normalization (NFC)
        return Normalizer.normalize(text, Normalizer.Form.NFC)
                .toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", "")
                .replaceAll("\\s", " ").trim();
    }

    public static List<Token> tokenizeToBytes(String text) {
        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);

        List<Token> list = new ArrayList<>();

        for (byte b : utf8Bytes) {
            list.add(new Token(b));
        }
        return list;
    }

    private static List<Token> compress(List<Token> tokens, ExecutorService executor) throws InterruptedException, ExecutionException {
        Vocabulary vocabulary = new Vocabulary(VOCABULARY_SIZE);
        vocabulary.addAll(tokens);

        Map<Pair, Integer> pairFrequencies = countPairFrequencies(tokens, executor);
        var bestPairs = pairFrequencies.entrySet().stream().collect(new MaxValueCollector());

        if (bestPairs.isEmpty()) {
            System.out.println("done compression");
            return tokens; //we are at max compression;
        }


        var merged = bestPairs.stream().map(p -> p.merge()).collect(Collectors.toSet());

        if (!vocabulary.addAll(merged)) {
            System.out.println("Maximum size of vocabulary");
            return tokens;
        }
        tokens = applyBPE(tokens, bestPairs);

        return tokens;
    }

    private static class MaxValueCollector implements Collector<Map.Entry<Pair, Integer>, Map<Pair, Integer>, Set<Pair>> {

        int maxValue = Integer.MIN_VALUE;

        public Supplier<Map<Pair, Integer>> supplier() {
            return HashMap::new;
        }

        @Override
        public BiConsumer<Map<Pair, Integer>, Map.Entry<Pair, Integer>> accumulator() {
            return (m, e) -> {
                if (e.getValue() > maxValue) {
                    m.clear();
                    maxValue = Math.max(2, e.getValue());
                }
                if (e.getValue() == maxValue) m.put(e.getKey(), e.getValue());
            };
        }

        @Override
        public BinaryOperator<Map<Pair, Integer>> combiner() {
            return (m1, m2) -> {
                int v1 = m1.values().stream().findAny().orElse(Integer.MIN_VALUE);
                int v2 = m2.values().stream().findAny().orElse(Integer.MIN_VALUE);
                if (v1 == v2) {
                    m1.entrySet().addAll(m2.entrySet());
                    return m1;
                }
                return v1 < v2 ? m2 : m1;
            };
        }

        @Override
        public Function<Map<Pair, Integer>, Set<Pair>> finisher() {
            return (m) -> m.keySet();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    // Parallel frequency counting
    private static Map<Pair, Integer> countPairFrequencies(List<Token> tokens, ExecutorService executor) throws InterruptedException, ExecutionException {
        int chunkSize = (tokens.size() / THREAD_COUNT) + 1;
        List<Callable<Map<Pair, Integer>>> tasks = new ArrayList<>();

        for (int i = 0; i < tokens.size() - 1; i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, tokens.size() - 1);
            tasks.add(() -> countFrequencies(tokens.subList(start, end + 1)));
        }

        // Execute tasks in parallel
        List<Future<Map<Pair, Integer>>> results = executor.invokeAll(tasks);
        Map<Pair, Integer> mergedFrequencies = new HashMap<>();

        // Merge frequency counts from all threads
        for (Future<Map<Pair, Integer>> result : results) {
            for (Map.Entry<Pair, Integer> entry : result.get().entrySet()) {
                mergedFrequencies.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return mergedFrequencies;
    }

    // Count adjacent token pair frequencies
    private static Map<Pair, Integer> countFrequencies(List<Token> tokens) {
        Map<Pair, Integer> pairFrequencies = new HashMap<>();
        for (int j = 0; j < tokens.size() - 1; j++) {
            Pair pair = new Pair(tokens.get(j), tokens.get(j + 1));
            pairFrequencies.put(pair, pairFrequencies.getOrDefault(pair, 0) + 1);
        }
        return pairFrequencies;
    }

    /**
     * @param tokens
     * @param mergedPair
     * @return
     */
    // Apply BPE merges to token list
    private static List<Token> applyBPE(List<Token> tokens, Set<Pair> mergedPair) {
        List<Token> newTokens = new ArrayList<>();
        int i = 0;

        while (i < tokens.size()) {
            if (i < tokens.size() - 1) {
                Pair pair = new Pair(tokens.get(i), tokens.get(i + 1));
                if (mergedPair.contains(pair)) {
                    newTokens.add(pair.merge());
                    i += 2; // Skip merged pair
                    continue;
                }
            }
            newTokens.add(tokens.get(i));
            i++;
        }

        return newTokens;
    }

    public static String exampleText() {
        return """
                In een klein dorpje aan de rand van een uitgestrekt bos leefde een oude man genaamd Hendrik. Hij was al decennialang de houtbewerker van het dorp en stond bekend om zijn vakmanschap. Elke ochtend, nog voor de zon opkwam, liep hij naar zijn werkplaats, een kleine schuur naast zijn huis. Daar begon hij met het schaven van planken, het snijden van patronen en het samenstellen van meubels die hij met liefde en precisie maakte.
                Het dorp waarin hij woonde, was vredig en stil. De mensen kenden elkaar allemaal, en het leven ging er in een rustig tempo. Kinderen speelden op de dorpspleinen, terwijl ouderen op bankjes zaten en verhalen vertelden over vroeger. Iedereen had zijn eigen rol in de gemeenschap: de bakker bakte brood, de smid smeedde ijzer, en Hendrik maakte de mooiste houten meubels.
                Op een dag kwam een vreemdeling naar het dorp. Hij had een lange mantel en droeg een grote rugzak. De man stelde zich voor als een reiziger die op zoek was naar bijzondere ambachtslieden. Hij had gehoord over Hendriks talent en wilde graag een stoel door hem laten maken. Maar niet zomaar een stoel – het moest een meesterwerk worden, iets dat generaties lang zou meegaan.
                Hendrik nam de opdracht aan en begon met zijn werk. Hij koos het beste hout, zorgvuldig gedroogd en zonder onvolkomenheden. Met vaste hand begon hij te zagen, schaven en beitelen. Dag en nacht werkte hij aan het project, elk detail kreeg zijn volledige aandacht. De dorpsbewoners kwamen af en toe kijken en bewonderden zijn vakmanschap.
                Na weken van hard werken was de stoel eindelijk klaar. Het was een waar kunstwerk, met prachtige versieringen en een afwerking die perfectie benaderde. De reiziger was onder de indruk en betaalde Hendrik royaal. Maar belangrijker nog, hij beloofde het verhaal van Hendriks vakmanschap door te vertellen aan andere dorpen en steden.
                Na het vertrek van de reiziger ging het leven in het dorp gewoon door. Maar Hendrik wist dat zijn naam nu buiten de grenzen van het dorp bekend zou worden. Zijn meubels zouden niet alleen in zijn kleine gemeenschap gewaardeerd worden, maar ook ver daarbuiten. En zo bleef hij dag in, dag uit zijn passie volgen, tevreden met het eenvoudige maar bevredigende leven dat hij leidde.
                """;
    }
}

