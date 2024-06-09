package me.zpq.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Metadata {

    private String id;

    private String hash;

    private String name;

    private Integer pieceLength;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdDateTime;

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
