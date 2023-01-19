import com.google.gson.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("参数不足，应该有两个参数（第一个是sus文件路径，第二个是txt/drb文件输出路径");
            return;
        }
        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("输入文件不存在");
            return;
        }
        String separator = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("#OFFSET=0;").append(separator);
        {
            String inputPath = inputFile.getAbsolutePath();
            int sssss = inputPath.lastIndexOf(".");
            JsonObject jsonObject = ReadSus2Json(inputPath, sssss < 0 ? inputPath + ".json" : inputPath.substring(0, sssss) + ".json");
            if (jsonObject == null) {
                System.err.println("转换失败，未知错误");
                return;
            }
            JsonArray requests = jsonObject.get("metadata").getAsJsonObject().get("requests").getAsJsonArray();
            if (requests.size() < 0) throw new IllegalArgumentException("No ticks_per_beat field found");
            int ticksPerBeat = -1;
            for (JsonElement jsonElement : requests) {
                String request = jsonElement.getAsString();
                if (request.startsWith("ticks_per_beat")) {
                    String[] strings = request.split(" ");
                    if (strings.length < 2)
                        throw new IllegalArgumentException("Illegal ticks_per_beat field: " + request);
                    try {
                        ticksPerBeat = Integer.parseInt(strings[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Illegal ticks_per_beat value: " + strings[1]);
                    }
                }
            }
            if (ticksPerBeat < 0) throw new IllegalArgumentException("No ticks_per_beat field found");
            sb.append("#BEAT=1;").append(separator);
            JsonArray bpms = jsonObject.get("bpms").getAsJsonArray();
            int bpmNumbers = bpms.size();
            sb.append("#BPM_NUMBER=").append(bpmNumbers).append(";").append(separator);
            for (int i = 0; i < bpmNumbers; i++) {
                JsonArray bpm = bpms.get(i).getAsJsonArray();
                sb.append("#BPM [").append(i).append("]=").append(bpm.get(1).getAsFloat()).append(";").append(separator)
                        .append("#BPMS[").append(i).append("]=").append(bpm.get(0).getAsInt() / 4f / ticksPerBeat).append(";").append(separator);
            }
            sb.append("#SCN=1;").append(separator).append("#SC [0]=1;").append(separator).append("#SCI[0]=0.000;").append(separator);
            int id = 0;
            JsonArray taps = jsonObject.get("taps").getAsJsonArray(),
                    directionals = jsonObject.get("directionals").getAsJsonArray(),
                    slides = jsonObject.get("slides").getAsJsonArray();
            NoteData lastNoteData = null;
            List<NoteData> tapPool = new LinkedList<>(), directionalPool = new LinkedList<>(), posIgnoreTapPool = new LinkedList<>();
            HashMap<Integer, NoteData> slideMap = new HashMap<>();
            HashMap<Integer, List<NoteData>> posIgnoreSlideMap = new HashMap<>();
            for (JsonElement jsonElement : taps) {
                JsonObject tap = jsonElement.getAsJsonObject();
                NoteData noteData = new NoteData();
                noteData.id = id;
                noteData.originalType = tap.get("type").getAsInt();
                noteData.type = tapTypeMap(noteData.originalType);
                if (noteData.type < 0) continue;
                noteData.tick = tap.get("tick").getAsInt();
                noteData.time = noteData.tick / 4f / ticksPerBeat;
                noteData.pjskPos = tap.get("lane").getAsInt();
                noteData.pjskWidth = tap.get("width").getAsInt();
                noteData.parent = 0;
                if (noteData.pjskPos < 2 || noteData.pjskPos > 14) continue;
                noteData.pos = pjskPos2Dr3Pos(noteData.pjskPos);
                noteData.width = pjskWidth2Dr3Width(noteData.pjskWidth);
                if (noteData.originalType != 3) tapPool.add(noteData);
                else posIgnoreTapPool.add(noteData);
                id++;
            }
            for (JsonElement jsonElement : directionals) {
                JsonObject directional = jsonElement.getAsJsonObject();
                NoteData noteData = new NoteData();
                noteData.id = id;
                noteData.originalType = directional.get("type").getAsInt();
                noteData.type = directionalsTypeMap(noteData.originalType);
                if (noteData.type < 0) continue;
                noteData.isValid = noteData.type > 0;
                noteData.tick = directional.get("tick").getAsInt();
                noteData.time = noteData.tick / 4f / ticksPerBeat;
                noteData.pjskPos = directional.get("lane").getAsInt();
                noteData.pjskWidth = directional.get("width").getAsInt();
                noteData.parent = 0;
                if (noteData.pjskPos < 2 || noteData.pjskPos > 14) continue;
                noteData.pos = pjskPos2Dr3Pos(noteData.pjskPos);
                noteData.width = pjskWidth2Dr3Width(noteData.pjskWidth);
                id++;
                directionalPool.add(noteData);
                if (noteData.isValid && (noteData.originalType == 3 || noteData.originalType == 4)) {
                    NoteData noteData1 = new NoteData();
                    noteData1.id = id;
                    noteData1.type = noteData.originalType == 3 ? 13 : 14;
                    noteData1.tick = noteData.tick;
                    noteData1.time = noteData.time;
                    noteData1.pjskPos = noteData.pjskPos;
                    noteData1.pjskWidth = noteData.pjskWidth;
                    noteData1.pos = noteData.pos;
                    noteData1.width = noteData.width;
                    noteData1.parent = 0;
                    id++;
                    directionalPool.add(noteData1);
                }
            }
            for (JsonElement jsonElement : slides) {
                JsonArray slide = jsonElement.getAsJsonArray();
                List<NoteData> slidePointPool = new LinkedList<>();
                boolean isYellow = false;
                for (JsonElement jsonElement1 : slide) {
                    JsonObject slidePoint = jsonElement1.getAsJsonObject();
                    NoteData noteData = new NoteData();
                    noteData.id = id;
                    noteData.originalType = slidePoint.get("type").getAsInt();
                    if (noteData.type < 0) continue;
                    noteData.tick = slidePoint.get("tick").getAsInt();
                    noteData.time = noteData.tick / 4f / ticksPerBeat;
                    noteData.pjskPos = slidePoint.get("lane").getAsInt();
                    noteData.pjskWidth = slidePoint.get("width").getAsInt();
                    if (noteData.pjskPos < 2 || noteData.pjskPos > 14) continue;
                    if (noteData.originalType == 1 || noteData.originalType == 2 || noteData.originalType == 3 || noteData.originalType == 5) {
                        for (int i = 0; i < tapPool.size(); i++) {
                            NoteData tapNoteData = tapPool.get(i);
                            if (tapNoteData.pjskWidth == noteData.pjskWidth && tapNoteData.pjskPos == noteData.pjskPos && tapNoteData.tick == noteData.tick) {
                                if (noteData.originalType == 1) {
                                    isYellow = tapNoteData.originalType == 2;
                                }
                                tapPool.remove(tapNoteData);
                            }
                        }
                        if (noteData.originalType == 1 || noteData.originalType == 3 || noteData.originalType == 5) {
                            for (int i = 0; i < directionalPool.size(); i++) {
                                NoteData directionalNoteData = directionalPool.get(i);
                                if (directionalNoteData.pjskWidth == noteData.pjskWidth && directionalNoteData.pjskPos == noteData.pjskPos && directionalNoteData.tick == noteData.tick) {
                                    switch (directionalNoteData.originalType) {
                                        case 2:
                                            noteData.easeType = EaseType.SINE_IN;
                                            break;
                                        case 5:
                                        case 6:
                                            noteData.easeType = EaseType.SINE_OUT;
                                            break;
                                        default:
                                            noteData.easeType = EaseType.LINEAR;
                                            break;
                                    }
                                    directionalPool.remove(directionalNoteData);
                                }
                            }
                        }
                    }
                    noteData.parent = noteData.originalType == 1 ? 0 : (lastNoteData == null ? -1 : lastNoteData.id);
                    if (noteData.originalType == 3) {
                        boolean posIgnore = false;
                        for (int i = 0; i < posIgnoreTapPool.size(); i++) {
                            NoteData posIgnoreTapNoteData = posIgnoreTapPool.get(i);
                            if (posIgnoreTapNoteData.pjskWidth == noteData.pjskWidth && posIgnoreTapNoteData.pjskPos == noteData.pjskPos && posIgnoreTapNoteData.tick == noteData.tick) {
                                posIgnore = true;
                                posIgnoreTapPool.remove(posIgnoreTapNoteData);
                                break;
                            }
                        }
                        if (posIgnore) {
                            List<NoteData> fuck;
                            if (posIgnoreSlideMap.containsKey(noteData.parent)) {
                                fuck = posIgnoreSlideMap.get(noteData.parent);
                                fuck.add(noteData);
                                posIgnoreSlideMap.replace(noteData.parent, fuck);
                            } else {
                                fuck = new LinkedList<>();
                                fuck.add(noteData);
                                posIgnoreSlideMap.put(noteData.parent, fuck);
                            }
                        } else {
                            lastNoteData = noteData;
                            if (noteData.parent < 0) throw new RuntimeException();
                            noteData.pos = pjskPos2Dr3Pos(noteData.pjskPos);
                            noteData.width = pjskWidth2Dr3Width(noteData.pjskWidth);
                            slidePointPool.add(noteData);
                        }
                    } else {
                        lastNoteData = noteData;
                        if (noteData.parent < 0) throw new RuntimeException();
                        noteData.pos = pjskPos2Dr3Pos(noteData.pjskPos);
                        noteData.width = pjskWidth2Dr3Width(noteData.pjskWidth);
                        slidePointPool.add(noteData);
                    }
                    id++;
                }
                for (NoteData noteData : slidePointPool) {
                    noteData.type = slideTypeMap(noteData.originalType, isYellow);
                    slideMap.put(noteData.id, noteData);
                }
            }
            for (NoteData noteData : tapPool) {
                sb.append(noteData).append(separator);
            }
            for (NoteData noteData : directionalPool) {
                if (noteData.isValid) sb.append(noteData).append(separator);
            }
            for (NoteData noteData : slideMap.values()) {
                if (!noteData.isStart()) {
                    NoteData parentNoteData = slideMap.get(noteData.parent);
                    if (parentNoteData == null) continue;
                    if (!noteData.isEnd() && posIgnoreSlideMap.containsKey(parentNoteData.id)) {
                        List<NoteData> list = posIgnoreSlideMap.get(parentNoteData.id);
                        int newParent = parentNoteData.id;
                        for (NoteData posIgnoreSlide : list) {
                            posIgnoreSlide.type = parentNoteData.type == 1 || parentNoteData.type == 6 || parentNoteData.type == 7 ? 7 : 4;
                            posIgnoreSlide.pos = pjskPos2Dr3Pos(lerp(parentNoteData.pjskPos, noteData.pjskPos, (noteData.tick - parentNoteData.tick) * 1f / (noteData.tick - parentNoteData.tick), parentNoteData.easeType));
                            if (parentNoteData.pjskWidth == noteData.pjskWidth) {
                                posIgnoreSlide.width = parentNoteData.width;
                            } else {
                                int parentRight = parentNoteData.pjskPos + parentNoteData.pjskWidth;
                                int noteRight = noteData.pjskPos + noteData.pjskWidth;
                                posIgnoreSlide.width = pjskPos2Dr3Pos(lerp(parentRight, noteRight, (noteData.tick - parentNoteData.tick) * 1f / (noteData.tick - parentNoteData.tick), parentNoteData.easeType)) - posIgnoreSlide.pos;
                            }
                            newParent = posIgnoreSlide.id;
                            sb.append(posIgnoreSlide).append(separator);
                        }
                        noteData.parent = newParent;
                    }
                    int lerpJingdu = Math.round((noteData.tick - parentNoteData.tick) * 8f / ticksPerBeat);
                    if (lerpJingdu < 2) lerpJingdu = 2;
                    int newParent = noteData.parent;
                    if (parentNoteData.pjskWidth != noteData.pjskWidth || parentNoteData.pjskPos != noteData.pjskPos) {
                        for (int i = 1; i < lerpJingdu; i++) {
                            NoteData lerpedNoteData = new NoteData();
                            lerpedNoteData.id = id;
                            lerpedNoteData.type = parentNoteData.type == 1 || parentNoteData.type == 6 || parentNoteData.type == 7 ? 6 : 11;
                            lerpedNoteData.time = i * (noteData.time - parentNoteData.time) / lerpJingdu + parentNoteData.time;
                            if (parentNoteData.pjskPos == noteData.pjskPos) {
                                lerpedNoteData.pos = parentNoteData.pos;
                            } else {
                                lerpedNoteData.pos = pjskPos2Dr3Pos(lerp(parentNoteData.pjskPos, noteData.pjskPos, i * 1f / lerpJingdu, parentNoteData.easeType));
                            }
                            if (parentNoteData.pjskWidth == noteData.pjskWidth) {
                                lerpedNoteData.width = parentNoteData.width;
                            } else {
                                int parentRight = parentNoteData.pjskPos + parentNoteData.pjskWidth;
                                int noteRight = noteData.pjskPos + noteData.pjskWidth;
                                lerpedNoteData.width = pjskPos2Dr3Pos(lerp(parentRight, noteRight, i * 1f / lerpJingdu, parentNoteData.easeType)) - lerpedNoteData.pos;
                            }
                            lerpedNoteData.parent = newParent;
                            newParent = lerpedNoteData.id;
                            id++;
                            sb.append(lerpedNoteData).append(separator);
                        }
                    }
                    noteData.parent = newParent;
                }
                sb.append(noteData).append(separator);
            }
            try (FileWriter fileWriter = new FileWriter(args[1], false)) {
                fileWriter.write(sb.toString());
                System.out.println("转换成功");
            }
        }
    }

    public static JsonObject ReadSus2Json(String susPath, String jsonPath) throws IOException {
        try {
            String[] args1 = {"python", ".\\sus-io\\process.py", susPath, jsonPath};
            Process proc = Runtime.getRuntime().exec(args1);

            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();
            proc.waitFor();
            File inputFile = new File(jsonPath);
            Gson gson = new Gson();
            try (FileReader fileReader = new FileReader(inputFile)) {
                JsonObject result = gson.fromJson(fileReader, JsonObject.class);
                inputFile.delete();
                return result;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static float lerp(int a, int b, float p, EaseType easeType) {
        switch (easeType) {
            case LINEAR:
                return p * (b - a) + a;
            case SINE_IN:
                return (float) (1 - Math.cos(p * Math.PI / 2)) * (b - a) + a;
            case SINE_OUT:
                return (float) Math.sin(p * Math.PI / 2) * (b - a) + a;
            default:
                throw new RuntimeException();
        }
    }

    private static float pjskPos2Dr3Pos(float pos) {
        return (pos - 2f) * 4f / 3f;
    }

    private static float pjskWidth2Dr3Width(float width) {
        return width * 4f / 3f;
    }

    private static int slideTypeMap(int type, boolean isYellow) {
        switch (type) {
            case 1:
                return isYellow ? 2 : 1;
            case 2:
            case 3:
                return isYellow ? 4 : 7;
            case 5:
                return isYellow ? 11 : 6;
            default:
                return -1;
        }
    }

    private static int directionalsTypeMap(int type) {
        switch (type) {
            case 1:
            case 3:
            case 4:
                return 15;
            case 2:
            case 5:
            case 6:
                return 16;
            default:
                return -1;
        }
    }

    private static int tapTypeMap(int type) {
        switch (type) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 0;
            default:
                return -1;
        }
    }
}

class NoteData {
    public int id;
    public int originalType;
    public int type;
    public int tick;
    public int pjskPos;
    public int pjskWidth;
    public float time;
    public float pos;
    public float width;
    public int parent;
    public boolean isValid = true;
    public EaseType easeType = EaseType.LINEAR;

    @Override
    public String toString() {
        return "<" + id + "><" + type + "><" + timeFormat.format(time) + "><" + regularFormat.format(pos) + "><" + regularFormat.format(width) + "><" + (isStart() ? 1 : 0) + "><" + (isStart() ? 0 : parent) + ">";
    }

    boolean isStart() {
        return type == 1 || type == 2 || type == 3 || type == 5 || type == 8 || type == 9 || type == 10 || type == 13 || type == 14 || type == 15 || type == 16;
    }

    boolean isEnd() {
        return type == 4 || type == 7 || type == 18 || type == 20 || type == 22 || type == 24;
    }

    static DecimalFormat timeFormat = new DecimalFormat("0.00000"),
            regularFormat = new DecimalFormat("0.##");
}

enum EaseType {
    LINEAR,
    SINE_IN,
    SINE_OUT
}