import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class AIChatbot {

    // ---------------------------
    // Main method: run GUI
    // ---------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatBotEngine engine = new ChatBotEngine();
            engine.trainDefaultDataset();
            new ChatWindow(engine);
        });
    }

    // ---------------------------
    // Chat engine (preprocess + classifier + rules)
    // ---------------------------
    static class ChatBotEngine {
        private NaiveBayesClassifier nb;
        private RuleMatcher rules;
        private Preprocessor preproc;

        public ChatBotEngine() {
            preproc = new Preprocessor();
            nb = new NaiveBayesClassifier(preproc);
            rules = new RuleMatcher(preproc);
        }

        // Load an embedded default training dataset (intents, example utterances, responses).
        public void trainDefaultDataset() {
            // Each intent: id, examples, responses
            List<Intent> intents = new ArrayList<>();

            intents.add(new Intent("greeting",
                    Arrays.asList("hello", "hi", "hey", "good morning", "good afternoon", "good evening"),
                    Arrays.asList("Hi there! How can I help you today?", "Hello! What can I do for you?")));

            intents.add(new Intent("goodbye",
                    Arrays.asList("bye", "goodbye", "see you", "talk later", "farewell"),
                    Arrays.asList("Goodbye! Have a great day.", "See you later — feel free to come back with more questions.")));

            intents.add(new Intent("thanks",
                    Arrays.asList("thanks", "thank you", "thx", "appreciate it"),
                    Arrays.asList("You're welcome!", "Glad to help!")));

            intents.add(new Intent("hours",
                    Arrays.asList("what are your hours", "working hours", "when are you open", "opening hours"),
                    Arrays.asList("We're open Monday–Friday 9am–5pm.", "Business hours are 9:00 to 17:00, Monday to Friday.")));

            intents.add(new Intent("pricing",
                    Arrays.asList("how much does it cost", "pricing", "what are the prices", "cost of service"),
                    Arrays.asList("We have multiple plans — basic, pro, and enterprise. Which one interests you?",
                                  "Pricing depends on usage; email sales@example.com for a custom quote.")));

            intents.add(new Intent("features",
                    Arrays.asList("what features", "features list", "what can it do", "capabilities"),
                    Arrays.asList("Our bot supports FAQs, small-talk, and simple task automation.",
                                  "It can answer FAQs, route users, and provide basic help.")));

            intents.add(new Intent("install",
                    Arrays.asList("how to install", "installation", "setup guide", "install steps"),
                    Arrays.asList("To install, download the package and run the installer. See the README for details.",
                                  "Installation steps: 1) download, 2) unzip, 3) run setup. Do you need platform-specific help?")));

            intents.add(new Intent("contact",
                    Arrays.asList("how can i contact support", "contact", "support email", "phone number"),
                    Arrays.asList("You can contact support at support@example.com or call +1-555-1234.",
                                  "Send an email to support@example.com and we'll reply within 24 hours.")));

            intents.add(new Intent("help",
                    Arrays.asList("i need help", "help me", "can you help", "support"),
                    Arrays.asList("Of course — tell me what you need help with.", "Sure — what's the issue you're facing?")));

            // Train Naive Bayes classifier
            nb.train(intents);

            // Add rule-based keywords (helpful for exact matching / fallback)
            rules.addRule("order", Arrays.asList("order", "buy", "purchase"), "If you'd like to order, visit our store or tell me what you'd like to buy.");
            rules.addRule("refund", Arrays.asList("refund", "return", "money back"), "To request a refund, please fill the refund form on our site or email refunds@example.com.");
            rules.addRule("security", Arrays.asList("secure", "security", "data protection", "privacy"), "We take data privacy seriously. See our privacy policy at example.com/privacy.");
        }

        // Get response for input text
        public String getResponse(String inputText) {
            if (inputText == null) return defaultFallback();

            String normalized = preproc.normalize(inputText);
            if (normalized.trim().isEmpty()) return "I didn't catch that — can you rephrase?";

            // 1) Try rule-based exact / keyword matching (fast)
            String ruleResp = rules.match(normalized);
            if (ruleResp != null) return ruleResp;

            // 2) Use Naive Bayes classifier
            ClassificationResult res = nb.classify(normalized);
            if (res != null && res.bestIntent != null) {
                // Confidence threshold — if low, use fallback
                if (res.confidence >= 0.35) {
                    // pick a random response from intent
                    return res.bestResponse;
                }
            }

            // 3) fallback small-talk style patterns
            String small = smallTalkFallback(normalized);
            if (small != null) return small;

            // 4) final fallback
            return defaultFallback();
        }

        private String defaultFallback() {
            return "Sorry, I don't know the answer to that yet. Would you like me to connect you to human support or rephrase?";
        }

        private String smallTalkFallback(String normalized) {
            if (normalized.matches(".*\\b(weather|temperature)\\b.*")) {
                return "I can't fetch live weather here, but you can tell me your city and I'll offer general advice.";
            }
            if (normalized.matches(".*\\b(joke|tell me a joke)\\b.*")) {
                return "Why did the programmer quit his job? Because he didn't get arrays. 😄";
            }
            return null;
        }
    }

    // ---------------------------
    // Intent class (training data container)
    // ---------------------------
    static class Intent {
        String id;
        List<String> examples;
        List<String> responses;

        public Intent(String id, List<String> examples, List<String> responses) {
            this.id = id;
            this.examples = examples;
            this.responses = responses;
        }
    }

    // ---------------------------
    // Simple Preprocessor
    // - lowercase, punctuation removal, tokenization, stopword removal, naive stemming
    // ---------------------------
    static class Preprocessor {
        private Set<String> stopwords;
        private Pattern punctuation = Pattern.compile("[^a-z0-9\\s]");

        public Preprocessor() {
            stopwords = new HashSet<>(Arrays.asList(
                    "a","an","the","is","are","am","i","you","it","we","they","of","in","on","for","to","and","or","do","does","did","me","my","your"
            ));
        }

        public String normalize(String text) {
            if (text == null) return "";
            String low = text.toLowerCase(Locale.ROOT);
            // remove URLs
            low = low.replaceAll("https?://\\S+\\s?", " ");
            low = punctuation.matcher(low).replaceAll(" ");
            low = low.replaceAll("\\s+", " ").trim();
            return low;
        }

        public List<String> tokenize(String normalized) {
            String[] parts = normalized.split("\\s+");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                if (p == null) continue;
                p = p.trim();
                if (p.isEmpty()) continue;
                if (stopwords.contains(p)) continue;
                String s = simpleStem(p);
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        }

        // Very light-weight stemmer: remove common suffixes
        private String simpleStem(String tok) {
            if (tok.length() <= 3) return tok;
            // common endings
            if (tok.endsWith("ing") && tok.length() > 4) return tok.substring(0, tok.length() - 3);
            if (tok.endsWith("ed") && tok.length() > 3) return tok.substring(0, tok.length() - 2);
            if (tok.endsWith("es") && tok.length() > 3) return tok.substring(0, tok.length() - 2);
            if (tok.endsWith("s") && tok.length() > 3) return tok.substring(0, tok.length() - 1);
            return tok;
        }

        // Helper used by RuleMatcher for normalizing keyword phrases
        public static String simpleLowerTrim(String s) {
            if (s == null) return "";
            String out = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", "").trim();
            out = out.replaceAll("\\s+", " ");
            return out;
        }
    }

    // ---------------------------
    // Naive Bayes classifier (multinomial) implemented from scratch
    // ---------------------------
    static class NaiveBayesClassifier {
        private Preprocessor preproc;

        // Data structures
        private Map<String, Map<String, Integer>> tokenCountsByIntent; // intent -> token -> count
        private Map<String, Integer> totalTokensByIntent;               // intent -> total token count
        private Map<String, Integer> docCountsByIntent;                 // intent -> number of training examples
        private int totalDocs;
        private Set<String> vocabulary;

        private Map<String, List<String>> intentResponses; // intent -> responses

        public NaiveBayesClassifier(Preprocessor preproc) {
            this.preproc = preproc;
            tokenCountsByIntent = new HashMap<>();
            totalTokensByIntent = new HashMap<>();
            docCountsByIntent = new HashMap<>();
            vocabulary = new HashSet<>();
            intentResponses = new HashMap<>();
            totalDocs = 0;
        }

        // Train with list of intents
        public void train(List<Intent> intents) {
            // reset
            tokenCountsByIntent.clear();
            totalTokensByIntent.clear();
            docCountsByIntent.clear();
            vocabulary.clear();
            intentResponses.clear();
            totalDocs = 0;

            for (Intent intent : intents) {
                Map<String, Integer> counts = tokenCountsByIntent.computeIfAbsent(intent.id, k -> new HashMap<>());
                int docCount = docCountsByIntent.getOrDefault(intent.id, 0);
                for (String ex : intent.examples) {
                    totalDocs++;
                    docCount++;
                    String norm = preproc.normalize(ex);
                    List<String> toks = preproc.tokenize(norm);
                    for (String t : toks) {
                        vocabulary.add(t);
                        counts.put(t, counts.getOrDefault(t, 0) + 1);
                        totalTokensByIntent.put(intent.id, totalTokensByIntent.getOrDefault(intent.id, 0) + 1);
                    }
                }
                tokenCountsByIntent.put(intent.id, counts);
                docCountsByIntent.put(intent.id, docCount);
                intentResponses.put(intent.id, intent.responses);
            }
        }

        // Classify input, return best intent + confidence and a response
        public ClassificationResult classify(String normalizedText) {
            List<String> toks = preproc.tokenize(normalizedText);
            if (toks.isEmpty()) return null;

            double bestScore = Double.NEGATIVE_INFINITY;
            String bestIntent = null;

            // compute log-probabilities
            for (String intent : tokenCountsByIntent.keySet()) {
                double logPrior = Math.log((double) docCountsByIntent.getOrDefault(intent, 0) + 1) - Math.log(totalDocs + tokenCountsByIntent.size());
                double logLikelihood = 0.0;

                Map<String, Integer> counts = tokenCountsByIntent.getOrDefault(intent, new HashMap<>());
                int totalTokens = totalTokensByIntent.getOrDefault(intent, 0);
                // add-one smoothing
                for (String tok : toks) {
                    int tc = counts.getOrDefault(tok, 0);
                    double prob = (tc + 1.0) / (totalTokens + Math.max(1, vocabulary.size()));
                    logLikelihood += Math.log(prob);
                }

                double score = logPrior + logLikelihood;
                if (score > bestScore) {
                    bestScore = score;
                    bestIntent = intent;
                }
            }

            // Rough confidence: compare best score to next best
            double secondBest = Double.NEGATIVE_INFINITY;
            for (String intent : tokenCountsByIntent.keySet()) {
                if (intent.equals(bestIntent)) continue;
                double logPrior = Math.log((double) docCountsByIntent.getOrDefault(intent, 0) + 1) - Math.log(totalDocs + tokenCountsByIntent.size());
                double logLikelihood = 0.0;
                Map<String, Integer> counts = tokenCountsByIntent.getOrDefault(intent, new HashMap<>());
                int totalTokens = totalTokensByIntent.getOrDefault(intent, 0);
                for (String tok : toks) {
                    int tc = counts.getOrDefault(tok, 0);
                    double prob = (tc + 1.0) / (totalTokens + Math.max(1, vocabulary.size()));
                    logLikelihood += Math.log(prob);
                }
                double score = logPrior + logLikelihood;
                if (score > secondBest) secondBest = score;
            }

            double confidence;
            if (secondBest == Double.NEGATIVE_INFINITY) {
                // only one class
                confidence = 1.0;
            } else {
                // logistic-like mapping of margin to 0..1
                double margin = bestScore - secondBest;
                confidence = 1.0 - Math.exp(-Math.abs(margin));
                // clamp
                if (confidence < 0) confidence = 0;
                if (confidence > 1) confidence = 1;
            }

            String resp = pickResponseForIntent(bestIntent);
            return new ClassificationResult(bestIntent, confidence, resp);
        }

        private String pickResponseForIntent(String intent) {
            List<String> res = intentResponses.getOrDefault(intent, Collections.singletonList("Okay."));
            // choose random response for variety
            return res.get(new Random().nextInt(res.size()));
        }
    }

    // ---------------------------
    // Classification result wrapper
    // ---------------------------
    static class ClassificationResult {
        String bestIntent;
        double confidence;
        String bestResponse;

        public ClassificationResult(String bestIntent, double confidence, String bestResponse) {
            this.bestIntent = bestIntent;
            this.confidence = confidence;
            this.bestResponse = bestResponse;
        }
    }

    // ---------------------------
    // Rule-based matcher (keyword -> response)
    // ---------------------------
    static class RuleMatcher {
        private Map<String, List<String>> rulesKeywords;
        private Map<String, String> rulesResponse;
        private Preprocessor preproc;

        public RuleMatcher(Preprocessor preproc) {
            this.preproc = preproc;
            rulesKeywords = new HashMap<>();
            rulesResponse = new HashMap<>();
        }

        public void addRule(String id, List<String> keywords, String response) {
            List<String> norm = new ArrayList<>();
            for (String k : keywords) norm.add(Preprocessor.simpleLowerTrim(k));
            rulesKeywords.put(id, norm);
            rulesResponse.put(id, response);
        }

        // return response if any rule matched, else null
        public String match(String normalized) {
            List<String> toks = preproc.tokenize(normalized);
            Set<String> set = new HashSet<>(toks);
            String normalizedForContains = normalized; // already normalized by preproc before calling
            for (String id : rulesKeywords.keySet()) {
                List<String> kws = rulesKeywords.get(id);
                // simple ANY match
                for (String kw : kws) {
                    if (kw == null || kw.isEmpty()) continue;
                    if (set.contains(kw) || normalizedForContains.contains(kw)) {
                        return rulesResponse.get(id);
                    }
                }
            }
            return null;
        }
    }

    // ---------------------------
    // GUI
    // ---------------------------
    static class ChatWindow extends JFrame {
        private JTextArea chatArea;
        private JTextField inputField;
        private JButton sendButton;
        private ChatBotEngine engine;

        public ChatWindow(ChatBotEngine engine) {
            super("AI Chatbot — Java (Simple NLP + Naive Bayes)");
            this.engine = engine;
            initComponents();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(700, 500);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void initComponents() {
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(chatArea);

            // autoscroll
            DefaultCaret caret = (DefaultCaret)chatArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

            inputField = new JTextField();
            sendButton = new JButton("Send");

            JPanel bottom = new JPanel(new BorderLayout(6,6));
            bottom.add(inputField, BorderLayout.CENTER);
            bottom.add(sendButton, BorderLayout.EAST);

            JLabel hint = new JLabel("Type a question (e.g., 'how to install', 'pricing', 'hello').");
            hint.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

            Container c = getContentPane();
            c.setLayout(new BorderLayout(6,6));
            c.add(hint, BorderLayout.NORTH);
            c.add(scroll, BorderLayout.CENTER);
            c.add(bottom, BorderLayout.SOUTH);

            // menu
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("Options");
            JMenuItem clear = new JMenuItem("Clear chat");
            JMenuItem sample = new JMenuItem("Show example questions");
            menu.add(sample);
            menu.add(clear);
            menuBar.add(menu);
            setJMenuBar(menuBar);

            // action handlers
            sendButton.addActionListener(e -> sendMessage());
            inputField.addActionListener(e -> sendMessage());
            clear.addActionListener(e -> chatArea.setText(""));
            sample.addActionListener(e -> {
                appendBot("Try: \"hello\", \"how to install\", \"pricing\", \"how can I contact support\", \"what features\".");
            });

            // welcome message
            appendBot("Hello! I'm a Java-based chatbot. Ask me about installation, pricing, contact, or say hello.");
        }

        private void sendMessage() {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            appendUser(text);
            inputField.setText("");

            // Get reply synchronously
            String reply = engine.getResponse(text);
            appendBot(reply);
        }

        private void appendUser(String s) {
            chatArea.append("\nYou: " + s + "\n");
        }

        private void appendBot(String s) {
            chatArea.append("\nBot: " + s + "\n");
        }
    }
}
