package me.zpq.server.peer;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonMetaInfoImpl implements BaseMetaInfo {

    @Override
    public void run(byte[] info) {

        try {
            Map<String, Object> metaInfo = new HashMap<>(6);
            BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(info));
            String name = decode.getMap().get("name").getString();
            if (decode.getMap().get("name.utf-8") != null) {

                // 存在uft-8扩展
                name = decode.getMap().get("name.utf-8").getString();
            }
            metaInfo.put("name", name);
            metaInfo.put("piece length", decode.getMap().get("piece length").getInt());
            if (decode.getMap().get("length") != null) {

                // single-file mode
                metaInfo.put("length", decode.getMap().get("length").getLong());
            } else {

                // multi-file mode
                ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
                List<BEncodedValue> files = decode.getMap().get("files").getList();
                for (BEncodedValue file : files) {

                    Map<String, Object> f = new HashMap<>(6);
                    f.put("length", file.getMap().get("length").getLong());
                    ArrayList<String> path = new ArrayList<>();
                    List<BEncodedValue> paths = file.getMap().get("path").getList();
                    if (file.getMap().get("path.utf-8") != null) {

                        // 存在uft-8扩展
                        paths = file.getMap().get("path.utf-8").getList();
                    }
                    for (BEncodedValue p : paths) {

                        path.add(p.getString());
                    }
                    f.put("path", path);
                    arrayList.add(f);
                }

                metaInfo.put("files", arrayList);
            }
            JSONObject jsonObject = new JSONObject(metaInfo);
            System.out.println(jsonObject);
        } catch (Exception e) {

            log.error("print json error", e);
        }
    }

    @Override
    public boolean checkContinue(byte[] hash) {
        return true;
    }
}
