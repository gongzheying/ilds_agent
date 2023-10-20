package org.iata.ilds.agent.service;

import lombok.Getter;
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
public class FileServiceImpl implements FileService {

    @Getter
    public enum TRANSFER_FILE_EXTENSION {

        ROUTING("orf"),
        TDF("tdf"),
        HOTADD("hotadd"),
        BINLIST("csv");
        private final String extension;
        TRANSFER_FILE_EXTENSION(String extension) {
            this.extension = extension;
        }

    }



    @Override
    public Map<FileType, List<File>> fetchOutboundTransferFiles(String extractedFolder) {


        return Arrays.stream(Objects.requireNonNull(new File(extractedFolder).listFiles(File::isFile))).map(file -> {
            String fileName = file.getName();
            FileType fileType = FileType.Normal;
            if (FilenameUtils.isExtension(fileName, TRANSFER_FILE_EXTENSION.ROUTING.getExtension())) {
                fileType = FileType.Routing;
            } else if (FilenameUtils.isExtension(fileName, TRANSFER_FILE_EXTENSION.TDF.getExtension())) {
                fileType = FileType.TDF;
            } else if (FilenameUtils.isExtension(fileName, TRANSFER_FILE_EXTENSION.HOTADD.getExtension())) {
                fileType = FileType.HOTADD;
            } else if (fileName.contains("_BINList_") && FilenameUtils.isExtension(fileName, TRANSFER_FILE_EXTENSION.BINLIST.getExtension())) {
                fileType = FileType.BINLIST;
            }
            return Map.entry(fileType, file);
        }).collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    }

}
