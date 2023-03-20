import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Test {
    private static Bar[] bars;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) return;
        File file = new File(args[0]);
        if (!file.exists()) return;
        List<String[]> metadatas = new LinkedList<>();
        List<String[]> scoredatas = new LinkedList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            String scoreRegex = "^#(\\w+):\\s*(.*)$";
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("#")) continue;
                if (!line.matches(scoreRegex)) {
                    String[] qwq = new String[2];
                    int a = line.indexOf(" ");
                    qwq[0] = line.substring(1, a);
                    qwq[1] = line.substring(a + 1);
                    metadatas.add(qwq);
                } else {
                    String[] qwq = new String[2];
                    int a = line.indexOf(":");
                    qwq[0] = line.substring(1, a);
                    qwq[1] = line.substring(a + 1).trim();
                    scoredatas.add(qwq);
                }
            }
        }
        processScore(scoredatas, metadatas);
    }

    private static void processScore(List<String[]> scoredatas, List<String[]> metadatas) {
        Metadata metadata = process_metadata(metadatas);

        String ticks_per_beat = metadata.requests.get("ticks_per_beat");
        int ticksPerBeat = 480;
        try {
            ticksPerBeat = Integer.parseInt(ticks_per_beat);
        } catch (NumberFormatException e) {
            System.out.println("Warning: No ticks_per_beat request found, defaulting to 480.");
        }

        List<String[]> bar_lengths = new LinkedList<>();
        for (String[] strs :
                scoredatas) {
            if (strs[0].length() == 5 && strs[0].endsWith("02") && isInteger(strs[0])) {
                bar_lengths.add(new String[]{strs[0].substring(0, 3), strs[1]});
            }
        }
        if (bar_lengths.size() == 0) {
            System.out.println("No bar lengths found, adding default 4/4 time signature (#00002:4)...");
            bar_lengths.add(new String[]{"0", "4.0"});
        }
        bar_lengths.sort(Comparator.comparingInt(o -> Integer.parseInt(o[0])));

        int ticks = 0;
        List<Bar> bars = new LinkedList<>();
        for (int i = 0; i < bar_lengths.size(); i++) {
            String[] bar_length = bar_lengths.get(i);
            int measure = Integer.parseInt(bar_length[0]);
            float beats = Float.parseFloat(bar_length[1]);
            if (i > 0) {
                bars.add(new Bar(measure, (int) beats * ticksPerBeat,
                        ticks += measure - Integer.parseInt(bar_lengths.get(i - 1)[0]) * Float.parseFloat(bar_lengths.get(i - 1)[1]) * ticksPerBeat));
            } else {
                bars.add(new Bar(measure, (int) beats * ticksPerBeat, ticks));
            }
        }
        Test.bars = bars.toArray(new Bar[0]);
    }

    private static Metadata process_metadata(List<String[]> lines) {
        Metadata result = new Metadata();
        for (String[] line : lines) {
            String key, value;
            if (line.length == 2) {
                key = line[0];
                value = line[1];
            } else {
                key = line[0];
                value = null;
            }
            value = value != null ? customTrim(value, "\"") : "";
            switch (key) {
                case "TITLE":
                    result.title = value;
                    break;
                case "SUBTITLE":
                    result.subtitle = value;
                    break;
                case "ARTIST":
                    result.artist = value;
                    break;
                case "GENRE":
                    result.genre = value;
                    break;
                case "DESIGNER":
                    result.designer = value;
                    break;
                case "DIFFICULTY":
                    result.difficulty = value;
                    break;
                case "PLAYLEVEL":
                    result.playlevel = value;
                    break;
                case "SONGID":
                    result.songid = value;
                    break;
                case "WAVE":
                    result.wave = value;
                    break;
                case "WAVEOFFSET":
                    result.waveoffset = Float.parseFloat(value);
                    break;
                case "JACKET":
                    result.jacket = value;
                    break;
                case "BACKGROUND":
                    result.background = value;
                    break;
                case "MOVIE":
                    result.movie = value;
                    break;
                case "MOVIEOFFSET":
                    result.movieoffset = Float.parseFloat(value);
                    break;
                case "BASEBPM":
                    result.basebpm = Float.parseFloat(value);
                    break;
                case "REQUEST":
                    int a = value.indexOf(" ");
                    result.requests.put(value.substring(0, a), value.substring(a + 1));
                    break;
            }
        }
        return result;
    }

    private static RawObject[] toRawObjects(int header, String data) {
        int measure = Integer.parseInt((header + "").substring(0, 3));
        String[] values = subStringByLength(data, 2);
        List<RawObject> tuples = new LinkedList<>();
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (!value.equals("00")) tuples.add(new RawObject(toTick(measure, i, values.length), value));
        }
        return tuples.toArray(new RawObject[0]);
    }


    private static int toTick(int measure, int i, int total) {

    }

    private static String[] subStringByLength(String str, int length) {
        List<String> strs = new LinkedList<>();
        int strLength = str.length();
        int max = (strLength - strLength % length) / length;
        for (int i = 0; i < max; i += length) {
            strs.add(str.substring(i, i + length));
        }
        return strs.toArray(new String[0]);
    }

    private static String customTrim(String src, String q) {
        while (src.startsWith(q)) {
            src = src.substring(q.length());
        }
        while (src.endsWith(q)) {
            src = src.substring(0, src.length() - q.length());
        }
        return src;
    }

    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-+]?\\d*$");
        return pattern.matcher(str).matches();
    }
}

class Metadata {
    public String title;
    public String subtitle;
    public String artist;
    public String genre;
    public String designer;
    public String difficulty;
    public String playlevel;
    public String songid;
    public String wave;
    public float waveoffset;
    public String jacket;
    public String background;
    public String movie;
    public float movieoffset;
    public float basebpm;
    public Map<String, String> requests = new HashMap<>();
}

class Bar {
    public int measure;
    public int beat;
    public int pos;

    public Bar(int measure, int beat, int pos) {
        this.measure = measure;
        this.beat = beat;
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "(" + measure + ", " + beat + ", " + pos + ")";
    }
}

class RawObject {
    public int i;
    public String value;

    public RawObject(int i, String value) {
        this.i = i;
        this.value = value;
    }
}