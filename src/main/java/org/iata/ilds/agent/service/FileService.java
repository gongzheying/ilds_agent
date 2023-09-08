package org.iata.ilds.agent.service;

import org.iata.ilds.agent.domain.entity.FileType;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FileService {
    Map<FileType, List<File>> fetchOutboundTransferFiles(String extractedFolder);
}
