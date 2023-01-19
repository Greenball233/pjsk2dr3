//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonPrimitive;
//
//import java.io.*;
//import java.io.FileReader;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class Sus2Json {
//    public static void readSus(String path) {
//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
//            String line;
//            List<ScoreDatum> scoreData = new LinkedList<>();
//            List<MetaDatum> metaData = new LinkedList<>();
//            while ((line = bufferedReader.readLine()) != null) {
//                if (!line.startsWith("#")) continue;
//                Pattern pattern = Pattern.compile("^#(\\w+):\\s*(.*)$");
//                Matcher matcher = pattern.matcher(line);
//                if (matcher.find()) {
//                    line = line.substring(1);
//                    int index = line.indexOf(":");
//                    String header = line.substring(0, index);
//                    String data = line.substring(index + 1);
//                    while (data.substring(0, 1).trim().equals("")) {
//                        data = data.substring(1);
//                    }
//                    scoreData.add(new ScoreDatum(header, data));
//                } else {
//                    line = line.substring(1);
//                    int index = line.indexOf(" ");
//                    String header = line.substring(0, index);
//                    String data = line.substring(index + 1);
//                    while (data.substring(0, 1).trim().equals("")) {
//                        data = data.substring(1);
//                    }
//                    metaData.add(new MetaDatum(header, data));
//                }
//            }
//            for (ScoreDatum scoreDatum : scoreData) {
//                System.out.println(scoreDatum.toString());
//            }
//            for (MetaDatum metaDatum : metaData) {
//                System.out.println(metaDatum.toString());
//            }
//            processData(scoreData.toArray(new ScoreDatum[0]), metaData.toArray(new MetaDatum[0]));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void processData(ScoreDatum[] scoreData, MetaDatum[] metaData) {
//        JsonObject processedMetaData = processMedaData(metaData);
//    }
//
//    private static JsonObject processMedaData(MetaDatum[] metaData) {
//        JsonObject jsonObject = new JsonObject();
//        JsonArray requests = new JsonArray();
//        String data;
//        for (MetaDatum metaDatum : metaData) {
//            switch (metaDatum.header) {
//                case "TITLE":
//                    data = metaDatum.data;
//                    jsonObject.add("title", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "SUBTITLE":
//                    data = metaDatum.data;
//                    jsonObject.add("subtitle", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "ARTIST":
//                    data = metaDatum.data;
//                    jsonObject.add("artist", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "GENRE":
//                    data = metaDatum.data;
//                    jsonObject.add("genre", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "DESIGNER":
//                    data = metaDatum.data;
//                    jsonObject.add("designer", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "DIFFICULTY":
//                    data = metaDatum.data;
//                    jsonObject.add("difficulty", new JsonPrimitive(Integer.parseInt(data)));
//                    break;
//                case "PLAYLEVEL":
//                    data = metaDatum.data;
//                    jsonObject.add("playlevel", new JsonPrimitive(Integer.parseInt(data)));
//                    break;
//                case "SONGID":
//                    data = metaDatum.data;
//                    jsonObject.add("songid", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "WAVE":
//                    data = metaDatum.data;
//                    jsonObject.add("wave", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "WAVEOFFSET":
//                    data = metaDatum.data;
//                    jsonObject.add("waveoffset", new JsonPrimitive(Integer.parseInt(data)));
//                    break;
//                case "JACKET":
//                    data = metaDatum.data;
//                    jsonObject.add("jacket", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "BACKGROUND":
//                    data = metaDatum.data;
//                    jsonObject.add("background", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "MOVIE":
//                    data = metaDatum.data;
//                    jsonObject.add("movie", new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                case "MOVIEOFFSET":
//                    data = metaDatum.data;
//                    jsonObject.add("movieoffset", new JsonPrimitive(Integer.parseInt(data)));
//                    break;
//                case "BASEBPM":
//                    data = metaDatum.data;
//                    jsonObject.add("basebpm", new JsonPrimitive(Float.parseFloat(data)));
//                    break;
//                case "REQUEST":
//                    data = metaDatum.data;
//                    requests.add(new JsonPrimitive(data.substring(1, data.length() - 1)));
//                    break;
//                default:
//                    System.out.println("Warning: Unknown Header '" + metaDatum.header + "', It will be ignored.");
//            }
//        }
//        if (requests.size() > 0) jsonObject.add("requests", requests);
//        return jsonObject;
//    }
//
//    public static void main(String[] args) {
//        readSus("H:\\Users\\Administrator\\WebstormProjects\\sus-io\\whatsuppop.master.sus");
//    }
//}
//
//class Datum {
//    public String header;
//    public String data;
//
//    public Datum(String header, String data) {
//        this.header = header;
//        this.data = data;
//    }
//}
//
//class ScoreDatum extends Datum {
//    public ScoreDatum(String header, String data) {
//        super(header, data);
//    }
//
//    @Override
//    public String toString() {
//        return "#" + header + ": " + data;
//    }
//}
//
//class MetaDatum extends Datum {
//    public MetaDatum(String header, String data) {
//        super(header, data);
//    }
//
//    @Override
//    public String toString() {
//        return "#" + header + " " + data;
//    }
//}
//
