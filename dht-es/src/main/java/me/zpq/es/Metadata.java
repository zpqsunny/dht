package me.zpq.es;

import lombok.Data;

import java.util.List;

@Data
public class Metadata {

    private String id;

    private String hash;

    private String name;

    private Integer pieceLength;

    private Long createdDateTime;

    private Long length;

    private List<File> files;

    private Long size;

    private Integer fileNumber;

    @Data
    public static class File {

        private Long length;

        private List<String> path;
    }
}
