package me.zpq.peer;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMetaInfo {

    private static final String NAME = "name";

    private static final String NAME_UTF8 = "name.utf-8";

    private static final String PIECE_LENGTH = "piece length";

    private static final String FILES = "files";

    private static final String LENGTH = "length";

    private static final String PATH = "path";

    private static final String PATH_UTF8 = "path.utf-8";

    public static String show(byte[] info) throws Exception {

        Map<String, Object> metaInfo = new HashMap<>(6);
        BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(info));
        String name = decode.getMap().get(NAME).getString();
        if (decode.getMap().get(NAME_UTF8) != null) {

            // 存在uft-8扩展
            name = decode.getMap().get(NAME_UTF8).getString();
        }
        metaInfo.put(NAME, name);
        metaInfo.put(PIECE_LENGTH, decode.getMap().get(PIECE_LENGTH).getInt());
        if (decode.getMap().get(LENGTH) != null) {

            // single-file mode
            metaInfo.put(LENGTH, decode.getMap().get(LENGTH).getLong());
        } else {

            // multi-file mode
            ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
            List<BEncodedValue> files = decode.getMap().get(FILES).getList();
            for (BEncodedValue file : files) {

                Map<String, Object> f = new HashMap<>(6);
                f.put(LENGTH, file.getMap().get(LENGTH).getLong());
                ArrayList<String> path = new ArrayList<>();
                List<BEncodedValue> paths = file.getMap().get(PATH).getList();
                if (file.getMap().get(PATH_UTF8) != null) {

                    // 存在uft-8扩展
                    paths = file.getMap().get(PATH_UTF8).getList();
                }
                for (BEncodedValue p : paths) {

                    path.add(p.getString());
                }
                f.put(PATH, path);
                arrayList.add(f);
            }

            metaInfo.put(FILES, arrayList);
        }
        return new JSONObject(metaInfo).toString();
    }
}
