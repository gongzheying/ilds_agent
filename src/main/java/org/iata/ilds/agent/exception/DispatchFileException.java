package org.iata.ilds.agent.exception;

import lombok.Getter;

import java.io.File;

@Getter
public class DispatchFileException extends RuntimeException {

    private File failedFile;

    public DispatchFileException(File failedFile, Throwable cause) {
        super(cause);
        this.failedFile = failedFile;
    }
}
