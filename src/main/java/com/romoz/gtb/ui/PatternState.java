public final class PatternState {
    private static final PatternState INSTANCE = new PatternState();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CFG = FabricLoader.getInstance().getConfigDir().resolve("gtbsolver.json");

    private int length = 0;
    private char[] letters = new char[0];

    public static PatternState get() { return INSTANCE; }

    public synchronized void setLength(int len) {
        if (len < 0) len = 0;
        this.length = len;
        this.letters = Arrays.copyOf(letters, len);
        save();
    }

    public synchronized int getLength() { return length; }

    public synchronized void setChar(int idx, char c) {
        if (idx < 0) return;
        ensureSize(idx + 1);
        letters[idx] = c;
        save();
    }

    public synchronized char getChar(int idx) {
        if (idx < 0 || idx >= letters.length) return '\0';
        return letters[idx];
    }

    public synchronized char[] snapshot() {
        return Arrays.copyOf(letters, letters.length);
    }

    public synchronized void clearAll() {
        Arrays.fill(letters, '\0');
        save();
    }

    public synchronized void runIfSlot(int idx, IntConsumer consumer) {
        if (idx >= 0 && idx < letters.length) consumer.accept(letters[idx]);
    }

    private void ensureSize(int size) {
        if (letters.length < size) {
            letters = Arrays.copyOf(letters, size);
        }
    }

    public synchronized void load() {
        try {
            if (Files.exists(CFG)) {
                var obj = JsonParser.parseString(Files.readString(CFG)).getAsJsonObject();
                this.length = obj.get("length").getAsInt();
                var arr = obj.getAsJsonArray("letters");
                this.letters = new char[length];
                for (int i = 0; i < length && i < arr.size(); i++) {
                    String s = arr.get(i).getAsString();
                    this.letters[i] = (s == null || s.isEmpty()) ? '\0' : s.charAt(0);
                }
            }
        } catch (Exception e) {
            // игнор, создадим заново при save()
        }
    }

    public synchronized void save() {
        try {
            var obj = new JsonObject();
            obj.addProperty("length", length);
            var arr = new JsonArray();
            for (int i = 0; i < length; i++) {
                arr.add(letters[i] == '\0' ? "" : String.valueOf(letters[i]));
            }
            obj.add("letters", arr);
            Files.createDirectories(CFG.getParent());
            Files.writeString(CFG, GSON.toJson(obj), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
    }
}
