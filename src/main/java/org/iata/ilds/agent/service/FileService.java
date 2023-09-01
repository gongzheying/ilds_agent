package org.iata.ilds.agent.service;

import org.apache.commons.io.FilenameUtils;
import org.iata.ilds.agent.domain.entity.FileType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class FileService {

    String ROUTING_FILE_EXTENSION = ".orf";
    String TDF_FILE_EXTENSION = ".tdf";
    String HOTADD_FILE_EXTENSION = ".hotadd";
    String BINLIST_FILE_EXTENSION = ".csv";

    public Map<FileType, List<File>> fetchOutboundTransferFiles(String extractedFolder) {


        return Arrays.stream(Objects.requireNonNull(new File(extractedFolder).listFiles(File::isFile))).map(file -> {
            String fileName = file.getName();
            FileType fileType = FileType.Normal;
            if (FilenameUtils.isExtension(fileName, ROUTING_FILE_EXTENSION)) {
                fileType = FileType.Routing;
            } else if (FilenameUtils.isExtension(fileName, TDF_FILE_EXTENSION)) {
                fileType = FileType.TDF;
            } else if (FilenameUtils.isExtension(fileName, HOTADD_FILE_EXTENSION)) {
                fileType = FileType.HOTADD;
            } else if (fileName.contains("_BINList_") && FilenameUtils.isExtension(fileName, BINLIST_FILE_EXTENSION)) {
                fileType = FileType.BINLIST;
            }
            return Map.entry(fileType, file);
        }).collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    }

}
