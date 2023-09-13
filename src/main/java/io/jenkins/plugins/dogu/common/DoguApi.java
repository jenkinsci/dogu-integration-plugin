package io.jenkins.plugins.dogu.common;

import io.jenkins.plugins.dogu.DoguOption;
import io.jenkins.plugins.dogu.api.DoguApiClient;
import io.jenkins.plugins.dogu.api.DoguApiResponse.RunRoutineResponse;
import io.jenkins.plugins.dogu.api.DoguWebSocketClient;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DoguApi {
    public static boolean uploadApplication(
            String applicationPath, String projectId, Boolean isLatest, DoguOption doguOption, PrintStream logger)
            throws Exception {
        Path filePath;
        byte[] fileContent;
        String mimeType;

        try {
            filePath = Paths.get(applicationPath);
            mimeType = Files.probeContentType(filePath);
            fileContent = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw e;
        }

        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new Exception("Error: File name is null");
        }

        try {
            DoguApiClient.uploadApplication(
                    projectId, fileContent, fileName.toString(), mimeType, isLatest, doguOption);
            logger.println("Application is uploaded to" + projectId);
        } catch (Exception e) {
            throw e;
        }

        return true;
    }

    public static boolean runRoutine(String projectId, String routineId, DoguOption doguOption, PrintStream logger)
            throws Exception {
        RunRoutineResponse routine;
        try {
            routine = DoguApiClient.runRoutine(projectId, routineId, doguOption);
            logger.println("Spawn pipeline, project-id: " + projectId + ", routine-id: " + routineId
                    + " routine-pipeline-id: " + routine.routinePipelineId);
        } catch (Exception e) {
            throw e;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Integer> future = executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                DoguWebSocketClient client;

                try {
                    client = DoguApiClient.connectRoutine(
                            logger, projectId, routineId, routine.routinePipelineId, doguOption);
                } catch (Exception e) {
                    return 1;
                }

                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        return 1;
                    }

                    try {
                        switch (client.state) {
                            case NONE:
                                break;
                            case SUCCESS:
                                logger.println("Success");
                                return 0;
                            case FAILURE:
                                logger.println("Failure");
                                return 1;
                        }

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        client.close(1006);
                        return 1;
                    }
                }
            }
        });

        int result;
        try {
            result = future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            throw new Exception("Cancelled");
        } catch (ExecutionException e) {
            throw e;
        } finally {
            executorService.shutdown();
        }

        return result == 0 ? true : false;
    }
}
